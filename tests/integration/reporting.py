"""Optional, service-independent delivery of the runner-owned JSON report."""
import http.client
import os
from pathlib import Path
import time
import urllib.error
import urllib.parse
import urllib.request

MAX_BYTES = 1024 * 1024


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, url):
        return None


def configuration():
    url = os.environ.get('TEST_REPORT_CALLBACK_URL', '')
    token = os.environ.get('TEST_REPORT_CALLBACK_TOKEN', '')
    if not url:
        if token:
            raise ValueError('Callback token requires a callback URL')
        return None
    parsed = urllib.parse.urlsplit(url)
    if (parsed.scheme not in ('http', 'https') or not parsed.hostname or parsed.username
            or parsed.password or parsed.query or parsed.fragment):
        raise ValueError('Invalid callback URL')
    if any(ord(c) < 33 or ord(c) > 126 for c in token) or len(token) > 8192:
        raise ValueError('Invalid callback token')
    budget = float(os.environ.get('TEST_REPORT_CALLBACK_TIMEOUT_SECONDS', '60'))
    if not 1 <= budget <= 300:
        raise ValueError('Callback timeout must be between 1 and 300 seconds')
    return url, token, budget


def deliver(config, payload, opener=None, monotonic=time.monotonic, sleep=time.sleep):
    """POST identical bytes; only 200/201/204 acknowledge receiver acceptance."""
    if len(payload) > MAX_BYTES:
        return False
    url, token, budget = config
    opener = opener or urllib.request.build_opener(NoRedirect())
    deadline = monotonic() + budget
    delay = 1
    while monotonic() < deadline:
        headers = {'Content-Type': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        request = urllib.request.Request(url, data=payload, headers=headers, method='POST')
        try:
            with opener.open(request, timeout=min(10, max(0.01, deadline - monotonic()))) as response:
                status = response.status
            if status in (200, 201, 204):
                return True
            if status not in (408, 429) and not 500 <= status < 600:
                return False
        except urllib.error.HTTPError as error:
            status = error.code
            error.close()
            if status not in (408, 429) and not 500 <= status < 600:
                return False
        except (OSError, urllib.error.URLError, http.client.HTTPException):
            pass
        remaining = deadline - monotonic()
        if remaining <= 0:
            break
        sleep(min(delay, remaining))
        delay = min(delay * 2, 5)
    return False


def finish(path, report, config, writer):
    """Preserve the test outcome; record delivery independently and reserve exit 3."""
    delivered = config is None or deliver(config, path.read_bytes())
    status = 'not_configured' if config is None else 'received' if delivered else 'failed'
    receipt = {'contractVersion': '1', 'runId': report['runId'],
               'outcome': report['outcome'], 'reportDelivery': status}
    delivery_path = Path(os.environ.get('TEST_REPORT_DELIVERY_PATH', str(path.parent / 'delivery.json'))).resolve()
    try:
        if delivery_path == path:
            raise ValueError('Delivery status path must differ from report path')
        writer(delivery_path, receipt)
    except (OSError, ValueError):
        print('Could not write delivery receipt', file=__import__('sys').stderr)
        return 3
    # Deliberately omit URL, credential, response body and transport exception details.
    print('Report delivery: ' + status, file=__import__('sys').stderr, flush=True)
    return {'passed': 0, 'failed': 1, 'error': 2}[report['outcome']] if delivered else 3
