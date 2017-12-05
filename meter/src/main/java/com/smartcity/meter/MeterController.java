package com.smartcity.meter;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.MongoClient;

@RestController
public class MeterController {
	@Autowired
	private MongoTemplate mongoTemplate = new MongoTemplate(
			new SimpleMongoDbFactory(new MongoClient("mongo"), "MeterModel"));

	@GetMapping("")
	public @ResponseBody List<MeterModel> getAll(String userId, String collectionId, String[] collectionIds,
			Boolean open, Long timestamp) {
		Query query = new Query();
		Criteria criteria = new Criteria();
		if (userId != null) {
			criteria = Criteria.where("userId").is(userId);
		}
		if (open != null) {
			criteria = Criteria.where("isOpen").is(open);
		}
		if (collectionId != null) {
			criteria.andOperator(Criteria.where("collectionId").is(collectionId));
		}
		if (timestamp != null) {
			criteria.andOperator(Criteria.where("timestamp").gt(new Date(timestamp)));
		}
		if (collectionIds != null) {
			criteria.andOperator(Criteria.where("collectionId").in((Object[]) collectionIds));
		}
		query.addCriteria(criteria).with(new Sort(Sort.Direction.DESC, "timestamp"));
		return mongoTemplate.find(query, MeterModel.class);

	}

	@SuppressWarnings("rawtypes")
	@PostMapping("")
	public ResponseEntity newMeter(@RequestBody MeterModel meter) {
		mongoTemplate.insert(new MeterModel(meter.getUserId(), meter.getCollectionId(), meter.isOpen(), meter.getType(),
				meter.getRecord(), meter.getSize()));
		return new ResponseEntity(HttpStatus.CREATED);
	}

}
