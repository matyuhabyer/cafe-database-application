/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.api.orders;

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

@WebServlet("/api/orders/get_order_details")
public class GetOrderDetailsServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String orderIdParam = request.getParameter("order_id");
        if (orderIdParam == null || orderIdParam.isEmpty()) {
            ResponseUtil.sendErrorResponse(response, "Missing order_id parameter", 400);
            return;
        }
        
        int orderId;
        try {
            orderId = Integer.parseInt(orderIdParam);
        } catch (NumberFormatException e) {
            ResponseUtil.sendErrorResponse(response, "Invalid order_id parameter", 400);
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
            // Get order basic info
            String orderQuery = "SELECT o.order_id, o.order_date, o.total_amount, o.status, o.earned_points, " +
                              "c.customer_id, c.name as customer_name, c.email, c.phone_num, " +
                              "b.branch_id, b.name as branch_name, " +
                              "l.card_number, l.points as loyalty_points " +
                              "FROM OrderTbl o " +
                              "LEFT JOIN Customer c ON o.customer_id = c.customer_id " +
                              "LEFT JOIN Branch b ON o.branch_id = b.branch_id " +
                              "LEFT JOIN LoyaltyCard l ON o.loyalty_id = l.loyalty_id " +
                              "WHERE o.order_id = ?";
            
            Map<String, Object> orderDetails = null;
            try (PreparedStatement stmt = conn.prepareStatement(orderQuery)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        ResponseUtil.sendErrorResponse(response, "Order not found", 404);
                        return;
                    }
                    
                    // Check authorization
                    String userType = (String) session.getAttribute("user_type");
                    if ("customer".equals(userType)) {
                        int customerId = SessionUtil.getUserId(session);
                        if (rs.getInt("customer_id") != customerId) {
                            ResponseUtil.sendErrorResponse(response, "Unauthorized access to this order", 403);
                            return;
                        }
                    }
                    
                    orderDetails = new HashMap<>();
                    orderDetails.put("order_id", rs.getInt("order_id"));
                    orderDetails.put("order_date", rs.getTimestamp("order_date").toString());
                    orderDetails.put("total_amount", Math.round(rs.getDouble("total_amount") * 100.0) / 100.0);
                    orderDetails.put("status", rs.getString("status"));
                    orderDetails.put("earned_points", rs.getInt("earned_points"));
                    
                    Map<String, Object> customer = new HashMap<>();
                    customer.put("customer_id", rs.getInt("customer_id"));
                    customer.put("name", rs.getString("customer_name"));
                    customer.put("email", rs.getString("email"));
                    customer.put("phone_num", rs.getString("phone_num"));
                    orderDetails.put("customer", customer);
                    
                    Map<String, Object> branch = new HashMap<>();
                    branch.put("branch_id", rs.getInt("branch_id"));
                    branch.put("name", rs.getString("branch_name"));
                    orderDetails.put("branch", branch);
                    
                    Map<String, Object> loyalty = new HashMap<>();
                    loyalty.put("card_number", rs.getString("card_number"));
                    loyalty.put("points", rs.getInt("loyalty_points"));
                    orderDetails.put("loyalty", loyalty);
                }
            }
            
            // Get order items
            // Use LEFT JOIN for Menu in case menu item was deleted, but still show the order item
            String itemsQuery = "SELECT oi.order_item_id, oi.menu_id, oi.quantity, oi.price, " +
                              "COALESCE(m.name, 'Unknown Item') as menu_name, " +
                              "COALESCE(m.description, '') as description, " +
                              "do.temperature, do.price_modifier " +
                              "FROM OrderItem oi " +
                              "LEFT JOIN Menu m ON oi.menu_id = m.menu_id " +
                              "LEFT JOIN DrinkOption do ON oi.drink_option_id = do.drink_option_id " +
                              "WHERE oi.order_id = ?";
            
            List<Map<String, Object>> items = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(itemsQuery)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        int orderItemId = rs.getInt("order_item_id");
                        int quantity = rs.getInt("quantity");
                        double price = rs.getDouble("price");
                        int menuId = rs.getInt("menu_id");
                        String menuName = rs.getString("menu_name");
                        String description = rs.getString("description");
                        String temperature = rs.getString("temperature");
                        
                        // Debug logging
                        System.out.println("GetOrderDetailsServlet: Processing order item - order_item_id: " + orderItemId + 
                            ", menu_id: " + menuId + ", menu_name: " + menuName);
                        
                        // Handle null menu name - fallback to description or menu_id if name is null
                        if (menuName == null || menuName.trim().isEmpty()) {
                            System.err.println("GetOrderDetailsServlet: WARNING - menu_name is null for menu_id: " + menuId);
                            menuName = description != null && !description.trim().isEmpty() 
                                ? description 
                                : "Menu Item #" + menuId;
                        }
                        
                        item.put("order_item_id", orderItemId);
                        item.put("menu_id", menuId);
                        item.put("name", menuName);  // Use "name" to match frontend expectation
                        item.put("menu_name", menuName);  // Keep for backward compatibility
                        item.put("description", description != null ? description : "");
                        item.put("quantity", quantity);
                        item.put("price", Math.round(price * 100.0) / 100.0);
                        item.put("total_price", Math.round(price * quantity * 100.0) / 100.0);  // Calculate total price (price * quantity)
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
            orderDetails.put("items", items);
            
            // Debug logging - log what we're sending
            System.out.println("GetOrderDetailsServlet: Sending order details with " + items.size() + " items");
            for (Map<String, Object> item : items) {
                System.out.println("GetOrderDetailsServlet: Item - menu_id: " + item.get("menu_id") + 
                    ", name: " + item.get("name") + ", menu_name: " + item.get("menu_name"));
            }
            
            // Get order history
            String historyQuery = "SELECT oh.history_id, oh.status, oh.timestamp, oh.remarks, " +
                                "e.name as employee_name, e.role " +
                                "FROM OrderHistory oh " +
                                "LEFT JOIN Employee e ON oh.employee_id = e.employee_id " +
                                "WHERE oh.order_id = ? " +
                                "ORDER BY oh.timestamp ASC";
            
            List<Map<String, Object>> history = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(historyQuery)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> h = new HashMap<>();
                        h.put("history_id", rs.getInt("history_id"));
                        h.put("status", rs.getString("status"));
                        h.put("timestamp", rs.getTimestamp("timestamp").toString());
                        h.put("remarks", rs.getString("remarks"));
                        h.put("employee_name", rs.getString("employee_name"));
                        h.put("employee_role", rs.getString("role"));
                        history.add(h);
                    }
                }
            }
            orderDetails.put("history", history);
            
            // Get transaction if exists
            String transactionQuery = "SELECT t.transaction_id, t.payment_method, t.amount_paid, " +
                                    "t.exchange_rate, t.transaction_date, t.status, " +
                                    "cur.code as currency_code, cur.symbol as currency_symbol " +
                                    "FROM TransactionTbl t " +
                                    "INNER JOIN Currency cur ON t.currency_id = cur.currency_id " +
                                    "WHERE t.order_id = ?";
            
            Map<String, Object> transaction = null;
            try (PreparedStatement stmt = conn.prepareStatement(transactionQuery)) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        transaction = new HashMap<>();
                        transaction.put("transaction_id", rs.getInt("transaction_id"));
                        transaction.put("payment_method", rs.getString("payment_method"));
                        transaction.put("amount_paid", Math.round(rs.getDouble("amount_paid") * 100.0) / 100.0);
                        transaction.put("exchange_rate", Math.round(rs.getDouble("exchange_rate") * 10000.0) / 10000.0);
                        transaction.put("transaction_date", rs.getTimestamp("transaction_date").toString());
                        transaction.put("status", rs.getString("status"));
                        transaction.put("currency_code", rs.getString("currency_code"));
                        transaction.put("currency_symbol", rs.getString("currency_symbol"));
                    }
                }
            }
            orderDetails.put("transaction", transaction);
            
            ResponseUtil.sendJSONResponse(response, orderDetails, 200, "");
            
        } catch (SQLException e) {
            System.err.println("Get order details error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while fetching order details", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}