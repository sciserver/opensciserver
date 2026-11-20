"""Common broker client for local and Kubernetes execution; no orchestration knowledge."""
import os
from pathlib import Path
from urllib.parse import urlsplit
import requests


def valid_url(value):
    url = urlsplit(value)
    if (url.scheme not in ('http', 'https') or not url.hostname or url.username
            or url.password or url.query or url.fragment):
        raise ValueError('Invalid test broker URL')
    return value.rstrip('/')


class Broker:
    def __init__(self, url, token):
        self.url = valid_url(url)
        if not token:
            raise ValueError('Test broker capability is required')
        self.token = token
        self.secrets = [token]
        self.accounts = {}
        self.fixtures = {}
        self.fixture_errors = {}

    def request(self, method, suffix='', expected=200):
        response = requests.request(method, self.url + suffix,
                                    headers={'Authorization': 'Bearer ' + self.token},
                                    timeout=30, allow_redirects=False)
        if response.status_code != expected:
            raise RuntimeError(f'Test broker request failed (HTTP {response.status_code})')
        return response.json()

    @classmethod
    def connect(cls, root=None, token_file=None):
        if root:
            if not token_file:
                raise ValueError('--admin-token-file is required with --broker-url')
            root = valid_url(root)
            admin = Path(token_file).read_text().strip()
            if not admin:
                raise ValueError('Administrator token file is empty')
            response = requests.post(root + '/api/broker/runs', headers={'X-Auth-Token': admin},
                                     timeout=30, allow_redirects=False)
            if response.status_code != 201:
                raise RuntimeError(f'Broker run reservation failed (HTTP {response.status_code})')
            reservation = response.json()
            # Do not forward the capability to a server-selected host or arbitrary URL.
            import uuid
            run_id = str(uuid.UUID(reservation['runId']))
            instance = cls(root + '/account-broker/' + run_id, reservation['token'])
            instance.secrets.append(admin)
        else:
            instance = cls(os.environ.get('TEST_ACCOUNT_BROKER_URL', ''),
                           os.environ.get('TEST_ACCOUNT_BROKER_TOKEN', ''))
        return instance

    def reserve(self, scope):
        if scope not in self.accounts:
            lease = self.request('POST', '/accounts/' + scope, expected=201)
            self.accounts[scope] = lease
            self.secrets.append(lease['password'])
        return self.accounts[scope]

    def registered(self, scope):
        return self.request('POST', '/accounts/' + scope + '/registered')

    def release(self):
        result = self.request('DELETE')
        if result.get('state') != 'deleted':
            raise RuntimeError('Account cleanup pending; broker will retry')
