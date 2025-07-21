import atexit
import requests
import socket
import pika
import json
import os
import sys
from abc import ABC, abstractmethod
from logging import Handler, LogRecord
from collections import deque
from threading import Thread, Event
from datetime import datetime
from .utils import recordToDict


class SciServerBaseHandler(Handler, ABC):
    """Base handler for use with SciServer logging, adds information to log prior to emitting record and records failure
    counts and exception information without raising exceptions in main program.

    This handler is abstract, and the emitlog method must implement the actual logging of a record.

    """

    def __init__(self, application: str = 'unkown-app', hostname: str = ''):
        self.app = application
        self.hostname = hostname if hostname else socket.gethostname()
        self.fail_count = 0
        self.last_exception = None
        super().__init__()

    def _handle_exception(self, e):
        self.fail_count += 1
        self.last_exception = e
        sys.stderr.write('SciServer logging emit exception: ' + str(e) + '\n')

    def form_record(self, record: LogRecord):
        extra = {
            'Application': self.app,
            'Host': self.hostname,
            'Time': datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%S.%fZ'),
            'MessageType': record.__dict__.get('MessageType', 'LOGRECORD'),
        }
        for k, v in extra.items():
            if k not in record.__dict__:
                record.__dict__[k] = v
        exc = sys.exc_info()
        if not record.exc_info and exc[0] is not None:
            record.exc_info = exc

    def emit(self, record):
        self.form_record(record)
        try:
            self.emitlog(record)
        except NotImplementedError as e:
            raise e
        except Exception as e:
            self._handle_exception(e)

    @abstractmethod
    def emitlog(self, record: LogRecord):
        raise NotImplementedError()


class SciServerBufferingAsyncHandler(SciServerBaseHandler):
    """
    Buffering asynchronous handler base class. This handler stores messages into a rotating queue and periodically calls
    the flush method either when a number of messages have been buffered, after a time interval, or at the time of
    program exit. The flush method is called in a separate thread.

    This class is abstract, the flush method must be implemented. It should read messages from the self.queue by
    successively calling pop() on it and handling messages as required. The queue is a double ended queue, where new
    messages are added to the left hand side, older messages removed from the right. The queue can continue to fill
    during the flush call, it is up to the base class implementation to determine the number of messages to flush in
    each call.

    the max items in the queue is computed by flush_after_num * queue_max_factor (in order to give flush time and handle
    some burstiness)

    """

    def __init__(self, flush_after_num: int = 100, flush_after_secs: int = 20, queue_max_factor: int = 2, **kwargs):
        self.queue: deque = deque(maxlen=max(int(flush_after_num * queue_max_factor), flush_after_num))
        self.flush_after_num = flush_after_num
        self.flush_after_secs = flush_after_secs
        self.notifier = Event()
        self.exitflag = False
        self.watcher = None
        # last ditch effort to transmit remaining logs, this should be called after the thread is terminated
        atexit.register(self.flush)
        super().__init__(**kwargs)

    def _set_exit(self):
        self.exitflag = True
        self.notifier.set()

    def emitlog(self, record):
        if self.watcher is None:
            self.watcher = Thread(target=self.watch, daemon=True)
            self.watcher.start()
        self.queue.appendleft(record)
        if len(self.queue) == self.flush_after_num:
            self.notifier.set()

    def watch(self):
        while True:
            self.notifier.wait(self.flush_after_secs)
            self.notifier.clear()
            try:
                self.flush()
            except Exception as e:
                self._handle_exception(e)
            if self.exitflag:
                return

    @abstractmethod
    def flush(self):
        raise NotImplementedError()


class SciServerRmqHandler(SciServerBufferingAsyncHandler):

    def __init__(self, rmq_endpoint: str, rmq_creds: str, exchange: str, **kwargs):
        self.host = rmq_endpoint.split(':')[0]
        self.port = pika.connection.ConnectionParameters._DEFAULT
        if ':' in rmq_endpoint:
            self.port = int(rmq_endpoint.split(':')[1])
        self.user = None
        if rmq_creds:
            self.user, self.pwd = rmq_creds.split(':', 1)
        self.exchange = exchange
        self.chan = None
        super().__init__(**kwargs)

    def _connect(self):
        creds = pika.connection.ConnectionParameters._DEFAULT
        if self.user is not None:
            creds = pika.credentials.PlainCredentials(self.user, self.pwd)
        self.conn = pika.BlockingConnection(
            pika.ConnectionParameters(
                heartbeat=300,
                blocked_connection_timeout=300,
                host=self.host,
                port=self.port,
                credentials=creds
            )
        )
        self.chan = self.conn.channel()
        self.chan.exchange_declare(self.exchange, exchange_type='topic')

    def flush(self):
        if self.chan is None:
            # if we have never started logging, don't make connection
            if len(self.queue) == 0:
                return
            self._connect()
        elif not self.conn.is_open:
            self._connect()
        # ensure we keep the connection open while logging
        if len(self.queue) == 0:
            self.conn.process_data_events()
        for i in range(len(self.queue)):
            record = self.queue.pop()
            rec = recordToDict(record)
            self.chan.basic_publish(self.exchange, '', json.dumps(rec).encode('utf-8'))


class SciServerRmqHandlerFromEnviron(SciServerRmqHandler):

    def __init__(self):
        super().__init__(
            rmq_endpoint=os.environ['SCISERVER_LOGGING_RMQ_EP'],
            rmq_creds=os.environ['SCISERVER_LOGGING_RMQ_CREDS'],
            exchange=os.environ['SCISERVER_LOGGING_RMQ_EXCHANGE'],
            application=os.getenv('SCISERVER_LOGGING_APPLICATION'),
        )


class SlackWebhookHandler(SciServerBaseHandler):

    def __init__(self, webhook_url: str, **kwargs):
        self.webhook_url = webhook_url
        super().__init__(**kwargs)

    def emitlog(self, record):
        emoji = ':scream:' if record.levelname in ['ERROR', 'WARNING'] else ':v:'
        text = self.format(record)
        if len(text) > 1000:
            text = text[:1000] + '<<<truncated>>>'
        msg = {
            'text': f'{record.levelname} from {record.Application}',
            'blocks': [
                {
                    'type': 'section',
                    'text': {
                        'type': 'mrkdwn',
                        'text': f'*{record.levelname}* from {record.Application} {emoji}',
                    }
                },
                {
                    'type': 'divider',
                },
                {
                    'type': 'section',
                    'text': {
                        'type': 'mrkdwn',
                        'text': f'```{text}```',
                    }
                }
            ]
        }
        resp = requests.post(self.webhook_url, json=msg)
        if resp.status_code != 200:
            sys.stderr.write('SciServer logging ERROR in slack message post:\n')
            sys.stderr.write('    ' + resp.text + '\n')
            resp.raise_for_status()


class SlackWebhookHandlerFromEnviron(SlackWebhookHandler):

    def __init__(self):
        super().__init__(
            webhook_url=os.environ['SCISERVER_LOGGING_SLACK_WEBHOOK'],
            application=os.getenv('SCISERVER_LOGGING_APPLICATION'),
        )
