package com.smartcity.collection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
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
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;
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
				.setSSLHostnameVerifier(new NoopHostnameVerifier()).build());
	}

	@GetMapping("/redirect")
	public ResponseEntity<Object> redirect(HttpServletResponse resp) {
		// String url = "http://data.tmd.go.th/api/WeatherToday/V1/?format=json";
		String urlString = "http://data.tmd.go.th/";
		// return
		// ResponseEntity.status(302).location(UriComponentsBuilder.fromUriString(url).build().toUri()).build();
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e2) {
			e2.printStackTrace();
		}
		URLConnection uc = null;
		try {
			uc = url.openConnection();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		uc.setRequestProperty("X-Requested-With", "Curl");

		try {
			InputStreamReader inputStreamReader = new InputStreamReader(uc.getInputStream());
			return new ResponseEntity<Object>(HttpStatus.OK);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);

	}

	@GetMapping("")
	public ResponseEntity<Object> getMeta(Pageable pageable, @RequestParam(defaultValue = "") String collectionName,
			@RequestParam(defaultValue = "") String collectionId, @RequestParam(defaultValue = "") String type,
			Boolean open, @RequestParam(defaultValue = "") String owner) {
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
				col.setThumbnail((String) json.getOrDefault("thumbnail", col.getThumbnail()));
				col.setDescription((String) json.getOrDefault("description", col.getDescription()));
				// col.setEndPoint((JSONObject) json.getOrDefault("endPoint",
				// col.getEndPoint()));
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

	@GetMapping(value = "/{collectionId}/thumbnail", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<InputStreamResource> getThumbnail(@PathVariable String collectionId) {
		try {
			String picture = collectionRepository.findByCollectionId(collectionId).getThumbnail();
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
				col.setThumbnail(imageString);
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
			JSONObject example = null;
			JSONObject endPoint = null;
			JSONArray columns = null;
			String collectionId = UUID.randomUUID().toString();
			int encryptionLevel = (int) json.getOrDefault("encryptionLevel", 0);
			String type = (String) json.get("type");
			try {
				example = new JSONObject((Map) json.get("example"));
				endPoint = new JSONObject((Map) json.get("endPoint"));
				if (type.equalsIgnoreCase("timeseries") || type.equalsIgnoreCase("geotemporal")) {
					columns = new JSONArray();
					columns.addAll((ArrayList<Object>) json.get("columns"));
				} else if (!type.equalsIgnoreCase("keyvalue")) {
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}

			} catch (Exception e) {
				e.printStackTrace();
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			if (!mongoTemplate
					.find(new Query(Criteria.where("collectionId").is(collectionId)), CollectionModel.class, METADATA)
					.isEmpty()) {
				return new ResponseEntity<>(HttpStatus.CONFLICT);
			}
			CollectionModel collection = new CollectionModel(collectionId, (String) json.get("collectionName"),
					(String) json.get("description"), (String) json.get("thumbnail"), endPoint, type, columns,
					encryptionLevel, userName, example, (boolean) json.get("isOpen"));
			JSONObject scdiBody = new JSONObject();
			scdiBody.put("type", collection.getType());
			scdiBody.put("columns", collection.getColumns());
			HttpResponse<String> scdiRes = Unirest.post(SCDI_URL + "/api/v1/{userName}/{bucketName}?create")
					.routeParam("userName", SCDI_USER).routeParam("bucketName", collection.getCollectionId())
					.header("Content-Type", "application/json").header("APIKEY", SCDI_API).body(scdiBody.toJSONString())
					.asString();
			if (scdiRes.getStatus() != 200) {
				return new ResponseEntity<>(scdiRes.getBody(), HttpStatus.BAD_REQUEST);
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

				if(scdiRes.getStatus()!=200) {
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}
				mongoTemplate.findAndRemove(new Query(Criteria.where("collectionId").is(collectionId)),
						CollectionModel.class, METADATA);
				Unirest.delete(AC_URL + "/collections/{collectionId}").routeParam("collectionId", collectionId)
						.asString();
				mongoTemplate.dropCollection(collectionId);
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
			String targetId = (String) jsonTicket.getOrDefault("targetId",
					(String) jsonTicket.getOrDefault("collectionId", ""));
			if (isValidTicket(collectionId, jsonTicket)) {
				CollectionModel collection = mongoTemplate.findOne(
						new Query(Criteria.where("collectionId").is(collectionId)), CollectionModel.class, METADATA);
				if (collection != null) {
					JSONObject endpoint = collection.getEndPoint();
					if (((String) endpoint.get("type")).equalsIgnoreCase("local")) {
						Query q = new Query();
						q.fields().exclude("_id");
						List<JSONObject> res = retrieveData(pageable, collectionId, allRequestParams);
						sendToMeter((String) jsonTicket.getOrDefault("userType", ""), (String) jsonTicket.get("userId"),
								targetId, "read", res.size(), res.toString().length());
						return new ResponseEntity<>(res, HttpStatus.OK);
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
			String targetId = (String) jsonTicket.getOrDefault("targetId",
					(String) jsonTicket.getOrDefault("collectionId", ""));
			sendToMeter((String) jsonTicket.getOrDefault("userType", ""), (String) jsonTicket.get("userId"), targetId,
					"write", arrayData.size(), arrayData.toString().length());
			if (!storingData(collectionId, arrayData)) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
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
		if (!ticket.getOrDefault("collectionId", ticket.getOrDefault("targetId", "")).equals(collectionId)) {
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
				}
			}
		});
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

	private String getUserName(String userToken) {
		org.json.JSONObject user = getUserByToken(userToken);
		try {
			return (String) user.get("userName");
		} catch (Exception e) {
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
