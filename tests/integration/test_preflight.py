import unittest
import requests
from testutil import sciserverBase


class TestPreflight(unittest.TestCase):

    def test_login_portal(self):
        """
        Basic test of login portal page, requiring 200
        """
        r = requests.get(f'{sciserverBase()}/login-portal/')
        assert(r.status_code == 200)

    def test_dashboard(self):
        """
        Basic test of dashboard page, requiring 200
        """
        r = requests.get(f'{sciserverBase()}/dashboard/')
        assert(r.status_code == 200)

    def test_compute(self):
        """
        Basic test of compute page, requiring 200
        """
        r = requests.get(f'{sciserverBase()}/compute/')
        assert(r.status_code == 200)
