import unittest
import requests
import random
from testutil import sciserverBase, writeUserInfo, tokenHeader


class TestRegister(unittest.TestCase):

    def test_register(self):
        username = 'test_user_' + str(random.getrandbits(64))
        password = str(random.getrandbits(64))
        email = f'{username}@example.com'
        data = f'username={username}&email={email}&password={password}&confirmPassword={password}'
        headers = {'content-type': 'application/x-www-form-urlencoded'}
        r = requests.post(f'{sciserverBase()}/login-portal/register', headers=headers, data=data, allow_redirects=False)
        token = r.cookies['portalCookie']
        assert(token != '')
        writeUserInfo({'username': username, 'email': email, 'password': password, 'token': token})
        assert(r.status_code == 302)

    def test_token(self):
        r = requests.get(f'{sciserverBase()}/racm/rest/resources', headers=tokenHeader())
        assert(r.status_code == 200)
