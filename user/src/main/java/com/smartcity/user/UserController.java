package com.smartcity.user;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@CrossOrigin
@RestController
public class UserController {
	@Autowired
	private UserModelRepository repository;

	@GetMapping("/")
	public @ResponseBody List<UserModel> getAllUser(String token, String userId) {
		if (token != null) {
			return repository.findByAccessToken(token);
		}
		if (userId != null) {
			return repository.findByUserId(userId);
		}
		return repository.findAll();
	}

	@GetMapping("/login")
	public ResponseEntity<Object> login(@RequestHeader(value = "Authorization") String authorization) {
		try {
			if (authorization == null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			if (authorization.indexOf(" ") != -1) {
				String[] userPass = new String(Base64.getDecoder().decode(authorization.split(" ")[1])).split(":");
				List<UserModel> list = repository.findByuserName(userPass[0]);
				if (list.isEmpty()) {
					return new ResponseEntity<>(HttpStatus.NOT_FOUND);
				}
				UserModel user = list.get(0);
				if (isValid(authorization, user)) {
					return new ResponseEntity<Object>(user, HttpStatus.OK);
				} else {
					return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
				}
			} else {
				List<UserModel> list = repository.findByAccessToken(authorization);
				return new ResponseEntity<Object>(list.get(0), HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	@PostMapping(path = "/")
	public ResponseEntity<String> createUser(@RequestBody UserModel user) {
		if (user.getPassword().isEmpty() || user.getUserName().isEmpty() || user.getEmail().isEmpty()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (repository.findByuserName(user.getUserName()).isEmpty()) {
			if (sendUserToAccessControl(user)) {
				repository.save(user);
				return new ResponseEntity<>(HttpStatus.CREATED);
			} else {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} else {
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}
	}

	@GetMapping("/{userName}")
	public @ResponseBody List<UserModel> getUserByName(@PathVariable(value = "userName") String userName) {
		return repository.findByuserName(userName);
	}

	@PutMapping("/{userName}/token")
	public ResponseEntity<Object> generateToken(@PathVariable(value = "userName") String userName,
			@RequestHeader String authorization) {
		UserModel user = null;
		try {
			user = repository.findByuserName(userName).get(0);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		if (isValid(authorization, user)) {
			user.setAccessToken(user.generateAccessToken());
			repository.save(user);
			return new ResponseEntity<Object>(user.getAccessToken(), HttpStatus.OK);
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

	@PutMapping("/{userName}/picture")
	public ResponseEntity<Object> updateProfilePicture(@PathVariable String userName,
			@RequestParam MultipartFile picture, @RequestHeader(value = "Authorization") String authorization) {
		byte[] bytes;
		if (picture.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		try {
			bytes = picture.getBytes();
			String imageString = Base64.getEncoder().encodeToString(bytes);
			UserModel user = (UserModel) login(authorization).getBody();
			if (user.getUserName().equalsIgnoreCase(userName)) {
				user.setProfilePicture(imageString);
				repository.save(user);
				return new ResponseEntity<>(HttpStatus.OK);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

	}

	private boolean isValid(String basicAuth, UserModel user) {
		String[] userPass = new String(Base64.getDecoder().decode(basicAuth.split(" ")[1])).split(":");
		String pass = userPass[1];
		try {
			if (user.getUserName().equals(userPass[0]) && UpdatableBCrypt.verifyHash(pass, user.getPassword())) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private boolean sendUserToAccessControl(UserModel user) {
		JSONObject json = new JSONObject();
		json.put("userId", user.getUserId());
		json.put("userName", user.getUserName());
		try {
			HttpResponse<String> res = Unirest.post("http://access-control-service:8080/api/v1/accesscontrol/users")
					.header("Content-Type", "application/json").body(json.toJSONString()).asString();
			if (res.getStatus() >= 200 && res.getStatus() < 300) {
				return true;
			}
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return false;
	}

}