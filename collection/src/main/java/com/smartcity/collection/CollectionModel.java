package com.smartcity.collection;

import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "MetaData")
public class CollectionModel {
	@Id
	@Indexed
	private String collectionId;
	private String collectionName;
	private String description;
	private String thumbnail;
	@JsonIgnore
	private String thumbnailBase64;
	@JsonIgnore
	private JSONObject endPoint;
	private String type;
	private String category;
	private JSONArray columns;
	private int encryptionLevel;
	private String owner;
	private JSONObject example;
	private Date createdAt;
	private boolean isOpen;

	public CollectionModel() {
	}

	public CollectionModel(String id, String collectionName, String description, String thumbnailBase64, JSONObject endPoint,
			String type,String category, JSONArray columns, int encryptionLevel, String owner, JSONObject example, boolean isOpen) {
		this.setCollectionId(id);
		this.collectionName = collectionName;
		this.isOpen = isOpen;
		this.description = description;
		this.thumbnailBase64 = null;
		this.thumbnail = null;
		this.endPoint = endPoint;
		this.type = type;
		this.category = category;
		this.columns = columns;
		if (encryptionLevel > 2 || encryptionLevel < 0) {
			this.encryptionLevel = 0;
		}
		this.encryptionLevel = encryptionLevel;
		this.owner = owner;
		this.example = example;
		this.setCreatedAt(new Date());
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

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String icon) {
		this.thumbnail = icon;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public JSONArray getColumns() {
		return columns;
	}

	public void setColumns(JSONArray columns) {
		this.columns = columns;
	}

	public String getThumbnailBase64() {
		return thumbnailBase64;
	}

	public void setThumbnailBase64(String thumbnailBase64) {
		this.thumbnailBase64 = thumbnailBase64;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
}
