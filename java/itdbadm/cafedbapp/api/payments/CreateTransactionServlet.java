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
        String paymentMethod = input.get("payment_method").getAsString();
        double amountPaid = input.get("amount_paid").getAsDouble();
        
        // Map GCash to bank_transfer
        if ("gcash".equalsIgnoreCase(paymentMethod)) {
            paymentMethod = "bank_transfer";
        }
        
        HttpSession session = request.getSession(false);
        Integer employeeId = null;
        if (session != null && "employee".equals(session.getAttribute("user_type"))) {
            employeeId = (Integer) session.getAttribute("user_id");
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            conn.setAutoCommit(false);
            
            // Verify order exists
            String orderQuery = "SELECT order_id, total_amount, status, branch_id FROM OrderTbl WHERE order_id = ?";
            Map<String, Object> order = null;
            try (PreparedStatement stmt = conn.prepareStatement(orderQuery)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
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
            
            // Check if order already has a completed transaction
            String existingQuery = "SELECT transaction_id FROM TransactionTbl WHERE order_id = ? AND status = 'completed'";
            try (PreparedStatement stmt = conn.prepareStatement(existingQuery)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
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
                        conn.rollback();
                        ResponseUtil.sendErrorResponse(response, "Currency not found", 404);
                        return;
                    }
                    exchangeRate = rs.getDouble("rate");
                }
            }
            
            // Calculate PHP equivalent
            double amountPhp = amountPaid / exchangeRate;
            double orderTotal = (Double) order.get("total_amount");
            double tolerance = 0.01;
            
            if (Math.abs(amountPhp - orderTotal) > tolerance) {
                conn.rollback();
                ResponseUtil.sendErrorResponse(response, 
                    "Payment amount mismatch. Expected: ₱" + orderTotal + ", Received: ₱" + amountPhp, 400);
                return;
            }
            
            // Create transaction
            String transactionQuery = "INSERT INTO TransactionTbl " +
                                    "(order_id, currency_id, payment_method, amount_paid, exchange_rate, status, branch_id) " +
                                    "VALUES (?, ?, ?, ?, ?, 'completed', ?)";
            int transactionId;
            try (PreparedStatement stmt = conn.prepareStatement(transactionQuery, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, orderId);
                stmt.setInt(2, currencyId);
                stmt.setString(3, paymentMethod);
                stmt.setDouble(4, amountPaid);
                stmt.setDouble(5, exchangeRate);
                Object branchId = order.get("branch_id");
                if (branchId != null) {
                    stmt.setInt(6, (Integer) branchId);
                } else {
                    stmt.setNull(6, Types.INTEGER);
                }
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Failed to create transaction", 500);
                    return;
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        transactionId = generatedKeys.getInt(1);
                    } else {
                        conn.rollback();
                        ResponseUtil.sendErrorResponse(response, "Failed to create transaction", 500);
                        return;
                    }
                }
            }
            
            // Update order status to confirmed if it's still pending
            if ("pending".equals(order.get("status"))) {
                String updateOrderQuery = "UPDATE OrderTbl SET status = 'confirmed' WHERE order_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateOrderQuery)) {
                    stmt.setInt(1, orderId);
                    stmt.executeUpdate();
                }
                
                // Log status change
                String historyQuery = "INSERT INTO OrderHistory (order_id, employee_id, status, remarks) " +
                                    "VALUES (?, ?, 'confirmed', 'Payment received')";
                try (PreparedStatement stmt = conn.prepareStatement(historyQuery)) {
                    stmt.setInt(1, orderId);
                    if (employeeId != null) {
                        stmt.setInt(2, employeeId);
                    } else {
                        stmt.setNull(2, Types.INTEGER);
                    }
                    stmt.executeUpdate();
                }
            }
            
            conn.commit();
            
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
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back: " + ex.getMessage());
            }
            System.err.println("Create transaction error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while creating transaction", 500);
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