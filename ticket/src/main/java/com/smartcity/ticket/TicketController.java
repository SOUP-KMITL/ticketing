package com.smartcity.ticket;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

@RestController
public class TicketController {

	@GetMapping("/{collectionId}")
	public ResponseEntity<Object> genTicket(@RequestHeader(value = "Authorization") String userToken,
			@PathVariable String collectionId) {
		SecurityModel security = SecurityModel.getInstance();
		try {
			HttpResponse<String> res = Unirest.get("http://user-service:8080/api/v1/users")
					.queryString("token", userToken).asString();
			JSONParser parser = new JSONParser();
			JSONArray json = (JSONArray) parser.parse(res.getBody());
			String userId = (String) ((JSONObject) json.get(0)).get("userId");
			res = Unirest.get("http://access-control-service:8080/api/v1/accesscontrol").queryString("userId", userId)
					.queryString("collectionId", collectionId).asString();
			String role = res.getBody();
			if(role!= null) {
				ObjectMapper mapper = new ObjectMapper();
				res = Unirest.get("http://collection-service:8080/api/v1/collections/{collectionId}/meta")
						.routeParam("collectionId", collectionId).asString();
				parser.reset();
				json = (JSONArray) parser.parse(res.getBody());
				
				if(role.equals("READ") && !(boolean)((JSONObject)json.get(0)).get("open")) {
					return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
				}
				String ticketString = mapper.writeValueAsString(new TicketModel(userId, collectionId, role));
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
	
	@GetMapping("/encrypt")
	public ResponseEntity<Object> en(String data) {
		SecurityModel security = SecurityModel.getInstance();
		try {
			return new ResponseEntity<Object>(Base64.getUrlEncoder().encodeToString(
					security.encrypt(data.getBytes("utf-8")).toJSONString().getBytes("utf-8")), HttpStatus.OK);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

}
