package com.smartcity.ticket;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

@CrossOrigin
@RestController
public class TicketController {
	private String AC_URL = System.getenv("AC_URL");

	private String USER_URL = System.getenv("USER_URL");
	public TicketController() {
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
	@PostMapping("")
	public ResponseEntity<Object> genTicket(@RequestHeader(value = "Authorization") String userToken,
			@RequestBody JSONObject jsonBody) {
		String type = "user";
		String userId;
		String targetId;
		String role;
		if (jsonBody.containsKey("collectionId") && jsonBody.containsKey("serviceId")) {
			userId = (String) jsonBody.get("serviceId");
			if (!getRoleUserService(getUserId(userToken), userId).equalsIgnoreCase("OWNER")) {
				return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
			}
			type = "service";
			targetId = (String) jsonBody.get("collectionId");
			role = getRoleServiceCollection(userId, targetId);
		} else {
			userId = getUserId(userToken);
			targetId = (String) jsonBody.getOrDefault("collectionId", null);
			if (targetId == null) {
				targetId = (String) jsonBody.getOrDefault("serviceId", null);
				if (targetId == null) {
					return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
				}
				role = getRoleUserService(userId, targetId);
			} else {
				role = getRoleUserCollection(userId, targetId);
			}
			if (userId == null) {
				return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
			}
		}
		SecurityModel security = SecurityModel.getInstance();
		try {
			System.err.println("role:"+role);
			if (!role.isEmpty()) {
				String ticketString;
				ObjectMapper mapper = new ObjectMapper();
				try {
					ticketString = mapper.writeValueAsString(
							new TicketModel(type, userId, targetId, role, (int) jsonBody.get("expire")));
				} catch (Exception e) {
					ticketString = mapper.writeValueAsString(new TicketModel(type, userId, targetId, role));
				}
				return new ResponseEntity<Object>(
						Base64.getUrlEncoder().encodeToString(
								security.encrypt(ticketString.getBytes("utf-8")).toJSONString().getBytes("utf-8")),
						HttpStatus.CREATED);
			}
			return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

//	private String getUserIdByName(String userName) {
//		HttpResponse<String> res = null;
//		JSONParser parser = new JSONParser();
//		JSONArray json = null;
//		try {
//			res = Unirest.get(USER_URL + "/{userName}").routeParam("userName", userName).asString();
//			json = (JSONArray) parser.parse(res.getBody());
//		} catch (Exception e) {
//			return null;
//		}
//		return (String) ((JSONObject) json.get(0)).get("userId");
//	}

	private String getUserId(String userToken) {
		HttpResponse<String> res = null;
		JSONParser parser = new JSONParser();
		JSONArray json = null;
		try {
			res = Unirest.get(USER_URL).queryString("token", userToken).asString();
			json = (JSONArray) parser.parse(res.getBody());
			return (String) ((JSONObject) json.get(0)).get("userId");
		} catch (Exception e) {
			return null;
		}
	}

	private String getRoleUserCollection(String userId, String collectionId) {
		HttpResponse<String> res;
		try {
			res = Unirest.get(AC_URL).queryString("userId", userId).queryString("collectionId", collectionId)
					.asString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		String role = res.getBody();
		return role;
	}

	private String getRoleUserService(String userId, String serviceId) {
		HttpResponse<String> res;
		try {
			res = Unirest.get(AC_URL).queryString("userId", userId).queryString("serviceId", serviceId).asString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		String role = res.getBody();
		return role;
	}

	private String getRoleServiceCollection(String serviceId, String collectionId) {
		HttpResponse<String> res;
		try {
			res = Unirest.get(AC_URL).queryString("serviceId", serviceId).queryString("collectionId", collectionId)
					.asString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		String role = res.getBody();
		return role;
	}

	// private boolean isUserCreditVaild(String userId, String collectionId, String
	// ownerId) {
	// JSONObject reqJson = new JSONObject();
	// reqJson.put("from", userId);
	// reqJson.put("to", ownerId);
	// reqJson.put("type", "TCKT");
	// reqJson.put("collectionId", collectionId);
	// try {
	// HttpResponse<String> res = Unirest.post(CREDIT_URL + "/transactions/")
	// .header("Content-Type",
	// "application/json").body(reqJson.toJSONString()).asString();
	// if (res.getStatus() == 200) {
	// return true;
	// }
	// } catch (UnirestException e) {
	// e.printStackTrace();
	// }
	// return true;
	// }

}
