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
