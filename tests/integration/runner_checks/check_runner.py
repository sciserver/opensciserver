"""Contract tests: no live SciServer and no Kubernetes required."""
import copy
import importlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import runner
from jsonschema import Draft202012Validator, FormatChecker


def validate(report):
    schema = json.loads((ROOT / 'contract/report.schema.json').read_text())
    Draft202012Validator(schema, format_checker=FormatChecker()).validate(report)
    for status, count in report['counts'].items():
        assert count == sum(t['status'] == status for t in report['tests'])


class RunnerChecks(unittest.TestCase):
    def test_examples_and_catalog(self):
        for path in (ROOT / 'contract/examples').glob('*.json'):
            validate(json.loads(path.read_text()))
        schema = json.loads((ROOT / 'contract/catalog.schema.json').read_text())
        Draft202012Validator(schema).validate(runner.load_catalog())

    def test_selection_expands_only_dependencies(self):
        plan = runner.resolve(runner.load_catalog(), ['fileservice.upload'])
        self.assertEqual([t['id'] for t in plan['tests']], [
            'preflight.login', 'identity.register', 'identity.token',
            'fileservice.volumes', 'fileservice.upload'])
        self.assertEqual(sum(t['selected'] for t in plan['tests']), 1)

    def test_catalog_covers_existing_suite_exactly(self):
        methods = []
        loader = unittest.TestLoader()
        for module, cls in [('test_preflight', 'TestPreflight'), ('test_register', 'TestRegister'),
                            ('test_api', 'TestApi'), ('test_logout', 'TestLogout')]:
            case = getattr(importlib.import_module(module), cls)
            methods.extend(f'{module}.{cls}.{name}' for name in loader.getTestCaseNames(case))
        self.assertCountEqual(methods, [t['implementation'] for t in runner.load_catalog()['tests']])

    def test_skip_blocks_dependents_and_errors_continue(self):
        plan = runner.resolve(runner.load_catalog(), ['identity.token', 'preflight.dashboard'])
        def execute(entry):
            if entry['id'] == 'preflight.login':
                return 'skipped', 'Not applicable'
            raise RuntimeError('Independent runner failure')
        results = runner.run_plan(plan, execute)
        self.assertEqual([r['status'] for r in results], ['skipped', 'error', 'blocked', 'blocked'])

    def test_session_diagnostics_redact_credentials(self):
        with tempfile.TemporaryDirectory() as directory:
            original = Path.cwd()
            try:
                os.chdir(directory)
                Path('sciserveruser').write_text(json.dumps({'token': 'secret-token', 'password': 'secret-password'}))
                self.assertEqual(runner.redact('secret-token secret-password'), '[REDACTED] [REDACTED]')
            finally:
                os.chdir(original)

    def test_logout_order_does_not_select_whole_suite(self):
        catalog = runner.load_catalog()
        plan = runner.resolve(catalog, ['identity.logout'])
        self.assertEqual(len(plan['tests']), 3)
        plan = runner.resolve(catalog, ['identity.revoked-token', 'fileservice.upload'])
        ids = [t['id'] for t in plan['tests']]
        self.assertLess(ids.index('fileservice.upload'), ids.index('identity.logout'))
        self.assertLess(ids.index('identity.logout'), ids.index('identity.revoked-token'))

    def test_bad_graphs_and_selection(self):
        catalog = runner.load_catalog()
        for mutate in ('cycle', 'missing', 'duplicate'):
            bad = copy.deepcopy(catalog)
            if mutate == 'cycle':
                bad['tests'][0]['dependsOn'] = ['identity.register']
            elif mutate == 'missing':
                bad['tests'][0]['dependsOn'] = ['absent']
            else:
                bad['tests'].append(bad['tests'][0])
            with self.assertRaises(ValueError):
                runner.resolve(bad, ['preflight.dashboard'])
        with self.assertRaises(ValueError):
            runner.resolve(catalog, ['typo'])

    def test_blocking_and_independent_continuation(self):
        plan = runner.resolve(runner.load_catalog(), ['fileservice.upload', 'preflight.dashboard'])
        called = []
        def execute(entry):
            called.append(entry['id'])
            return ('failed', 'HTTP 503') if entry['id'] == 'preflight.login' else ('passed', None)
        results = runner.run_plan(plan, execute)
        self.assertEqual(called, ['preflight.login', 'preflight.dashboard'])
        self.assertEqual(results[-1]['blockedBy'], ['fileservice.volumes'])

    def test_real_unittest_results_and_reports(self):
        import requests
        for code, outcome, exit_code in [(200, 'passed', 0), (503, 'failed', 1), (None, 'error', 2)]:
            with tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / 'report.json'
                response = unittest.mock.Mock(status_code=code)
                with patch.dict(os.environ, {'SCISERVER_BASE_URL': 'http://example.test/sciserver',
                                            'TEST_REPORT_PATH': str(path)}, clear=True), \
                     patch.object(requests, 'get', return_value=response,
                                  side_effect=RuntimeError('transport failed') if code is None else None):
                    self.assertEqual(runner.main(['--test', 'preflight.login']), exit_code)
                report = json.loads(path.read_text())
                validate(report)
                self.assertEqual(report['outcome'], outcome)

    def test_config_errors_emit_report_and_do_not_run(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'report.json'
            with patch.dict(os.environ, {'TEST_REPORT_PATH': str(path), 'TEST_IDS': '[]'}, clear=True):
                self.assertEqual(runner.main([]), 2)
            report = json.loads(path.read_text())
            validate(report)
            self.assertIn('Empty selections', report['runnerError'])
            self.assertEqual(report['tests'], [])

    def test_runs_do_not_reuse_session_files_or_cached_identity(self):
        import testutil
        original = Path.cwd()
        workspaces = []
        with tempfile.TemporaryDirectory() as directory:
            try:
                os.chdir(directory)
                Path('sciserveruser').write_text('original session')
                def execute(entry):
                    workspaces.append(Path.cwd())
                    self.assertFalse(Path('sciserveruser').exists())
                    self.assertIsNone(testutil.USERINFO)
                    testutil.USERINFO = {'token': 'previous-run-token'}
                    Path('sciserveruser').write_text('{"token":"generated-token"}')
                    return 'passed', None
                for index in range(2):
                    with patch.dict(os.environ, {'SCISERVER_BASE_URL': 'http://example.test',
                                                'TEST_REPORT_PATH': str(Path(directory) / f'{index}.json')}, clear=True), \
                         patch.object(runner, 'execute', side_effect=execute):
                        self.assertEqual(runner.main(['--test', 'preflight.login']), 0)
                self.assertEqual(Path('sciserveruser').read_text(), 'original session')
                self.assertNotEqual(workspaces[0], workspaces[1])
                self.assertTrue(all(not path.exists() for path in workspaces))
            finally:
                os.chdir(original)
                testutil.USERINFO = None

    def test_discovery_and_plan_do_not_need_target(self):
        for args in (['--list'], ['--plan', '--test', 'fileservice.upload']):
            result = subprocess.run([sys.executable, str(ROOT / 'runner.py'), *args],
                                    env={'PATH': os.environ['PATH']}, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            value = json.loads(result.stdout)
            self.assertEqual(value['contractVersion'], '1')
            name = 'catalog.schema.json' if args == ['--list'] else 'plan.schema.json'
            Draft202012Validator(json.loads((ROOT / 'contract' / name).read_text())).validate(value)


if __name__ == '__main__':
    unittest.main()
