import requests
import model_driver
from flask import Flask, jsonify, request
from os import getenv
from custom_auth import requires_auth

application = Flask(__name__)
USER_URL = getenv('USER_URL', None)
COLLECTION_URL = getenv('COLLECTION_URL', None)
SERVICE_URL = getenv('SERVICE_URL', None)


@application.route("/test", methods=['GET'])
def test():
    return 'OK', 200


@application.route("/migrate", methods=['GET'])
def migrate():
    model_driver.delete_all()
    users = requests.get(USER_URL).json()
    list(map(lambda x: model_driver.create_user(
        x.get("userId"), x.get("userName")), users))
    collections = requests.get(
        COLLECTION_URL, params={"size": 1000}).json().get('content')
    list(map(lambda x: model_driver.create_collection(
        x.get('owner'), x.get('collectionId'), x.get('open')), collections))
    services = requests.get(SERVICE_URL).json().get('content')
    list(map(lambda x: model_driver.create_service(
        x.get('owner'), x.get('serviceId'), x.get('open', True)), services))
    return jsonify({"result": "OK"}), 200


@application.route("/", methods=['GET'])
def find_role():
    """find role by userId and collectionId in query string

    Decorators:
        application

    Returns:
        Reponse -- http status
    """
    user_id = request.args.get('userId', None)
    collection_id = request.args.get('collectionId', None)
    service_id = request.args.get('serviceId', None)
    result = False
    if(user_id is not None and collection_id is not None):
        result = model_driver.get_collection_role(user_id, collection_id)
    elif (user_id is not None and service_id is not None):
        result = model_driver.get_service_role(user_id, service_id)
    elif (collection_id is not None and service_id is not None):
        result = model_driver.get_service_collection_role(
            service_id, collection_id)
    if not result:
        return '', 200
    return result.upper(), 200


@application.route("/", methods=['PUT'])
@requires_auth
def change_role(owner):
    """chang role

    Decorators:
        requires_auth
        application

    Arguments:
        owner{string} - owner_name

    Returns:
        Response -- http status
    """
    user_name = request.json.get("userName", None)
    collection_id = request.json.get("collectionId", None)
    service_id = request.json.get("serviceId", None)
    role = request.json.get("role", None)
    role_type = ['CONTRIBUTOR', 'READ']
    result = False
    if (role.upper() not in role_type):
        return error_handler(400)
    if(user_name is not None and collection_id is not None):
        if not (model_driver.get_collection_role(owner,
                                                 collection_id).upper() == 'OWNER'):
            return error_handler(403)
        result = model_driver.update_user_collection(
            user_name, collection_id, role)
    elif (user_name is not None and service_id is not None):
        if not (model_driver.get_service_role(owner,
                                              service_id).upper() == 'OWNER'):
            return error_handler(403)
        result = model_driver.update_user_service(
            user_name, service_id, role)
    elif (collection_id is not None and service_id is not None):
        if not (model_driver.get_collection_role(owner,
                                                 collection_id).upper() == 'OWNER'):
            return error_handler(403)
        result = model_driver.update_service_collection(
            service_id, collection_id, role)
    if not result:
        return error_handler(400)
    return '', 200


@application.route("/", methods=['DELETE'])
@requires_auth
def delete_role(owner):
    """delete role

    Decorators:
        requires_auth
        application

    Arguments:
        owner{string} - owner_name

    Returns:
        Response -- http status
    """
    user_name = request.json.get("userName", None)
    collection_id = request.json.get("collectionId", None)
    service_id = request.json.get("serviceId", None)
    result = False
    if(user_name is not None and collection_id is not None):
        if not (model_driver.get_collection_role(owner,
                                                 collection_id).upper() == 'OWNER'):
            return error_handler(403)
        result = model_driver.delete_relationship_user_collection(
            user_name, collection_id)
    elif (user_name is not None and service_id is not None):
        if not (model_driver.get_service_role(owner,
                                              service_id).upper() == 'OWNER'):
            return error_handler(403)
        result = model_driver.delete_relationship_user_service(
            user_name, service_id)
    elif (collection_id is not None and service_id is not None):
        if not (model_driver.get_collection_role(owner,
                                                 collection_id).upper() == 'OWNER'):
            return error_handler(403)
        result = model_driver.delete_relationship_service_collection(
            service_id, collection_id)
    if not result:
        return error_handler(400)
    return '', 200


@application.route("/users", methods=['POST'])
def create_user():
    """create user

    Decorators:
        application

    Arguments:
        request.json.get("userId", None) -- userId from post body
        request.json.get("userName", None) -- userName from post body

    Returns:
        Reponse -- http status
    """
    user_id = request.json.get("userId", None)
    user_name = request.json.get("userName", None)
    if (user_id is None or user_name is None):
        return error_handler(400)
    result = model_driver.create_user(
        user_id, user_name)
    if(not result):
        return error_handler(409)
    return jsonify({'result': result}), 201


