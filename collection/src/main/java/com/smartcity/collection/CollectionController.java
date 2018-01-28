package com.smartcity.collection;

import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

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
	private final String USER_URL = System.getenv("USER_URL");
	private final String AC_URL = System.getenv("AC_URL");
	private final String METER_URL = System.getenv("METER_URL");

	@GetMapping("/redirect")
	public ResponseEntity<Object> redirect(HttpServletResponse resp) {
		String url = "https://api.smartcity.kmitl.io/api/v1/collections/c3d4d1863df29aed97f8a9bbdcc2d3423d439f7c63dd1f4660a93a379c7bbd4c";
		String header = "eyJkYXRhIjoiMTBSc1JkZE5waEh1RXlGSU5JbU5tWUNlcFVHc1VVY1kxQ085b2VyWFVpNkdWNlhFbzhTRkpudFNoMTlXdlhQTjBlbTJzOVdqUkZqZE1ySVZ6Sm1QUGd1NGRwa1B0d2toMjd3QnEwZUhfMjhTbXYyNEVqVUR6Q19JWkRDVVBHX1RxVUhnekZFNHJPanZuNDAxVkJ2ZlVGNmRZamFsX0QycU9rYUJUalZ3MUdTNW4zT3FkWlI1NTJDSjBveDJ1Zl9BbjBEa0Q3OXFwczNkTE0wQ0tlN0QtRExWVlNGeFdGUDZUTDBMY3JlWDljMjNvMkRUcHlzZlZxX05IVkptX183Z1BzU3ktREJMeEdGU19GLVVRdWh3cnVudVRNUGFtVzltNmNTWnVwdmhLVXJhUnYwREJZTEFoTlpPRVNmZ0RaX0VyOVhnZnRLUzRuMW44SEMtZ0I3NEVuWW1UVE9kS2NTVXk1UlJGLUlvLTR5cTlJa0dFYktjSUhONjFaR2NYOFpLdllubjdqVl93ZGVIRm1ubUhZNmhSVm1GTkM1aWxOaTZYR2dBQ285dEJhdDFYdnhXcmZrTENmLWtOM1JtS1UzelVuLTBENkpCbzVJdjFJYkJJSW8zTk1XYU1iN1lWOHdsdEdrdWNjSm1UOS1EbG1vVzVMZ0dSMVR4QnBrX0VfVFJhSm5UX0s5Wkp3LUxyYUxscU1ZYTVnPT0iLCJrZXkiOiJGelBlaEZiUzRza1d2MjdJTk5BN1hsVUhYRG9VSzFOaFU1Z3VVdzRERUZEQ242X1NmWTVTcXZPRUZlNU1YSkpDWnlwQjNiMW9TRW9kTUcxdzJGS3BSYXNQd1J3X0wxVHh6MWtXXzJqR1d0QVZraExzQmtOdzJvQmdGZGNMU1JLUXFxbEpYUnJSR0hRYmJGVnRDQllrZGlxU0FoR1dLcDJHSHVSbExId0I2NW5TdkNwV2tGNEFSU05kaE01dUNLWkVLYUFYc0lUVllOSjd6MVd2eUd5UW5Oelc4WENYX01SV3NWM2J0ZE5uMVpVdFZwakdQTWczU3dNaE5KcjVKS3NuV3Z2eUtZTXktWE00bEV6ZTRnRWg3QTEwSGhDbGpZdmdyZ09WTzVkb3I4Vk9kMU1FUmVqNmV5cDh0TElScTZ4X1dqS2xuRi1yZ0FwUU83WGhTbll3U1E9PSJ9";
		return ResponseEntity.status(302).location(UriComponentsBuilder.fromUriString(url).build().toUri())
				.header("Authorization", header).build();
	}

	@GetMapping(value = { "", "/api/v1/collections" })
	public ResponseEntity<Object> getMeta(Pageable pageable, String collectionName, String collectionId, String type,
			Boolean open, String owner) {
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
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@GetMapping("/{collectionId}/meta")
	public List<CollectionModel> getMetaById(@PathVariable String collectionId) {
		Query query = new Query(Criteria.where("collectionId").is(collectionId));
		query.fields().exclude("endPoint");
		return mongoTemplate.find(query, CollectionModel.class, METADATA);
	}

	@PutMapping("/{collectionId}/meta")
	public ResponseEntity<Object> changeMeta(@PathVariable String collectionId, @RequestBody JSONObject json,
			@RequestHeader(value = "Authorization") String userToken) {
		org.json.JSONObject user = getUserByToken(userToken);
		if (user == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		try {
			Query query = new Query(Criteria.where("collectionId").is(collectionId));
			CollectionModel col = mongoTemplate.findOne(query, CollectionModel.class, METADATA);
			col.setIcon((String) json.getOrDefault("icon", col.getIcon()));
			col.setEndPoint((JSONObject) json.getOrDefault("endPoint", col.getEndPoint()));
			if (col.getOwner().equalsIgnoreCase((user.getString("userName")))) {
				col.setOpen((boolean) json.get("isOpen"));
				HttpResponse<String> res = Unirest.put(AC_URL + "/collections/{collectionId}/")
						.header("Content-Type", "application/json").routeParam("collectionId", collectionId)
						.body(json.toJSONString()).asString();
				if (res.getStatus() != 200) {
					return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
				}
				mongoTemplate.save(col, METADATA);
				return new ResponseEntity<>(HttpStatus.OK);
			} else {
				return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	@SuppressWarnings({ "rawtypes" })
	@PostMapping("")
	public ResponseEntity<Object> createCollection(@RequestHeader(value = "Authorization") String userToken,
			@RequestBody JSONObject json) {
		try {
			HttpResponse<JsonNode> res = Unirest.get(USER_URL).queryString("token", userToken).asJson();
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
					(String) json.get("description"), (String) json.get("icon"), endPoint, (String) json.get("type"),
					encryptionLevel, userName, example, (boolean) json.get("isOpen"));
			JSONObject body = new JSONObject();
			body.put("userId", userId);
			body.put("collectionId", collection.getCollectionId());
			body.put("type", "OWNER");
			body.put("isOpen", collection.isOpen());
			HttpResponse<String> acRes = Unirest.post(AC_URL + "/collections")
					.header("Content-Type", "application/json").body(body.toJSONString()).asString();
			if (!(acRes.getStatus() >= 200 && acRes.getStatus() < 300)) {
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
			org.json.JSONObject json = getUserByToken(userToken);
			String userId = json.getString("userId");
			HttpResponse<String> res_ac = Unirest.get(AC_URL).queryString("collectionId", collectionId)
					.queryString("userId", userId).asString();
			String role = res_ac.getBody();
			if (role.equalsIgnoreCase("OWNER")) {
				mongoTemplate.dropCollection(collectionId);
				System.out.println(mongoTemplate.findAndRemove(
						new Query(Criteria.where("collectionId").is(collectionId)), CollectionModel.class, METADATA));
				Unirest.delete(AC_URL + "/collections/{collectionId}").routeParam("collectionId", collectionId)
						.asString();
				return new ResponseEntity<>(HttpStatus.OK);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@GetMapping("/{collectionId}")
	public ResponseEntity<Object> getCollection(Pageable pageable, @PathVariable String collectionId,
			@RequestHeader(value = "Authorization") String ticket, @RequestParam Map<String, Object> allRequestParams) {
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
						List<JSONObject> res = retrieveData(pageable, collectionId, allRequestParams);
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
			@RequestHeader(value = "Authorization") String ticket, @RequestBody Object data) {
		JSONObject jsonTicket = decrypt(ticket);
		JSONArray arrayData = getReqBody(data);
		if (arrayData == null) {
			return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
		}
		if (jsonTicket == null) {
			return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
		}
		if (isValidTicket(collectionId, jsonTicket)
				&& (jsonTicket.get("role").equals("OWNER") || jsonTicket.get("role").equals("CONTRIBUTOR"))
				&& mongoTemplate.collectionExists(collectionId)) {
			sendToMeter((String) jsonTicket.get("userId"), (String) jsonTicket.get("collectionId"), "write",
					arrayData.size(), arrayData.toString().length());
			if (!storingData(collectionId, arrayData)) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			return new ResponseEntity<>(HttpStatus.CREATED);
		}
		return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
	}

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
			return Unirest.post(METER_URL).header("Content-Type", "application/json").body(body.toJSONString())
					.asString();
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

	private boolean storingData(String collectionId, JSONArray arrayData) {
		CollectionModel collection = mongoTemplate.findById(collectionId, CollectionModel.class, METADATA);
		if (collection.getEncryptionLevel() == 2) {
			try {
				SecretKey key = getAesKey(collectionId);
				arrayData.forEach(map -> {
					try {
						JSONObject tmp = new JSONObject();
						tmp.put("data", encryptData(map.toString(), key));
						mongoTemplate.insert(tmp, collectionId);
					} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
							| IllegalBlockSizeException | BadPaddingException e) {
						e.printStackTrace();
					}
				});
				return true;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				mongoTemplate.insert(arrayData, collectionId);
				return true;
			}

		} else if (collection.getEncryptionLevel() == 1) {
			System.out.println("System encrypt");
			mongoTemplate.insert(arrayData, collectionId);
			return true;
		} else {
			System.out.println("No encrypt");
			mongoTemplate.insert(arrayData, collectionId);
			return true;
		}
	}

	private List<JSONObject> retrieveData(Pageable pageable, String collectionId,
			Map<String, Object> allRequestParams) {
		CollectionModel collection = mongoTemplate.findById(collectionId, CollectionModel.class, METADATA);
		Query q = new Query();
		q.fields().exclude("_id");
		q.with(pageable);
		JSONObject tmp = mongoTemplate.findOne(new Query(), JSONObject.class, collectionId);
		allRequestParams.keySet().forEach(key -> {
			if (!("sortpagesize".toLowerCase().contains(key.toLowerCase()))) {
				try {
					q.addCriteria(Criteria.where(key).is(tmp.get(key).getClass().getMethod("valueOf", String.class)
							.invoke(tmp.get(key), allRequestParams.get(key))));
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		});
		System.err.println(q);
		List<JSONObject> res = mongoTemplate.find(q, JSONObject.class, collectionId);
		if (collection.getEncryptionLevel() == 2) {
			SecretKey key;
			try {
				key = getAesKey(collectionId);
				List<JSONObject> deRes = new ArrayList<JSONObject>();
				res.forEach(obj -> {
					try {
						deRes.add(decryptData((String) obj.get("data"), key));
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

	private JSONObject decryptData(String enData, SecretKey aesKey) throws IllegalBlockSizeException,
			BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, ParseException {
		Cipher aesCipher;
		aesCipher = Cipher.getInstance("AES");
		aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
		byte[] textDecrypted = aesCipher.doFinal(Base64.getDecoder().decode(enData));
		JSONParser parser = new JSONParser();
		return (JSONObject) parser.parse(new String(textDecrypted));
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

	@SuppressWarnings("rawtypes")
	private JSONArray getReqBody(Object json) {
		JSONArray reqBody = new JSONArray();
		try {
			if (json.getClass() == LinkedHashMap.class) {
				reqBody.add(new JSONObject((Map) json));
			} else {
				((ArrayList) json).forEach(mapObject -> {
					reqBody.add(new JSONObject((Map) mapObject));
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return reqBody;
	}
}
