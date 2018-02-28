package com.smartcity.meter;

import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.MongoClient;
@CrossOrigin
@RestController
public class MeterController {
	@Autowired
	private MongoTemplate mongoTemplate = new MongoTemplate(
			new SimpleMongoDbFactory(new MongoClient("mongo"), "MeterModel"));
	private final String USER_URL = System.getenv("USER_URL");
	@GetMapping("")
	public @ResponseBody List<MeterModel> getAll(String userType,String userId, String collectionId, String[] collectionIds,
			Boolean open, Long timestamp,Pageable pageable) {
		Query query = new Query();
		Criteria criteria = new Criteria();
		if (userType != null) {
			criteria = criteria.and("userType").is(userType);
		}
		if (userId != null) {
			criteria = criteria.and("userId").is(userId);
		}
		if (open != null) {
			criteria = criteria.and("isOpen").is(open);
		}
		if (collectionId != null) {
			criteria = criteria.and("collectionId").is(collectionId);
		}
		if (timestamp != null) {
			criteria = criteria.and("timestamp").gt(new Date(timestamp));
		}
		if (collectionIds != null) {
			criteria = criteria.and("timestamp").in((Object[]) collectionIds);
		}
		query.addCriteria(criteria).with(new Sort(Sort.Direction.DESC, "timestamp"));
		query.with(pageable);
		return mongoTemplate.find(query, MeterModel.class);

	}

	@SuppressWarnings("rawtypes")
	@PostMapping("")
	public ResponseEntity newMeter(@RequestBody MeterModel meter) {
		mongoTemplate.insert(new MeterModel(meter.getUserType(),meter.getUserId(), meter.getCollectionId(), meter.isOpen(), meter.getType(),
				meter.getRecord(), meter.getSize()));
		return new ResponseEntity(HttpStatus.CREATED);
	}
	private org.json.JSONObject getUserByToken(String userToken) {
		try {
			HttpResponse<JsonNode> res = Unirest.get(USER_URL).queryString("token", userToken).asJson();
			return res.getBody().getArray().getJSONObject(0);
		} catch (UnirestException | JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

}
