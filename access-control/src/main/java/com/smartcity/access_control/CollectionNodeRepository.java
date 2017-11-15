package com.smartcity.access_control;

import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

public interface CollectionNodeRepository extends GraphRepository<CollectionNode> {
	CollectionNode findByCollectionId(String collectionId);
	@Query("match(u:UserNode)-[r:Role]->(c:CollectionNode) "
			+ "where r.role = \"OWNER\" return u.userName as userId,COLLECT(c.collectionId) as collections")
	List<Map<String,Object>> findAllCollectionAndOwner();
	
	@Query("match (u)-[r]->(c) where c.collectionId in {collectionId} return c.collectionId as collectionId,u.userName as userName")
	List<Map<String, Object>> findCollectionsAndOwner(@Param(value = "collectionId") String[] collectionId);
}
