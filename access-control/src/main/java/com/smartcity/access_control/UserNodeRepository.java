package com.smartcity.access_control;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

public interface UserNodeRepository extends GraphRepository<UserNode> {
	UserNode findByUserId(String userId);

	@Query("match(u:UserNode)-[r:Role]->(c:CollectionNode) "
			+ "where u.userId = {userId} and r.role = {type} and c.collectionId={collectionId}" + "return true")
	boolean checkRule(@Param(value = "userId") String userId, @Param(value = "type") String type,
			@Param(value = "collectionId") String collectionId);

	@Query("match(u:UserNode)-[r:Role]->(c:CollectionNode) "
			+ "where u.userId = {userId} and c.collectionId={collectionId}" + "return r")
	Role findRole(@Param(value = "userId") String userId, @Param(value = "collectionId") String collectionId);
	@Query("match (u)-[r]->(c) where u.userName = {userId} and r.role = \"OWNER\" return c.collectionId")
	List<String> findOwnedByUser(@Param(value = "userName") String userName);
}
