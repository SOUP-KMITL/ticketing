package com.smartcity.collection;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import com.mashape.unirest.request.HttpRequest;
import com.mongodb.MongoClient;
import com.smartcity.collection.SecurityModel;
@CrossOrigin
@RestController
public class CollectionController {
	private MongoTemplate mongoTemplate = new MongoTemplate(
			new SimpleMongoDbFactory(new MongoClient("mongo"), "CollectionModel"));
	private String METADATA = "MetaData";

	@GetMapping("/")
	public List<Object> getMeta(Pageable pageable,String collectionName, String collectionId, String type, Boolean open, String owner) {
		Criteria criteria = new Criteria();
		if (collectionName != null) {
			criteria = criteria.and("collectionName").is(collectionName);
		}
		if (collectionId != null) {
			criteria = criteria.and("_id").regex(".*" + collectionId + ".*");
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
		query.fields().exclude("endPoint");
		query.with(pageable);
		List<Object> res = mongoTemplate.find(query, Object.class, METADATA);
		return res;
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
			JSONObject endPoint = new JSONObject((Map) json.get("endPoint"));
			String collectionId = DigestUtils.sha256Hex((String) json.get("collectionName") + userId);
			int encryptionLevel = (int) json.getOrDefault("encryptionLevel", 0);
			if (!mongoTemplate
					.find(new Query(Criteria.where("collectionId").is(collectionId)), CollectionModel.class, METADATA)
					.isEmpty()) {
				return new ResponseEntity<>(HttpStatus.CONFLICT);
			}
			CollectionModel collection = new CollectionModel(collectionId, (String) json.get("collectionName"),
					endPoint, (String) json.get("type"), encryptionLevel, userName, example,
					(boolean) json.get("isOpen"));
			JSONObject body = new JSONObject();
			body.put("userId", userId);
			body.put("collectionId", collection.getCollectionId());
			body.put("type", "OWNER");
			body.put("isOpen", collection.isOpen());
			HttpResponse<String> acRes = Unirest.post("http://access-control-service:8080/api/v1/accesscontrol/collections")
					.header("Content-Type", "application/json").body(body.toJSONString()).asString();
			if(!(acRes.getStatus() >= 200 && acRes.getStatus() < 300)) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			mongoTemplate.insert(collection, METADATA);
			mongoTemplate.createCollection(collection.getCollectionId());
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

	@SuppressWarnings("unchecked")
	@GetMapping("/{collectionId}")
	public ResponseEntity<Object> getCollection(Pageable pageable,@PathVariable String collectionId,
			@RequestHeader(value = "Authorization") String ticket) {
		JSONObject jsonTicket = decrypt(ticket);
		if (jsonTicket != null) {
			if (isValidTicket(collectionId, jsonTicket)) {
				CollectionModel collection = mongoTemplate.findOne(
						new Query(Criteria.where("collectionId").is(collectionId)), CollectionModel.class, METADATA);
				if (collection != null) {
					JSONObject endpoint = collection.getEndPoint();
					if (((String) endpoint.get("type")).equalsIgnoreCase("local")) {
						Query q = new Query();
						q.fields().exclude("_id");
						List<JSONObject> res = retrieveData(pageable,collectionId);
						sendToMeter((String) jsonTicket.get("userId"), (String) jsonTicket.get("collectionId"), "read",
								res.size(), res.toString().length());
						return new ResponseEntity<>(res, HttpStatus.OK);
					} else if (((String) endpoint.get("type")).equalsIgnoreCase("remote")) {
						try {
							HttpRequest req_remote = Unirest.get((String) endpoint.get("url"));
							if (endpoint.containsKey("queryString")) {
								req_remote = req_remote.queryString((Map<String, Object>) endpoint.get("queryString"));
							}
							if (endpoint.containsKey("headers")) {
								req_remote = req_remote.headers((Map<String, String>) endpoint.get("headers"));
							}
							HttpResponse<String> res_remote = req_remote.asString();
							JSONParser parser = new JSONParser();
							try {
								String type = parser.parse(res_remote.getBody()).getClass().getName();
								Class<?> c = null;
								try {
									c = Class.forName(type);
								} catch (ClassNotFoundException e) {
									e.printStackTrace();
									return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
								}
								Object json = c.cast(parser.parse(res_remote.getBody()));
								int record = 1;
								if (type.equalsIgnoreCase("org.json.simple.JSONArray")) {
									record = ((JSONArray) json).size();
								}
								sendToMeter((String) jsonTicket.get("userId"), (String) jsonTicket.get("collectionId"),
										"read", record, json.toString().length());
								return new ResponseEntity<Object>(json, HttpStatus.OK);
							} catch (ParseException e) {
								e.printStackTrace();
							}
							sendToMeter((String) jsonTicket.get("userId"), (String) jsonTicket.get("collectionId"),
									"read", 1, res_remote.getBody().length());
							return new ResponseEntity<Object>(res_remote.getBody(), HttpStatus.OK);
						} catch (UnirestException e) {
							e.printStackTrace();
						}
					}
				}
			}
			return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
	}

	@PostMapping("/{collectionId}")
	public ResponseEntity<Object> insertCollection(@PathVariable String collectionId,
			@RequestHeader(value = "Authorization") String ticket, @RequestBody JSONObject data) {
		JSONObject jsonTicket = decrypt(ticket);
		if (jsonTicket == null) {
			return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
		}
		if (isValidTicket(collectionId, jsonTicket)
				&& (jsonTicket.get("role").equals("OWNER") || jsonTicket.get("role").equals("CONTRIBUTOR"))
				&& mongoTemplate.collectionExists(collectionId)) {
			sendToMeter((String) jsonTicket.get("userId"), (String) jsonTicket.get("collectionId"), "write", 1,
					data.toString().length());
			storingData(collectionId, data);
			return new ResponseEntity<>(HttpStatus.CREATED);
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
		body.put("size", size - 2);
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

	@SuppressWarnings("unchecked")
	private void storingData(String collectionId, JSONObject data) {
		CollectionModel collection = mongoTemplate.findById(collectionId, CollectionModel.class, METADATA);
		if (collection.getEncryptionLevel() == 2) {
			JSONObject enData = new JSONObject();
			try {
				enData.put("data", encryptData(data.toJSONString(), getAesKey(collectionId)));
				mongoTemplate.save(enData,collectionId);
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
					| BadPaddingException e) {
				e.printStackTrace();
				mongoTemplate.save(data, collectionId);
			}
			
		} else if (collection.getEncryptionLevel() == 1) {
			System.out.println("System encrypt");
			mongoTemplate.save(data, collectionId);
		} else {
			System.out.println("No encrypt");
			mongoTemplate.save(data, collectionId);
		}
	}
	private List<JSONObject> retrieveData(Pageable pageable,String collectionId) {
		CollectionModel collection = mongoTemplate.findById(collectionId, CollectionModel.class, METADATA);
		Query q = new Query();
		q.fields().exclude("_id");
		q.with(pageable);
		List<JSONObject> res = mongoTemplate.find(q, JSONObject.class, collectionId);
		if (collection.getEncryptionLevel() == 2) {
			SecretKey key;
			try {
				key = getAesKey(collectionId);
				List<JSONObject> deRes = new ArrayList<JSONObject>();
				res.forEach(obj -> {
					try {
						deRes.add(decryptData((String)obj.get("data"),key));
					} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
							| NoSuchAlgorithmException | NoSuchPaddingException | ParseException e) {
						e.printStackTrace();
					}
				});
				return deRes;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			return res;
			
		} else if (collection.getEncryptionLevel() == 1) {
			System.out.println("System encrypt");
			return res;
		} else {
			System.out.println("No encrypt");
			return res;
		}
	}
	@SuppressWarnings("unchecked")
	private SecretKey getAesKey(String collectionId) throws NoSuchAlgorithmException {
		JSONObject key = mongoTemplate.findOne(new Query(Criteria.where("collectionId").is(collectionId)),
				JSONObject.class, "KeyMoc");
		SecretKey aesKey;
		if (key == null) {
			KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
			aesKey = keygenerator.generateKey();
			JSONObject keyObject = new JSONObject();
			keyObject.put("collectionId", collectionId);
			keyObject.put("key", Base64.getEncoder().encodeToString(aesKey.getEncoded()));
			mongoTemplate.save(keyObject, "KeyMoc");
		} else {
			String encodeKey = (String) key.get("key");
			byte[] decodedKey = Base64.getDecoder().decode(encodeKey);
			aesKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
		}
		return aesKey;
	}

	private String encryptData(String data, SecretKey aesKey) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Cipher aesCipher;
		aesCipher = Cipher.getInstance("AES");
		aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
		byte[] text = data.getBytes();
		byte[] textEncrypted = aesCipher.doFinal(text);
		return Base64.getEncoder().encodeToString(textEncrypted);

	}

	private JSONObject decryptData(String enData, SecretKey aesKey) throws IllegalBlockSizeException, BadPaddingException,
			InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, ParseException {
		Cipher aesCipher;
		aesCipher = Cipher.getInstance("AES");
		aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
		byte[] textDecrypted = aesCipher.doFinal(Base64.getDecoder().decode(enData));
		JSONParser parser = new JSONParser();
		return (JSONObject) parser.parse(new String(textDecrypted));
	}
}
