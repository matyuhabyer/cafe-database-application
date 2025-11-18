/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.util;

/**
 *
 * @author Matthew Javier
 */
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for password hashing (MD5 - matching PHP implementation)
 * Note: In production, use bcrypt or Argon2 instead of MD5
 */
public class PasswordUtil {
    
    /**
     * Hash password using MD5 (matching PHP md5() function)
     * @param password Plain text password
     * @return MD5 hash (lowercase)
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            // Try UTF-8 encoding first (most common)
            byte[] hashBytes = md.digest(password.getBytes("UTF-8"));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 hashing failed", e);
        }
    }
    
    /**
     * Hash password using MD5 with default encoding (fallback)
     * @param password Plain text password
     * @return MD5 hash (lowercase)
     */
    public static String hashPasswordDefaultEncoding(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            // Use default platform encoding
            byte[] hashBytes = md.digest(password.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 hashing failed", e);
        }
    }
    
    /**
     * Check if password matches database hash, trying multiple hash formats
     * @param password Plain text password
     * @param dbHash Hash from database
     * @return true if password matches
     */
    public static boolean verifyPassword(String password, String dbHash) {
        if (dbHash == null || dbHash.trim().isEmpty()) {
            return false;
        }
        
        // Trim whitespace from database hash
        String trimmedDbHash = dbHash.trim();
        
        // Try UTF-8 encoding (most common)
        String hashUTF8 = hashPassword(password);
        if (hashUTF8.equalsIgnoreCase(trimmedDbHash)) {
            return true;
        }
        
        // Try default encoding (fallback)
        String hashDefault = hashPasswordDefaultEncoding(password);
        if (hashDefault.equalsIgnoreCase(trimmedDbHash)) {
            return true;
        }
        
        return false;
    }
}

