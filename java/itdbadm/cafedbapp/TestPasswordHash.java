/*
 * Test utility to verify password hashing matches database
 */
package itdbadm.cafedbapp;

import itdbadm.cafedbapp.util.PasswordUtil;
import java.sql.*;

public class TestPasswordHash {
    
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: TestPasswordHash <username> <password> <role>");
            System.out.println("Example: TestPasswordHash testuser mypassword customer");
            return;
        }
        
        String username = args[0];
        String password = args[1];
        String role = args[2];
        
        System.out.println("========================================");
        System.out.println("Password Hash Test");
        System.out.println("========================================");
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
        System.out.println("Role: " + role);
        System.out.println();
        
        // Generate hashes
        String hashUTF8 = PasswordUtil.hashPassword(password);
        String hashDefault = PasswordUtil.hashPasswordDefaultEncoding(password);
        
        System.out.println("Generated Hash (UTF-8): " + hashUTF8);
        System.out.println("Generated Hash (Default): " + hashDefault);
        System.out.println();
        
        // Connect to database and check
        String url = "jdbc:mysql://localhost:3307/cafe_db?useSSL=false&serverTimezone=UTC";
        String user = "student2";
        String pass = "ITDBADM";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Connected to database");
            System.out.println();
            
            if ("customer".equals(role)) {
                String query = "SELECT username, password, LENGTH(password) as pwd_len FROM Customer WHERE username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String dbHash = rs.getString("password");
                            int pwdLen = rs.getInt("pwd_len");
                            
                            System.out.println("Database Hash: " + dbHash);
                            System.out.println("Database Hash Length: " + pwdLen);
                            System.out.println();
                            
                            System.out.println("Comparison Results:");
                            System.out.println("UTF-8 hash matches: " + hashUTF8.equalsIgnoreCase(dbHash.trim()));
                            System.out.println("Default hash matches: " + hashDefault.equalsIgnoreCase(dbHash.trim()));
                            System.out.println();
                            
                            boolean verified = PasswordUtil.verifyPassword(password, dbHash);
                            System.out.println("PasswordUtil.verifyPassword(): " + verified);
                            
                        } else {
                            System.out.println("❌ User not found in database!");
                        }
                    }
                }
            } else {
                String query = "SELECT username, password, role, LENGTH(password) as pwd_len FROM Employee WHERE username = ? AND role = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    stmt.setString(2, role);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String dbHash = rs.getString("password");
                            int pwdLen = rs.getInt("pwd_len");
                            String dbRole = rs.getString("role");
                            
                            System.out.println("Database Hash: " + dbHash);
                            System.out.println("Database Hash Length: " + pwdLen);
                            System.out.println("Database Role: " + dbRole);
                            System.out.println();
                            
                            System.out.println("Comparison Results:");
                            System.out.println("UTF-8 hash matches: " + hashUTF8.equalsIgnoreCase(dbHash.trim()));
                            System.out.println("Default hash matches: " + hashDefault.equalsIgnoreCase(dbHash.trim()));
                            System.out.println();
                            
                            boolean verified = PasswordUtil.verifyPassword(password, dbHash);
                            System.out.println("PasswordUtil.verifyPassword(): " + verified);
                            
                        } else {
                            System.out.println("❌ Employee not found in database with username: " + username + " and role: " + role);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("========================================");
    }
}

