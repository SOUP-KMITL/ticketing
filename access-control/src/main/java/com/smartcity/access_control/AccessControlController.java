package com.smartcity.access_control;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccessControlController {
	@Autowired
	UserNodeRepository userRepo;
	@Autowired
	CollectionNodeRepository collectionRepo;

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
		UserNode user = new UserNode((String) json.get("userId"));
		Iterable<CollectionNode> collections = collectionRepo.findAll();
		if (collections != null) {
			collections.forEach(c ->user.addRole(new Role("READ", user, c)));
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
				users.forEach(user -> user.addRole(new Role("READ", user, collection)));
				userRepo.save(users);	
			}
			return createRelationship(json);
		}	
		else {
			UserNode user = userRepo.findByUserId((String)json.get("userId"));
			user.addRole(new Role("OWNER",user,collection));
			userRepo.save(user);
		}
		collectionRepo.save(collection);
		return new ResponseEntity<>(HttpStatus.CREATED);

	}
	@DeleteMapping("/collections/{collectionId}")
	public ResponseEntity<Object> deleteCollectionNode(@PathVariable String collectionId){
		CollectionNode collection = collectionRepo.findByCollectionId(collectionId);
		collectionRepo.delete(collection);
		return new ResponseEntity<Object>(HttpStatus.OK);
	}

}
