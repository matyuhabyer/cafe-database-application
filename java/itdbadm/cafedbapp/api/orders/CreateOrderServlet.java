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
            conn.setAutoCommit(false);
            
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
            
            // Calculate total amount
            double totalAmount = 0;
            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                if (!item.has("menu_id") || !item.has("quantity") || !item.has("price")) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Invalid item data", 400);
                    return;
                }
                double itemTotal = item.get("price").getAsDouble() * item.get("quantity").getAsInt();
                totalAmount += itemTotal;
            }
            
            // Create order
            String orderQuery = "INSERT INTO OrderTbl (customer_id, loyalty_id, branch_id, total_amount, status) " +
                              "VALUES (?, ?, ?, ?, 'pending')";
            int orderId;
            try (PreparedStatement stmt = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, customerId);
                if (loyaltyId != null) {
                    stmt.setInt(2, loyaltyId);
                } else {
                    stmt.setNull(2, Types.INTEGER);
                }
                stmt.setInt(3, branchId);
                stmt.setDouble(4, totalAmount);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Failed to create order", 500);
                    return;
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        orderId = generatedKeys.getInt(1);
                    } else {
                        conn.rollback();
                        ResponseUtil.sendErrorResponse(response, "Failed to create order", 500);
                        return;
                    }
                }
            }
            
            // Insert order items
            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                int menuId = item.get("menu_id").getAsInt();
                int quantity = item.get("quantity").getAsInt();
                double price = item.get("price").getAsDouble();
                Integer drinkOptionId = item.has("drink_option_id") && !item.get("drink_option_id").isJsonNull() 
                                       ? item.get("drink_option_id").getAsInt() : null;
                
                // Insert order item
                String itemQuery = "INSERT INTO OrderItem (order_id, menu_id, drink_option_id, quantity, price) " +
                                 "VALUES (?, ?, ?, ?, ?)";
                int orderItemId;
                try (PreparedStatement stmt = conn.prepareStatement(itemQuery, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, orderId);
                    stmt.setInt(2, menuId);
                    if (drinkOptionId != null) {
                        stmt.setInt(3, drinkOptionId);
                    } else {
                        stmt.setNull(3, Types.INTEGER);
                    }
                    stmt.setInt(4, quantity);
                    stmt.setDouble(5, price);
                    
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected == 0) {
                        conn.rollback();
                        ResponseUtil.sendErrorResponse(response, "Failed to create order item", 500);
                        return;
                    }
                    
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            orderItemId = generatedKeys.getInt(1);
                        } else {
                            conn.rollback();
                            ResponseUtil.sendErrorResponse(response, "Failed to create order item", 500);
                            return;
                        }
                    }
                }
                
                // Insert order item extras if any
                if (item.has("extras") && item.get("extras").isJsonArray()) {
                    JsonArray extras = item.getAsJsonArray("extras");
                    for (int j = 0; j < extras.size(); j++) {
                        JsonObject extra = extras.get(j).getAsJsonObject();
                        if (extra.has("extra_id") && extra.has("quantity")) {
                            String extraQuery = "INSERT INTO OrderItemExtra (order_item_id, extra_id, quantity) " +
                                             "VALUES (?, ?, ?)";
                            try (PreparedStatement extraStmt = conn.prepareStatement(extraQuery)) {
                                extraStmt.setInt(1, orderItemId);
                                extraStmt.setInt(2, extra.get("extra_id").getAsInt());
                                extraStmt.setInt(3, extra.get("quantity").getAsInt());
                                
                                int rowsAffected = extraStmt.executeUpdate();
                                if (rowsAffected == 0) {
                                    conn.rollback();
                                    ResponseUtil.sendErrorResponse(response, "Failed to add extra to order item", 500);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            
            // Create initial order history entry
            String historyQuery = "INSERT INTO OrderHistory (order_id, status, remarks) " +
                                "VALUES (?, 'pending', 'Order created by customer')";
            try (PreparedStatement historyStmt = conn.prepareStatement(historyQuery)) {
                historyStmt.setInt(1, orderId);
                int rowsAffected = historyStmt.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Failed to create order history", 500);
                    return;
                }
            }
            
            // Commit transaction
            conn.commit();
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("order_id", orderId);
            responseData.put("total_amount", Math.round(totalAmount * 100.0) / 100.0);
            responseData.put("status", "pending");
            
            ResponseUtil.sendJSONResponse(response, responseData, 201, "Order created successfully");
            
        } catch (SQLException e) {
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
            System.err.println("Create order error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while creating order", 500);
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





