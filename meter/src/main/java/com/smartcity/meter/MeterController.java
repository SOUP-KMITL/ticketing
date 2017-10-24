package com.smartcity.meter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
	public @ResponseBody List<MeterModel> getAll(String userId, String collectionId, Boolean paid) {
		Query query = new Query();
		Criteria criteria = new Criteria();

		if (userId != null) {
			criteria = Criteria.where("userId").is(userId);
		}
		if (collectionId != null) {
			criteria.andOperator(Criteria.where("collectionId").is(collectionId));
		}
		if (paid != null) {
			criteria.andOperator(Criteria.where("isPaid").is(paid));
		}
		query.addCriteria(criteria);
		return mongoTemplate.find(query, MeterModel.class);

	}

	@SuppressWarnings("rawtypes")
	@PostMapping("")
	public ResponseEntity newMeter(@RequestBody MeterModel meter) {
		mongoTemplate
				.insert(new MeterModel(meter.getUserId(), meter.getCollectionId(), meter.getType(), meter.getCredit()));
		return new ResponseEntity(HttpStatus.CREATED);
	}

	@SuppressWarnings("rawtypes")
	@PutMapping("/{meterId}")
	public ResponseEntity updateMeter(@PathVariable String meterId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("meterId").is(meterId));
		MeterModel meter = mongoTemplate.findOne(query, MeterModel.class);
		meter.setPaid(true);
		mongoTemplate.save(meter);
		return new ResponseEntity<>(HttpStatus.OK);

	}
	// @GetMapping("/test")
	// public void queryMethod(@RequestParam MultiValueMap<String, Object> multiMap)
	// {
	// multiMap.forEach((s,o)->System.out.println(s+o.get(0)));
	// }
}
