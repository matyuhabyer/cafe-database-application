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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/orders/staff_dashboard")
public class StaffDashboardServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (!SessionUtil.isEmployeeLoggedIn(session)) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Employee login required.", 401);
            return;
        }
        
        Integer employeeId = SessionUtil.getUserId(session);
        if (employeeId == null) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Invalid session.", 401);
            return;
        }
        
        String role = (String) session.getAttribute("role");
        Object branchIdObj = session.getAttribute("branch_id");
        boolean isAdmin = "admin".equals(role);
        Integer branchId = null;
        
        if (!isAdmin) {
            if (branchIdObj == null) {
                ResponseUtil.sendErrorResponse(response, "Branch ID not found for employee.", 400);
                return;
            }
            branchId = (Integer) branchIdObj;
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            Map<String, Object> stats = fetchStats(conn, branchId, isAdmin);
            List<Map<String, Object>> pendingPayments = fetchPendingPayments(conn, branchId, isAdmin);
            List<Map<String, Object>> orderHistory = fetchOrderHistory(conn, employeeId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("stats", stats);
            responseData.put("pending_payments", pendingPayments);
            responseData.put("order_history", orderHistory);
            
            ResponseUtil.sendJSONResponse(response, responseData, 200, "Staff dashboard data loaded");
        } catch (SQLException e) {
            System.err.println("Staff dashboard error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while loading staff dashboard data", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
    
    private Map<String, Object> fetchStats(Connection conn, Integer branchId, boolean isAdmin) throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        StringBuilder query = new StringBuilder(
            "SELECT " +
            "SUM(CASE WHEN o.status IN ('pending','confirmed') THEN 1 ELSE 0 END) AS active_orders, " +
            "SUM(CASE WHEN o.status = 'completed' THEN 1 ELSE 0 END) AS completed_orders, " +
            "SUM(CASE WHEN o.status IN ('pending','confirmed') AND NOT EXISTS (" +
            "    SELECT 1 FROM TransactionTbl t WHERE t.order_id = o.order_id AND t.status = 'completed'" +
            ") THEN 1 ELSE 0 END) AS pending_payments " +
            "FROM OrderTbl o"
        );
        
        if (!isAdmin) {
            query.append(" WHERE o.branch_id = ?");
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            if (!isAdmin) {
                stmt.setInt(1, branchId);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("active_orders", rs.getInt("active_orders"));
                    stats.put("completed_orders", rs.getInt("completed_orders"));
                    stats.put("pending_payments", rs.getInt("pending_payments"));
                } else {
                    stats.put("active_orders", 0);
                    stats.put("completed_orders", 0);
                    stats.put("pending_payments", 0);
                }
            }
        }
        
        return stats;
    }
    
    private List<Map<String, Object>> fetchPendingPayments(Connection conn, Integer branchId, boolean isAdmin) throws SQLException {
        List<Map<String, Object>> pendingPayments = new ArrayList<>();
        StringBuilder query = new StringBuilder(
            "SELECT o.order_id, o.order_date, o.total_amount, o.status, " +
            "c.name AS customer_name, b.name AS branch_name " +
            "FROM OrderTbl o " +
            "LEFT JOIN Customer c ON o.customer_id = c.customer_id " +
            "LEFT JOIN Branch b ON o.branch_id = b.branch_id " +
            "LEFT JOIN TransactionTbl t ON o.order_id = t.order_id AND t.status = 'completed' " +
            "WHERE t.transaction_id IS NULL " +
            "AND o.status IN ('pending','confirmed')"
        );
        
        if (!isAdmin) {
            query.append(" AND o.branch_id = ?");
        }
        
        query.append(" ORDER BY o.order_date DESC LIMIT 50");
        
        try (PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            if (!isAdmin) {
                stmt.setInt(1, branchId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> payment = new HashMap<>();
                    payment.put("order_id", rs.getInt("order_id"));
                    payment.put("order_date", rs.getTimestamp("order_date").toString());
                    payment.put("total_amount", Math.round(rs.getDouble("total_amount") * 100.0) / 100.0);
                    payment.put("status", rs.getString("status"));
                    payment.put("customer_name", rs.getString("customer_name"));
                    payment.put("branch_name", rs.getString("branch_name"));
                    pendingPayments.add(payment);
                }
            }
        }
        
        return pendingPayments;
    }
    
    private List<Map<String, Object>> fetchOrderHistory(Connection conn, int employeeId) throws SQLException {
        List<Map<String, Object>> history = new ArrayList<>();
        String query = "SELECT oh.order_id, oh.status, oh.timestamp, oh.remarks, " +
                       "o.total_amount, c.name AS customer_name " +
                       "FROM OrderHistory oh " +
                       "LEFT JOIN OrderTbl o ON oh.order_id = o.order_id " +
                       "LEFT JOIN Customer c ON o.customer_id = c.customer_id " +
                       "WHERE oh.employee_id = ? " +
                       "ORDER BY oh.timestamp DESC " +
                       "LIMIT 50";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("order_id", rs.getInt("order_id"));
                    entry.put("status", rs.getString("status"));
                    entry.put("timestamp", rs.getTimestamp("timestamp").toString());
                    entry.put("remarks", rs.getString("remarks"));
                    entry.put("customer_name", rs.getString("customer_name"));
                    entry.put("total_amount", Math.round(rs.getDouble("total_amount") * 100.0) / 100.0);
                    history.add(entry);
                }
            }
        }
        
        return history;
    }
}


