package com.smartcity.ticket;

import java.util.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

@RestController
public class TicketController {
	private String AC_URL = "http://access-control-service:8080/api/v1/accesscontrol";
	private String COLLECTION_URL = "http://collection-service:8080/api/v1/collections";
	private String USER_URL = "http://user-service:8080/api/v1/users";
	private String CREDIT_URL = "http://credit-service:5000/credits";

	@PostMapping("")
	public ResponseEntity<Object> genTicket(@RequestHeader(value = "Authorization") String userToken,
			@RequestBody JSONObject jsonBody) {
		String collectionId;
		try {
			collectionId = (String) jsonBody.get("collectionId");
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		SecurityModel security = SecurityModel.getInstance();
		try {
			String userId = getUserId(userToken);
			if (userId == null) {
				return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
			}
			String role = getRole(userId, collectionId);
			if (role != null) {
				HttpResponse<String> res = Unirest.get(COLLECTION_URL + "/{collectionId}/meta")
						.routeParam("collectionId", collectionId).asString();
				JSONParser parser = new JSONParser();
				JSONArray json = (JSONArray) parser.parse(res.getBody());
				JSONObject collectionObj = (JSONObject) json.get(0);
				String collectionOwnerId = getUserIdByName((String) collectionObj.get("owner"));
				if (role.equals("READ") && !(boolean) collectionObj.get("open")) {
					return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
				}
				if (!isUserCreditVaild(userId, collectionId, collectionOwnerId)) {
					return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
				}
				String ticketString;
				ObjectMapper mapper = new ObjectMapper();
				try {
					ticketString = mapper.writeValueAsString(new TicketModel(userId, collectionId, collectionOwnerId,
							role, (int) jsonBody.get("expire")));
				} catch (Exception e) {
					ticketString = mapper
							.writeValueAsString(new TicketModel(userId, collectionId, collectionOwnerId, role));
				}
				return new ResponseEntity<Object>(
						Base64.getUrlEncoder().encodeToString(
								security.encrypt(ticketString.getBytes("utf-8")).toJSONString().getBytes("utf-8")),
						HttpStatus.OK);
			}
			return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	private String getUserIdByName(String userName) {
		HttpResponse<String> res = null;
		JSONParser parser = new JSONParser();
		JSONArray json = null;
		try {
			res = Unirest.get(USER_URL + "/{userName}").routeParam("userName", userName).asString();
			json = (JSONArray) parser.parse(res.getBody());
		} catch (UnirestException | ParseException e) {
			return null;
		}
		return (String) ((JSONObject) json.get(0)).get("userId");
	}

	private String getUserId(String userToken) {
		HttpResponse<String> res = null;
		JSONParser parser = new JSONParser();
		JSONArray json = null;
		try {
			res = Unirest.get(USER_URL).queryString("token", userToken).asString();
			json = (JSONArray) parser.parse(res.getBody());
		} catch (UnirestException | ParseException e) {
			return null;
		}
		return (String) ((JSONObject) json.get(0)).get("userId");
	}

	private String getRole(String userId, String collectionId) {
		HttpResponse<String> res;
		try {
			res = Unirest.get(AC_URL).queryString("userId", userId).queryString("collectionId", collectionId)
					.asString();
		} catch (UnirestException e) {
			e.printStackTrace();
			return null;
		}
		String role = res.getBody();
		return role;
	}

	@SuppressWarnings("unchecked")
	private boolean isUserCreditVaild(String userId, String collectionId, String ownerId) {
		JSONObject reqJson = new JSONObject();
		reqJson.put("buyer", userId);
		reqJson.put("seller", ownerId);
		reqJson.put("collectionId", collectionId);
		try {
			HttpResponse<String> res = Unirest.post(CREDIT_URL + "/transactions/")
					.header("Content-Type", "application/json").body(reqJson.toJSONString()).asString();
			JSONParser parser = new JSONParser();
			JSONObject jsonRes = (JSONObject) parser.parse(res.getBody());
			return (boolean) jsonRes.get("is_transfered");
		} catch (UnirestException | ParseException e) {
			e.printStackTrace();
		}
		return false;
	}

}
