package com.scasp.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PBKDF2-HMAC-SHA256 password hashing using only the JDK. Stored format:
 * {@code pbkdf2-sha256$<iterations>$<base64 salt>$<base64 hash>}. Verification is constant-time.
 * Plain-text passwords are never stored or logged.
 */
public final class PasswordHasher {
    public static final int DEFAULT_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16, KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private PasswordHasher() {}

    public static String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return hash(password, salt, DEFAULT_ITERATIONS);
    }
    static String hash(char[] password, byte[] salt, int iterations) {
        byte[] dk = derive(password, salt, iterations);
        return "pbkdf2-sha256$" + iterations + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(dk);
    }
    public static boolean verify(char[] password, String stored) {
        if (password == null || stored == null) return false;
        String[] p = stored.split("\\$");
        if (p.length != 4 || !p[0].equals("pbkdf2-sha256")) return false;
        try {
            int iterations = Integer.parseInt(p[1]);
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] expected = Base64.getDecoder().decode(p[3]);
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        } finally {
            spec.clearPassword();
        }
    }
    /** Utility for creating sample-data hashes: java -cp core.jar com.scasp.security.PasswordHasher &lt;password&gt; */
    public static void main(String[] args) {
        for (String a : args) System.out.println(hash(a.toCharArray()));
    }
}
