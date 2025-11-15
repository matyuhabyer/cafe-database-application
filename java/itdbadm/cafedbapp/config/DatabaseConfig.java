/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.config;

import java.sql.*;

/**
 * Database Configuration and Connection Utility
 */
public class DatabaseConfig {
    
    // Database connection settings
    // Using localhost:3307 for SSH tunnel connection
    // Make sure SSH tunnel is active: ssh -L 3307:ccscloud.dlsu.edu.ph:21013 your_username@ccscloud.dlsu.edu.ph
    private static final String DB_HOST = "localhost:3307";
    private static final String DB_USER = "student2";
    private static final String DB_PASS = "ITDBADM";
    private static final String DB_NAME = "cafe_db";
    
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + "/" + DB_NAME 
            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }
    
    /**
     * Get database connection
     * @return Connection object or null on failure
     */
    public static Connection getDBConnection() {
        try {
            System.out.println("Attempting database connection...");
            System.out.println("URL: " + DB_URL);
            System.out.println("User: " + DB_USER);
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("✅ Database connection successful!");
            return conn;
        } catch (SQLException e) {
            System.err.println("========================================");
            System.err.println("❌ DATABASE CONNECTION FAILED!");
            System.err.println("========================================");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Connection URL: " + DB_URL);
            System.err.println("User: " + DB_USER);
            System.err.println();
            System.err.println("Troubleshooting Steps:");
            System.err.println("1. Check if SSH tunnel is active:");
            System.err.println("   ssh -L 3307:ccscloud.dlsu.edu.ph:21013 your_username@ccscloud.dlsu.edu.ph");
            System.err.println("2. Verify port 3307 is listening:");
            System.err.println("   Windows: netstat -an | findstr 3307");
            System.err.println("   Linux/Mac: netstat -an | grep 3307");
            System.err.println("3. Test connection using TestConnection.java");
            System.err.println("4. Verify MySQL credentials are correct");
            System.err.println("5. Check if MySQL JDBC driver is in classpath");
            System.err.println("========================================");
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Close database connection
     * @param conn Connection to close
     */
    public static void closeDBConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
}





