/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.api.orders;

import itdbadm.cafedbapp.config.DatabaseConfig;
import itdbadm.cafedbapp.util.ResponseUtil;
import itdbadm.cafedbapp.util.SessionUtil;
import com.google.gson.JsonArray;
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

@WebServlet("/api/orders/create_order")
public class CreateOrderServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (!SessionUtil.isCustomerLoggedIn(session)) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Customer login required.", 401);
            return;
        }
        
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
        if (!input.has("items") || !input.get("items").isJsonArray() || 
            input.getAsJsonArray("items").size() == 0) {
            ResponseUtil.sendErrorResponse(response, "Order must contain at least one item", 400);
            return;
        }
        
        if (!input.has("branch_id")) {
            ResponseUtil.sendErrorResponse(response, "Branch ID is required", 400);
            return;
        }
        
        int customerId = SessionUtil.getUserId(session);
        int branchId = input.get("branch_id").getAsInt();
        JsonArray items = input.getAsJsonArray("items");
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            // Get customer's loyalty card
            Integer loyaltyId = null;
            String loyaltyQuery = "SELECT loyalty_id FROM LoyaltyCard WHERE customer_id = ? AND is_active = 1";
            try (PreparedStatement stmt = conn.prepareStatement(loyaltyQuery)) {
                stmt.setInt(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        loyaltyId = rs.getInt("loyalty_id");
                    }
                }
            }
            
            // Validate items have required fields
            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                if (!item.has("menu_id") || !item.has("quantity")) {
                    ResponseUtil.sendErrorResponse(response, "Invalid item data: menu_id and quantity required", 400);
                    return;
                }
            }
            
            // Convert items JSON array to JSON string for stored procedure
            // The stored procedure expects JSON format with menu_id, quantity, drink_option_id, and extras
            String itemsJson = items.toString();
            
            // Call stored procedure sp_create_order
            // Stored procedure handles: order creation, item insertion, extras, and triggers handle price/total calculation
            String callQuery = "{CALL sp_create_order(?, ?, ?, ?, ?)}";
            int orderId;
            double finalTotal = 0;
            
            try (CallableStatement cstmt = conn.prepareCall(callQuery)) {
                cstmt.setInt(1, customerId);  // p_customer_id
                cstmt.setNull(2, Types.INTEGER);  // p_employee_id (NULL for customer orders)
                cstmt.setInt(3, branchId);  // p_branch_id
                if (loyaltyId != null) {
                    cstmt.setInt(4, loyaltyId);  // p_loyalty_id
                } else {
                    cstmt.setNull(4, Types.INTEGER);  // p_loyalty_id
                }
                cstmt.setString(5, itemsJson);  // p_items JSON
                
                // Execute stored procedure
                cstmt.execute();
                
                // Get the order_id that was just created by the stored procedure
                // Query the most recent pending order for this customer at this branch
                String orderIdQuery = "SELECT order_id FROM OrderTbl WHERE customer_id = ? AND branch_id = ? AND status = 'pending' ORDER BY order_id DESC LIMIT 1";
                try (PreparedStatement stmt = conn.prepareStatement(orderIdQuery)) {
                    stmt.setInt(1, customerId);
                    stmt.setInt(2, branchId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            orderId = rs.getInt("order_id");
                        } else {
                            ResponseUtil.sendErrorResponse(response, "Failed to create order - no order ID returned", 500);
                            return;
                        }
                    }
                }
                
                // Get final total_amount calculated by triggers
                String totalQuery = "SELECT total_amount FROM OrderTbl WHERE order_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(totalQuery)) {
                    stmt.setInt(1, orderId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            finalTotal = rs.getDouble("total_amount");
                        }
                    }
                }
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("order_id", orderId);
            responseData.put("total_amount", Math.round(finalTotal * 100.0) / 100.0);
            responseData.put("status", "pending");
            
            ResponseUtil.sendJSONResponse(response, responseData, 201, "Order created successfully");
            
        } catch (SQLException e) {
            System.err.println("Create order error: " + e.getMessage());
            e.printStackTrace();
            
            // Handle stored procedure and trigger violations with better error messages
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("Menu price must be greater than zero")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Invalid menu item: " + errorMsg, 400);
                } else if (errorMsg.contains("extra") || errorMsg.contains("Extra")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Extra validation error: " + errorMsg, 400);
                } else if (errorMsg.contains("Order item does not exist")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Invalid order item: " + errorMsg, 400);
                } else if (errorMsg.contains("SQLSTATE")) {
                    // Stored procedure error
                    ResponseUtil.sendErrorResponse(response, 
                        "Order creation failed: " + errorMsg, 500);
                } else {
                    ResponseUtil.sendErrorResponse(response, 
                        "An error occurred while creating order: " + errorMsg, 500);
                }
            } else {
                ResponseUtil.sendErrorResponse(response, "An error occurred while creating order", 500);
            }
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}