@application.route("/users/<user_name>", methods=['DELETE'])
def delete_user(user_name):
    """delete user

    Decorators:
        application

    Arguments:
        user_name {string} -- user_name

       Returns:
           Response -- http status
       """
    if model_driver.delete_user(user_name):
        return jsonify({'result': True}), 200
    return error_handler(400)


@application.route("/collections", methods=['POST'])
def create_collection():
    """create collection

    Decorators:
        application

    Arguments:
        request.json.get("isOpen", None) -- open from post body
        request.json.get("userId", None) -- userId from post body
        request.json.get("collectionId", None) -- collectionId from post body

    Returns:
        Reponse -- http status
    """
    is_open = request.json.get("isOpen", request.json.get("open", True))
    user_id = request.json.get("userId", request.json.get("owner", None))
    collection_id = request.json.get("collectionId", None)
    if (user_id is None or collection_id is None):
        return error_handler(400)
    result = model_driver.create_collection(user_id, collection_id, is_open)
    if not result:
        return error_handler(404)
    return jsonify({'result': result}), 201


@application.route("/collections/<collection_id>", methods=['PUT'])
def update_open_collection(collection_id):
    """update collection

    Decorators:
        application

    Arguments:
        collectionId {string} -- collection_uid
        *args {[type]} -- [description]
        **kwargs {[type]} -- [description]

    Returns:
        Response -- http status
    """
    is_open = request.json.get("isOpen", request.json.get("open", True))
    result = model_driver.update_collection(collection_id, is_open)
    if result:
        return jsonify({'result': True}), 200
    return error_handler(400)


@application.route("/collections/<collectionId>", methods=['GET'])
@requires_auth
def get_collection_role(user_name, collectionId):
    """get role for collection

    Decorators:
        requires_auth
        application

    Arguments:
        user_name {string} -- user name
        collectionId {string} -- collectionId

    Returns:
        Response -- http status
    """
    result = model_driver.get_collection_role(user_name, collectionId)
    if result is None:
        return '', 200
    return result.upper(), 200


@application.route("/collections/<collection_id>", methods=['DELETE'])
def delete_collection(collection_id):
    """delete collection

    Decorators:
        application

    Arguments:
        collection_id {string} -- collection_uid
        *args {[type]} -- [description]
        **kwargs {[type]} -- [description]

    Returns:
        Response -- http status
    """
    if model_driver.delete_collection(collection_id):
        return jsonify({'result': True}), 200
    return error_handler(400)


@application.route("/services", methods=['POST'])
def create_service():
    """create service

    Decorators:
        application

    Arguments:
        isOpen -- from post body
        userId -- from post body
        serviceId -- from post body
        *arg {[type]} -- [description]
        **kwargs {[type]} -- [description]

    Returns:
        Response -- http status
    """
    is_open = request.json.get("isOpen", request.json.get("open", True))
    user_id = request.json.get("userId", request.json.get("owner", None))
    service_id = request.json.get("serviceId", None)
    if (user_id is None or service_id is None):
        return error_handler(400)
    result = model_driver.create_service(user_id, service_id, is_open)
    if(not result):
        return error_handler(404)
    return jsonify({'result': result}), 201


@application.route("/services/<serviceId>", methods=['GET'])
@requires_auth
def get_service_role(user_name, serviceId):
    """get service role

    Decorators:
        requires_auth
        application

    Arguments:
        user_name {string} -- user name
        serviceId {string} -- service_uid

    Returns:
        Response -- http status
    """
    result = model_driver.get_service_role(user_name, serviceId)
    if result is None:
        return '', 200
    return result.upper(), 200


@application.route("/services/<serviceId>", methods=['DELETE'])
def delete_service(serviceId):
    """delete service

    Decorators:
        application

    Arguments:
        serviceId {string} -- service_uid

    Returns:
        Response -- http status
    """
    result = model_driver.delete_service(serviceId)
    if result:
        return jsonify({'result': result}), 200
    return error_handler(400)


@application.route("/services/<serviceId>", methods=['PUT'])
def update_open_service(serviceId):
    """update open service

    Decorators:
        application

    Arguments:
        serviceId {string} -- service_uid

    Returns:
        Response -- http status
    """
    is_open = request.json.get("isOpen", request.json.get("open", True))
    if model_driver.update_service(serviceId, is_open):
        return jsonify({'result': True}), 200
    return error_handler(400)


def error_handler(status, message=None):
    return jsonify({'result': message}), status


if __name__ == "__main__":
    application.run()
