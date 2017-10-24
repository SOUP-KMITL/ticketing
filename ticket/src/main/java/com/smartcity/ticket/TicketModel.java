package com.smartcity.ticket;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;

public class TicketModel {
	private String ticketId;
	private String userId;
	private String collectionId;
	private String role;
	private Date expire;
	private Date timstamp;
	public TicketModel() {

	}

	public TicketModel(String userId, String collectionId, String role) {
		Date date = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.DATE, 180);
		this.ticketId = DigestUtils.sha256Hex(userId + collectionId + date.getTime());
		this.userId = userId;
		this.collectionId = collectionId;
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

	public String getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
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


}
