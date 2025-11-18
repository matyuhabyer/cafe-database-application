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
        
        Integer employeeIdObj = SessionUtil.getUserId(session);
        if (employeeIdObj == null) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Invalid session.", 401);
            return;
        }
        int employeeId = employeeIdObj;
        String role = (String) session.getAttribute("role");
        Object branchIdObj = session.getAttribute("branch_id");
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            // Verify order exists and get status for validation
            // Stored procedure will also validate order existence, but we check here for early validation
            String checkQuery = "SELECT order_id, status, customer_id, loyalty_id, total_amount, branch_id " +
                              "FROM OrderTbl WHERE order_id = ?";
            Map<String, Object> order = null;
            try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
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
            
            // Business Rule: Orders must have at least one transaction before being marked as "completed"
            // This check is kept here as it's business logic validation before calling the stored procedure
            // The stored procedure handles authorization and status transitions
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
                                ResponseUtil.sendErrorResponse(response, 
                                    "Cannot complete order. Order must have at least one completed transaction before being marked as completed.", 400);
                                return;
                            }
                        }
                    }
                }
            }
            
            // Business Rule: Cannot cancel completed orders
            // This check is kept here as it's business logic validation before calling the stored procedure
            if ("cancelled".equals(status)) {
                String currentStatus = (String) order.get("status");
                if ("completed".equals(currentStatus)) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Cannot cancel a completed order. Completed orders cannot be changed.", 400);
                    return;
                }
            }
            
            // Call stored procedure sp_update_order_status
            // Stored procedure handles: authorization checks, branch validation, status transitions, and OrderHistory logging
            String callQuery = "{CALL sp_update_order_status(?, ?, ?, ?)}";
            try (CallableStatement cstmt = conn.prepareCall(callQuery)) {
                cstmt.setInt(1, orderId);  // p_order_id
                cstmt.setString(2, status);  // p_new_status
                cstmt.setInt(3, employeeId);  // p_employee_id
                cstmt.setString(4, remarks);  // p_remarks
                
                // Execute stored procedure
                cstmt.execute();
                
                System.out.println("UpdateOrderStatusServlet: Updated order " + orderId + 
                    " status to " + status + " with employee_id " + employeeId + 
                    " using stored procedure");
            }
            
            // Get earned_points if status was changed to completed (set by trigger 5)
            int pointsEarned = 0;
            if ("completed".equals(status) && order.get("loyalty_id") != null) {
                String pointsQuery = "SELECT earned_points FROM OrderTbl WHERE order_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(pointsQuery)) {
                    stmt.setInt(1, orderId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            pointsEarned = rs.getInt("earned_points");
                        }
                    }
                }
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("order_id", orderId);
            responseData.put("status", status);
            responseData.put("points_earned", pointsEarned);
            
            ResponseUtil.sendJSONResponse(response, responseData, 200, "Order status updated successfully");
            
        } catch (SQLException e) {
            System.err.println("Update order status error: " + e.getMessage());
            e.printStackTrace();
            
            // Handle stored procedure and trigger violations with better error messages
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("not authorized") || errorMsg.contains("another branch") || 
                    errorMsg.contains("not authorized to modify orders")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Authorization error: " + errorMsg, 403);
                } else if (errorMsg.contains("branch") || errorMsg.contains("Branch")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Branch restriction: " + errorMsg, 403);
                } else if (errorMsg.contains("Employee not found")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Authorization error: " + errorMsg, 403);
                } else if (errorMsg.contains("Order not found")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Order not found: " + errorMsg, 404);
                } else if (errorMsg.contains("Cannot change status")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Status transition error: " + errorMsg, 400);
                } else {
                    ResponseUtil.sendErrorResponse(response, 
                        "An error occurred while updating order status: " + errorMsg, 500);
                }
            } else {
                ResponseUtil.sendErrorResponse(response, "An error occurred while updating order status", 500);
            }
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}





