from functools import wraps
from os import getenv
import requests
from flask import Response, request

USER_URL = getenv('USER_URL', None)


def check_auth(auth_string):
    """check auth string is_valid(basic or apikey)

    Arguments:
        auth {string} -- auth_string

    Returns:
        boolean -- is_valid
        string -- user_name
    """
    if auth_string == None:
        return False, None
    auth = auth_string.split(" ")
    if (auth[0].upper() == 'BASIC'):
        user = requests.get(USER_URL + '/login',
                            headers={'Authorization': auth_string}).json()
        if user is None:
            return False, None
        return True, user.get('userName')
    else:
        users = requests.get(USER_URL, params={'token': auth_string}).json()
        if len(users) == 0:
            return False, None
        user = users[0]
        return True, user.get('userName')


def auth_fail():
    """reponse fot auth when iw fail

    Returns:
        Response -- reponse unauthorized
    """
    resp = Response()
    resp.status_code = 401
    return resp


def requires_auth(func):
    """ custom auth for api

    Arguments:
        func {funtion} -- func to call after auth   

    Returns:
        func -- call with user_name
    """
    @wraps(func)
    def decorated(*args, **kwargs):
        try:
            is_valid, user_name = check_auth(
                request.headers.get('authorization', None))
            if not is_valid:
                return auth_fail()
            return func(user_name, *args, **kwargs)
        except Exception:
            return auth_fail()

    return decorated
