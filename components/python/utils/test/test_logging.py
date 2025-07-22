import atexit
import os
import time
import logging
import sciserverutils.logging
import pika
from unittest import TestCase
from unittest.mock import MagicMock, patch
from sciserverutils.logging import handlers
from sciserverutils.logging.utils import recordToDict
from logging import LogRecord


def async_handler_exit_and_join(h):
    h._set_exit()
    if h.watcher:
        h.watcher.join()

class CaptureHandler(handlers.SciServerBaseHandler):
    def emitlog(self, rec):
        self.rec = rec


class TestHandlers(TestCase):

    def setUp(self):
        self.pikamock = MagicMock()
        handlers.pika = self.pikamock
        self.pikaconmock = MagicMock(spec=pika.BlockingConnection)
        self.pikamock.BlockingConnection.return_value = self.pikaconmock
        self.requestsmock = MagicMock()
        handlers.requests = self.requestsmock
        self.dummy_lr = LogRecord('name', 0, 'path', 0, 'msg', None, None)
        self.captureLogger = logging.getLogger('captureHandlerLogger')
        self.captureHandler = CaptureHandler()
        self.captureLogger.addHandler(self.captureHandler)

    def test_base_instantiate_no_args(self):
        with self.assertRaises(TypeError):
            bh = handlers.SciServerBaseHandler()

    def test_handler_adds_info(self):
        class tester_handler(handlers.SciServerBaseHandler):
            def emitlog(self):
                pass
        th = tester_handler()
        assert(th.app is not None)
        assert(th.hostname is not None)

    def test_rmq_handler_emit_connects_and_publishes(self):
        rmqh = handlers.SciServerRmqHandler('ep:1', 'user:pass', 'exch')
        rmqh.emit(self.dummy_lr)
        rmqh.flush()
        self.pikamock.BlockingConnection.assert_called_once()
        self.pikamock.BlockingConnection().channel().exchange_declare \
                                                  .assert_called_once_with('exch', exchange_type='topic')
        self.pikamock.BlockingConnection().channel().basic_publish.assert_called_once()

    def test_rmq_handler_triggers_publish_automatically(self):
        rmqh = handlers.SciServerRmqHandler('ep:1', 'user:pass', 'exch', flush_after_num=10)
        for i in range(10):
            rmqh.emit(self.dummy_lr)
        # give a moment to do async tasks
        time.sleep(0.1)
        # set exit flag and join thread to ensure completion
        async_handler_exit_and_join(rmqh)
        self.pikamock.BlockingConnection.assert_called()
        self.pikamock.BlockingConnection().channel().exchange_declare \
                                                  .assert_called_once_with('exch', exchange_type='topic')
        self.assertEquals(self.pikamock.BlockingConnection().channel().basic_publish.call_count, 10)

    def test_rmq_handler_multiple_publish_connects_only_once(self):
        rmqh = handlers.SciServerRmqHandler('ep:1', 'user:pass', 'exch')
        rmqh.emit(self.dummy_lr)
        rmqh.emit(self.dummy_lr)
        self.assertEquals(len(rmqh.queue), 2)
        rmqh.flush()
        self.pikamock.BlockingConnection.assert_called_once()
        self.assertEquals(len(rmqh.queue), 0)

    def test_rmq_handler_publish_failure_tries_reconnect_without_exception(self):
        rmqh = handlers.SciServerRmqHandler('ep:1', 'user:pass', 'exch')
        self.pikamock.BlockingConnection.return_value.channel.return_value.basic_publish.side_effect = Exception
        self.pikamock.BlockingConnection.return_value.is_open = False
        rmqh.emit(self.dummy_lr)
        try:
            rmqh.flush()
        except:
            pass
        rmqh.emit(self.dummy_lr)
        try:
            rmqh.flush()
        except:
            pass
        with self.assertRaises(AssertionError):
            self.pikamock.BlockingConnection.assert_called_once()
        self.pikamock.BlockingConnection.assert_called()

    def test_rmq_handler_operates_connection_when_idle(self):
        rmqh = handlers.SciServerRmqHandler('ep:1', 'u:p', 'exch')
        # trigger conection
        rmqh.emit(self.dummy_lr)
        rmqh.flush()
        self.pikaconmock.reset_mock()
        # trigger another flush, there are no messages, so the process events is called
        rmqh.flush()
        self.pikaconmock.process_data_events.assert_called()


    @patch.dict(os.environ, {
        'SCISERVER_LOGGING_RMQ_EP': 'ep:1',
        'SCISERVER_LOGGING_RMQ_CREDS': 'user:pass',
        'SCISERVER_LOGGING_RMQ_EXCHANGE': 'exch'})
    def test_rmq_initializaion_from_environ(self):
        rmqh = handlers.SciServerRmqHandlerFromEnviron()
        assert(rmqh.exchange == 'exch')
        assert(rmqh.host == 'ep')
        assert(rmqh.port == 1)
        assert(rmqh.user == 'user')
        assert(rmqh.pwd == 'pass')

    def test_slack_handler_requests_to_url_on_emit(self):
        self.requestsmock.post.return_value.status_code = 200
        slack = handlers.SlackWebhookHandler('url')
        slack.emit(self.dummy_lr)
        print(self.requestsmock.mock_calls)
        assert(self.requestsmock.post.call_args.args[0] == 'url')

    @patch.dict(os.environ, {'SCISERVER_LOGGING_SLACK_WEBHOOK': 'url'})
    def test_slack_initializaion_from_environ(self):
        slack = handlers.SlackWebhookHandlerFromEnviron()
        assert(slack.webhook_url == 'url')

    def test_handler_fail_count_increment_on_exception(self):
        rmqh = handlers.SciServerRmqHandler('ep:1', 'user:pass', 'exch')
        self.pikamock.BlockingConnection.return_value.channel.return_value.basic_publish.side_effect = Exception
        assert(rmqh.fail_count == 0)
        rmqh.emit(self.dummy_lr)
        async_handler_exit_and_join(rmqh)
        assert(rmqh.fail_count == 1)

    def test_async_handler_flushes_on_cleanup(self):
        class tester(handlers.SciServerBufferingAsyncHandler):
            def flush(self):
                self.value = 99
        testerobj = tester()
        testerobj.emit(self.dummy_lr)
        with self.assertRaises(AttributeError):
            testerobj.value == 99
        atexit._run_exitfuncs()
        assert(testerobj.value == 99)

    @patch.dict(os.environ, {
        'SCISERVER_LOGGING_RMQ_EP': 'ep:1',
        'SCISERVER_LOGGING_RMQ_CREDS': 'user:pass',
        'SCISERVER_LOGGING_RMQ_EXCHANGE': 'exch',
        'SCISERVER_LOGGING_SLACK_WEBHOOK': 'url'})
    def test_logging_standard_config_uses_environment_config(self):
        sciserverutils.logging.configure()

    @patch.dict(os.environ, {'SCISERVER_LOGGING_RMQ_EP': 'ep:1'})
    def test_logging_standard_config_raises_exception_for_missing_config(self):
        with self.assertRaises(Exception):
            sciserverutils.logging.configure()

    @patch.dict(os.environ, {'SCISERVER_LOGGING_USE_DUMMY': 'true'})
    def test_logging_standard_config_using_dummy_override(self):
        sciserverutils.logging.configure()

    def test_logmessage_resolves_to_dict(self):
        self.captureLogger.info('test')
        ldict = recordToDict(self.captureHandler.rec)
        assert(ldict['message'] == 'test')
        assert(ldict['levelname'] == 'INFO')

    def test_logmessage_with_exc_info_adds_trace(self):
        try:
            _ = junk + 1
        except:
            self.captureLogger.error('test', exc_info=True)
        ldict = recordToDict(self.captureHandler.rec)
        assert(ldict['message'] == 'test')
        assert(ldict['exceptionType'] == 'NameError')
        assert(ldict['exceptionTrace'] is not None)

    def test_logmessage_auto_adds_exc_info(self):
        try:
            _ = junk + 1
        except:
            self.captureLogger.error('test')
        ldict = recordToDict(self.captureHandler.rec)
        assert(ldict['message'] == 'test')
        assert(ldict['exceptionType'] == 'NameError')
        assert(ldict['exceptionTrace'] is not None)
