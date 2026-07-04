package com.example.oms.platform.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {
    private static final String ALGORITHM = "pbkdf2_sha256";
    private static final int DEFAULT_ITERATIONS = 120000;
    private static final int KEY_LENGTH_BITS = 256;
    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return hash(password, java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(salt), DEFAULT_ITERATIONS);
    }

    public String hash(String password, String salt, int iterations) {
        return ALGORITHM + "$" + iterations + "$" + salt + "$" + digest(password, salt, iterations);
    }

    public boolean verify(String password, String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return false;
        }
        String[] parts = encoded.split("\\$", 4);
        if (parts.length != 4 || !ALGORITHM.equals(parts[0])) {
            return false;
        }
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            return false;
        }
        String actual = digest(password, parts[2], iterations);
        return java.security.MessageDigest.isEqual(
                HexFormat.of().parseHex(actual),
                HexFormat.of().parseHex(parts[3]));
    }

    private String digest(String password, String salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(java.nio.charset.StandardCharsets.UTF_8), iterations, KEY_LENGTH_BITS);
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Password hashing unavailable", exception);
        }
    }
}
