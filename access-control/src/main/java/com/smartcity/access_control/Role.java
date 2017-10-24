package com.smartcity.access_control;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@RelationshipEntity(type = "Role")
public class Role {

	@GraphId
	Long id;

	private String role;
	@StartNode
	private UserNode user;
	@EndNode
	private CollectionNode collection;

	@SuppressWarnings("unused")
	private Role() {
	}

	public Role(String role, UserNode user, CollectionNode collection) {
		this.role = role;
		this.user = user;
		this.collection = collection;
	}

	public CollectionNode getCollection() {
		return collection;
	}

	public void setCollection(CollectionNode collection) {
		this.collection = collection;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

}
