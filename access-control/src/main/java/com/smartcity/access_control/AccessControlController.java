package com.smartcity.access_control;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

@CrossOrigin
@RestController
public class AccessControlController {
	@Autowired
	UserNodeRepository userRepo;
	@Autowired
	CollectionNodeRepository collectionRepo;
	private String OWNER = "OWNER";
	private String CONTRIBUTOR = "CONTRIBUTOR";
	private String READ = "READ";

	@GetMapping("/")
	public @ResponseBody String findRole(String userId, String collectionId) {
		try {
			return userRepo.findRole(userId, collectionId).getRole();
		} catch (Exception e) {
			return null;
		}
	}

	@GetMapping("/{collectionId}")
	public ResponseEntity<String> getRoleByCollection(@PathVariable String collectionId,
			@RequestHeader String authorization) {
		JSONObject user = userAuth(authorization);
		if (user == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		String role = findRole((String) (user.get("userId")), collectionId);
		return new ResponseEntity<String>(role, HttpStatus.OK);
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

	@PutMapping("/")
	public ResponseEntity<Object> changeRole(@RequestHeader String authorization, @RequestBody JSONObject jsonBody) {
		String collectionId;
		String userName;
		String role;
		try {
			collectionId = (String) jsonBody.get("collectionId");
			userName = (String) jsonBody.get("userName");
			role = (String) jsonBody.get("role");
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		JSONObject ownerUser = userAuth(authorization);
		if (ownerUser != null) {
			if (!findRole((String) ownerUser.get("userId"), collectionId).contains(OWNER)) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}
			String targetUserId = getUserId(userName);
			if (targetUserId == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			if (targetUserId.equalsIgnoreCase((String) ownerUser.get("userId"))) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
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

	@GetMapping("/users")
	public @ResponseBody Iterable<UserNode> getUser(String userId) {
		return userRepo.findAll();
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
		}
		UserNode user = userRepo.findByUserId((String) json.get("userId"));
		user.addRole(new Role(OWNER, user, collection));
		userRepo.save(user);
		collectionRepo.save(collection);
		return new ResponseEntity<>(HttpStatus.CREATED);

	}

	@DeleteMapping("/collections/{collectionId}")
	public ResponseEntity<Object> deleteCollectionNode(@PathVariable String collectionId) {
		CollectionNode collection = collectionRepo.findByCollectionId(collectionId);
		collectionRepo.delete(collection);
		return new ResponseEntity<Object>(HttpStatus.OK);
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
		return user;
	}

}
