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

@WebServlet("/api/orders/get_orders")
public class GetOrdersServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
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
            String userType = (String) session.getAttribute("user_type");
            List<Map<String, Object>> orders = new ArrayList<>();
            
            if ("customer".equals(userType)) {
                // Customer: get their own orders
                int customerId = SessionUtil.getUserId(session);
                String query = "SELECT o.order_id, o.order_date, o.total_amount, o.status, o.earned_points, o.loyalty_id, " +
                              "b.name as branch_name, " +
                              "(SELECT COALESCE(m.image_url, '') FROM OrderItem oi " +
                                "LEFT JOIN Menu m ON oi.menu_id = m.menu_id " +
                                "WHERE oi.order_id = o.order_id AND COALESCE(m.image_url, '') <> '' " +
                                "ORDER BY oi.order_item_id ASC LIMIT 1) AS preview_image_url, " +
                              "(SELECT COALESCE(m.name, 'Menu Item') FROM OrderItem oi " +
                                "LEFT JOIN Menu m ON oi.menu_id = m.menu_id " +
                                "WHERE oi.order_id = o.order_id " +
                                "ORDER BY oi.order_item_id ASC LIMIT 1) AS preview_item_name " +
                              "FROM OrderTbl o " +
                              "LEFT JOIN Branch b ON o.branch_id = b.branch_id " +
                              "WHERE o.customer_id = ? " +
                              "ORDER BY o.order_date DESC";
                
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, customerId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> order = new HashMap<>();
                            order.put("order_id", rs.getInt("order_id"));
                            order.put("order_date", rs.getTimestamp("order_date").toString());
                            order.put("total_amount", Math.round(rs.getDouble("total_amount") * 100.0) / 100.0);
                            order.put("status", rs.getString("status"));
                    order.put("earned_points", rs.getInt("earned_points"));
                    order.put("loyalty_id", rs.getObject("loyalty_id"));
                            order.put("branch_name", rs.getString("branch_name"));
                            order.put("preview_image_url", rs.getString("preview_image_url"));
                            order.put("preview_item_name", rs.getString("preview_item_name"));
                            orders.add(order);
                        }
                    }
                }
            } else if ("employee".equals(userType)) {
                // Employee/Manager/Admin
                String role = (String) session.getAttribute("role");
                Object branchIdObj = session.getAttribute("branch_id");
                
                if ("admin".equals(role)) {
                    // Admin: see all orders
                    String query = "SELECT o.order_id, o.order_date, o.total_amount, o.status, o.earned_points, o.loyalty_id, " +
                                  "c.name as customer_name, b.name as branch_name, " +
                                  "(SELECT COALESCE(m.image_url, '') FROM OrderItem oi " +
                                    "LEFT JOIN Menu m ON oi.menu_id = m.menu_id " +
                                    "WHERE oi.order_id = o.order_id AND COALESCE(m.image_url, '') <> '' " +
                                    "ORDER BY oi.order_item_id ASC LIMIT 1) AS preview_image_url, " +
                                  "(SELECT COALESCE(m.name, 'Menu Item') FROM OrderItem oi " +
                                    "LEFT JOIN Menu m ON oi.menu_id = m.menu_id " +
                                    "WHERE oi.order_id = o.order_id " +
                                    "ORDER BY oi.order_item_id ASC LIMIT 1) AS preview_item_name " +
                                  "FROM OrderTbl o " +
                                  "LEFT JOIN Customer c ON o.customer_id = c.customer_id " +
                                  "LEFT JOIN Branch b ON o.branch_id = b.branch_id " +
                                  "ORDER BY o.order_date DESC";
                    
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {
                        while (rs.next()) {
                            Map<String, Object> order = new HashMap<>();
                            order.put("order_id", rs.getInt("order_id"));
                            order.put("order_date", rs.getTimestamp("order_date").toString());
                            order.put("total_amount", Math.round(rs.getDouble("total_amount") * 100.0) / 100.0);
                            order.put("status", rs.getString("status"));
                            order.put("earned_points", rs.getInt("earned_points"));
                            order.put("loyalty_id", rs.getObject("loyalty_id"));
                            order.put("customer_name", rs.getString("customer_name"));
                            order.put("branch_name", rs.getString("branch_name"));
                            order.put("preview_image_url", rs.getString("preview_image_url"));
                            order.put("preview_item_name", rs.getString("preview_item_name"));
                            orders.add(order);
                        }
                    }
                } else {
                    // Manager/Staff: see orders for their branch
                    if (branchIdObj == null) {
                        ResponseUtil.sendErrorResponse(response, "Branch ID not found", 400);
                        return;
                    }
                    int branchId = (Integer) branchIdObj;
                    String query = "SELECT o.order_id, o.order_date, o.total_amount, o.status, o.earned_points, o.loyalty_id, " +
                                  "c.name as customer_name, b.name as branch_name, " +
                                  "(SELECT COALESCE(m.image_url, '') FROM OrderItem oi " +
                                    "LEFT JOIN Menu m ON oi.menu_id = m.menu_id " +
                                    "WHERE oi.order_id = o.order_id AND COALESCE(m.image_url, '') <> '' " +
                                    "ORDER BY oi.order_item_id ASC LIMIT 1) AS preview_image_url, " +
                                  "(SELECT COALESCE(m.name, 'Menu Item') FROM OrderItem oi " +
                                    "LEFT JOIN Menu m ON oi.menu_id = m.menu_id " +
                                    "WHERE oi.order_id = o.order_id " +
                                    "ORDER BY oi.order_item_id ASC LIMIT 1) AS preview_item_name " +
                                  "FROM OrderTbl o " +
                                  "LEFT JOIN Customer c ON o.customer_id = c.customer_id " +
                                  "LEFT JOIN Branch b ON o.branch_id = b.branch_id " +
                                  "WHERE o.branch_id = ? " +
                                  "ORDER BY o.order_date DESC";
                    
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setInt(1, branchId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> order = new HashMap<>();
                                order.put("order_id", rs.getInt("order_id"));
                                order.put("order_date", rs.getTimestamp("order_date").toString());
                                order.put("total_amount", Math.round(rs.getDouble("total_amount") * 100.0) / 100.0);
                                order.put("status", rs.getString("status"));
                                order.put("earned_points", rs.getInt("earned_points"));
                                order.put("loyalty_id", rs.getObject("loyalty_id"));
                                order.put("customer_name", rs.getString("customer_name"));
                                order.put("branch_name", rs.getString("branch_name"));
                                order.put("preview_image_url", rs.getString("preview_image_url"));
                                order.put("preview_item_name", rs.getString("preview_item_name"));
                                orders.add(order);
                            }
                        }
                    }
                }
            } else {
                ResponseUtil.sendErrorResponse(response, "Unauthorized", 401);
                return;
            }
            
            ResponseUtil.sendJSONResponse(response, orders, 200, "");
            
        } catch (SQLException e) {
            System.err.println("Get orders error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while fetching orders", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}





