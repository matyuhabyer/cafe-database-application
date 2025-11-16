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
        String role = (String) session.getAttribute("role");
        Object branchIdObj = session.getAttribute("branch_id");
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            conn.setAutoCommit(false);
            
            // Verify order exists and get branch_id
            String checkQuery = "SELECT order_id, status, customer_id, loyalty_id, total_amount, branch_id " +
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
                    order.put("branch_id", rs.getObject("branch_id"));
                }
            }
            
            // Business Rule: Branch restriction - Staff/Managers can only process orders from their branch
            // Admin can process orders from any branch
            if (!"admin".equals(role) && branchIdObj != null) {
                Integer orderBranchId = (Integer) order.get("branch_id");
                Integer employeeBranchId = (Integer) branchIdObj;
                
                if (orderBranchId == null || !orderBranchId.equals(employeeBranchId)) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, 
                        "Unauthorized. You can only process orders from your assigned branch.", 403);
                    return;
                }
            }
            
            // Business Rule: Orders must have at least one transaction before being marked as "completed"
            if ("completed".equals(status)) {
                String transactionCheckQuery = "SELECT COUNT(*) as transaction_count " +
                                             "FROM TransactionTbl " +
                                             "WHERE order_id = ? AND status = 'completed'";
                try (PreparedStatement stmt = conn.prepareStatement(transactionCheckQuery)) {
                    stmt.setInt(1, orderId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int transactionCount = rs.getInt("transaction_count");
                            if (transactionCount == 0) {
                                conn.rollback();
                                ResponseUtil.sendErrorResponse(response, 
                                    "Cannot complete order. Order must have at least one completed transaction before being marked as completed.", 400);
                                return;
                            }
                        }
                    }
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
            
            // Business Rule: Loyalty Points - Customers earn 1 point per ₱50.00 spent on completed transactions
            // totalAmount is stored in PHP in the database
            int pointsEarned = 0;
            if ("completed".equals(status) && order.get("loyalty_id") != null) {
                double totalAmount = (Double) order.get("total_amount");
                pointsEarned = (int) Math.floor(totalAmount / 50); // 1 point per ₱50.00
                
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





