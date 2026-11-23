import unittest
import requests
from testutil import sciserverBase, writeUserInfo, tokenHeader, reserveAccount, recordAccount


class TestRegister(unittest.TestCase):

    def test_register(self):
        lease = reserveAccount()
        username = lease['username']
        password = lease['password']
        email = f'{username}@example.com'
        data = {'username': username, 'email': email, 'password': password, 'confirmPassword': password}
        headers = {'content-type': 'application/x-www-form-urlencoded'}
        r = requests.post(f'{sciserverBase()}/login-portal/register', headers=headers, data=data, allow_redirects=False)
        token = r.cookies['portalCookie']
        assert(token != '')
        writeUserInfo({'username': username, 'email': email, 'password': password, 'token': token})
        recordAccount()
        assert(r.status_code == 302)

    def test_token(self):
        r = requests.get(f'{sciserverBase()}/racm/rest/resources', headers=tokenHeader())
        assert(r.status_code == 200)

    def test_account_lifecycle(self):
        """Create a fresh profile and prove exact Keystone/RACM records are removed."""
        import testutil
        broker = testutil.BROKER
        # Fail before creating anything if SQL cleanup/inspection is not configured.
        # The context does not disclose database credentials.
        context = broker.request('GET')
        self.assertTrue(context.get('racmCleanupEnabled'), 'Enable testService.racmCleanup before this test')
        lease = broker.reserve('lifecycle')
        self.addCleanup(broker.request, 'DELETE', '/accounts/lifecycle')
        data = {'username': lease['username'], 'email': lease['email'],
                'password': lease['password'], 'confirmPassword': lease['password']}
        response = requests.post(testutil.sciserverBase() + '/login-portal/register', data=data,
                                 timeout=30, allow_redirects=False)
        self.assertEqual(response.status_code, 302)
        token = response.cookies.get('portalCookie')
        self.assertTrue(token, 'Registration must return a session')
        broker.secrets.append(token)
        broker.registered('lifecycle')
        response = requests.get(testutil.sciserverBase() + '/racm/ugm/rest/user',
                                headers={'X-Auth-Token': token}, timeout=20, allow_redirects=False)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['username'], lease['username'])
        before = broker.request('GET', '/accounts/lifecycle/state')
        self.assertIs(before['keystoneUserAbsent'], False)
        self.assertIs(before['keystoneProjectAbsent'], False)
        self.assertEqual(before['racm']['state'], 'deletable')
        self.assertGreaterEqual(before['racm']['publicMemberships'], 1)
        self.assertEqual(broker.request('DELETE', '/accounts/lifecycle')['state'], 'deleted')
        after = broker.request('GET', '/accounts/lifecycle/state')
        self.assertIs(after['keystoneUserAbsent'], True)
        self.assertIs(after['keystoneProjectAbsent'], True)
        self.assertEqual(after['racm']['state'], 'absent')
