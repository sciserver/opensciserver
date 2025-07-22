import logging
import time
from sciserverutils.logging.servicelog import ServiceLog
from unittest import TestCase
from unittest.mock import MagicMock


class TestServiceLog(TestCase):
    def testCreateRecord(self):
        log = ServiceLog()
        assert(isinstance(log, logging.LogRecord))

    def testServiceLogEmitWithoutException(self):
        log = ServiceLog()
        log.emit()

    def testServiceLogCounterAdd(self):
        log = ServiceLog()
        log.counterAdd('new')
        log.counterAdd('new')
        self.assertEquals(log.asDict()['counters']['new'], 2)

    def testServiceLogTimerContext(self):
        log = ServiceLog()
        with log.timer('timer'):
            time.sleep(0.01)
        assert(log.asDict()['counters']['timer'] > 0)

    def testServiceLogAttributeSetAsString(self):
        log = ServiceLog()
        log.setAttr('testattr', 'value')
        log.setAttr('testintattr', 1)
        msg = log.asDict()
        assert(msg['attrs']['testattr'] == 'value')
        assert(msg['attrs']['testintattr'] == '1')

    def testServiceLogEmitOnlyOnce(self):
        log = ServiceLog()
        loggermock = MagicMock()
        log.emit(loggermock)
        log.emit(loggermock)
        loggermock.handle.assert_called_once()

    def testServiceLogBasicRequestDetailsSet(self):
        log = ServiceLog()
        log.emit(MagicMock())  # ensures log is "closed" out
        msg = log.asDict()  # essentially what handler would do
        assert(isinstance(msg['requestTime'], int))
        assert(isinstance(msg['Time'], str))
        assert(isinstance(msg['created'], float))

    def testServiceLogExcludesEmptyValues(self):
        log = ServiceLog()
        assert('counters' not in log.asDict())

    def testServiceLogExcInfo(self):
        log = ServiceLog()
        try:
            _ = junk + 1
        except:
            log.emit()
        msg = log.asDict()
        assert(msg['exceptionType'] == 'NameError')
        assert(msg['exceptionText'] is not None)
        assert(msg['exceptionTrace'] is not None)

    def testServiceLogDoesNotOverrideExcInfo(self):
        log = ServiceLog()
        e = Exception('test exception')
        log.exc_info = (type(e), e, None)
        try:
            _ = junk + 1
        except:
            log.emit()
        msg = log.asDict()
        assert(msg['exceptionType'] == 'Exception')
