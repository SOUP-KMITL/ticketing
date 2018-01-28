package com.smartcity.access_control;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class CollectionNode {
	@GraphId Long id;
	private String collectionId;
	private boolean isOpen;
	@SuppressWarnings("unused")
	private CollectionNode() {
	}

	public CollectionNode(String collectionId,boolean isOpen) {
		this.setCollectionId(collectionId);
		this.setOpen(isOpen);
	}

	public boolean isOpen() {
		return isOpen;
	}

	public void setOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	public String getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}
}
