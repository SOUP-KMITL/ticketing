import hashlib
import uuid

from neomodel import (BooleanProperty, RelationshipFrom, RelationshipTo,
                      StringProperty, StructuredNode, StructuredRel, config,
                      db)

# config.DATABASE_URL = 'bolt://none:none@neo4j-neo4j-core-0.neo4j-neo4j.test.svc.cluster.local:7687'
config.DATABASE_URL = 'bolt://none:none@neo4j-server:7687'


class Role(StructuredRel):
    type = StringProperty()


class User(StructuredNode):
    uid = StringProperty(unique_index=True)
    name = StringProperty(unique_index=True)
    service = RelationshipTo('Service', "Role", model=Role)
    collection = RelationshipTo('Collection', "Role", model=Role)


class Service(StructuredNode):
    uid = StringProperty(unique_index=True)
    open = BooleanProperty()
    user = RelationshipFrom("User", "Role", model=Role)
    collection = RelationshipTo('Collection', "Role", model=Role)


class Collection(StructuredNode):
    uid = StringProperty(unique_index=True)
    open = BooleanProperty()
    service = RelationshipFrom(Service, "Role", model=Role)
    user = RelationshipFrom(User, "Role", model=Role)


def create_user(user_uid, user_name):
    try:
        User.nodes.get(uid=user_uid)
        return False
    except:
        User(uid=user_uid, name=user_name).save()
        """user - read -> service"""
        db.cypher_query(
            "MATCH (u:User),(s:Service) where s.open = true and u.name = {user_name} create (u)-[:Role {type:'read'}]->(s)",
            {'user_name': user_name})
        """user - read -> collection"""
        db.cypher_query(
            "MATCH (u:User),(c:Collection) where c.open = true and u.name = {user_name} create (u)-[:Role {type:'read'}]->(c)",
            {'user_name': user_name})
        return True


def create_service(user_name, service_uid, open):
    try:
        user = None
        try:
            user = User.nodes.get(name=user_name)
        except:
            user = User.nodes.get(uid=user_name)
        service = Service(uid=service_uid, open=open).save()
        user.service.connect(service, {"type": "owner"})
        if open:
            """user - read -> service"""
            db.cypher_query(
                "MATCH (u:User),(s:Service) where s.uid = {service_uid} and not (u.name = {user_name} or u.uid = {user_name}) create (u)-[:Role {type:'read'}]->(s)",
                {'service_uid': service_uid, 'user_name': user_name})
        """service - read -> collection"""
        db.cypher_query(
            "MATCH (s:Service),(c:Collection) where s.uid = {service_uid} and c.open = true create (s)-[:Role {type:'read'}]->(c)",
            {'service_uid': service_uid})
        return True
    except Exception:
        return False


def create_collection(user_name, collection_uid, open):
    try:
        user = None
        try:
            user = User.nodes.get(name=user_name)
        except:
            user = User.nodes.get(uid=user_name)
        collection = Collection(
            uid=collection_uid, open=open).save()
        user.collection.connect(collection, {'type': 'owner'})
        if open:
            """user - read -> collection"""
            db.cypher_query(
                "MATCH (u:User),(c:Collection) where not (u.name = {user_name} or u.uid = {user_name}) and c.uid = {collection_uid} create (u)-[:Role {type:'read'}]->(c)",
                {'collection_uid': collection_uid, 'user_name': user_name})
            """service - read -> collection"""
            db.cypher_query(
                "MATCH (s:Service),(c:Collection) where c.uid = {collection_uid} create (s)-[:Role {type:'read'}]->(c)",
                {'collection_uid': collection_uid})

        return True
    except Exception:
        return False


def create_relationship_user_service(user_name, service_uid, role_type='read'):
    try:
        db.cypher_query(
            "MATCH (u:User),(s:Service) where u.name={user_name} and s.uid={service_uid} create (u)-[r:Role{type:{role_type}}]->(s)",
            {'user_name': user_name, 'service_uid': service_uid, 'role_type': role_type})
        return True
    except Exception:
        return False


def create_relationship_service_collection(service_uid, collection_uid, role_type='read'):
    try:
        db.cypher_query(
            "MATCH (s:Service),(s:Collectiond) where s.uid={service_uid} and c.uid={collection_uid} create (s)-[r:Role{type:{role_type}}]->(c)",
            {'service_uid': service_uid, 'collection_uid': collection_uid, 'role_type': role_type})
        return True
    except Exception:
        return False


def create_relationship_user_collection(user_name, collection_uid, role_type='read'):
    try:
        db.cypher_query(
            "MATCH (u:User),(s:Collectiond) where u.name={user_name} and c.uid={collection_uid} create (s)-[r:Role{type:{role_type}}]->(c)",
            {'service_uid': user_name, 'collection_uid': collection_uid, 'role_type': role_type})
        return True
    except Exception:
        return False


