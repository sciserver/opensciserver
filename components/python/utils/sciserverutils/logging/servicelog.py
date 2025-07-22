import time
import sys
import logging
import inspect
from datetime import datetime
from .utils import recordToDict


class Timer:
    def __init__(self, name, autostart=False, recorder=None):
        self.started = False
        self.name = name
        self.recorder = recorder
        self.accum = 0
        if autostart:
            self.start()

    def start(self):
        self.starttime = time.time()
        self.started = True

    def stop(self):
        if self.started:
            self.accum += self.gettime()
            self.started = False
            if self.recorder is not None:
                self.recorder[self.name] = self.accum

    def gettime(self):
        if self.started:
            return int((time.time() - self.starttime)*1000)
        return self.accum

    def __enter__(self):
        self.start()

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.stop()


class ServiceLog(logging.LogRecord):
    def __init__(self, application=None, hostname=None):
        if application:
            self.Application = application
        if hostname:
            self.Host = hostname
        frame = sys._getframe(1)
        super().__init__(self, logging.INFO, frame.f_code.co_filename, frame.f_lineno, '', {}, None)
        self.funcName = frame.f_code.co_name
        self.module = inspect.getmodulename(self.pathname) or self.pathname
        self.method = self.module + '.' + self.funcName
        self.levelname = 'SERVICELOG'
        self.counters = {}
        self.attrs = {}
        self.message = None
        self.created = time.time()
        self.Time = datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%S.%fZ')
        self._logtimer = Timer('', autostart=True)
        self._emitted = False

    def __repr__(self):
        return 'ServiceLog<' + 'in progress' if self._emitted else 'emitted' + '>'

    def asDict(self):
        return recordToDict(self)

    def getMessage(self):
        return str(self.asDict())

    def counterAdd(self, name):
        self.counters[name] = self.counters.get(name, 0) + 1

    def timer(self, name):
        return Timer(name, recorder=self.counters)

    def setAttr(self, name, value):
        self.attrs[name] = str(value)

    def emit(self, logger=None):
        if self._emitted:
            return
        self.requestTime = self._logtimer.gettime()
        if not logger:
            logger = logging.getLogger('servicelog')
        if self.exc_info is None:
            exc = sys.exc_info()
            if exc[0] is not None:
                self.exc_info = exc
        self.name = logger.name
        logger.handle(self)
        self._emitted = True
