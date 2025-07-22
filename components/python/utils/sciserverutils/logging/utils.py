from logging import LogRecord
import traceback


def recordToDict(record: LogRecord):
    logdict = {
        k: v for k, v in record.__dict__.items()
        if k in ['levelname', 'pathname', 'method', 'method', 'lineno', 'message',
                 'created', 'counters', 'attrs', 'Application', 'Host', 'Time', 'MessageType']
        and v
    }
    if record.exc_info:
        etype, evalue, etb = record.exc_info
        logdict['exceptionType'] = etype.__name__ if etype else 'NA'
        logdict['exceptionText'] = str(evalue)
        logdict['exceptionTrace'] = ''.join(traceback.format_exception(None, value=evalue, tb=etb))
    if 'name' in record.__dict__:
        logdict['logger'] = record.name
    if 'method' not in logdict:
        module = record.__dict__.get('module')
        func = record.__dict__.get('funcName')
        if module:
            logdict['method'] = module
            if func:
                logdict['method'] += '.' + func
        elif func:
            logdict['method'] = func
    if 'MessageType' not in logdict and 'levelname' in logdict:
        logdict['MessageType'] = logdict['levelname']
    if 'requestTime' in record.__dict__:
        logdict['requestTime'] = record.__dict__['requestTime']

    return logdict
