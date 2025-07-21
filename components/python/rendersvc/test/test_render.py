import logging
from sciserverutils.logging.servicelog import ServiceLog
from sciserverapp.rendersvc import app
from fastapi.testclient import TestClient
from unittest import TestCase


class Test(TestCase):

    def setUp(self):
        self.client = TestClient(app)

    def test_convert_empty_notebook(self):
        response = self.client.post('/notebook/convert', json={'cells': []})
        assert response.status_code == 200
        assert response.text.startswith('<!DOCTYPE html>')

    def test_convert_400_on_bad_input(self):
        response = self.client.post('/notebook/convert', content='notjson')
        assert response.status_code == 400

    def test_servicelog_recorded(self):
        with self.assertLogs() as logs:
            _ = self.client.post('/notebook/convert', json={'cells': []})
        slogs = [i for i in logs.records if isinstance(i, ServiceLog)]
        assert(len(slogs) == 1)
        assert(slogs[0].requestTime > 0)
