package com.smartcity.collection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import com.mongodb.MongoClient;

@CrossOrigin
@RestController
public class CollectionController {
	private MongoTemplate mongoTemplate = new MongoTemplate(
			new SimpleMongoDbFactory(new MongoClient("mongo"), "CollectionModel"));
	@Autowired
	private CollectionModelRepository collectionRepository;

	private String METADATA = "MetaData";
	private final String USER_URL = System.getenv("USER_URL");
	private final String AC_URL = System.getenv("AC_URL");
	private final String METER_URL = System.getenv("METER_URL");
	private final String SCDI_API = System.getenv("SCDI_API");
	private final String SCDI_USER = System.getenv("SCDI_USER");
	private final String SCDI_URL = System.getenv("SCDI_URL");

	CollectionController() {
		SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			e.printStackTrace();
		}
		Unirest.setHttpClient(HttpClients.custom().setSSLContext(sslContext)
				.setSSLHostnameVerifier(new NoopHostnameVerifier()).setRetryHandler(new HttpRequestRetryHandler() {
					@Override
					public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
						if (executionCount > 3) {
							return false;
						}
						if (exception instanceof org.apache.http.NoHttpResponseException) {
							return true;
						}
						return false;
					}
				}).build());
	}

	@GetMapping("/test")
	public ResponseEntity<Object> test() {
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("")
	public ResponseEntity<Object> getMeta(Pageable pageable, @RequestParam(defaultValue = "") String collectionName,
			@RequestParam(defaultValue = "") String collectionId, @RequestParam(defaultValue = "") String type,
			Boolean open, @RequestParam(defaultValue = "") String owner,
			@RequestParam(defaultValue = "") String keyword) {
		if (!keyword.isEmpty()) {
			return new ResponseEntity<Object>(collectionRepository.findByCollectionNameLikeOrOwnerLikeOrDescriptionLike(
					keyword, keyword, keyword, pageable), HttpStatus.OK);
		}
		if (open != null) {
			open = !open;
		}
		return new ResponseEntity<Object>(
				collectionRepository.findAllCustom(owner, type, collectionName, collectionId, open, pageable),
				HttpStatus.OK);
	}

	@GetMapping("/{collectionId}/meta")
	public ResponseEntity<CollectionModel> getMetaById(@PathVariable String collectionId,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		Query query = new Query(Criteria.where("collectionId").is(collectionId));
		List<CollectionModel> cols = mongoTemplate.find(query, CollectionModel.class, METADATA);
		String userName = getUserName(authorization);
		if (cols.isEmpty()) {
			return new ResponseEntity<CollectionModel>(HttpStatus.NOT_FOUND);
		}
		CollectionModel col = cols.get(0);
		if (userName != null && col.getCollectionName().equalsIgnoreCase(userName)) {
			return new ResponseEntity<CollectionModel>(col, HttpStatus.OK);
		}
		col.setEndPoint(null);
		return new ResponseEntity<CollectionModel>(col, HttpStatus.OK);
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
			if (col.getOwner().equalsIgnoreCase((user.getString("userName")))) {
				col.setOpen((boolean) json.get("isOpen"));
				// col.setThumbnail((String) json.getOrDefault("thumbnail",
				// col.getThumbnail()));
				col.setDescription((String) json.getOrDefault("description", col.getDescription()));
				// col.setEndPoint((JSONObject) json.getOrDefault("endPoint",
				// col.getEndPoint()));
				HttpResponse<String> res = Unirest.put(AC_URL + "/collections/{collectionId}")
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

	@GetMapping(value = "/{collectionId}/thumbnail", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<InputStreamResource> getThumbnail(@PathVariable String collectionId) {
		try {
			String picture = collectionRepository.findByCollectionId(collectionId).getThumbnailBase64();
			byte[] pictureBtyes = Base64.getDecoder().decode(picture);
			ByteArrayInputStream targetStream = new ByteArrayInputStream(pictureBtyes);
			InputStreamResource isr = new InputStreamResource(targetStream);
			return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(isr);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@PutMapping("/{collectionId}/thumbnail")
	public ResponseEntity<Object> updateThumbnail(@PathVariable String collectionId,
			@RequestParam(required = true) MultipartFile thumbnail,
			@RequestHeader(value = "Authorization") String authorization) {
		byte[] bytes;
		if (thumbnail.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		try {
			bytes = thumbnail.getBytes();
			String imageString = Base64.getEncoder().encodeToString(bytes);
			String userName = getUserName(authorization);
			CollectionModel col = collectionRepository.findByCollectionId(collectionId);
			if (col == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			if (col.getOwner().equalsIgnoreCase(userName)) {
				col.setThumbnailBase64(imageString);
				col.setThumbnail("https://api.smartcity.kmitl.io/api/v1/collections/" + collectionId + "/thumbnail");
				collectionRepository.save(col);
				return new ResponseEntity<>(HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}
		} catch (IOException e) {
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
			if (res.getBody().getArray().isNull(0)) {
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}
			String userId = (String) res.getBody().getArray().getJSONObject(0).get("userId");
			String userName = (String) res.getBody().getArray().getJSONObject(0).get("userName");
			String collectionName = (String) json.getOrDefault("collectionName", null);
			if (collectionName != null) {
				if (collectionRepository.findByCollectionNameAndOwner(collectionName, userName) != null) {
					return new ResponseEntity<>(HttpStatus.CONFLICT);
				}
			} else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			JSONObject example = null;
			JSONObject endPoint = null;
			JSONArray columns = null;
			String collectionId = "sc-" + UUID.randomUUID().toString();
			int encryptionLevel = (int) json.getOrDefault("encryptionLevel", 0);
			String type = (String) json.get("type");
			try {
				if (!type.equalsIgnoreCase("remote")) {
					endPoint = new JSONObject();
					endPoint.put("type", "local");
				} else {
					endPoint = new JSONObject((Map) json.get("endPoint"));
				}
				example = new JSONObject((Map) json.get("example"));
				if (((String) endPoint.get("type")).equalsIgnoreCase("local")) {
					if (type.equalsIgnoreCase("timeseries") || type.equalsIgnoreCase("geotemporal")) {
						columns = new JSONArray();
						columns.addAll((ArrayList<Object>) json.get("columns"));
					} else if (!type.equalsIgnoreCase("keyvalue")) {
						return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
					}
				} else if (!type.equalsIgnoreCase("remote")) {
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}

			} catch (Exception e) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			CollectionModel collection = new CollectionModel(collectionId, collectionName,
					(String) json.get("description"), null, endPoint, type, (String) json.get("category"), columns,
					encryptionLevel, userName, example, (boolean) json.get("isOpen"));
			if (((String) endPoint.get("type")).equalsIgnoreCase("local")) {
				JSONObject scdiBody = new JSONObject();
				scdiBody.put("type", collection.getType());
				if (!collection.getType().equalsIgnoreCase("keyvalue")) {
					if (collection.getEncryptionLevel() == 2) {
						JSONArray tmpColumn = collection.getColumns();
						Map<String, Object> tmpJson;
						String key;
						Iterator<?> iterColumn = tmpColumn.iterator();
						while (iterColumn.hasNext()) {
							tmpJson = (Map<String, Object>) iterColumn.next();
							key = (String) tmpJson.getOrDefault("name", "");
							if (!(key.equalsIgnoreCase("ts") || key.equalsIgnoreCase("lat")
									|| key.equalsIgnoreCase("lng"))) {
								iterColumn.remove();
							}
						}
						Map<String, Object> tmpMap = new HashMap<>();
						tmpMap.put("name", "data");
						tmpMap.put("type", "text");
						tmpMap.put("indexed", false);
						tmpColumn.add(tmpMap);
						scdiBody.put("columns", tmpColumn);
					} else {
						scdiBody.put("columns", collection.getColumns());
					}
				}
				HttpResponse<String> scdiRes = Unirest.post(SCDI_URL + "/api/v1/{userName}/{bucketName}?create")
						.routeParam("userName", SCDI_USER).routeParam("bucketName", collection.getCollectionId())
						.header("Content-Type", "application/json").header("APIKEY", SCDI_API)
						.body(scdiBody.toJSONString()).asString();
				if (scdiRes.getStatus() != 200) {
					return new ResponseEntity<>(collectionId, HttpStatus.BAD_REQUEST);
				}
			}
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
			mongoTemplate.createCollection(collectionId);
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
				HttpResponse<String> scdiRes = Unirest.delete(SCDI_URL + "/api/v1/{userName}/{bucketName}?delete")
						.routeParam("userName", SCDI_USER).routeParam("bucketName", collectionId)
						.header("apikey", SCDI_API).asString();
				// if (scdiRes.getStatus() != 200) {
				// return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				// }
				mongoTemplate.findAndRemove(new Query(Criteria.where("collectionId").is(collectionId)),
						CollectionModel.class, METADATA);
				for (int i = 0; i < 5; i++) {
					try {
						Unirest.delete(AC_URL + "/collections/{collectionId}").routeParam("collectionId", collectionId)
								.asString();
						break;
					} catch (Exception e) {
					}
				}
				mongoTemplate.remove(new Query(Criteria.where("collectionId").is(collectionId)), "KeyMoc");
				mongoTemplate.dropCollection(collectionId);
				return new ResponseEntity<>(HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	@GetMapping("/{collectionId}")
	public ResponseEntity<Object> getCollection(@PathVariable String collectionId,
			@RequestHeader(value = "Authorization") String ticket, @RequestParam Map<String, Object> allRequestParams) {
		JSONObject jsonTicket = decrypt(ticket);
		if (jsonTicket != null) {
			String targetId = (String) jsonTicket.getOrDefault("targetId", jsonTicket.getOrDefault("collectionId", ""));
			if (isValidTicket(collectionId, jsonTicket)) {
				CollectionModel collection = mongoTemplate.findOne(
						new Query(Criteria.where("collectionId").is(collectionId)), CollectionModel.class, METADATA);
				if (collection != null) {
					JSONObject endpoint = collection.getEndPoint();
					if (((String) endpoint.get("type")).equalsIgnoreCase("local")) {
						if (!collection.getType().equalsIgnoreCase("keyvalue")) {
							Query q = new Query();
							q.fields().exclude("_id");
							List<JSONObject> res = retrieveJsonData(
									(String) allRequestParams.getOrDefault("query", null), collection);
							sendToMeter((String) jsonTicket.getOrDefault("userType", ""),
									(String) jsonTicket.get("userId"), targetId, "read", res.size(),
									res.toString().length());
							return new ResponseEntity<>(res, HttpStatus.OK);
						} else {
							try {
								String res = retrieveKeyValueData(collectionId, (String) allRequestParams.get("key"));
								sendToMeter((String) jsonTicket.getOrDefault("userType", ""),
										(String) jsonTicket.get("userId"), targetId, "read", 1, res.length());
								return new ResponseEntity<>(res, HttpStatus.OK);
							} catch (Exception e) {
								return new ResponseEntity<>(HttpStatus.NOT_FOUND);
							}
						}

					} else if (((String) endpoint.get("type")).equalsIgnoreCase("remote")) {
						try {
							HttpRequest req_remote = Unirest.get((String) endpoint.get("url"));
							allRequestParams.remove("sort");
							allRequestParams.remove("page");
							allRequestParams.remove("size");
							if (endpoint.containsKey("queryString") || !allRequestParams.isEmpty()) {
								Map<String, Object> tmp = new HashMap<String, Object>();
								tmp.putAll(allRequestParams);
								tmp.putAll((Map<String, Object>) endpoint.getOrDefault("queryString", null));
								req_remote = req_remote.queryString(tmp);
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
									return new ResponseEntity<Object>(res_remote.getBody(), HttpStatus.OK);
								}
								Object json = c.cast(parser.parse(res_remote.getBody()));
								int record = 1;
								if (type.equalsIgnoreCase("org.json.simple.JSONArray")) {
									record = ((JSONArray) json).size();
								}
								sendToMeter((String) jsonTicket.getOrDefault("userType", ""),
										(String) jsonTicket.get("userId"), targetId, "read", record,
										json.toString().length());
								return new ResponseEntity<Object>(json, HttpStatus.OK);
							} catch (ParseException e) {
								e.printStackTrace();
							}
							sendToMeter((String) jsonTicket.getOrDefault("userType", ""),
									(String) jsonTicket.get("userId"), targetId, "read", 1,
									res_remote.getBody().length());
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

	@PostMapping(value = "/{collectionId}")
	public ResponseEntity<Object> insertCollection(@PathVariable String collectionId,
			@RequestHeader(value = "Authorization") String ticket, @RequestBody String data,
			@RequestParam(required = false, defaultValue = "") String key) {
		JSONObject jsonTicket = decrypt(ticket);
		if (jsonTicket == null) {
			return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
		}
		if (isValidTicket(collectionId, jsonTicket)
				&& (jsonTicket.get("role").equals("OWNER") || jsonTicket.get("role").equals("CONTRIBUTOR"))
				&& mongoTemplate.collectionExists(collectionId)) {
			CollectionModel collection = mongoTemplate.findOne(
					new Query(Criteria.where("collectionId").is(collectionId)), CollectionModel.class, METADATA);
			if (collection.getType().equalsIgnoreCase("keyvalue")) {
				String dataString = data;
				if (key.isEmpty() || dataString.isEmpty()) {
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				} else {
					if (!storingKeyValue(collection, key, dataString)) {
						return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
					}
					sendToMeter((String) jsonTicket.getOrDefault("userType", ""), (String) jsonTicket.get("userId"),
							collectionId, "write", 1, dataString.length());
				}
			} else {
				JSONArray arrayData = getReqBodyJson(data);
				if (arrayData == null) {
					return new ResponseEntity<Object>(data, HttpStatus.BAD_REQUEST);
				} else {
					if (!storingJson(collection, arrayData)) {
						return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
					}
					sendToMeter((String) jsonTicket.getOrDefault("userType", ""), (String) jsonTicket.get("userId"),
							collectionId, "write", arrayData.size(), arrayData.toString().length() - 2);
				}
			}

			return new ResponseEntity<>(HttpStatus.CREATED);
		}
		return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
	}

	private HttpResponse<String> sendToMeter(String userType, String userId, String collectionId, String type,
			int record, int size) {
		JSONObject body = new JSONObject();
		if (userType.isEmpty()) {
			userType = "user";
		}
		body.put("userType", userType);
		body.put("userId", userId);
		body.put("collectionId", collectionId);
		CollectionModel col = mongoTemplate.findOne(new Query(Criteria.where("collectionId").is(collectionId)),
				CollectionModel.class, METADATA);
		body.put("open", col.isOpen());
		body.put("type", type);
		body.put("record", record);
		body.put("size", size);
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
		if (!ticket.getOrDefault("collectionId", ticket.getOrDefault("targetId", "")).equals(collectionId)) {
			return false;
		} else if (now > (Long) ticket.get("expire")) {
			return false;
		}
		return true;
	}

	private JSONObject decrypt(String data) {
		SecurityModel security = new SecurityModel();
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

	private boolean storingKeyValue(CollectionModel collection, String key, String value) {
		String collectionId = collection.getCollectionId();
		if (collection.getEncryptionLevel() == 2) {
			try {
				SecretKey enKey = getAesKey(collectionId);
				value = encryptData(value, enKey);
			} catch (Exception e) {
			}
		}
		mongoTemplate.upsert(new Query(), new Update().set(key, value), collectionId);
		HttpResponse<String> response = null;
		try {
			response = Unirest.post(SCDI_URL + "/api/v1/{userName}/{collectionId}?key=" + key)
					.routeParam("userName", SCDI_USER).routeParam("collectionId", collectionId)
					.header("apikey", SCDI_API).header("Content-Type", "text/plain").header("Cache-Control", "no-cache")
					.body(value).asString();
			if (response.getStatus() != 200) {
				return false;
			}
		} catch (UnirestException e) {
			return false;
		}
		return true;
	}

	private boolean storingJson(CollectionModel collection, JSONArray arrayData) {
		String collectionId = collection.getCollectionId();
		JSONArray enDataArray = new JSONArray();
		if (collection.getEncryptionLevel() == 2) {
			try {
				SecretKey enKey = getAesKey(collection.getCollectionId());
				for (int i = 0; i < arrayData.size(); i++) {
					JSONObject json = (JSONObject) arrayData.get(i);
					json.put("data", encryptData(json.toJSONString(), enKey));
					Iterator<?> keys = json.keySet().iterator();
					while (keys.hasNext()) {
						String key = (String) keys.next();
						if (!(key.equalsIgnoreCase("ts") || key.equalsIgnoreCase("lat") || key.equalsIgnoreCase("lng")
								|| key.equalsIgnoreCase("data"))) {
							keys.remove();
						}
					}
					enDataArray.add(json);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			arrayData = enDataArray;
		}
		try {
			HttpResponse<String> response = Unirest.post(SCDI_URL + "/api/v1/{userName}/{collectionId}?batch")
					.routeParam("userName", SCDI_USER).routeParam("collectionId", collectionId)
					.header("apikey", SCDI_API).header("Content-Type", "application/json")
					.header("Cache-Control", "no-cache").body(arrayData.toJSONString()).asString();
			if (response.getStatus() != 200) {
				return false;
			}
		} catch (UnirestException e) {
			return false;
		}
		mongoTemplate.insert(arrayData, collectionId);
		return true;
	}

	private String retrieveKeyValueData(String collectionId, String key) {
		CollectionModel collection = mongoTemplate.findById(collectionId, CollectionModel.class, METADATA);
		String reData = null;
		try {
			HttpResponse<String> response = Unirest.get(SCDI_URL + "/api/v1/{userName}/{collectionId}?key=" + key)
					.routeParam("userName", SCDI_USER).routeParam("collectionId", collectionId)
					.header("apikey", SCDI_API).header("Content-Type", "text/plain").header("Cache-Control", "no-cache")
					.asString();
			reData = response.getBody();
		} catch (UnirestException e1) {
		}
		if (collection.getEncryptionLevel() == 2) {
			SecretKey enKey;
			try {
				enKey = getAesKey(collectionId);
				Cipher aesCipher;
				aesCipher = Cipher.getInstance("AES");
				aesCipher.init(Cipher.DECRYPT_MODE, enKey);
				byte[] textDecrypted = aesCipher.doFinal(Base64.getDecoder().decode(reData));
				reData = new String(textDecrypted);
			} catch (Exception e) {
			}
		}
		if (reData == null) {
			JSONObject data = mongoTemplate.findOne(new Query(), JSONObject.class);
			reData = (String) data.getOrDefault(key, null);
		}
		return reData;
	}

	private List<JSONObject> retrieveJsonData(String base64query, CollectionModel collection) {
		String collectionId = collection.getCollectionId();
		String query;
		if (base64query == null) {
			query = new JSONObject().toJSONString();
		} else {
			query = new String(Base64.getDecoder().decode(base64query));
		}
		try {
			HttpResponse<String> response = Unirest.post(SCDI_URL + "/api/v1/{userName}/{collectionId}?query")
					.routeParam("userName", SCDI_USER).routeParam("collectionId", collectionId)
					.header("apikey", SCDI_API).header("Content-Type", "application/json")
					.header("Cache-Control", "no-cache").body(query).asString();
			JSONParser parser = new JSONParser();
			List<JSONObject> jsonArray = (List<JSONObject>) parser.parse(new String(response.getBody()));
			if (collection.getEncryptionLevel() == 2) {
				List<JSONObject> tmpList = new ArrayList<JSONObject>();
				SecretKey enKey = getAesKey(collectionId);
				Iterator<JSONObject> iterArray = jsonArray.iterator();
				JSONObject tmpJson = new JSONObject();
				while (iterArray.hasNext()) {
					tmpJson = iterArray.next();
					parser.reset();
					tmpList.add((JSONObject) parser.parse(decryptData((String) tmpJson.get("data"), enKey)));
				}
				jsonArray = tmpList;
			}
			return jsonArray;
		} catch (Exception e) {
			e.printStackTrace();
			return mongoTemplate.findAll(JSONObject.class, collectionId);
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

	private String decryptData(String enData, SecretKey aesKey) throws IllegalBlockSizeException, BadPaddingException,
			InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, ParseException {
		Cipher aesCipher;
		aesCipher = Cipher.getInstance("AES");
		aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
		byte[] textDecrypted = aesCipher.doFinal(Base64.getDecoder().decode(enData));
		return new String(textDecrypted);
	}

	private org.json.JSONObject getUserByToken(String userToken) {
		try {
			HttpResponse<JsonNode> res = Unirest.get(USER_URL).queryString("token", userToken).asJson();
			return res.getBody().getArray().getJSONObject(0);
		} catch (UnirestException | JSONException e) {
			return null;
		}
	}

	private String getUserName(String userToken) {
		org.json.JSONObject user = getUserByToken(userToken);
		try {
			return (String) user.get("userName");
		} catch (Exception e) {
			return null;
		}
	}

	private JSONArray getReqBodyJson(String json) {
		JSONArray reqBody = new JSONArray();
		JSONParser parser = new JSONParser();
		try {
			JSONObject jsonObj = (JSONObject) parser.parse(new String(json));
			reqBody.add(jsonObj);
		} catch (Exception e) {
			try {
				reqBody = (JSONArray) parser.parse(new String(json));
			} catch (ParseException e1) {
				return null;
			}
		}
		return reqBody;
	}
}
