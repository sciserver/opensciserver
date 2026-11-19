"""Cleanup checks use fake HTTP responses and never touch a deployment."""
import sys
from pathlib import Path
import unittest
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import runner
import test_api
import testutil


class CleanupChecks(unittest.TestCase):
    def test_upload_is_deleted_after_verification_fails(self):
        with patch.object(test_api, 'getUser', return_value='temporary'), \
                patch.object(testutil, 'sciserverBase', return_value='https://example.org/test'), \
                patch.object(test_api, 'sciserverBase', return_value='https://example.org/test'), \
                patch.object(testutil, 'tokenHeader', return_value={'x-auth-token': 'temporary-token'}), \
                patch.object(test_api, 'tokenHeader', return_value={'x-auth-token': 'temporary-token'}), \
                patch('requests.put', return_value=Mock(status_code=200)), \
                patch('requests.get', return_value=Mock(status_code=500)), \
                patch('requests.delete', return_value=Mock(status_code=204)) as delete:
            case = test_api.TestApi('test_fileservice_upload_file')
            result = unittest.TestResult()
            case.run(result)
        self.assertEqual(len(result.failures), 1)
        self.assertFalse(result.errors)
        self.assertEqual(delete.call_count, 1)
        self.assertIn('/fileservice/api/data/Storage/temporary/persistent/testfile_',
                      delete.call_args.args[0])
        self.assertFalse(delete.call_args.kwargs['allow_redirects'])

    def test_group_cleanup_only_deletes_exact_owned_group(self):
        payload = {'_embedded': {
            'userList': [{'id': 1, 'username': 'temporary'}],
            'collaborationList': [
                {'id': 2, 'name': 'other', 'members': [{'id': 1, 'role': 'OWNER'}]},
                {'id': 3, 'name': 'unique-test-group', 'members': [{'id': 1, 'role': 'OWNER'}]},
            ]}}
        with patch.object(testutil, 'sciserverBase', return_value='https://example.org/test'), \
                patch.object(testutil, 'getUser', return_value='temporary'), \
                patch.object(testutil, 'tokenHeader', return_value={}), \
                patch('requests.get', return_value=Mock(json=lambda: payload)), \
                patch('requests.delete', return_value=Mock(status_code=200)) as delete:
            testutil.deleteCreatedGroup('unique-test-group')
            self.assertEqual(delete.call_count, 1)
            self.assertTrue(delete.call_args.args[0].endswith('/groups/3'))
            payload['_embedded']['collaborationList'][1]['members'][0]['role'] = 'MEMBER'
            with self.assertRaisesRegex(RuntimeError, 'not owned'):
                testutil.deleteCreatedGroup('unique-test-group')
            self.assertEqual(delete.call_count, 1)

    def test_failed_cleanup_is_reported_alongside_original_failure(self):
        class Case(unittest.TestCase):
            def runTest(self):
                self.addCleanup(lambda: (_ for _ in ()).throw(RuntimeError('cleanup failed')))
                self.fail('original failure')
        with patch.object(runner.importlib, 'import_module', return_value=Mock(Case=Case)):
            status, detail = runner.execute({'implementation': 'fake.Case.runTest'})
        self.assertEqual(status, 'error')
        self.assertIn('original failure', detail)
        self.assertIn('cleanup failed', detail)

    def test_cleanup_does_not_accept_authentication_redirect(self):
        with patch.object(testutil, 'sciserverBase', return_value='https://example.org/test'), \
                patch.object(testutil, 'tokenHeader', return_value={}), \
                patch('requests.get', return_value=Mock(status_code=302, headers={'Location': '/login'})):
            with self.assertRaisesRegex(RuntimeError, 'Container cleanup failed'):
                testutil.deleteCreatedContainer(123)
        with patch.object(testutil, 'sciserverBase', return_value='https://example.org/test'), \
                patch.object(testutil, 'tokenHeader', return_value={}), \
                patch('requests.delete', return_value=Mock(status_code=302)):
            with self.assertRaisesRegex(RuntimeError, 'File cleanup failed'):
                testutil.deleteCreatedFile('Storage/temporary/persistent/unique')


class AccountBrokerChecks(unittest.TestCase):
    def setUp(self):
        self.previous = testutil.BROKER
        testutil.BROKER = None

    def tearDown(self):
        testutil.BROKER = self.previous

    def test_missing_broker_cannot_fall_back_to_unowned_registration(self):
        with self.assertRaisesRegex(RuntimeError, 'test broker is required'):
            testutil.reserveAccount()

    def test_runner_releases_after_execution_error_and_reports_cleanup_failure(self):
        import os
        import json
        import tempfile
        fake = Mock(secrets=[])
        fake.request.return_value = {'runId': '12345678-1234-1234-1234-123456789012',
                                     'baseUrl': 'https://example.org/test'}
        with tempfile.TemporaryDirectory() as directory, \
                patch.dict(os.environ, {'TEST_REPORT_PATH': directory + '/report.json'}, clear=True), \
                patch('broker.Broker.connect', return_value=fake), \
                patch.object(runner, 'run_plan', side_effect=RuntimeError('execution failed')):
            fake.release.side_effect = RuntimeError('cleanup failed')
            self.assertEqual(runner.main(['--test', 'preflight.login']), 2)
            fake.release.assert_called_once()
            report = json.loads(Path(directory + '/report.json').read_text())
            self.assertIn('execution failed', report['runnerError'])
            self.assertIn('broker will retry', report['runnerError'])