def get_collection_role(user_name, collection_uid):
    try:
        results = db.cypher_query(
            "MATCH (u:User)-[r:Role]->(c:Collection) where (u.name={user_name} or u.uid = {user_name}) and c.uid = {collection_uid} return r.type",
            {'user_name': user_name, 'collection_uid': collection_uid})
        if len(results) > 0:
            """ return role"""
            return results[0][0][0]
        else:
            return False
    except Exception:
        return False


def get_service_role(user_name, service_uid):
    try:
        results = db.cypher_query(
            "MATCH (u:User)-[r:Role]->(s:Service) where (u.name={user_name} or u.uid = {user_name}) and s.uid={service_uid} return r.type",
            {'user_name': user_name, 'service_uid': service_uid})
        if len(results) > 0:
            """ return role"""
            return results[0][0][0]
        else:
            return False
    except Exception:
        return False


def get_service_collection_role(service_uid, collection_uid):
    try:
        results = db.cypher_query(
            "MATCH (s:Service)-[r:Role]->(c:Collection) where s.uid={service_uid} and c.uid={collection_uid} return r.type",
            {'service_uid': service_uid, 'collection_uid': collection_uid})
        if len(results) > 0:
            """ return role"""
            return results[0][0][0]
        else:
            return False
    except Exception:
        return False

def get_all_user_role_collection(uid):
    try:
        is_open = db.cypher_query("MATCH (c:Collection) where c.uid = {uid} return c.open as open",{'uid': uid})
        if(is_open[0][0][0]==True):
            results = db.cypher_query(
                "MATCH (u:User)-[r:Role]->(c:Collection) where c.uid = {uid} and r.type in ['contributor','owner'] with {user_name:u.name,role:r.type} as Tmp return collect(Tmp);",
                {'uid': uid})
        else:
            results = db.cypher_query(
                "MATCH (u:User)-[r:Role]->(c:Collection) where c.uid = {uid} with {user_name:u.name,role:r.type} as Tmp return collect(Tmp);",
                {'uid': uid})
        if len(results) > 0:
            """ return user_name|role.type"""
            return results[0][0][0]
        else:
            return []
    except Exception :
        return []

def get_all_user_role_service(uid):
    try:
        is_open = db.cypher_query("MATCH (c:Service) where c.uid = {uid} return c.open as open",{'uid': uid})
        if(is_open[0][0][0]==True):
            results = db.cypher_query(
                "MATCH (u:User)-[r:Role]->(c:Service) where c.uid = {uid} and r.type in ['contributor','owner'] with {user_name:u.name,role:r.type} as Tmp return collect(Tmp);",
                {'uid': uid})
        else:
            results = db.cypher_query(
                "MATCH (u:User)-[r:Role]->(c:Service) where c.uid = {uid} with {user_name:u.name,role:r.type} as Tmp return collect(Tmp);",
                {'uid': uid})
        if len(results) > 0:
            """ return user_name|role.type"""
            return results[0][0][0]
        else:
            return []
    except Exception :
        return []

def get_all_service(user_name, role_type="owner"):
    role_type = role_type.lower()
    results = db.cypher_query(
        "MATCH (u:User)-[r:Role]->(s:Service) where u.name={user_name} and r.type={role_type} return s.uid",
        {'user_name': user_name, 'role_type': role_type})
    if len(results) > 0:
        """ return [role]"""
        return list(map(lambda x: x[0], results))
    else:
        return None


def get_all_collection(user_name, role_type="owner"):
    role_type = role_type.lower()
    results = db.cypher_query(
        "MATCH (u:User)-[r:Role]->(c:Collection) where u.name={user_name} and r.type={role_type} return c.uid",
        {'user_name': user_name, 'role_type': role_type})
    if len(results) > 0:
        """ return [collection_uid]"""
        return list(map(lambda x: x[0], results))
    else:
        return None


def get_all_service_collection(service_uid, role_type="contributor"):
    role_type = role_type.lower()
    results = db.cypher_query(
        "MATCH (s:Service)-[r:Role]->(c:Collection) where s.uid={service_uid} and r.type={role_type} return c.uid",
        {'service_uid': service_uid, 'role_type': role_type})
    if len(results) > 0:
        """ return [collection_uid]"""
        return list(map(lambda x: x[0], results))
    else:
        return None


def update_user_collection(user_name, collection_uid, role_type):
    try:
        user = User.nodes.get(name=user_name)
        collection = Collection.nodes.get(uid=collection_uid)
        try:
            rel = user.collection.relationship(collection)
            rel.type = role_type.lower()
            rel.save()
        except:
            user.collection.connect(collection, {'type': role_type.lower()})
        return True
    except Exception:
        return False


