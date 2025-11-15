/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.api.auth;

import itdbadm.cafedbapp.config.DatabaseConfig;
import itdbadm.cafedbapp.util.PasswordUtil;
import itdbadm.cafedbapp.util.ResponseUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Read JSON input
        StringBuilder jsonInput = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonInput.append(line);
            }
        }
        
        JsonObject input = JsonParser.parseString(jsonInput.toString()).getAsJsonObject();
        
        // Validate input
        if (!input.has("username") || !input.has("password") || !input.has("role")) {
            ResponseUtil.sendErrorResponse(response, "Missing required fields: username, password, role", 400);
            return;
        }
        
        String username = input.get("username").getAsString().trim();
        String password = input.get("password").getAsString();
        String role = input.get("role").getAsString();
        
        // Validate role
        String[] validRoles = {"customer", "staff", "manager", "admin"};
        boolean validRole = false;
        for (String r : validRoles) {
            if (r.equals(role)) {
                validRole = true;
                break;
            }
        }
        if (!validRole) {
            ResponseUtil.sendErrorResponse(response, "Invalid role specified", 400);
            return;
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            System.err.println("LoginServlet: Failed to get database connection");
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            // Verify connection is valid
            if (conn.isClosed()) {
                System.err.println("LoginServlet: Database connection is closed");
                ResponseUtil.sendErrorResponse(response, "Database connection is closed", 500);
                return;
            }
            String hashedPassword = PasswordUtil.hashPassword(password);
            
            // Debug logging (remove in production)
            System.out.println("Login attempt - Username: " + username + ", Role: " + role);
            System.out.println("Hashed password: " + hashedPassword);
            
            if ("customer".equals(role)) {
                // Customer login
                String query = "SELECT c.customer_id, c.name, c.email, c.phone_num, c.password, " +
                              "l.loyalty_id, l.card_number, l.points, l.is_active " +
                              "FROM Customer c " +
                              "LEFT JOIN LoyaltyCard l ON c.customer_id = l.customer_id " +
                              "WHERE c.username = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("User not found: " + username);
                            ResponseUtil.sendErrorResponse(response, "Invalid username or password", 401);
                            return;
                        }
                        
                        String dbPassword = rs.getString("password");
                        System.out.println("Database password hash: " + dbPassword);
                        System.out.println("Database password hash length: " + (dbPassword != null ? dbPassword.length() : "null"));
                        
                        // Check if password matches using verification method
                        if (!PasswordUtil.verifyPassword(password, dbPassword)) {
                            System.out.println("Password verification failed");
                            System.out.println("Generated hash (UTF-8): " + hashedPassword);
                            System.out.println("Generated hash (default): " + PasswordUtil.hashPasswordDefaultEncoding(password));
                            ResponseUtil.sendErrorResponse(response, "Invalid username or password", 401);
                            return;
                        }
                        
                        System.out.println("Password verified successfully!");
                        
                        // Start session
                        HttpSession session = request.getSession(true);
                        session.setAttribute("user_id", rs.getInt("customer_id"));
                        session.setAttribute("user_type", "customer");
                        session.setAttribute("username", username);
                        session.setAttribute("name", rs.getString("name"));
                        
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("user_id", rs.getInt("customer_id"));
                        userData.put("name", rs.getString("name"));
                        userData.put("email", rs.getString("email"));
                        userData.put("phone_num", rs.getString("phone_num"));
                        userData.put("loyalty_id", rs.getObject("loyalty_id"));
                        userData.put("card_number", rs.getString("card_number"));
                        userData.put("points", rs.getInt("points"));
                        userData.put("role", "customer");
                        
                        ResponseUtil.sendJSONResponse(response, userData, 200, "Login successful");
                    }
                }
            } else {
                // Employee login
                String query = "SELECT e.employee_id, e.name, e.role, e.contact_num, e.branch_id, e.password, " +
                              "b.name as branch_name " +
                              "FROM Employee e " +
                              "LEFT JOIN Branch b ON e.branch_id = b.branch_id " +
                              "WHERE e.username = ? AND e.role = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    stmt.setString(2, role);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("Employee not found: " + username + " with role: " + role);
                            ResponseUtil.sendErrorResponse(response, "Invalid username, password, or role", 401);
                            return;
                        }
                        
                        String dbPassword = rs.getString("password");
                        System.out.println("Database password hash: " + dbPassword);
                        System.out.println("Database password hash length: " + (dbPassword != null ? dbPassword.length() : "null"));
                        
                        // Check if password matches using verification method
                        if (!PasswordUtil.verifyPassword(password, dbPassword)) {
                            System.out.println("Password verification failed");
                            System.out.println("Generated hash (UTF-8): " + hashedPassword);
                            System.out.println("Generated hash (default): " + PasswordUtil.hashPasswordDefaultEncoding(password));
                            ResponseUtil.sendErrorResponse(response, "Invalid username, password, or role", 401);
                            return;
                        }
                        
                        System.out.println("Password verified successfully!");
                        
                        // Start session
                        HttpSession session = request.getSession(true);
                        session.setAttribute("user_id", rs.getInt("employee_id"));
                        session.setAttribute("user_type", "employee");
                        session.setAttribute("username", username);
                        session.setAttribute("name", rs.getString("name"));
                        session.setAttribute("role", rs.getString("role"));
                        session.setAttribute("branch_id", rs.getObject("branch_id"));
                        
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("employee_id", rs.getInt("employee_id"));
                        userData.put("name", rs.getString("name"));
                        userData.put("role", rs.getString("role"));
                        userData.put("contact_num", rs.getString("contact_num"));
                        userData.put("branch_id", rs.getObject("branch_id"));
                        userData.put("branch_name", rs.getString("branch_name"));
                        
                        ResponseUtil.sendJSONResponse(response, userData, 200, "Login successful");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred during login: " + e.getMessage(), 500);
        } catch (Exception e) {
            System.err.println("Unexpected error in LoginServlet: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An unexpected error occurred during login", 500);
        } finally {
            if (conn != null) {
                DatabaseConfig.closeDBConnection(conn);
            }
        }
    }
}





