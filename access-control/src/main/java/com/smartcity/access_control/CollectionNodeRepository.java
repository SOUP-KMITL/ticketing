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
	List<Map<String, Object>> findAllCollectionAndOwner();

	@Query("match (u)-[r]->(c) where c.collectionId in {collectionId} return c.collectionId as collectionId,u.userName as userName")
	List<Map<String, Object>> findCollectionsAndOwner(@Param(value = "collectionId") String[] collectionId);

	@Query("match ()-[]->(c:CollectionNode) where c.isOpen = true  return c")
	List<CollectionNode> findAllOpenCollection();

	@Query("match (u)-[r]->(c:CollectionNode) where c.collectionId = {collectionId} and r.role <> \"OWNER\" delete r ")
	void deleteAllNotOwnerRelationship(@Param(value = "collectionId") String collectionId);
	@Query("MATCH (u:UserNode),(c:CollectionNode) WHERE NOT (u)-[:Role]->(c) and c.collectionId = {collectionId} create (u)-[r:Role{role:\"READ\"}]->(c)")
	void createAllReadRelationship(@Param(value = "collectionId") String collectionId);
}
