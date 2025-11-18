/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.api.payments;

import itdbadm.cafedbapp.config.DatabaseConfig;
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

@WebServlet("/api/payments/create_transaction")
public class CreateTransactionServlet extends HttpServlet {
    
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
        
        if (!input.has("order_id") || !input.has("currency_id") || 
            !input.has("payment_method") || !input.has("amount_paid")) {
            ResponseUtil.sendErrorResponse(response, "Missing required fields", 400);
            return;
        }
        
        int orderId = input.get("order_id").getAsInt();
        int currencyId = input.get("currency_id").getAsInt();
        String paymentMethod = input.get("payment_method").getAsString().toLowerCase().trim();
        double amountPaid = input.get("amount_paid").getAsDouble();
        
        // Business Rule: Validate payment method - must be one of: cash, card, bank_transfer, others
        // Validate payment method
        String[] validPaymentMethods = {"cash", "card", "bank_transfer", "others"};
        boolean isValidPaymentMethod = false;
        for (String validMethod : validPaymentMethods) {
            if (validMethod.equals(paymentMethod)) {
                isValidPaymentMethod = true;
                break;
            }
        }
        
        if (!isValidPaymentMethod) {
            ResponseUtil.sendErrorResponse(response, 
                "Invalid payment method. Must be one of: cash, card, bank_transfer, others", 400);
            return;
        }
        
        HttpSession session = request.getSession(false);
        Integer employeeId = null;
        String role = null;
        Object branchIdObj = null;
        if (session != null && "employee".equals(session.getAttribute("user_type"))) {
            employeeId = (Integer) session.getAttribute("user_id");
            role = (String) session.getAttribute("role");
            branchIdObj = session.getAttribute("branch_id");
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            // Verify order exists and get total amount for validation
            String orderQuery = "SELECT order_id, total_amount, status, branch_id FROM OrderTbl WHERE order_id = ?";
            Map<String, Object> order = null;
            try (PreparedStatement stmt = conn.prepareStatement(orderQuery)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        ResponseUtil.sendErrorResponse(response, "Order not found", 404);
                        return;
                    }
                    order = new HashMap<>();
                    order.put("order_id", rs.getInt("order_id"));
                    order.put("total_amount", rs.getDouble("total_amount"));
                    order.put("status", rs.getString("status"));
                    order.put("branch_id", rs.getObject("branch_id"));
                }
            }
            
            // Business Rule: Check if order already has a completed transaction
            // This check is kept here as it's business logic validation before calling the stored procedure
            String existingQuery = "SELECT transaction_id FROM TransactionTbl WHERE order_id = ? AND status = 'completed'";
            try (PreparedStatement stmt = conn.prepareStatement(existingQuery)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        ResponseUtil.sendErrorResponse(response, "Order already has a completed payment", 400);
                        return;
                    }
                }
            }
            
            // Get currency exchange rate
            String currencyQuery = "SELECT rate FROM Currency WHERE currency_id = ?";
            double exchangeRate = 1.0;
            try (PreparedStatement stmt = conn.prepareStatement(currencyQuery)) {
                stmt.setInt(1, currencyId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        ResponseUtil.sendErrorResponse(response, "Currency not found", 404);
                        return;
                    }
                    exchangeRate = rs.getDouble("rate");
                }
            }
            
            // Calculate PHP equivalent and validate amount
            double amountPhp = amountPaid / exchangeRate;
            double orderTotal = (Double) order.get("total_amount");
            double tolerance = 0.01;
            
            if (Math.abs(amountPhp - orderTotal) > tolerance) {
                ResponseUtil.sendErrorResponse(response, 
                    "Payment amount mismatch. Expected: ₱" + orderTotal + ", Received: ₱" + amountPhp, 400);
                return;
            }
            
            // Get employee_id if available (can be null for customer payments)
            Integer empId = employeeId != null ? employeeId : null;
            
            // Call stored procedure sp_record_payment
            // Stored procedure handles: transaction creation, branch alignment, order status update to 'confirmed' (if pending), and OrderHistory logging
            // NOTE: Order status is NOT auto-completed - staff/admin must manually complete orders after payment is recorded
            String callQuery = "{CALL sp_record_payment(?, ?, ?, ?, ?, ?)}";
            int transactionId;
            
            try (CallableStatement cstmt = conn.prepareCall(callQuery)) {
                cstmt.setInt(1, orderId);  // p_order_id
                cstmt.setInt(2, currencyId);  // p_currency_id
                cstmt.setString(3, paymentMethod);  // p_payment_method
                cstmt.setDouble(4, amountPaid);  // p_amount_paid
                cstmt.setDouble(5, exchangeRate);  // p_exchange_rate
                if (empId != null) {
                    cstmt.setInt(6, empId);  // p_employee_id
                } else {
                    cstmt.setNull(6, Types.INTEGER);  // p_employee_id (can be null for customer payments)
                }
                
                // Execute stored procedure
                cstmt.execute();
                
                // Get transaction_id that was just created by the stored procedure
                // Query the most recent completed transaction for this order
                String transactionIdQuery = "SELECT transaction_id FROM TransactionTbl WHERE order_id = ? AND status = 'completed' ORDER BY transaction_id DESC LIMIT 1";
                try (PreparedStatement stmt = conn.prepareStatement(transactionIdQuery)) {
                    stmt.setInt(1, orderId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            transactionId = rs.getInt("transaction_id");
                        } else {
                            ResponseUtil.sendErrorResponse(response, "Failed to create transaction - no transaction ID returned", 500);
                            return;
                        }
                    }
                }
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("transaction_id", transactionId);
            responseData.put("order_id", orderId);
            responseData.put("amount_paid", Math.round(amountPaid * 100.0) / 100.0);
            responseData.put("amount_php", Math.round(amountPhp * 100.0) / 100.0);
            responseData.put("exchange_rate", Math.round(exchangeRate * 10000.0) / 10000.0);
            responseData.put("payment_method", paymentMethod);
            responseData.put("status", "completed");
            
            ResponseUtil.sendJSONResponse(response, responseData, 201, "Transaction created successfully");
            
        } catch (SQLException e) {
            System.err.println("Create transaction error: " + e.getMessage());
            e.printStackTrace();
            
            // Handle stored procedure and trigger violations with better error messages
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("Order does not exist") || errorMsg.contains("Order not found")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Transaction error: " + errorMsg, 404);
                } else if (errorMsg.contains("branch") || errorMsg.contains("Branch")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Branch validation error: " + errorMsg, 400);
                } else if (errorMsg.contains("Assign the order to a branch")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Order validation: " + errorMsg, 400);
                } else if (errorMsg.contains("Transaction branch must match")) {
                    ResponseUtil.sendErrorResponse(response, 
                        "Branch mismatch: " + errorMsg, 400);
                } else {
                    ResponseUtil.sendErrorResponse(response, 
                        "An error occurred while creating transaction: " + errorMsg, 500);
                }
            } else {
                ResponseUtil.sendErrorResponse(response, "An error occurred while creating transaction", 500);
            }
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}