import unittest
import requests
from testutil import sciserverBase, tokenHeader, getToken

class TestLogout(unittest.TestCase):

    def test_logout(self):
        r = requests.get(f'{sciserverBase()}/login-portal/logout', cookies={'portalCookie': getToken()}, allow_redirects=False)
        assert(r.status_code == 302)
        assert(r.headers['Location'] == f'{sciserverBase()}/login-portal/login')

    def test_post_logout_token(self):
        r = requests.get(f'{sciserverBase()}/racm/resources', headers=tokenHeader())
        assert(r.status_code == 401)