def update_user_service(user_name, service_uid, role_type):
    try:
        user = User.nodes.get(name=user_name)
        service = Service.nodes.get(uid=service_uid)
        try:
            rel = user.service.relationship(service)
            rel.type = role_type.lower()
            rel.save()
        except:
            user.service.connect(service, {'type': role_type.lower()})
        return True
    except Exception:
        return False


def update_service_collection(service_uid, collection_uid, role_type):
    try:
        service = Service.nodes.get(uid=service_uid)
        collection = Collection.nodes.get(uid=collection_uid)
        try:
            rel = service.collection.relationship(collection)
            rel.type = role_type.lower()
            rel.save()
        except:
            service.collection.connect(collection, {'type': role_type.lower()})
        return True
    except Exception:
        return False


def update_service(service_uid, open):
    try:
        service = Service.nodes.get(uid=service_uid)
        service.open = open
        service.save()
        if open:
            db.cypher_query("MATCH (u:User),(s:Service) where s.uid = {service_uid} and not (u)-[:Role]->(s) create (u)-[:Role{type:'read'}]->(s)",
                            {"service_uid": service_uid})
            return True
        else:
            db.cypher_query("MATCH (u:User)-[r:Role]->(s:Service) where s.uid = {service_uid} and not (r.type = 'owner') delete r",
                            {"service_uid": service_uid})
            return True
    except Exception:
        return False


def update_collection(collection_uid, open):
    try:
        if open:
            col = Collection.nodes.get(uid=collection_uid)
            col.open = True
            col.save()
            db.cypher_query("MATCH (s:Service),(c:Collection) where c.uid = {collection_uid} and not (s)-[:Role]->(c) create (s)-[:Role{type:'read'}]->(c)",
                            {"collection_uid": collection_uid})
            db.cypher_query("MATCH (u:User),(c:Collection) where c.uid = {collection_uid} and not (u)-[:Role]->(c) create (u)-[:Role{type:'read'}]->(c)",
                            {"collection_uid": collection_uid})
            return True
        else:
            col = Collection.nodes.get(uid=collection_uid)
            col.open = False
            col.save()
            db.cypher_query("MATCH (s:Service)-[r:Role]->(c:Collection) where c.uid = {collection_uid} and not (r.type = 'owner') delete r",
                            {"collection_uid": collection_uid})
            db.cypher_query("MATCH (u:User)-[r:Role]->(c:Collection) where c.uid = {collection_uid} and not (r.type = 'owner') delete r",
                            {"collection_uid": collection_uid})
            return True
    except Exception:
        return False


def delete_collection(collection_uid):
    try:
        db.cypher_query("MATCH (c:Collection) where c.uid = {collection_uid} detach delete c",
                        {"collection_uid": collection_uid})
        return True
    except Exception:
        return False


def delete_service(service_uid):
    try:
        db.cypher_query("MATCH (s:Service) where s.uid = {service_uid} detach delete s",
                        {"service_uid": service_uid})
        return True
    except Exception:
        return False


def delete_user(user_name):
    try:
        db.cypher_query("MATCH p=(u:User)-[r:Role]->() where u.name = {user_name} and r.type = 'owner' detach delete p",
                        {"user_name": user_name})
        db.cypher_query("MATCH (u:User) where u.name = {user_name} delete u",
                        {"user_name": user_name})
        return True
    except Exception:
        return False


def delete_relationship_user_service(user_name, service_uid):
    try:
        db.cypher_query(
            "match (u:User)-[r:Role]->(s:Service) where u.name={user_name} and not (r.type ='owner') and s.uid = {service_uid} delete r",
            {'user_name': user_name, 'service_uid': service_uid})
        return True
    except Exception:
        return False


def delete_relationship_service_collection(service_uid, collection_uid):
    try:
        db.cypher_query(
            "match (s:Service)-[r:Role]->(c:Collection) where s.uid={service_uid} and not (r.type ='owner') and c.uid = {collection_uid} delete r",
            {'service_uid': service_uid, 'collection_uid': collection_uid})
        return True
    except Exception:
        return False


def delete_relationship_user_collection(user_name, collection_uid):
    try:
        db.cypher_query(
            "match (u:User)-[r:Role]->(c:Collection) where u.name={user_name} and not (r.type ='owner') and c.uid = {collection_uid} delete r",
            {'user_name': user_name, 'collection_uid': collection_uid})
        return True
    except Exception:
        return False


def delete_all():
    db.cypher_query("MATCH (n) detach delete n")
