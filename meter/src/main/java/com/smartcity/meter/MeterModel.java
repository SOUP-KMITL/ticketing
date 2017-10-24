package com.smartcity.meter;

import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;

public class MeterModel {
	private String meterId;
	private String userId;
	private String collectionId;
	private String type;
	private boolean isPaid;
	private double credit;
	private Date timestamp;

	public MeterModel() {
	}

	public MeterModel(String userId, String collectionId,String type, double credit) {
		this.setMeterId(DigestUtils.sha256Hex(userId + collectionId + (new Date().getTime())));
		this.setUserId(userId);
		this.setCollectionId(collectionId);
		this.setType(type);
		this.setPaid(false);
		this.setCredit(credit);
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

	public boolean isPaid() {
		return isPaid;
	}

	public void setPaid(boolean isPaid) {
		this.isPaid = isPaid;
	}

	public double getCredit() {
		return credit;
	}

	public void setCredit(double credit) {
		this.credit = credit;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
