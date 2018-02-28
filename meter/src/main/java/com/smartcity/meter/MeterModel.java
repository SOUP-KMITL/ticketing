package com.smartcity.meter;

import java.util.Date;
import java.util.UUID;

public class MeterModel {
	private String meterId;
	private String userType;
	private String userId;
	private String collectionId;
	private boolean isOpen;
	private Date timestamp;
	private String type;
	private int record;
	private int size;

	public MeterModel() {
	}

	public MeterModel(String userType,String userId, String collectionId,boolean isOpen,String type ,int record, int size) {
		this.setMeterId(UUID.randomUUID().toString());
		this.setUserType(userType);
		this.setUserId(userId);
		this.setCollectionId(collectionId);
		this.setOpen(isOpen);
		this.setType(type);
		this.setRecord(record);
		this.setSize(size);
		this.setTimestamp(new Date());
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getMeterId() {
		return meterId;
	}

	public void setMeterId(String meterId) {
		this.meterId = meterId;
	}

	public int getRecord() {
		return record;
	}

	public void setRecord(int record) {
		this.record = record;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public void setOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}
	
}
