package com.smartcity.ticket;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import org.json.simple.JSONObject;

public class SecurityModel {
	private static SecurityModel instance = null;
	private Cipher cipher;
	private Cipher aesCipher;
	private PublicKey publicKey;
	private PrivateKey privateKey;
	private final String publicKeyPath = "/key/public";
	private final String privateKeyPath = "/key/private";

	private SecurityModel() {
		try {
			cipher = Cipher.getInstance("RSA");
			aesCipher = Cipher.getInstance("AES");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e1) {
			e1.printStackTrace();
		}
		File f = new File(publicKeyPath);
		if (publicKey == null && !f.exists()) {
			KeyPairGenerator keyGen = null;
			try {
				keyGen = KeyPairGenerator.getInstance("RSA");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			keyGen.initialize(2048);
			KeyPair keyPair = keyGen.generateKeyPair();
			publicKey = keyPair.getPublic();
			privateKey = keyPair.getPrivate();
			try {
				writeToFile(publicKeyPath, publicKey.getEncoded());
				writeToFile(privateKeyPath, privateKey.getEncoded());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				publicKey = readPublic();
				privateKey = readPrivate();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	public static SecurityModel getInstance() {
		if (instance == null) {
			instance = new SecurityModel();
		}
		return instance;
	}

	private PublicKey readPublic() throws Exception {
		byte[] keyBytes = Files.readAllBytes(new File(publicKeyPath).toPath());
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
	}

	private PrivateKey readPrivate() throws Exception {
		byte[] keyBytes = Files.readAllBytes(new File(privateKeyPath).toPath());
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	@SuppressWarnings("unchecked")
	public JSONObject encrypt(byte[] input) {
		try {
			SecretKey aesKey = generateAESKey();
			aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
			byte[] enData = aesCipher.doFinal(input);
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] dataKey = cipher.doFinal(aesKey.getEncoded());
			JSONObject json = new JSONObject();
			json.put("key", Base64.getUrlEncoder().encodeToString(dataKey));
			json.put("data", Base64.getUrlEncoder().encodeToString(enData));
			return json;
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private SecretKey generateAESKey() {
		KeyGenerator keygenerator = null;
		try {
			keygenerator = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		SecretKey aesKey = keygenerator.generateKey();
		return aesKey;
	}
	private void writeToFile(String path, byte[] key) throws IOException {
		File f = new File(path);
		f.getParentFile().mkdirs();

		FileOutputStream fos = new FileOutputStream(f);
		fos.write(key);
		fos.flush();
		fos.close();
	}
}
