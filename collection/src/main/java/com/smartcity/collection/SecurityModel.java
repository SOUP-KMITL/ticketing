package com.smartcity.collection;

import java.io.File;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SecurityModel {
	private Cipher cipher;
	private Cipher aesCipher;
	private PrivateKey privateKey;
	private final String privateKeyPath = "/key/private";

	SecurityModel() {
		try {
			privateKey = readPrivate();
			cipher = Cipher.getInstance("RSA");
			aesCipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private PrivateKey readPrivate() throws Exception {
		byte[] keyBytes = Files.readAllBytes(new File(privateKeyPath).toPath());
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}


	public byte[] decrypt(byte[] key,byte[] data) {
		try {
			byte[] aesByteKey = cipher.doFinal(key);
			SecretKey aesKey = new SecretKeySpec(aesByteKey, 0, aesByteKey.length, "AES");
			aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
			return aesCipher.doFinal(data);
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}


}
