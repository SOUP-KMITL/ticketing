package com.smartcity.user;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
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
import org.springframework.web.bind.annotation.ResponseBody;

@CrossOrigin
@RestController
public class UserController {
	@Autowired
	private UserModelRepository repository;
	private final String AC_URL = System.getenv("AC_URL");

	public UserController() {
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
				user.setAccessToken(user.generateAccessToken());
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

	@GetMapping(value = "/{userName}/thumbnail", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<InputStreamResource> getThumbnail(@PathVariable String userName) {
		try {
			String picture = repository.findByuserName(userName).get(0).getThumbnailBase64();
			byte[] pictureBtyes = Base64.getDecoder().decode(picture);
			ByteArrayInputStream targetStream = new ByteArrayInputStream(pictureBtyes);
			InputStreamResource isr = new InputStreamResource(targetStream);
			return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(isr);

		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@PutMapping("/{userName}/thumbnail")
	public ResponseEntity<Object> updateThumbnail(@PathVariable String userName, @RequestParam MultipartFile thumbnail,
			@RequestHeader(value = "Authorization") String authorization) {
		byte[] bytes;
		if (thumbnail.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		try {
			bytes = thumbnail.getBytes();
			String imageString = Base64.getEncoder().encodeToString(bytes);
			UserModel user = (UserModel) login(authorization).getBody();
			if (user.getUserName().equalsIgnoreCase(userName)) {
				user.setThumbnailBase64(imageString);
				user.setThumbnail("https://api.smartcity.kmitl.io/api/v1/users/" + user.getUserName() + "/thumbnail");
				repository.save(user);
				return new ResponseEntity<>(HttpStatus.OK);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

	}

	@SuppressWarnings("unchecked")
	@PutMapping("/{userName}")
	public ResponseEntity<Object> updateMeta(@PathVariable String userName,
			@RequestHeader(value = "Authorization") String authorization, @RequestBody JSONObject json) {
		UserModel user = null;
		try {
			user = repository.findByuserName(userName).get(0);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		String email = (String) json.getOrDefault("email", "");
		String firstName = (String) json.getOrDefault("firstName", "");
		String lastName = (String) json.getOrDefault("lastName", "");
		String password = (String) json.getOrDefault("password", "");
		if (!email.isEmpty()) {
			user.setEmail(email);
		}
		if (!firstName.isEmpty()) {
			user.setFirstName(firstName);
		}
		if (!lastName.isEmpty()) {
			user.setLastName(lastName);;
		}
		if (!password.isEmpty()) {
			user.setPassword(password);;
		}
		repository.save(user);
		return new ResponseEntity<Object>(HttpStatus.OK);
	}

	@DeleteMapping("/{userName}")
	public ResponseEntity<Object> deleteUser(@PathVariable String userName,
			@RequestHeader(value = "Authorization") String authorization) {
		UserModel user = (UserModel) login(authorization).getBody();
		if (user.getUserName().equalsIgnoreCase(userName)) {
			try {
				HttpResponse<String> res = Unirest.delete(AC_URL + "/users/{userName}")
						.routeParam("userName", user.getUserName()).header("Content-Type", "application/json")
						.asString();
			} catch (UnirestException e) {
			}
			repository.delete(user);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
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
			HttpResponse<String> res = Unirest.post(AC_URL + "/users").header("Content-Type", "application/json")
					.body(json.toJSONString()).asString();

			if (res.getStatus() >= 200 && res.getStatus() < 300) {
				return true;
			}
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return false;
	}

}