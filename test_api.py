import unittest
import requests
import random
from testutil import sciserverBase, tokenHeader, getUser, getEmail, getToken

class TestApi(unittest.TestCase):

    def test_fileservice_basic_volumes(self):
        r = requests.get(f'{sciserverBase()}/fileservice/api/volumes/', headers=tokenHeader())
        storage = [i for i in r.json()['rootVolumes'] if i['name'] == 'Storage'][0]
        persistent = [i for i in storage['userVolumes'] if i['name'] == 'persistent'][0]
        temporary = [i for i in r.json()['rootVolumes'] if i['name'] == 'Temporary'][0]
        scratch = [i for i in temporary['userVolumes'] if i['name'] == 'scratch'][0]

    def test_fileservice_upload_file(self):
        data = 'this is some test data'
        url = f'{sciserverBase()}/fileservice/api/file/Storage/{getUser()}/persistent/testfile_{random.getrandbits(32)}'
        r = requests.put(url, headers=tokenHeader(), data=data)
        assert(r.status_code == 200)
        r = requests.get(url, headers=tokenHeader())
        assert(r.status_code == 200)
        assert(r.text == data)

    def test_fileservice_tree(self):
        url = f'{sciserverBase()}/fileservice/api/jsontree/Storage/{getUser()}/persistent'
        r = requests.get(url, headers=tokenHeader())
        assert(r.status_code == 200)
        assert(r.json()['root']['name'] == 'persistent')

    def test_racm_user(self):
        r = requests.get(f'{sciserverBase()}/racm/ugm/rest/user', headers=tokenHeader())
        assert(r.status_code == 200)
        assert(r.json()['username'] == getUser())
        assert(r.json()['visibility'] == 'PUBLIC')

    def test_racm_create_group(self):
        r = requests.get(f'{sciserverBase()}/racm/collaborations', headers=tokenHeader())
        glink = r.json()['_links']['createGroup']['href']
        group = {
            'groupName': f'test_group_{random.getrandbits(32)}',
            'description': 'test',
            'memberUsers': []
        }
        r = requests.post(glink, headers=tokenHeader(), json=group)
        assert(r.status_code == 200)
        r = requests.get(f'{sciserverBase()}/racm/collaborations', headers=tokenHeader())
        grouplist = [i['name'] for i in r.json()['_embedded']['collaborationList']]
        assert(group['groupName'] in grouplist)
        # Check we are the OWNER
        userid = [i['id'] for i in r.json()['_embedded']['userList'] if i['username'] == getUser()][0]
        members = [i['members'] for i in r.json()['_embedded']['collaborationList'] if i['name'] == group['groupName']][0]
        assert(len(members) == 1)
        userrole = [i['role'] for i in members if i['id'] == userid][0]
        assert(userrole == 'OWNER')

    def test_racm_public_no_info_leak(self):
        r = requests.get(f'{sciserverBase()}/racm/ugm/rest/users/public', headers=tokenHeader())
        for group in r.json()['groups']:
            assert('owner' not in group)
            assert('description' not in group)
            assert(group['memberUsers'] == [])
            assert(group['memberGroups'] == [])

    def test_login_portal_public_user_endpoints_no_info_leak(self):
        r = requests.get(f'{sciserverBase()}/login-portal/api/users?name={getUser()}')
        userinfo = r.json()[0]
        if 'email' in userinfo:
            assert(userinfo['email'] == '')
        userid = userinfo['user_id']
        r = requests.get(f'{sciserverBase()}/login-portal/api/users/{userid}')
        userinfo = r.json()
        if 'email' in userinfo:
            assert(userinfo['email'] == '')
        r = requests.get(f'{sciserverBase()}/login-portal/keystone/v3/users/{userid}')
        userinfo = r.json()['user']
        if 'email' in userinfo:
            assert(userinfo['email'] == '')

    def test_token_validation(self):
        r = requests.get(f'{sciserverBase()}/login-portal/api/validate/{getToken()}')
        assert(r.json()['email'] == getEmail())
        assert(r.json()['username'] == getUser())
        assert(type(r.json()['user_id']) == str)
        assert(r.json()['user_id'] != '')
