import json

URLFILE = 'sciserverbase'
USERFILE = 'sciserveruser'
SCISERVERBASE = None
USERINFO = None


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
