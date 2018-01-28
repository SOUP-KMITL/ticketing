package com.smartcity.collection;

import java.util.Date;

import org.json.simple.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

public class CollectionModel {
	@Id
	@Indexed
	private String collectionId;

	private String collectionName;
	private String description;
	private String icon;
	private JSONObject endPoint;
	private String type;
	private int encryptionLevel;
	private String owner;
	private JSONObject example;
	private Date timestamp;
	private boolean isOpen;

	public CollectionModel() {
	}

	public CollectionModel(String id, String collectionName,String description,String icon, JSONObject endPoint, String type, int encryptionLevel,
			String owner, JSONObject example, boolean isOpen) {
		this.setCollectionId(id);
		this.collectionName = collectionName;
		this.isOpen = isOpen;
		this.description = description;
		this.icon = icon;
		this.endPoint = endPoint;
		this.type = type;
		if (encryptionLevel > 2 || encryptionLevel < 0) {
			this.encryptionLevel = 0;
		}
		this.encryptionLevel = encryptionLevel;
		this.owner = owner;
		this.example = example;
		this.setTimestamp(new Date());
	}

	public String getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public JSONObject getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(JSONObject endPoint) {
		this.endPoint = endPoint;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public JSONObject getExample() {
		return example;
	}

	public void setExample(JSONObject example) {
		this.example = example;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public void setOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public int getEncryptionLevel() {
		return encryptionLevel;
	}

	public void setEncryptionLevel(int encryptionLevel) {
		this.encryptionLevel = encryptionLevel;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
}
