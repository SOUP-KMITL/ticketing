package com.smartcity.user;

import org.springframework.web.bind.annotation.RestController;

import com.google.common.hash.Hashing;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@RestController
public class UserController {
	@Autowired
	private UserModelRepository repository;

	@GetMapping("/")
	public @ResponseBody List<UserModel> getAllUser(String token) {
		if (token != null) {
			return repository.findByAccessToken(token);
		}
		return repository.findAll();
	}

	@SuppressWarnings("unchecked")
	@PostMapping(path = "/")
	public ResponseEntity<String> addUser(@RequestBody UserModel user) {
		if (repository.findByuserName(user.getUserName()).isEmpty()) {
			repository.save(user);
			JSONObject json1 = new JSONObject();
			json1.put("userId", user.getUserId());
			try {
				Unirest.post("http://access-control-service:8080/api/v1/accesscontrol/users")
						.header("Content-Type", "application/json").body(json1.toJSONString()).asString();
			} catch (UnirestException e) {
				e.printStackTrace();
			}
			return new ResponseEntity<>(HttpStatus.CREATED);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

	}

	@GetMapping("/{userName}")
	public @ResponseBody List<UserModel> getUserByName(@PathVariable(value = "userName") String userName) {
		return repository.findByuserName(userName);
	}

	@PutMapping("/{userName}/token")
	public ResponseEntity<Object> generateToken(@PathVariable(value = "userName") String userName,
			@RequestHeader String authorization) {
		UserModel user = repository.findByuserName(userName).get(0);
		if (isValid(authorization, user)) {
			user.setAccessToken(user.generateAccessToken());
			repository.save(user);
			return new ResponseEntity<Object>(user.getAccessToken(),HttpStatus.OK);
		}
		return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
	}
	@GetMapping("/{userName}/token")
	public ResponseEntity<Object> getToken(@PathVariable(value = "userName") String userName,
			@RequestHeader String authorization) {
		UserModel user = repository.findByuserName(userName).get(0);
		if (isValid(authorization, user)) {
			return new ResponseEntity<Object>(user.getAccessToken(), HttpStatus.OK);
		}
		return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
	}

	@PutMapping("/{userName}/credit")
	public ResponseEntity<Object> chageCredit(@PathVariable String userName, double credit) {
		try {
			UserModel user = repository.findByuserName(userName).get(0);
			user.setCredit(user.getCredit() + credit);
			repository.save(user);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	private boolean isValid(String basicAuth, UserModel user) {
		String[] userPass = new String(Base64.getDecoder().decode(basicAuth.split(" ")[1])).split(":");
		String pass = Hashing.sha1().hashString(userPass[1], StandardCharsets.UTF_8).toString();
		if (user.getUserName().equals(userPass[0]) && user.getPassword().equals(pass)) {
			return true;
		}
		return false;
	}

}