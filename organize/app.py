from flask import Flask, jsonify, request
from flask_mongoengine import MongoEngine
from datetime import datetime

app = Flask(__name__)
db = MongoEngine(app)
app.config['MONGODB_SETTINGS'] = {
    'db': 'organize',
    'host': 'localhost',
    'port': 27017
}


class Member(db.EmbeddedDocument):
    name = db.StringField(max_length=50)
    role = db.StringField(choices=('owner', 'member'))
    create_at = db.DateTimeField(datetime.utcnow)


class Organize(db.Document):
    name = db.StringField(required=True, primary_key=True, max_length=100)
    description = db.StringField(max_length=200)
    members = db.ListField(db.EmbeddedDocumentField(Member))
    create_at = db.DateTimeField(datetime.utcnow)


@app.route("/", methods=['GET', 'POST'])
def index_org():
    if request.method == 'GET':
        return jsonify(Organize.objects()), 200
    elif request.method == 'POST':
        json = request.get_json()
        name = json.get('name', None)
        if name is None:
            return '', 400
        if len(Organize.objects(name__iexact=name)) != 0:
            return '', 409
        description = json.get('description', None)
        org = Organize(name=name, description=description)
        org.save()
        return '', 201


@app.route('/<org_name>', methods=['GET', 'PUT', 'DELETE'])
def spec_org(org_name):
    org = Organize.objects(name=org_name).first()
    if org is None:
        return '', 404
    if request.method == 'GET':
        return jsonify(org)
    elif request.method == 'PUT':
        description = request.get_json().get('description')
        org.description = description
        org.save()
    else:
        org.delete()
    return '', 200


@app.route('/<org_name>/members', methods=['POST'])
def add_member(org_name):
    org = Organize.objects(name=org_name).first()
    if org is None:
        return '', 404
    if request.method == 'POST':
        try:
            member_name = request.get_json().get('name', None)
            if member_name is None:
                return '', 400
            member_role = request.get_json().get('role', 'member')
            member = Member(name=member_name, role=member_role)
            if in_member(org.members, member_name) == -1:
                org.members.append(member)
                org.save()
                return '', 201
            return '', 409
        except :
            return '', 400
    return '', 200


@app.route('/<org_name>/members/<member_name>', methods=['PUT', 'DELETE'])
def update_member(org_name, member_name):
    org = Organize.objects(name=org_name).first()
    if org is None:
        return '', 404
    member_index = in_member(org.members, member_name)
    if member_index == -1:
        return '', 404
    if request.method == 'PUT':
        role = request.get_json().get('role')
        tmp = org.members[member_index]
        tmp.role = role
        org.members[member_index] = tmp
        org.save()
    else:
        del org.members[member_index]
        org.save()
    return '', 200


def in_member(members, member_name):
    for i, m in enumerate(members):
        if m.name.lower() == member_name.lower():
            return i
    return -1


app.run()
