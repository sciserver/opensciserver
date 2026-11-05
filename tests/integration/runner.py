"""Version 1 integration runner. Only standard-library dependencies for discovery."""
import argparse
import datetime
import importlib
import json
import os
from pathlib import Path
import sys
import tempfile
import time
import traceback
import unittest
import uuid
import reporting

ROOT = Path(__file__).resolve().parent


def load_catalog():
    return json.loads((ROOT / 'contract/catalog.json').read_text())


def resolve(catalog, selected):
    """Expand hard dependencies; order-only edges apply only to included tests."""
    entries = {t['id']: t for t in catalog['tests']}
    if len(entries) != len(catalog['tests']):
        raise ValueError('Duplicate test IDs')
    for entry in entries.values():
        for dep in entry['dependsOn'] + entry.get('after', []):
            if dep not in entries:
                raise ValueError(f'Unknown dependency {dep}')
    def visit(node, active, done, output, included):
        if node in active:
            raise ValueError(f'Dependency cycle at {node}')
        if node in done:
            return
        active.add(node)
        for dep in entries[node]['dependsOn'] + entries[node].get('after', []):
            if dep in included:
                visit(dep, active, done, output, included)
        active.remove(node)
        done.add(node)
        output.append(node)
    # Reject invalid graphs even when a particular selection avoids the cycle.
    done = set()
    for node in entries:
        visit(node, set(), done, [], entries)
    requested = list(dict.fromkeys(selected if selected is not None else entries))
    included = set()
    def include(node):
        if node not in entries:
            raise ValueError(f'Unknown test ID: {node}')
        if node in included:
            return
        included.add(node)
        for dep in entries[node]['dependsOn']:
            include(dep)
    for node in requested:
        include(node)
    output, done = [], set()
    for node in entries:
        if node in included:
            visit(node, set(), done, output, included)
    return {'contractVersion': '1', 'requested': requested, 'tests': [
        {**entries[node], 'selected': node in requested} for node in output]}


def execute(entry):
    module, cls, method = entry['implementation'].rsplit('.', 2)
    case = getattr(importlib.import_module(module), cls)(method)
    result = unittest.TestResult()
    case.run(result)
    if result.errors:
        return 'error', result.errors[0][1]
    if result.failures:
        return 'failed', result.failures[0][1]
    if result.skipped:
        return 'skipped', result.skipped[0][1]
    if result.unexpectedSuccesses:
        return 'failed', 'Unexpected success'
    if result.expectedFailures:
        return 'skipped', 'Expected failure'
    return 'passed', None


def redact(detail):
    if detail is None:
        return None
    # Current suite keeps credentials in this private per-run file.
    path = Path('sciserveruser')
    if path.exists():
        info = json.loads(path.read_text())
        for key in ('password', 'token'):
            if info.get(key):
                detail = detail.replace(info[key], '[REDACTED]')
    return detail


def run_plan(plan, executor=None):
    executor = executor or execute
    results, states = [], {}
    for entry in plan['tests']:
        blocked = [dep for dep in entry['dependsOn'] if states[dep] != 'passed']
        start = time.monotonic()
        if blocked:
            status, detail = 'blocked', 'Prerequisites did not pass'
        else:
            try:
                status, detail = executor(entry)
            except Exception:
                status, detail = 'error', traceback.format_exc()
        states[entry['id']] = status
        results.append({'id': entry['id'], 'name': entry['name'],
                        'selected': entry['selected'], 'status': status,
                        'durationSeconds': time.monotonic() - start,
                        'blockedBy': blocked, 'detail': redact(detail)})
        print(f"{entry['id']}: {status}", file=sys.stderr, flush=True)
        if results[-1]['detail']:
            print(results[-1]['detail'], file=sys.stderr, flush=True)
    return results


def now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def write_report(path, report):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(mode='w', dir=path.parent, delete=False) as f:
        tmp = Path(f.name)
        json.dump(report, f, indent=2)
        f.write('\n')
    os.replace(tmp, path)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--list', action='store_true', help='Print JSON catalog without running tests')
    parser.add_argument('--plan', action='store_true', help='Print resolved JSON selection without running')
    parser.add_argument('--test', action='append', help='Select a stable test ID; repeatable')
    args = parser.parse_args(argv)
    if args.list:
        catalog = load_catalog()
        resolve(catalog, None)
        print(json.dumps(catalog, indent=2))
        return 0
    report_path = Path(os.environ.get('TEST_REPORT_PATH', '/results/report.json')).resolve()
    report = {'contractVersion': '1', 'runId': os.environ.get('TEST_RUN_ID') or str(uuid.uuid4()),
              'startedAt': now(), 'finishedAt': None, 'outcome': 'error',
              'requested': [], 'tests': [], 'counts': {}, 'runnerError': None}
    callback = None
    try:
        if not args.plan:
            callback = reporting.configuration()
            delivery_path = Path(os.environ.get('TEST_REPORT_DELIVERY_PATH',
                                                str(report_path.parent / 'delivery.json'))).resolve()
            if delivery_path == report_path:
                raise ValueError('Delivery status path must differ from report path')
        selected = args.test
        if selected is None and 'TEST_IDS' in os.environ:
            selected = json.loads(os.environ['TEST_IDS'])
            if not isinstance(selected, list) or not all(isinstance(x, str) for x in selected):
                raise ValueError('TEST_IDS must be a JSON array of test IDs')
        if selected == []:
            raise ValueError('Empty selections are not allowed')
        plan = resolve(load_catalog(), selected)
        report['requested'] = plan['requested']
        if args.plan:
            print(json.dumps(plan, indent=2))
            return 0
        base = os.environ.get('SCISERVER_BASE_URL', '').rstrip('/')
        from urllib.parse import urlsplit
        url = urlsplit(base)
        if url.scheme not in ('http', 'https') or not url.hostname or url.username or url.password or url.query or url.fragment:
            raise ValueError('SCISERVER_BASE_URL must be an HTTP(S) base URL without credentials, query or fragment')
        # Never reuse a previous run's identity or session, including CLI runs.
        with tempfile.TemporaryDirectory(prefix='sciserver-test-') as workspace:
            original = Path.cwd()
            try:
                os.chdir(workspace)
                Path('sciserverbase').write_text(base)
                import testutil
                testutil.SCISERVERBASE = None
                testutil.USERINFO = None
                report['tests'] = run_plan(plan)
            finally:
                os.chdir(original)
        statuses = {r['status'] for r in report['tests']}
        report['outcome'] = ('error' if 'error' in statuses else
                             'failed' if statuses & {'failed', 'blocked'} else 'passed')
    except Exception as exc:
        # Configuration/import failures are runner errors, not assertion failures.
        report['runnerError'] = f'{type(exc).__name__}: {exc}'
        print(report['runnerError'], file=sys.stderr)
    report['counts'] = {s: sum(t['status'] == s for t in report['tests'])
                        for s in ('passed', 'failed', 'error', 'skipped', 'blocked')}
    report['finishedAt'] = now()
    write_report(report_path, report)
    print(f"Run {report['runId']}: {report['outcome']}; report: {report_path}", file=sys.stderr)
    return reporting.finish(report_path, report, callback, write_report)


if __name__ == '__main__':
    sys.exit(main())
