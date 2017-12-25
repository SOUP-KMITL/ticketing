package com.smartcity.user;

import java.util.function.Function;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdatableBCrypt {
	private static final Logger log = LoggerFactory.getLogger(UpdatableBCrypt.class);

	private static final int logRounds = 11;

	public static String hash(String password) {
		return BCrypt.hashpw(password, BCrypt.gensalt(logRounds));
	}

	public static boolean verifyHash(String password, String hash) {
		return BCrypt.checkpw(password, hash);
	}

	public boolean verifyAndUpdateHash(String password, String hash, Function<String, Boolean> updateFunc) {
		if (BCrypt.checkpw(password, hash)) {
			int rounds = getRounds(hash);
			if (rounds != logRounds) {
				log.debug("Updating password from {} rounds to {}", rounds, logRounds);
				String newHash = hash(password);
				return updateFunc.apply(newHash);
			}
			return true;
		}
		return false;
	}

	private int getRounds(String salt) {
		char minor = (char) 0;
		int off = 0;

		if (salt.charAt(0) != '$' || salt.charAt(1) != '2')
			throw new IllegalArgumentException("Invalid salt version");
		if (salt.charAt(2) == '$')
			off = 3;
		else {
			minor = salt.charAt(2);
			if (minor != 'a' || salt.charAt(3) != '$')
				throw new IllegalArgumentException("Invalid salt revision");
			off = 4;
		}

		if (salt.charAt(off + 2) > '$')
			throw new IllegalArgumentException("Missing salt rounds");
		return Integer.parseInt(salt.substring(off, off + 2));
	}
}
