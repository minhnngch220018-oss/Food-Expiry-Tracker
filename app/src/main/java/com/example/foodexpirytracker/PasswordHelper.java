package com.example.foodexpirytracker;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/*
 * Function: PasswordHelper
 * Purpose: Generate salts, hash passwords using PBKDF2, and verify hashes
 */
public class PasswordHelper {
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256; // 32 bytes

    /*
     * Function: generateSalt
     * Purpose: Create a cryptographically secure random salt
     * Returns: Base64 salt string
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    /*
     * Function: hashPassword
     * Purpose: Derive a PBKDF2 hash from password and salt
     * Returns: Base64 hash string
     */
    public static String hashPassword(char[] password, String saltBase64) {
        try {
            byte[] salt = Base64.decode(saltBase64, Base64.NO_WRAP);
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /*
     * Function: verifyPassword
     * Purpose: Check password against stored hash via recomputation
     * Returns: true if hashes match
     */
    public static boolean verifyPassword(char[] password, String saltBase64, String expectedHashBase64) {
        String calculated = hashPassword(password, saltBase64);
        return constantTimeEquals(calculated, expectedHashBase64);
    }

    /*
     * Function: constantTimeEquals
     * Purpose: Compare two strings without timing leaks
     * Returns: true if equal
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}