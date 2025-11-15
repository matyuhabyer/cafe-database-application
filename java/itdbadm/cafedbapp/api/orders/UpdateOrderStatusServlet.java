/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.api.orders;

import itdbadm.cafedbapp.config.DatabaseConfig;
import itdbadm.cafedbapp.util.ResponseUtil;
import itdbadm.cafedbapp.util.SessionUtil;
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

@WebServlet("/api/orders/update_order_status")
public class UpdateOrderStatusServlet extends HttpServlet {
    
    private static final String[] VALID_STATUSES = {"pending", "confirmed", "completed", "cancelled"};
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (!SessionUtil.isEmployeeLoggedIn(session)) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Employee login required.", 401);
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
        
        if (!input.has("order_id") || !input.has("status")) {
            ResponseUtil.sendErrorResponse(response, "Missing required fields: order_id, status", 400);
            return;
        }
        
        int orderId = input.get("order_id").getAsInt();
        String status = input.get("status").getAsString();
        String remarks = input.has("remarks") ? input.get("remarks").getAsString() : "";
        
        // Validate status
        boolean validStatus = false;
        for (String s : VALID_STATUSES) {
            if (s.equals(status)) {
                validStatus = true;
                break;
            }
        }
        if (!validStatus) {
            ResponseUtil.sendErrorResponse(response, "Invalid status", 400);
            return;
        }
        
        int employeeId = SessionUtil.getUserId(session);
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            conn.setAutoCommit(false);
            
            // Verify order exists
            String checkQuery = "SELECT order_id, status, customer_id, loyalty_id, total_amount " +
                              "FROM OrderTbl WHERE order_id = ?";
            Map<String, Object> order = null;
            try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        ResponseUtil.sendErrorResponse(response, "Order not found", 404);
                        return;
                    }
                    order = new HashMap<>();
                    order.put("order_id", rs.getInt("order_id"));
                    order.put("status", rs.getString("status"));
                    order.put("customer_id", rs.getInt("customer_id"));
                    order.put("loyalty_id", rs.getObject("loyalty_id"));
                    order.put("total_amount", rs.getDouble("total_amount"));
                }
            }
            
            // Update order status
            String updateQuery = "UPDATE OrderTbl SET status = ? WHERE order_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                stmt.setString(1, status);
                stmt.setInt(2, orderId);
                stmt.executeUpdate();
            }
            
            // Log in order history
            String historyQuery = "INSERT INTO OrderHistory (order_id, employee_id, status, remarks) " +
                                "VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(historyQuery)) {
                stmt.setInt(1, orderId);
                stmt.setInt(2, employeeId);
                stmt.setString(3, status);
                stmt.setString(4, remarks.isEmpty() ? "Status updated to " + status : remarks);
                stmt.executeUpdate();
            }
            
            // If order is completed, update loyalty points
            int pointsEarned = 0;
            if ("completed".equals(status) && order.get("loyalty_id") != null) {
                double totalAmount = (Double) order.get("total_amount");
                pointsEarned = (int) Math.floor(totalAmount / 50);
                
                if (pointsEarned > 0) {
                    int loyaltyId = (Integer) order.get("loyalty_id");
                    String pointsQuery = "UPDATE LoyaltyCard SET points = points + ? WHERE loyalty_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(pointsQuery)) {
                        stmt.setInt(1, pointsEarned);
                        stmt.setInt(2, loyaltyId);
                        stmt.executeUpdate();
                    }
                    
                    String earnedQuery = "UPDATE OrderTbl SET earned_points = ? WHERE order_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(earnedQuery)) {
                        stmt.setInt(1, pointsEarned);
                        stmt.setInt(2, orderId);
                        stmt.executeUpdate();
                    }
                }
            }
            
            conn.commit();
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("order_id", orderId);
            responseData.put("status", status);
            responseData.put("points_earned", pointsEarned);
            
            ResponseUtil.sendJSONResponse(response, responseData, 200, "Order status updated successfully");
            
        } catch (SQLException e) {
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back: " + ex.getMessage());
            }
            System.err.println("Update order status error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while updating order status", 500);
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





