package com.smartcity.access_control;

import org.springframework.data.neo4j.repository.GraphRepository;

public interface CollectionNodeRepository extends GraphRepository<CollectionNode> {
	CollectionNode findByCollectionId(String collectionId);
}
