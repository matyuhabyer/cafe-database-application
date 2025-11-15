/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.api.auth;

/**
 *
 * @author Matthew Javier
 */
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
import java.util.regex.Pattern;

@WebServlet("/api/auth/register")
public class RegisterServlet extends HttpServlet {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
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
        
        // Validate required fields
        String[] required = {"username", "password", "name", "email", "phone_num"};
        for (String field : required) {
            if (!input.has(field) || input.get(field).getAsString().trim().isEmpty()) {
                ResponseUtil.sendErrorResponse(response, "Missing required field: " + field, 400);
                return;
            }
        }
        
        String username = input.get("username").getAsString().trim();
        String password = input.get("password").getAsString().trim();
        String name = input.get("name").getAsString().trim();
        String email = input.get("email").getAsString().trim();
        String phoneNum = input.get("phone_num").getAsString().trim();
        
        // Validate email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            ResponseUtil.sendErrorResponse(response, "Invalid email format", 400);
            return;
        }
        
        // Validate password strength
        if (password.length() < 6) {
            ResponseUtil.sendErrorResponse(response, "Password must be at least 6 characters long", 400);
            return;
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            System.err.println("RegisterServlet: Failed to get database connection");
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            // Verify connection is valid
            if (conn.isClosed()) {
                System.err.println("RegisterServlet: Database connection is closed");
                ResponseUtil.sendErrorResponse(response, "Database connection is closed", 500);
                return;
            }
            
            conn.setAutoCommit(false); // Start transaction
            
            // Check if username already exists
            String checkQuery = "SELECT customer_id FROM Customer WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        ResponseUtil.sendErrorResponse(response, "Username already exists", 400);
                        return;
                    }
                }
            }
            
            // Check if email already exists
            checkQuery = "SELECT customer_id FROM Customer WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        ResponseUtil.sendErrorResponse(response, "Email already registered", 400);
                        return;
                    }
                }
            }
            
            // Insert customer
            String insertCustomer = "INSERT INTO Customer (username, password, name, email, phone_num) " +
                                   "VALUES (?, ?, ?, ?, ?)";
            int customerId;
            try (PreparedStatement stmt = conn.prepareStatement(insertCustomer, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, username);
                stmt.setString(2, PasswordUtil.hashPassword(password));
                stmt.setString(3, name);
                stmt.setString(4, email);
                stmt.setString(5, phoneNum);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Failed to create customer account", 500);
                    return;
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        customerId = generatedKeys.getInt(1);
                    } else {
                        conn.rollback();
                        ResponseUtil.sendErrorResponse(response, "Failed to create customer account", 500);
                        return;
                    }
                }
            }
            
            // Generate loyalty card number
            String cardNumber = "LC-" + String.format("%06d", customerId);
            
            // Create loyalty card
            String insertLoyalty = "INSERT INTO LoyaltyCard (customer_id, card_number, points, is_active) " +
                                  "VALUES (?, ?, 0, 1)";
            int loyaltyId;
            try (PreparedStatement stmt = conn.prepareStatement(insertLoyalty, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, customerId);
                stmt.setString(2, cardNumber);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Failed to create loyalty card", 500);
                    return;
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        loyaltyId = generatedKeys.getInt(1);
                    } else {
                        conn.rollback();
                        ResponseUtil.sendErrorResponse(response, "Failed to create loyalty card", 500);
                        return;
                    }
                }
            }
            
            // Commit transaction
            conn.commit();
            
            // Start session
            HttpSession session = request.getSession(true);
            session.setAttribute("user_id", customerId);
            session.setAttribute("user_type", "customer");
            session.setAttribute("username", username);
            session.setAttribute("name", name);
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("customer_id", customerId);
            userData.put("name", name);
            userData.put("email", email);
            userData.put("phone_num", phoneNum);
            userData.put("loyalty_id", loyaltyId);
            userData.put("card_number", cardNumber);
            userData.put("points", 0);
            userData.put("role", "customer");
            
            ResponseUtil.sendJSONResponse(response, userData, 201, "Registration successful");
            
        } catch (SQLException e) {
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                    System.out.println("Transaction rolled back due to error");
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
                ex.printStackTrace();
            }
            System.err.println("Registration error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred during registration: " + e.getMessage(), 500);
        } catch (Exception e) {
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                    System.out.println("Transaction rolled back due to unexpected error");
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
            System.err.println("Unexpected error in RegisterServlet: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An unexpected error occurred during registration", 500);
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}





