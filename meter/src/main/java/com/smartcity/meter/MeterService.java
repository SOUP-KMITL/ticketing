package com.smartcity.meter;

import java.util.Date;
import java.util.UUID;

public class MeterService {
	private String meterId;
	private String userType;
	private String userId;
	private String serviceId;
	private Date timestamp;

	public MeterService() {
		this.setMeterId(UUID.randomUUID().toString());
		this.setTimestamp(new Date());
	}

	public MeterService(String userType,String userId,String serviceId) {
		this.setMeterId(UUID.randomUUID().toString());
		this.setUserType(userType);
		this.setUserId(userId);
		this.setServiceId(serviceId);
		this.setTimestamp(new Date());
	}

	public String getMeterId() {
		return meterId;
	}

	public void setMeterId(String meterId) {
		this.meterId = meterId;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

}