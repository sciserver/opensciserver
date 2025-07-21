from fastapi import Request, FastAPI as _FastAPI
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.exceptions import HTTPException as StarletteHTTPException
from fastapi.exceptions import RequestValidationError
from fastapi.exception_handlers import http_exception_handler
from ..logging.servicelog import ServiceLog
from .. import logging


class ServiceLogMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        request.state.servicelog = ServiceLog()
        request.state.servicelog.setAttr('uri', request.url.path)
        if request.url.query:
            request.state.servicelog.setAttr('query', request.url.query)
        if request.headers.get('content-length'):
            request.state.servicelog.setAttr('size', request.headers.get('content-length'))
        response = await call_next(request)
        request.state.servicelog.setAttr('status', response.status_code)
        request.state.servicelog.emit()
        return response


async def servicelog_inject_exception_handler(request, exc):
    request.state.servicelog.exc_info = (type(exc), exc, exc.__traceback__)
    if isinstance(exc, StarletteHTTPException):
        request.state.servicelog.setAttr('status', exc.status_code)
    else:
        request.state.servicelog.setAttr('status', '400')
    return await http_exception_handler(request, exc)


def FastAPI(*args, **kwargs):
    api = _FastAPI(*args, **kwargs)
    api.add_middleware(ServiceLogMiddleware)
    api.add_exception_handler(StarletteHTTPException, servicelog_inject_exception_handler)
    api.add_exception_handler(RequestValidationError, servicelog_inject_exception_handler)
    logging.configure()
    return api


def getServiceLog(request: Request):
    return request.state.servicelog
