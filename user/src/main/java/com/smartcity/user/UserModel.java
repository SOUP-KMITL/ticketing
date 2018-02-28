package com.smartcity.user;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.hash.Hashing;

@JsonIgnoreProperties(ignoreUnknown = true)

public class UserModel {
	@Id
	@Indexed
	private String userId;
	private String userName;
	private String password;
	private String firstName;
	private String lastName;
	private String thumbnail;
	private String email;
	private String accessToken;
	private Date createdAt;

	public UserModel() {
		this.setUserId(userName);
		this.setCreatedAt(new Date());
	}

	public UserModel(String userName, String password, String firstName,String lastName,String email) {
		this.setUserId(userName);
		this.setUserName(userName);
		this.setPassword(password);
		this.setFirstName(firstName);
		this.setPassword(password);
		this.setThumbnail(null);
		this.setAccessToken(null);
		this.setEmail(email);
		this.setCreatedAt(new Date());
	}

	public String generateAccessToken() {
		String str = this.getUserName();
		str += new Date().getTime();
		String sha256hex = Hashing.sha256().hashString(str, StandardCharsets.UTF_8).toString();
		return sha256hex;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		String hashPass = UpdatableBCrypt.hash(password);
		this.password = hashPass;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		if (createdAt == null) {
			createdAt = new Date();
		}
		this.createdAt = createdAt;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userName) {
		this.userId = UUID.randomUUID().toString();
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String profilePicture) {
		this.thumbnail = profilePicture;
	}
}
