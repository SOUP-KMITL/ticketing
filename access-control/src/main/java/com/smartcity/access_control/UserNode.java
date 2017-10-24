package com.smartcity.access_control;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class UserNode {
	@GraphId
	Long id;
	String userId;

	@SuppressWarnings("unused")
	private UserNode() {

	}

	public UserNode(String userId) {
		this.userId = userId;
	}

	public List<Role> getRoles() {
		return roles;
	}

	public void addRole(Role roles) {
		if(this.roles == null) {
			this.roles = new ArrayList<>();
		}
		this.roles.add(roles);
	}

	@Relationship(type = "Role")
	private List<Role> roles;
}
