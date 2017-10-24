package com.smartcity.access_control;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class CollectionNode {
	@GraphId Long id;
	String collectionId;
	@SuppressWarnings("unused")

	private CollectionNode() {
	}

	public CollectionNode(String collectionId) {
		this.collectionId = collectionId;
	}
}
