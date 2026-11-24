"""CLI and pod configuration must execute the same fixture lifecycle and tests."""
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import runner
import broker
import testutil

RUN_ID = '12345678-1234-1234-1234-123456789012'
ROOT = 'https://example.org/test/test-service'
TARGET = 'https://example.org/test'


class DualExecutionChecks(unittest.TestCase):
    def test_cli_and_pod_use_identical_fixture_and_cleanup_protocol(self):
        results = []
        for cli in (True, False):
            events = []
            def response(status=200, payload=None, cookies=None):
                return Mock(status_code=status, json=lambda: payload, cookies=cookies or {})
            def post(url, **kwargs):
                if url.endswith('/api/broker/runs'):
                    self.assertEqual(kwargs['headers'], {'X-Auth-Token': 'admin-session'})
                    events.append('reserve-run')
                    return response(201, {'runId': RUN_ID, 'token': 'run-capability'})
                self.assertEqual(url, TARGET + '/login-portal/register')
                self.assertEqual(kwargs['data']['password'], 'generated-password')
                events.append('register-session')
                return response(302, cookies={'portalCookie': 'test-session'})
            def request(method, url, **kwargs):
                self.assertEqual(kwargs['headers'], {'Authorization': 'Bearer run-capability'})
                self.assertFalse(kwargs['allow_redirects'])
                events.append(method + ' ' + url.removeprefix(ROOT))
                if method == 'GET':
                    return response(payload={'runId': RUN_ID, 'baseUrl': TARGET})
                if url.endswith('/accounts/session'):
                    return response(201, {'username': 'temporary', 'password': 'generated-password',
                                          'email': 'temporary@example.com'})
                return response(payload={'state': 'deleted' if method == 'DELETE' else 'active'})
            with tempfile.TemporaryDirectory() as directory:
                token_file = Path(directory) / 'token'
                token_file.write_text('admin-session')
                env = {'TEST_REPORT_PATH': directory + '/report.json'}
                if not cli:
                    env.update(TEST_ACCOUNT_BROKER_URL=ROOT + '/account-broker/' + RUN_ID,
                               TEST_ACCOUNT_BROKER_TOKEN='run-capability', TEST_RUN_ID=RUN_ID,
                               SCISERVER_BASE_URL=TARGET)
                args = ['--test', 'racm.user']
                if cli:
                    args += ['--broker-url', ROOT, '--admin-token-file', str(token_file)]
                with patch.dict(os.environ, env, clear=True), patch('requests.post', side_effect=post), \
                        patch('requests.request', side_effect=request), \
                        patch('requests.get', return_value=response(payload={'username': 'temporary',
                                                                            'visibility': 'PUBLIC'})):
                    self.assertEqual(runner.main(args), 0)
                report = json.loads(Path(directory + '/report.json').read_text())
                self.assertEqual([t['id'] for t in report['tests']], ['racm.user'])
                self.assertNotIn('generated-password', json.dumps(report))
                self.assertNotIn('run-capability', json.dumps(report))
                results.append([event for event in events if event != 'reserve-run'])
        self.assertEqual(results[0], results[1])
        self.assertIn('register-session', results[0])
        self.assertTrue(results[0][-1].startswith('DELETE'))

    def test_execution_requires_broker_but_discovery_does_not(self):
        with tempfile.TemporaryDirectory() as directory, \
                patch.dict(os.environ, {'TEST_REPORT_PATH': directory + '/report.json'}, clear=True), \
                patch.object(runner, 'run_plan') as execute:
            self.assertEqual(runner.main(['--test', 'preflight.login']), 2)
            execute.assert_not_called()

    def test_logout_fixture_is_cached_and_isolated_from_session_fixture(self):
        fake = Mock(fixtures={}, fixture_errors={}, secrets=[])
        fake.reserve.side_effect = lambda scope: {'username': scope, 'email': scope + '@example.com',
                                                  'password': 'password-' + scope}
        with patch.object(testutil, 'BROKER', fake), patch.object(testutil, 'sciserverBase', return_value=TARGET), \
                patch('requests.post', side_effect=lambda *a, **k: Mock(status_code=302,
                       cookies={'portalCookie': 'token-' + k['data']['username']})):
            session = testutil.fixtureAccount('session')
            logout = testutil.fixtureAccount('logout')
            self.assertNotEqual(session['token'], logout['token'])
            self.assertIs(testutil.fixtureAccount('logout'), logout)
            self.assertEqual(fake.reserve.call_count, 2)
