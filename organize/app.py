from json import dumps
from os import getenv
from time import time

import requests
from flask import Flask, jsonify, request
from jsonmerge import merge
from py2neo import Graph
from py2neo.ogm import GraphObject, Property, RelatedFrom, RelatedTo

import auth

app = Flask(__name__)
graph = Graph(host="localhost")
USER_URL = getenv('USER_URL', None)


class Organize(GraphObject):
    __primarykey__ = "name"

    name = Property()
    description = Property()
    createAt = Property()

    owner = RelatedFrom("Person", "OWNER")
    member = RelatedFrom("Person", "MEMBER")

    def to_json(self):
        return {
            'name': self.name,
            'description': self.description,
            'createAt': self.createAt
        }

    def get_member(self):
        return{
            'owner': [owner.to_json() for owner in self.owner],
            'member': [member.to_json() for member in self.member]
        }


class Person(GraphObject):
    __primarykey__ = "name"

    name = Property()
    createAt = Property()

    member_in = RelatedTo(Organize)
    own = RelatedTo(Organize)

    def to_json(self):
        return {
            'name': self.name,
            'createAt': self.createAt
        }
    def get_organize(self):
        return {
            'own': [org.to_json() for org in self.own],
            'member_in': [org.to_json() for org in self.member_in]
        }


@app.route("/healthz", methods=['GET'])
def healthz():
    return '', 200


@app.route("/", methods=['GET', 'POST'])
# @auth.requires_auth
def index_org(user_name=None):
    user_name = 'test'
    if request.method == 'GET':
        # orgs = [merge(org.to_json(), org.get_member())
        #         for org in Organize.match(graph)]
        # return jsonify(orgs), 200
        member = [merge(per.to_json(), per.get_organize())
                for per in Person.match(graph)]
        return jsonify(member),200
    elif request.method == 'POST':
        name = request.json.get('name', None)
        if name is None:
            return '', 400
        orgs = graph.nodes.match("Organize", name=name).first()
        if not orgs is None:
            return '', 409
        owner = Person()
        owner.name = user_name
        org = Organize()
        org.name = name
        org.description = request.json.get('description', None)
        org.createAt = time()
        org.owner.add(owner)
        owner.own.add(org)
        graph.push(org)
        graph.push(owner)
        return '', 201


@app.route('/<org_name>', methods=['GET', 'PUT', 'DELETE'])
# @auth.requires_auth
def spec_org(user_name, org_name):
    return '', 200


@app.route('/<org_name>/members', methods=['GET', 'POST'])
# @auth.requires_auth
def add_member(user_name, org_name):
    return '', 200


@app.route('/<org_name>/members/<member_name>', methods=['PUT', 'DELETE'])
# @auth.requires_auth
def update_member(user_name, org_name, member_name):
    return '', 200


if __name__ == "__main__":
    app.run()
