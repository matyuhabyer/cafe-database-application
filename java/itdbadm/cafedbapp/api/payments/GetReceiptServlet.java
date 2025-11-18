/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.api.payments;

import itdbadm.cafedbapp.config.DatabaseConfig;
import itdbadm.cafedbapp.util.ResponseUtil;
import itdbadm.cafedbapp.util.SessionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/payments/get_receipt")
public class GetReceiptServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String transactionIdParam = request.getParameter("transaction_id");
        String orderIdParam = request.getParameter("order_id");
        
        if ((transactionIdParam == null || transactionIdParam.isEmpty()) && 
            (orderIdParam == null || orderIdParam.isEmpty())) {
            ResponseUtil.sendErrorResponse(response, "Missing transaction_id or order_id parameter", 400);
            return;
        }
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized", 401);
            return;
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            // Get transaction and order information
            String receiptQuery;
            boolean useTransactionId = transactionIdParam != null && !transactionIdParam.isEmpty();
            
            if (useTransactionId) {
                receiptQuery = "SELECT t.transaction_id, t.order_id, t.payment_method, t.amount_paid, " +
                              "t.exchange_rate, t.transaction_date, t.status, " +
                              "cur.code as currency_code, cur.symbol as currency_symbol, cur.name as currency_name, " +
                              "o.order_id, o.order_date, o.total_amount, o.status as order_status, o.earned_points, " +
                              "c.customer_id, c.name as customer_name, c.email, c.phone_num, " +
                              "b.branch_id, b.name as branch_name, b.address as branch_address, b.contact_num as branch_contact, " +
                              "l.card_number, l.points as loyalty_points " +
                              "FROM TransactionTbl t " +
                              "INNER JOIN Currency cur ON t.currency_id = cur.currency_id " +
                              "INNER JOIN OrderTbl o ON t.order_id = o.order_id " +
                              "LEFT JOIN Customer c ON o.customer_id = c.customer_id " +
                              "LEFT JOIN Branch b ON o.branch_id = b.branch_id " +
                              "LEFT JOIN LoyaltyCard l ON o.loyalty_id = l.loyalty_id " +
                              "WHERE t.transaction_id = ?";
            } else {
                receiptQuery = "SELECT t.transaction_id, t.order_id, t.payment_method, t.amount_paid, " +
                              "t.exchange_rate, t.transaction_date, t.status, " +
                              "cur.code as currency_code, cur.symbol as currency_symbol, cur.name as currency_name, " +
                              "o.order_id, o.order_date, o.total_amount, o.status as order_status, o.earned_points, " +
                              "c.customer_id, c.name as customer_name, c.email, c.phone_num, " +
                              "b.branch_id, b.name as branch_name, b.address as branch_address, b.contact_num as branch_contact, " +
                              "l.card_number, l.points as loyalty_points " +
                              "FROM TransactionTbl t " +
                              "INNER JOIN Currency cur ON t.currency_id = cur.currency_id " +
                              "INNER JOIN OrderTbl o ON t.order_id = o.order_id " +
                              "LEFT JOIN Customer c ON o.customer_id = c.customer_id " +
                              "LEFT JOIN Branch b ON o.branch_id = b.branch_id " +
                              "LEFT JOIN LoyaltyCard l ON o.loyalty_id = l.loyalty_id " +
                              "WHERE t.order_id = ? AND t.status = 'completed' " +
                              "ORDER BY t.transaction_id DESC LIMIT 1";
            }
            
            Map<String, Object> receipt = null;
            try (PreparedStatement stmt = conn.prepareStatement(receiptQuery)) {
                if (useTransactionId) {
                    stmt.setInt(1, Integer.parseInt(transactionIdParam));
                } else {
                    stmt.setInt(1, Integer.parseInt(orderIdParam));
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        ResponseUtil.sendErrorResponse(response, "Receipt not found", 404);
                        return;
                    }
                    
                    // Check authorization
                    String userType = (String) session.getAttribute("user_type");
                    if ("customer".equals(userType)) {
                        int customerId = SessionUtil.getUserId(session);
                        if (rs.getInt("customer_id") != customerId) {
                            ResponseUtil.sendErrorResponse(response, "Unauthorized access to this receipt", 403);
                            return;
                        }
                    }
                    
                    receipt = new HashMap<>();
                    
                    // Transaction information
                    Map<String, Object> transaction = new HashMap<>();
                    transaction.put("transaction_id", rs.getInt("transaction_id"));
                    transaction.put("payment_method", rs.getString("payment_method"));
                    transaction.put("amount_paid", Math.round(rs.getDouble("amount_paid") * 100.0) / 100.0);
                    transaction.put("exchange_rate", Math.round(rs.getDouble("exchange_rate") * 10000.0) / 10000.0);
                    transaction.put("transaction_date", rs.getTimestamp("transaction_date"));
                    transaction.put("status", rs.getString("status"));
                    transaction.put("currency_code", rs.getString("currency_code"));
                    transaction.put("currency_symbol", rs.getString("currency_symbol"));
                    transaction.put("currency_name", rs.getString("currency_name"));
                    receipt.put("transaction", transaction);
                    
                    // Order information
                    Map<String, Object> order = new HashMap<>();
                    order.put("order_id", rs.getInt("order_id"));
                    order.put("order_date", rs.getTimestamp("order_date"));
                    order.put("total_amount", Math.round(rs.getDouble("total_amount") * 100.0) / 100.0);
                    order.put("status", rs.getString("order_status"));
                    order.put("earned_points", rs.getInt("earned_points"));
                    receipt.put("order", order);
                    
                    // Customer information
                    Map<String, Object> customer = new HashMap<>();
                    customer.put("customer_id", rs.getInt("customer_id"));
                    customer.put("name", rs.getString("customer_name"));
                    customer.put("email", rs.getString("email"));
                    customer.put("phone_num", rs.getString("phone_num"));
                    receipt.put("customer", customer);
                    
                    // Branch information
                    Map<String, Object> branch = new HashMap<>();
                    branch.put("branch_id", rs.getInt("branch_id"));
                    branch.put("name", rs.getString("branch_name"));
                    branch.put("address", rs.getString("branch_address"));
                    branch.put("contact_num", rs.getString("branch_contact"));
                    receipt.put("branch", branch);
                    
                    // Loyalty information
                    Map<String, Object> loyalty = new HashMap<>();
                    loyalty.put("card_number", rs.getString("card_number"));
                    loyalty.put("points", rs.getInt("loyalty_points"));
                    receipt.put("loyalty", loyalty);
                }
            }
            
            // Get order items
            @SuppressWarnings("unchecked")
            Map<String, Object> orderMap = (Map<String, Object>) receipt.get("order");
            int orderId = (Integer) orderMap.get("order_id");
            
            String itemsQuery = "SELECT oi.order_item_id, oi.menu_id, oi.quantity, oi.price, " +
                              "COALESCE(m.name, 'Unknown Item') as menu_name, " +
                              "COALESCE(m.description, '') as description, " +
                              "do.temperature, do.price_modifier " +
                              "FROM OrderItem oi " +
                              "LEFT JOIN Menu m ON oi.menu_id = m.menu_id " +
                              "LEFT JOIN DrinkOption do ON oi.drink_option_id = do.drink_option_id " +
                              "WHERE oi.order_id = ? " +
                              "ORDER BY oi.order_item_id";
            
            List<Map<String, Object>> items = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(itemsQuery)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        int orderItemId = rs.getInt("order_item_id");
                        int quantity = rs.getInt("quantity");
                        double price = rs.getDouble("price");
                        String menuName = rs.getString("menu_name");
                        String description = rs.getString("description");
                        String temperature = rs.getString("temperature");
                        
                        item.put("order_item_id", orderItemId);
                        item.put("menu_id", rs.getInt("menu_id"));
                        item.put("name", menuName);
                        item.put("description", description != null ? description : "");
                        item.put("quantity", quantity);
                        item.put("unit_price", Math.round(price * 100.0) / 100.0);
                        item.put("total_price", Math.round(price * quantity * 100.0) / 100.0);
                        item.put("temperature", temperature != null ? temperature : "");
                        item.put("extras", new ArrayList<>());
                        
                        // Get extras for this order item
                        String extrasQuery = "SELECT e.extra_id, e.name, e.price, oie.quantity " +
                                           "FROM OrderItemExtra oie " +
                                           "INNER JOIN Extra e ON oie.extra_id = e.extra_id " +
                                           "WHERE oie.order_item_id = ?";
                        try (PreparedStatement extraStmt = conn.prepareStatement(extrasQuery)) {
                            extraStmt.setInt(1, orderItemId);
                            try (ResultSet extraRs = extraStmt.executeQuery()) {
                                List<Map<String, Object>> extras = new ArrayList<>();
                                while (extraRs.next()) {
                                    Map<String, Object> extra = new HashMap<>();
                                    extra.put("extra_id", extraRs.getInt("extra_id"));
                                    extra.put("name", extraRs.getString("name"));
                                    extra.put("price", Math.round(extraRs.getDouble("price") * 100.0) / 100.0);
                                    extra.put("quantity", extraRs.getInt("quantity"));
                                    extras.add(extra);
                                }
                                item.put("extras", extras);
                            }
                        }
                        items.add(item);
                    }
                }
            }
            receipt.put("items", items);
            
            ResponseUtil.sendJSONResponse(response, receipt, 200, "");
            
        } catch (SQLException e) {
            System.err.println("Get receipt error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while fetching receipt", 500);
        } catch (NumberFormatException e) {
            ResponseUtil.sendErrorResponse(response, "Invalid transaction_id or order_id parameter", 400);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}

