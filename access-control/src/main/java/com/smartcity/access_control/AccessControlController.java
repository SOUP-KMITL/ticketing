package com.smartcity.access_control;

import javax.servlet.http.HttpServletRequest;

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
	private final String USER_URL = "http://user-service:8080/api/v1/users";
	private final String COLLECIONT_URL = "http://collection-service:8080/api/v1/collections";

	@GetMapping(value = { "/hello" })
	public ResponseEntity<Object> hello(HttpServletRequest request) {
		return new ResponseEntity<>("hello", HttpStatus.OK);
	}

	@GetMapping("/")
	public @ResponseBody String findRole(String userId, String collectionId) {
		try {
			return userRepo.findRole(userId, collectionId).getRole();
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@GetMapping("/migrate")
	public ResponseEntity<String> migrate() {
		try {
			HttpResponse<String> user_res = Unirest.get(USER_URL).asString();
			JSONParser parser = new JSONParser();
			JSONArray user_array = (JSONArray) parser.parse(user_res.getBody());
			user_array.forEach(user -> {
				createUserNode((JSONObject) user);
			});
			HttpResponse<String> col_res = Unirest.get(COLLECIONT_URL).asString();
			JSONArray col_array = (JSONArray) (new JSONParser()).parse(col_res.getBody());
			col_array.forEach(col -> {
				createCollectionNode((JSONObject) col);
			});
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (UnirestException | ParseException e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
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
		if (user == null) {
			user = userRepo.findByUserName((String) json.get("owner"));
			type = "OWNER";
		}
		CollectionNode collection = collectionRepo.findByCollectionId((String) json.get("collectionId"));
		Role role = userRepo.findRole(user.userId, collection.getCollectionId());
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
		if (userRepo.findByUserId(user.userId) != null) {
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}
		Iterable<CollectionNode> collections = collectionRepo.findAllOpenCollection();
		if (collections != null) {
			collections.forEach(c -> user.addRole(new Role(READ, user, c)));
		}
		userRepo.save(user);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/collections")
	public ResponseEntity<Object> createCollectionNode(@RequestBody JSONObject json) {
		boolean isOpen = ((boolean) json.getOrDefault("isOpen", json.getOrDefault("open", true)));
		CollectionNode collection = new CollectionNode((String) json.get("collectionId"), isOpen);
		if (collectionRepo.findByCollectionId(collection.getCollectionId()) != null) {
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}
		if (isOpen) {
			Iterable<UserNode> users = userRepo.findAll();
			if (users != null) {
				users.forEach(user -> user.addRole(new Role(READ, user, collection)));
				userRepo.save(users);
			}
			createRelationship(json);
		} else {
			UserNode user = userRepo.findByUserId((String) json.get("userId"));
			if(user == null) {
				user = userRepo.findByUserName((String)json.get("owner"));
			}
			user.addRole(new Role(OWNER, user, collection));
			userRepo.save(user);
			collectionRepo.save(collection);
		}
		return new ResponseEntity<>(HttpStatus.CREATED);

	}

	@PutMapping("/collections/{collectionId}")
	public ResponseEntity<String> changeOpenCollection(@PathVariable String collectionId,
			@RequestBody JSONObject json) {
		try {
			boolean isOpen = (boolean) json.get("isOpen");
			if (changeOpenCollection(collectionId, isOpen)) {
				return new ResponseEntity<String>(HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
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
			res = Unirest.get(USER_URL + "{userName}").routeParam("userName", userName).asString();
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
			res = Unirest.get(USER_URL).queryString("token", userToken).asString();
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

	private boolean changeOpenCollection(String collectionId, boolean isOpen) {
		try {
			CollectionNode col = collectionRepo.findByCollectionId(collectionId);
			col.setOpen(isOpen);
			collectionRepo.save(col);
			if (isOpen) {
				collectionRepo.createAllReadRelationship(collectionId);
			} else {
				collectionRepo.deleteAllNotOwnerRelationship(collectionId);
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
