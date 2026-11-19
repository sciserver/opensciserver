import json
import os
import requests
from urllib.parse import urljoin, urlsplit

URLFILE = 'sciserverbase'
USERFILE = 'sciserveruser'
SCISERVERBASE = None
USERINFO = None
ACCOUNTLEASE = None
BROKER = None
RUN_ID = None


def sciserverBase():
    global SCISERVERBASE
    if not SCISERVERBASE:
        with open(URLFILE, 'r') as f:
            SCISERVERBASE = f.read().strip()
    return SCISERVERBASE


def writeUserInfo(info):
    with open(USERFILE, 'w') as f:
        f.write(json.dumps(info))


def getUserInfo():
    global USERINFO
    if not USERINFO:
        with open(USERFILE, 'r') as f:
            USERINFO = json.load(f)
    return USERINFO


def getToken():
    return getUserInfo()['token']


def getUser():
    return getUserInfo()['username']


def getEmail():
    return getUserInfo()['email']


def tokenHeader():
    return {'x-auth-token': getToken()}


def deleteCreatedFile(path):
    response = requests.delete(f'{sciserverBase()}/fileservice/api/data/{path}',
                               headers=tokenHeader(), params={'quiet': 'true'},
                               timeout=20, allow_redirects=False)
    if response.status_code not in (200, 204, 404):
        raise RuntimeError(f'File cleanup failed (HTTP {response.status_code})')


def deleteCreatedGroup(name):
    # Discover by this test's unique name even if creation's response was lost.
    response = requests.get(f'{sciserverBase()}/racm/collaborations',
                            headers=tokenHeader(), timeout=20, allow_redirects=False)
    response.raise_for_status()
    payload = response.json()['_embedded']
    user_id = next(u['id'] for u in payload['userList'] if u['username'] == getUser())
    for group in payload['collaborationList']:
        if group['name'] != name:
            continue
        if not any(m['id'] == user_id and m['role'] == 'OWNER' for m in group['members']):
            raise RuntimeError('Refusing to clean up a group not owned by the test user')
        response = requests.delete(f"{sciserverBase()}/racm/ugm/rest/groups/{group['id']}",
                                   headers=tokenHeader(), timeout=20, allow_redirects=False)
        if response.status_code not in (200, 204, 404):
            raise RuntimeError(f'Group cleanup failed (HTTP {response.status_code})')


def deleteCreatedContainer(container_id):
    # The API DELETE requires a service credential; the UI endpoint checks ownership.
    response = requests.get(f'{sciserverBase()}/compute/delete',
                            params={'id': container_id}, headers=tokenHeader(),
                            timeout=20, allow_redirects=False)
    base = urlsplit(sciserverBase())
    expected_path = base.path.rstrip('/') + '/compute/'
    location = response.headers.get('Location', '')
    target = urlsplit(urljoin(sciserverBase() + '/compute/delete', location))
    if (response.status_code != 302 or not location or target.scheme != base.scheme
            or target.netloc != base.netloc or target.path not in ('/', expected_path)
            or target.query or target.fragment):
        raise RuntimeError(f'Container cleanup failed (HTTP {response.status_code})')


def reserveAccount(scope='registration'):
    global ACCOUNTLEASE
    if BROKER is None:
        raise RuntimeError('A test broker is required; use runner.py with broker configuration')
    ACCOUNTLEASE = BROKER.reserve(scope)
    return ACCOUNTLEASE


def recordAccount(scope='registration'):
    return BROKER.registered(scope)


def releaseAccount():
    if BROKER is not None:
        BROKER.release()


def fixtureAccount(scope):
    if BROKER is None:
        raise RuntimeError('A test broker is required')
    if scope in BROKER.fixture_errors:
        raise RuntimeError(BROKER.fixture_errors[scope])
    if scope not in BROKER.fixtures:
        try:
            lease = reserveAccount(scope)
            data = {'username': lease['username'], 'email': lease['email'],
                    'password': lease['password'], 'confirmPassword': lease['password']}
            response = requests.post(sciserverBase() + '/login-portal/register', data=data,
                                     timeout=30, allow_redirects=False)
            if response.status_code != 302 or not response.cookies.get('portalCookie'):
                raise RuntimeError(f'Account fixture registration failed (HTTP {response.status_code})')
            info = {**data, 'token': response.cookies['portalCookie']}
            BROKER.secrets.append(info['token'])
            recordAccount(scope)
            BROKER.fixtures[scope] = info
        except Exception:
            BROKER.fixture_errors[scope] = f'Account fixture {scope} could not be provisioned'
            raise
    return BROKER.fixtures[scope]


def prepareFixtures(names):
    global USERINFO
    for scope in names:
        USERINFO = fixtureAccount(scope)
        writeUserInfo(USERINFO)
