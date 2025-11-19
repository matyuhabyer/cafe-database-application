/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp;
import java.sql.*;

public class TestConnection {

    public static void main(String[] args) {

        // --- 1. Connection Details ---
        // These MUST match your setup:
        
        // Connect to the LOCAL end of your SSH tunnel
        String url = "jdbc:mysql://localhost:3307/cafe_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"; 
        
        // Your MySQL username
        String user = "student2"; 
        
        // Your MySQL password (NOT your SSH password)
        String password = "ITDBADM"; 

        // --- 2. Try to Connect ---
        System.out.println("Attempting to connect to database...");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            
            if (conn != null) {
                System.out.println("----------------------------------------");
                System.out.println("✅ SUCCESS: Connection established!");
                System.out.println("Connected to: " + conn.getMetaData().getURL());
                System.out.println("Database: " + conn.getCatalog());
                System.out.println("MySQL Version: " + conn.getMetaData().getDatabaseProductVersion());
                System.out.println("Driver: " + conn.getMetaData().getDriverName());
                System.out.println("----------------------------------------");
                
                // Test a simple query
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1 as test")) {
                    if (rs.next()) {
                        System.out.println("✅ Query test successful! Database is accessible.");
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("----------------------------------------");
            System.out.println("❌ FAILED: Connection error!");
            System.out.println("Error Message: " + e.getMessage());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("SQL State: " + e.getSQLState());
            System.out.println();
            System.out.println("Troubleshooting Tips:");
            System.out.println("1. Make sure SSH tunnel is active:");
            System.out.println("   ssh -L 3307:ccscloud.dlsu.edu.ph:3306 your_username@ccscloud.dlsu.edu.ph");
            System.out.println("2. Verify MySQL credentials are correct");
            System.out.println("3. Check if port 3307 is available");
            System.out.println("4. Ensure MySQL JDBC driver is in classpath");
            System.out.println("----------------------------------------");
            e.printStackTrace();
        }
    }
}