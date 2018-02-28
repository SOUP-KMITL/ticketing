package com.smartcity.ticket;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class TicketModel {
	private String ticketId;
	private String userType;
	private String userId;
	private String targetId;
	private String role;
	private Date expire;
	private Date timstamp;

	public TicketModel() {

	}

	public TicketModel(String userType,String userId, String targetId, String role) {
		Date date = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.DATE, 180);
		this.ticketId = UUID.randomUUID().toString();
		this.userId = userId;
		this.userType = userType;
		this.setTargetId(targetId);
		this.role = role;
		this.expire = c.getTime();
		this.timstamp = date;
	}

	public TicketModel(String userId,String userType, String targetId,  String role, int expire) {
		Date date = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.DATE, expire);
		this.ticketId = UUID.randomUUID().toString();
		this.userId = userId;
		this.userType = userType;
		this.setTargetId(targetId);
		this.role = role;
		this.expire = c.getTime();
		this.timstamp = date;
	}

	public String getTicketId() {
		return ticketId;
	}

	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}


	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Date getTimstamp() {
		return timstamp;
	}

	public void setTimstamp(Date timstamp) {
		this.timstamp = timstamp;
	}

	public Date getExpire() {
		return expire;
	}

	public void setExpire(Date expire) {
		this.expire = expire;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	

}
