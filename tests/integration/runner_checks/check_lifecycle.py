"""Account lifecycle and pod-log records use fake APIs, never a real deployment."""
import contextlib
import copy
import io
import json
import sys
from pathlib import Path
import unittest
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import runner
import testutil
import test_register


class LifecycleChecks(unittest.TestCase):
    def execute_case(self, enabled=True, removed=True):
        broker = Mock(secrets=[])
        broker.reserve.return_value = {'username': 'test_user_example', 'email': 'test@example.com', 'password': 'secret'}
        deleted = False
        def request(method, suffix='', **kwargs):
            nonlocal deleted
            if method == 'DELETE':
                deleted = True
                return {'state': 'deleted'}
            if not suffix:
                return {'racmCleanupEnabled': enabled}
            return {'keystoneUserAbsent': deleted and removed, 'keystoneProjectAbsent': deleted and removed,
                    'racm': {'state': 'absent' if deleted and removed else 'deletable', 'publicMemberships': 1}}
        broker.request.side_effect = request
        post = Mock(status_code=302, cookies={'portalCookie': 'session-secret'})
        get = Mock(status_code=200)
        get.json.return_value = {'username': 'test_user_example'}
        with patch.object(testutil, 'BROKER', broker), patch.object(testutil, 'SCISERVERBASE', 'http://example.test'), \
                patch('test_register.requests.post', return_value=post) as create, \
                patch('test_register.requests.get', return_value=get):
            result = unittest.TestResult()
            test_register.TestRegister('test_account_lifecycle').run(result)
            return result, broker, create

    def test_lifecycle_checks_before_and_after_and_repeats_cleanup(self):
        result, broker, create = self.execute_case()
        self.assertTrue(result.wasSuccessful(), result.errors + result.failures)
        self.assertEqual(create.call_count, 1)
        self.assertEqual(sum(call.args[0] == 'DELETE' for call in broker.request.call_args_list), 2)
        broker.registered.assert_called_once_with('lifecycle')

    def test_missing_cleanup_config_fails_before_creating_user(self):
        result, broker, create = self.execute_case(enabled=False)
        self.assertEqual(len(result.failures), 1)
        create.assert_not_called()
        broker.reserve.assert_not_called()

    def test_false_cleanup_success_is_a_test_failure(self):
        result, _, _ = self.execute_case(removed=False)
        self.assertEqual(len(result.failures), 1)

    def test_log_record_is_redacted_bounded_and_keeps_report_identity(self):
        report = json.loads((runner.ROOT / 'contract/examples/passed.json').read_text())
        report['runnerError'] = 'private-token'
        with patch.object(testutil, 'BROKER', Mock(secrets=['private-token'])), contextlib.redirect_stdout(io.StringIO()) as out:
            runner.emit_log_report(report)
        record = out.getvalue().strip()
        self.assertTrue(record.startswith(runner.REPORT_PREFIX))
        self.assertNotIn('private-token', record)
        self.assertEqual(json.loads(record[len(runner.REPORT_PREFIX):])['runId'], report['runId'])
        report = copy.deepcopy(report)
        report['runnerError'] = 'x' * (runner.MAX_LOG_REPORT_BYTES + 1)
        with contextlib.redirect_stdout(io.StringIO()) as out:
            runner.emit_log_report(report)
        compact = json.loads(out.getvalue().strip()[len(runner.REPORT_PREFIX):])
        self.assertLess(len(out.getvalue()), runner.MAX_LOG_REPORT_BYTES)
        self.assertEqual(compact['outcome'], 'error')
