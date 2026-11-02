"""Callback delivery is generic HTTP, independent of a test service implementation."""
import contextlib
import io
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
import urllib.error
from unittest.mock import patch
from jsonschema import Draft202012Validator

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import reporting
import runner


class ReportingChecks(unittest.TestCase):
    def test_retry_sends_identical_payload_and_bearer_with_bounded_time(self):
        requests = []
        clock = [0]
        class Response:
            status = 201
            def __enter__(self): return self
            def __exit__(self, *args): pass
        class Opener:
            def open(self, request, timeout):
                requests.append((request, timeout))
                if len(requests) == 1:
                    raise urllib.error.HTTPError(request.full_url, 503, 'retry', {}, io.BytesIO())
                return Response()
        self.assertTrue(reporting.deliver(('http://receiver/report', 'secret', 3), b'{"runId":"run"}',
                                         Opener(), lambda: clock[0], lambda delay: clock.__setitem__(0, clock[0] + delay)))
        self.assertEqual(len(requests), 2)
        self.assertEqual(requests[0][0].data, requests[1][0].data)
        self.assertEqual(requests[0][0].get_header('Authorization'), 'Bearer secret')
        self.assertLessEqual(requests[1][1], 2)

    def test_unauthorized_conflicting_or_redirected_delivery_is_not_retried(self):
        for code in (302, 307, 401, 409, 413):
            opener = unittest.mock.Mock()
            opener.open.side_effect = urllib.error.HTTPError('http://receiver', code, 'no', {}, io.BytesIO())
            self.assertFalse(reporting.deliver(('http://receiver', 'token', 1), b'{}', opener))
            self.assertEqual(opener.open.call_count, 1)
        handler = reporting.NoRedirect()
        self.assertIsNone(handler.redirect_request(None, None, 307, '', {}, 'http://other'))

    def test_retry_deadline_and_report_size_are_bounded(self):
        clock = [0]
        opener = unittest.mock.Mock()
        opener.open.side_effect = OSError('unavailable')
        self.assertFalse(reporting.deliver(('http://receiver', '', 3), b'{}', opener,
                                          lambda: clock[0], lambda delay: clock.__setitem__(0, clock[0] + delay)))
        self.assertEqual(clock[0], 3)
        self.assertEqual(opener.open.call_count, 2)
        opener.reset_mock()
        self.assertFalse(reporting.deliver(('http://receiver', '', 1), b'x' * (reporting.MAX_BYTES + 1), opener))
        opener.open.assert_not_called()

    def test_delivery_failure_has_own_exit_code_without_changing_test_outcome(self):
        for status, expected in [('passed', 0), ('failed', 1), ('error', 2)]:
            for delivered in (True, False):
                with tempfile.TemporaryDirectory() as directory:
                    report = Path(directory) / 'report.json'
                    with patch.dict(os.environ, {'SCISERVER_BASE_URL': 'http://example.test',
                                                'TEST_REPORT_PATH': str(report),
                                                'TEST_REPORT_CALLBACK_URL': 'http://receiver/report',
                                                'TEST_REPORT_CALLBACK_TOKEN': 'secret'}, clear=True), \
                         patch.object(runner, 'execute', return_value=(status, None)), \
                         patch.object(reporting, 'deliver', return_value=delivered), \
                         contextlib.redirect_stderr(io.StringIO()) as logs:
                        code = runner.main(['--test', 'preflight.login'])
                    self.assertEqual(code, expected if delivered else 3)
                    self.assertEqual(json.loads(report.read_text())['outcome'], status)
                    receipt = json.loads((Path(directory) / 'delivery.json').read_text())
                    schema = json.loads((Path(runner.ROOT) / 'contract/delivery.schema.json').read_text())
                    Draft202012Validator(schema).validate(receipt)
                    self.assertEqual(receipt['reportDelivery'], 'received' if delivered else 'failed')
                    self.assertNotIn('secret', logs.getvalue() + report.read_text())

    def test_callback_is_opt_in_and_configuration_rejects_secrets_in_url(self):
        with patch.dict(os.environ, {}, clear=True):
            self.assertIsNone(reporting.configuration())
        for url in ('http://user:pass@receiver/report', 'http://receiver/report?token=secret', 'file:///tmp/report'):
            with patch.dict(os.environ, {'TEST_REPORT_CALLBACK_URL': url}, clear=True):
                self.assertRaises(ValueError, reporting.configuration)
