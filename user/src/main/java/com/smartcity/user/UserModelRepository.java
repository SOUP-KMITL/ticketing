package com.smartcity.user;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserModelRepository extends MongoRepository<UserModel, String> {
	public List<UserModel> findByuserName(String name);
	public List<UserModel> findByAccessToken(String accesstoken); 
	public List<UserModel> findByUserId(String userId);
}
