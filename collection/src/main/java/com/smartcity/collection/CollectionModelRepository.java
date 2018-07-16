package com.smartcity.collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface CollectionModelRepository extends PagingAndSortingRepository<CollectionModel, String> {
	@Query(value = "{}", fields = "{ endPoint : 0}")
	Page<CollectionModel> findAllNoEndPoint(Pageable pageable);

	@Query(value = "{$and :[{ owner : {$regex: ?0}},{ type : {$regex: ?1 }},{ collectionName : {$regex:?2}} , {_id : {$regex:?3}},{isOpen : {$ne:?4}}]}", fields = "{ endPoint : 0,thumbnailBase64:0}")
	Page<CollectionModel> findAllCustom(String owner, String type, String collectionName, String collectionId,
			Boolean open, Pageable pageable);

	CollectionModel findByCollectionId(String collectionId);
	CollectionModel findByCollectionNameAndOwner(String collectionName,String owner);
	Page<CollectionModel> findByCollectionNameLikeOrOwnerLikeOrDescriptionLikeOrCategoryLike(String collectionName,String owner,String description,String category,Pageable pageable);
}
