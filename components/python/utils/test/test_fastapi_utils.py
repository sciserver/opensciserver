import os
from unittest import TestCase
from unittest.mock import patch
from fastapi import Request
from fastapi.testclient import TestClient
from sciserverutils.apps.fastapi import FastAPI, getServiceLog
from sciserverutils.logging.servicelog import ServiceLog
from starlette.exceptions import HTTPException


def create_app():
    app = FastAPI()

    @app.get("/")
    def testroute():
        return 1

    @app.get("/exception")
    def testexception():
        raise HTTPException(500, 'test exception')

    @app.get("/notfound")
    def test404exception():
        raise HTTPException(404, 'not found')

    @app.get("/timer")
    def testtimercounter(request: Request):
        with getServiceLog(request).timer('testtimer'):
            pass
        getServiceLog(request).counterAdd('testcounter')

    return app


class TestFastApi(TestCase):

    @patch.dict(os.environ, {'SCISERVER_LOGGING_USE_DUMMY': 'true'})
    def setUp(self):
        self.app = create_app()
        self.client = TestClient(self.app)

    def test_app_bootstraps_and_serves(self):
        r = self.client.get('/')
        assert r.status_code == 200

    def test_servicelog_injected_in_request_and_recorded(self):
        with self.assertLogs() as logs:
            _ = self.client.get('/')
        assert(isinstance(logs.records[0], ServiceLog))

    def test_servicelog_adds_exception_info_when_raised(self):
        with self.assertLogs() as logs:
            _ = self.client.get('/exception')
        assert(isinstance(logs.records[0].asDict()['exceptionText'], str))
        assert(logs.records[0].attrs['status'] == '500')
        with self.assertLogs() as logs:
            _ = self.client.get('/notfound')
        assert(logs.records[0].attrs['status'] == '404')

    def test_servicelog_adds_non_exception_status_code(self):
        with self.assertLogs() as logs:
            _ = self.client.get('/')
        assert(logs.records[0].attrs['status'] == '200')

    def test_servicelog_adds_url_path_info(self):
        with self.assertLogs() as logs:
            _ = self.client.get('/somepath')
        assert(logs.records[0].attrs['uri'] == '/somepath')
        assert('query' not in logs.records[0].attrs)

    def test_servicelog_adds_url_query_info(self):
        with self.assertLogs() as logs:
            _ = self.client.get('/somepath?query1=a&query2=b')
        assert(logs.records[0].attrs['query'] == 'query1=a&query2=b')

    def test_servicelog_adds_content_length_when_present(self):
        with self.assertLogs() as logs:
            _ = self.client.get('/', headers={'content-length': '100'})
        assert(logs.records[0].attrs['size'] == '100')
        with self.assertLogs() as logs:
            _ = self.client.get('/')
        assert('size' not in logs.records[0].attrs)
