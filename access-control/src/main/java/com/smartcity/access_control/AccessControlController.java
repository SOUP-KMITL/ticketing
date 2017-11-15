package com.smartcity.access_control;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

@RestController
public class AccessControlController {
	@Autowired
	UserNodeRepository userRepo;
	@Autowired
	CollectionNodeRepository collectionRepo;
	private String OWNER = "OWNER";
	private String CONTRIBUTOR = "CONTRIBUTOR";
	private String READ = "READ";

	@GetMapping("/users")
	public @ResponseBody Iterable<UserNode> getUser(String userId) {
		// if (userId != null) {
		// return userRepo.findByUserId(userId);
		// }
		return userRepo.findAll();
	}

	@GetMapping("/")
	public @ResponseBody String findRole(String userId, String collectionId) {
		try {
			return userRepo.findRole(userId, collectionId).getRole();
		} catch (Exception e) {
			return null;
		}
	}

	@PostMapping("/")
	public ResponseEntity<Object> createRelationship(@RequestBody JSONObject json) {
		String type = ((String) json.get("type")).toUpperCase();
		UserNode user = userRepo.findByUserId((String) json.get("userId"));
		CollectionNode collection = collectionRepo.findByCollectionId((String) json.get("collectionId"));
		Role role = userRepo.findRole(user.userId, collection.collectionId);
		role.setRole(type);
		userRepo.save(user);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/users")
	public ResponseEntity<Object> createUserNode(@RequestBody JSONObject json) {
		UserNode user = new UserNode((String) json.get("userId"), (String) json.get("userName"));
		Iterable<CollectionNode> collections = collectionRepo.findAll();
		if (collections != null) {
			collections.forEach(c -> user.addRole(new Role(READ, user, c)));
		}
		userRepo.save(user);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/collections")
	public ResponseEntity<Object> createCollectionNode(@RequestBody JSONObject json) {
		CollectionNode collection = new CollectionNode((String) json.get("collectionId"));
		if ((boolean) json.get("isOpen")) {
			Iterable<UserNode> users = userRepo.findAll();
			if (users != null) {
				users.forEach(user -> user.addRole(new Role(READ, user, collection)));
				userRepo.save(users);
			}
			return createRelationship(json);
		} else {
			UserNode user = userRepo.findByUserId((String) json.get("userId"));
			user.addRole(new Role(OWNER, user, collection));
			userRepo.save(user);
		}
		collectionRepo.save(collection);
		return new ResponseEntity<>(HttpStatus.CREATED);

	}

	@DeleteMapping("/collections/{collectionId}")
	public ResponseEntity<Object> deleteCollectionNode(@PathVariable String collectionId) {
		CollectionNode collection = collectionRepo.findByCollectionId(collectionId);
		collectionRepo.delete(collection);
		return new ResponseEntity<Object>(HttpStatus.OK);
	}

	@PutMapping("/users/{userName}/collections/{collectionId}/role/{role}")
	public ResponseEntity<Object> changeRole(@PathVariable String userName, @PathVariable String collectionId,
			@PathVariable String role, @RequestHeader String authorization) {
		JSONObject ownerUser = userAuth(authorization);
		if (ownerUser != null) {
			if (!findRole((String) ownerUser.get("userId"), collectionId).contains(OWNER)) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}
			String targetUserId = getUserId(userName);
			if (targetUserId == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			UserNode targetUserNode = userRepo.findByUserId(targetUserId);
			if (!(OWNER + CONTRIBUTOR + READ).contains(role.toUpperCase())) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			Role role2 = userRepo.findRole(targetUserId, collectionId);
			if (role2 != null) {
				targetUserNode.deleteRole(role2);
				role2.setRole(role.toUpperCase());
				targetUserNode.addRole(role2);
			} else {
				CollectionNode collectionNode = collectionRepo.findByCollectionId(collectionId);
				Role r = new Role(role, targetUserNode, collectionNode);
				targetUserNode.addRole(r);
			}
			userRepo.save(targetUserNode);
			return new ResponseEntity<>(HttpStatus.OK);

		}
		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

	}

	private String getUserId(String userName) {
		HttpResponse<String> res = null;
		try {
			res = Unirest.get("http://user-service:8080/api/v1/users/{userName}").routeParam("userName", userName)
					.asString();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		JSONParser parser = new JSONParser();
		JSONArray json = null;
		try {
			json = (JSONArray) parser.parse(res.getBody());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if (json.size() == 0) {
			return null;
		}
		return (String) ((JSONObject) json.get(0)).get("userId");
	}

	private JSONObject userAuth(String userToken) {
		HttpResponse<String> res = null;
		try {
			res = Unirest.get("http://user-service:8080/api/v1/users").queryString("token", userToken).asString();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		JSONObject user = null;
		JSONParser parser = new JSONParser();
		JSONArray array = null;
		try {
			array = (JSONArray) parser.parse(res.getBody());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		user = (JSONObject) array.get(0);
		System.out.println(user);
		return (JSONObject) user;
	}

}
