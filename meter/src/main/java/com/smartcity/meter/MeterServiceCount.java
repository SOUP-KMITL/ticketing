package com.smartcity.meter;

import java.util.Date;

public class MeterServiceCount {
	private String serviceId;
	private int count;
	private Date timestamp;
	public MeterServiceCount() {
		this.setTimestamp(new Date());
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public String getServiceId() {
		return serviceId;
	}
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
}
