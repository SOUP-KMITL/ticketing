package com.smartcity.collection;

import java.util.ArrayList;
import java.util.Date;

import org.json.simple.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

public class CollectionModel {
	@Id
	@Indexed
	private String collectionId;
	
	private String collectionName;
	private String endPoint;
	private ArrayList<String> tags;
	private JSONObject example;
	private Date timestamp;
	private boolean isOpen;

	public CollectionModel() {
	}

	public CollectionModel(String id,String collectionName, String endPoint, ArrayList<String> tags,
			JSONObject example, boolean isOpen) {
		this.setCollectionId(id);
		this.collectionName = collectionName;
		if (endPoint.equals("local")) {
			endPoint += ":" + getCollectionId();
		}
		this.isOpen = isOpen;
		this.endPoint = endPoint;
		this.tags = tags;
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

	public String getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(String endPoint) {
		this.endPoint = endPoint;
	}

	public ArrayList<String> getTag() {
		return tags;
	}

	public void setTag(ArrayList<String> tags) {
		this.tags = tags;
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
}
