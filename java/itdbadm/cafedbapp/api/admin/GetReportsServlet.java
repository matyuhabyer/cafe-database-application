/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.api.admin;

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

@WebServlet("/api/admin/get_reports")
public class GetReportsServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (!SessionUtil.isAdminOrManager(session)) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Admin or Manager access required.", 401);
            return;
        }
        
        String role = (String) session.getAttribute("role");
        Object branchIdObj = session.getAttribute("branch_id");
        
        // Get date range from query params
        String startDate = request.getParameter("start_date");
        if (startDate == null || startDate.isEmpty()) {
            startDate = java.time.LocalDate.now().withDayOfMonth(1).toString();
        }
        String endDate = request.getParameter("end_date");
        if (endDate == null || endDate.isEmpty()) {
            endDate = java.time.LocalDate.now().withDayOfMonth(
                java.time.LocalDate.now().lengthOfMonth()).toString();
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            Map<String, Object> reports = new HashMap<>();
            
            // Total Sales
            String salesQuery;
            if ("admin".equals(role)) {
                salesQuery = "SELECT SUM(amount_paid * exchange_rate) as total_sales_php, COUNT(*) as total_transactions " +
                           "FROM TransactionTbl " +
                           "WHERE status = 'completed' AND DATE(transaction_date) BETWEEN ? AND ?";
            } else {
                if (branchIdObj == null) {
                    ResponseUtil.sendErrorResponse(response, "Branch ID not found", 400);
                    return;
                }
                salesQuery = "SELECT SUM(amount_paid * exchange_rate) as total_sales_php, COUNT(*) as total_transactions " +
                           "FROM TransactionTbl " +
                           "WHERE status = 'completed' AND branch_id = ? AND DATE(transaction_date) BETWEEN ? AND ?";
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(salesQuery)) {
                if ("admin".equals(role)) {
                    stmt.setString(1, startDate);
                    stmt.setString(2, endDate);
                } else {
                    int branchId = (Integer) branchIdObj;
                    stmt.setInt(1, branchId);
                    stmt.setString(2, startDate);
                    stmt.setString(3, endDate);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> sales = new HashMap<>();
                        sales.put("total_sales_php", Math.round(rs.getDouble("total_sales_php") * 100.0) / 100.0);
                        sales.put("total_transactions", rs.getInt("total_transactions"));
                        reports.put("sales", sales);
                    }
                }
            }
            
            // Top Selling Items
            String topItemsQuery;
            if ("admin".equals(role)) {
                topItemsQuery = "SELECT m.name, SUM(oi.quantity) as total_quantity, " +
                              "SUM(oi.price * oi.quantity) as total_revenue " +
                              "FROM OrderItem oi " +
                              "INNER JOIN Menu m ON oi.menu_id = m.menu_id " +
                              "INNER JOIN OrderTbl o ON oi.order_id = o.order_id " +
                              "INNER JOIN TransactionTbl t ON o.order_id = t.order_id " +
                              "WHERE t.status = 'completed' AND DATE(t.transaction_date) BETWEEN ? AND ? " +
                              "GROUP BY m.menu_id, m.name ORDER BY total_quantity DESC LIMIT 10";
            } else {
                topItemsQuery = "SELECT m.name, SUM(oi.quantity) as total_quantity, " +
                              "SUM(oi.price * oi.quantity) as total_revenue " +
                              "FROM OrderItem oi " +
                              "INNER JOIN Menu m ON oi.menu_id = m.menu_id " +
                              "INNER JOIN OrderTbl o ON oi.order_id = o.order_id " +
                              "INNER JOIN TransactionTbl t ON o.order_id = t.order_id " +
                              "WHERE t.status = 'completed' AND t.branch_id = ? AND DATE(t.transaction_date) BETWEEN ? AND ? " +
                              "GROUP BY m.menu_id, m.name ORDER BY total_quantity DESC LIMIT 10";
            }
            
            List<Map<String, Object>> topItems = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(topItemsQuery)) {
                if ("admin".equals(role)) {
                    stmt.setString(1, startDate);
                    stmt.setString(2, endDate);
                } else {
                    int branchId = (Integer) branchIdObj;
                    stmt.setInt(1, branchId);
                    stmt.setString(2, startDate);
                    stmt.setString(3, endDate);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", rs.getString("name"));
                        item.put("total_quantity", rs.getInt("total_quantity"));
                        item.put("total_revenue", Math.round(rs.getDouble("total_revenue") * 100.0) / 100.0);
                        topItems.add(item);
                    }
                }
            }
            reports.put("top_items", topItems);
            
            // Orders by Status
            String statusQuery;
            if ("admin".equals(role)) {
                statusQuery = "SELECT status, COUNT(*) as count FROM OrderTbl " +
                            "WHERE DATE(order_date) BETWEEN ? AND ? GROUP BY status";
            } else {
                statusQuery = "SELECT status, COUNT(*) as count FROM OrderTbl " +
                            "WHERE branch_id = ? AND DATE(order_date) BETWEEN ? AND ? GROUP BY status";
            }
            
            Map<String, Integer> ordersByStatus = new HashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement(statusQuery)) {
                if ("admin".equals(role)) {
                    stmt.setString(1, startDate);
                    stmt.setString(2, endDate);
                } else {
                    int branchId = (Integer) branchIdObj;
                    stmt.setInt(1, branchId);
                    stmt.setString(2, startDate);
                    stmt.setString(3, endDate);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ordersByStatus.put(rs.getString("status"), rs.getInt("count"));
                    }
                }
            }
            reports.put("orders_by_status", ordersByStatus);
            
            Map<String, Object> responseData = new HashMap<>();
            Map<String, String> period = new HashMap<>();
            period.put("start_date", startDate);
            period.put("end_date", endDate);
            responseData.put("period", period);
            responseData.put("reports", reports);
            
            ResponseUtil.sendJSONResponse(response, responseData, 200, "");
            
        } catch (SQLException e) {
            System.err.println("Get reports error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while fetching reports", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}





