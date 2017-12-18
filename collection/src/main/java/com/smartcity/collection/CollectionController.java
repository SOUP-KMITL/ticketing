package com.smartcity.collection;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.MongoClient;
import com.smartcity.collection.SecurityModel;

@RestController
public class CollectionController {
	private MongoTemplate mongoTemplate = new MongoTemplate(
			new SimpleMongoDbFactory(new MongoClient("mongo"), "CollectionModel"));
	private String METADATA = "MetaData";

	@GetMapping("/")
	public List<Object> getMeta(String collectionName, String collectionId, String type, Boolean open,
			String owner) {
		Criteria criteria = new Criteria();
		if (collectionName != null) {
			criteria = criteria.and("collectionName").is(collectionName);
		}
		if (collectionId != null) {
			criteria = criteria.and("collectionId").is(collectionId);
		}
		if (type != null) {
			criteria = criteria.and("type").is(type);
		}
		if (open != null) {
			criteria = criteria.and("isOpen").is(open);
		}
		if (owner != null) {
			criteria = criteria.and("owner").is(owner);
		}
		Query query = new Query(criteria);
//		query.fields().exclude("endPoint");
		return mongoTemplate.find(query, Object.class, METADATA);
	}

	@GetMapping("/{collectionId}/meta")
	public List<CollectionModel> getMetaById(@PathVariable String collectionId) {
		Query query = new Query(Criteria.where("collectionId").is(collectionId));
		query.fields().exclude("endPoint");
		return mongoTemplate.find(query, CollectionModel.class, METADATA);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@PostMapping("")
	public ResponseEntity<Object> createCollection(@RequestHeader(value = "Authorization") String userToken,
			@RequestBody JSONObject json) {
		try {
			HttpResponse<JsonNode> res = Unirest.get("http://user-service:8080/api/v1/users")
					.queryString("token", userToken).asJson();
			String userId = (String) res.getBody().getArray().getJSONObject(0).get("userId");
			String userName = (String) res.getBody().getArray().getJSONObject(0).get("userName");
			JSONObject example = new JSONObject((Map) json.get("example"));
			String collectionId = DigestUtils.sha256Hex((String) json.get("collectionName") + userId);
			if (!mongoTemplate
					.find(new Query(Criteria.where("collectionId").is(collectionId)), CollectionModel.class, METADATA)
					.isEmpty()) {
				return new ResponseEntity<>(HttpStatus.CONFLICT);
			}
			CollectionModel collection = new CollectionModel(collectionId, (String) json.get("collectionName"),
					(String) json.get("endPoint"), (String) json.get("type"), userName, example,
					(boolean) json.get("isOpen"));
			mongoTemplate.insert(collection, METADATA);
			mongoTemplate.createCollection(collection.getCollectionId());
			JSONObject body = new JSONObject();
			body.put("userId", userId);
			body.put("collectionId", collection.getCollectionId());
			body.put("type", "OWNER");
			body.put("isOpen", collection.isOpen());
			Unirest.post("http://access-control-service:8080/api/v1/accesscontrol/collections")
					.header("Content-Type", "application/json").body(body.toJSONString()).asString();
			return new ResponseEntity<Object>(collection.getCollectionId(), HttpStatus.CREATED);
		} catch (UnirestException e1) {
			e1.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	@DeleteMapping("/{collectionId}")
	public ResponseEntity<Object> deleteCollection(@PathVariable String collectionId,
			@RequestHeader(value = "Authorization") String userToken) {
		try {
			HttpResponse<JsonNode> res = Unirest.get("http://user-service:8080/api/v1/users")
					.queryString("token", userToken).asJson();
			String userId = (String) res.getBody().getArray().getJSONObject(0).get("userId");
			HttpResponse<String> res_ac = Unirest.get("http://access-control-service:8080/api/v1/accesscontrol")
					.queryString("collectionId", collectionId).queryString("userId", userId).asString();
			String role = res_ac.getBody();
			if (role.equalsIgnoreCase("OWNER")) {
				mongoTemplate.dropCollection(collectionId);
				System.out.println(mongoTemplate.findAndRemove(
						new Query(Criteria.where("collectionId").is(collectionId)), CollectionModel.class, METADATA));
				Unirest.delete("http://access-control-service:8080/api/v1/accesscontrol/collections/{collectionId}")
						.routeParam("collectionId", collectionId).asString();
				return new ResponseEntity<>(HttpStatus.OK);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@GetMapping("/{collectionId}")
	public ResponseEntity<Object> getCollection(@PathVariable String collectionId,
			@RequestHeader(value = "Authorization") String ticket) {
		JSONObject jsonTicket = decrypt(ticket);
		if (jsonTicket != null) {
			if (isValidTicket(collectionId, jsonTicket) && mongoTemplate.collectionExists(collectionId)) {
				System.out.println(jsonTicket);
				Query q = new Query();
				q.fields().exclude("_id");
				List<JSONObject> res = mongoTemplate.find(q, JSONObject.class, collectionId);
				sendToMeter((String) jsonTicket.get("userId"), (String) jsonTicket.get("collectionId"), "read",
						res.size(), res.toString().length());
				return new ResponseEntity<>(res, HttpStatus.OK);
			}
			return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
	}

	@PostMapping("/{collectionId}")
	public ResponseEntity<Object> insertCollection(@PathVariable String collectionId,
			@RequestHeader(value = "Authorization") String ticket, @RequestBody JSONObject data) {
		JSONObject jsonTicket = decrypt(ticket);
		if (isValidTicket(collectionId, jsonTicket)
				&& (jsonTicket.get("role").equals("OWNER") || jsonTicket.get("role").equals("CONTRIBUTOR"))
				&& mongoTemplate.collectionExists(collectionId)) {
			sendToMeter((String) jsonTicket.get("userId"), (String) jsonTicket.get("collectionId"), "write", 1,
					data.toString().length());
			mongoTemplate.save(data, collectionId);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
	}

	@SuppressWarnings("unchecked")
	private HttpResponse<String> sendToMeter(String userId, String collectionId, String type, int record, int size) {
		JSONObject body = new JSONObject();
		body.put("userId", userId);
		body.put("collectionId", collectionId);
		CollectionModel col = mongoTemplate.findOne(new Query(Criteria.where("collectionId").is(collectionId)),
				CollectionModel.class, METADATA);
		body.put("open", col.isOpen());
		body.put("type", type);
		body.put("record", record);
		body.put("size", size);
		try {
			return Unirest.post("http://meter-service:8080/api/v1/meters/").header("Content-Type", "application/json")
					.body(body.toJSONString()).asString();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean isValidTicket(String collectionId, JSONObject ticket) {
		long now = new Date().getTime();
		if (!ticket.get("collectionId").equals(collectionId)) {
			return false;
		} else if (now > (Long) ticket.get("expire")) {
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private JSONObject decrypt(String data) {
		SecurityModel security = SecurityModel.getInstance();
		try {
			String deData = new String(Base64.getUrlDecoder().decode(data));
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(deData);
			json.replace("key", Base64.getUrlDecoder().decode((String) json.get("key")));
			json.replace("data", Base64.getUrlDecoder().decode((String) json.get("data")));
			String dataString = new String(security.decrypt((byte[]) json.get("key"), (byte[]) json.get("data")));
			JSONParser parser1 = new JSONParser();
			json = (JSONObject) parser1.parse(dataString);
			return json;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
