package itdbadm.cafedbapp.api.manager;

import itdbadm.cafedbapp.config.DatabaseConfig;
import itdbadm.cafedbapp.util.ResponseUtil;

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

@WebServlet("/api/manager/dashboard")
public class ManagerDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !"employee".equals(session.getAttribute("user_type"))) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Manager login required.", 401);
            return;
        }

        String role = (String) session.getAttribute("role");
        boolean isAdmin = "admin".equals(role);
        if (!isAdmin && !"manager".equals(role)) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Manager access required.", 403);
            return;
        }

        Integer branchId = (Integer) session.getAttribute("branch_id");
        if (!isAdmin && branchId == null) {
            ResponseUtil.sendErrorResponse(response, "Branch information not found for user.", 400);
            return;
        }

        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }

        try {
            Map<String, Object> stats = fetchStats(conn, branchId, isAdmin);
            List<Map<String, Object>> topItems = fetchTopItems(conn, branchId, isAdmin);
            List<Map<String, Object>> pendingOrders = fetchPendingOrders(conn, branchId, isAdmin);

            Map<String, Object> data = new HashMap<>();
            data.put("stats", stats);
            data.put("top_items", topItems);
            data.put("pending_orders_preview", pendingOrders);

            ResponseUtil.sendJSONResponse(response, data, 200, "Manager dashboard data loaded");
        } catch (SQLException e) {
            System.err.println("ManagerDashboardServlet error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "Failed to load manager dashboard data", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }

    private Map<String, Object> fetchStats(Connection conn, Integer branchId, boolean isAdmin) throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        StringBuilder salesQuery = new StringBuilder(
                "SELECT COALESCE(SUM(t.amount_paid * t.exchange_rate), 0) AS today_sales, " +
                "COUNT(DISTINCT t.transaction_id) AS today_transactions " +
                "FROM TransactionTbl t " +
                "INNER JOIN OrderTbl o ON o.order_id = t.order_id " +
                "WHERE DATE(t.transaction_date) = CURDATE()");

        if (!isAdmin) {
            salesQuery.append(" AND o.branch_id = ?");
        }

        try (PreparedStatement stmt = conn.prepareStatement(salesQuery.toString())) {
            if (!isAdmin) {
                stmt.setInt(1, branchId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double todaySales = Math.round(rs.getDouble("today_sales") * 100.0) / 100.0;
                    int todayTransactions = rs.getInt("today_transactions");
                    stats.put("today_sales", todaySales);
                    stats.put("today_transactions", todayTransactions);
                    stats.put("avg_ticket_value",
                            todayTransactions > 0 ? Math.round((todaySales / todayTransactions) * 100.0) / 100.0 : 0.0);
                }
            }
        }

        StringBuilder pendingQuery = new StringBuilder(
                "SELECT COUNT(*) AS pending_orders " +
                "FROM OrderTbl " +
                "WHERE status IN ('pending', 'confirmed')");
        if (!isAdmin) {
            pendingQuery.append(" AND branch_id = ?");
        }

        try (PreparedStatement stmt = conn.prepareStatement(pendingQuery.toString())) {
            if (!isAdmin) {
                stmt.setInt(1, branchId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("pending_orders", rs.getInt("pending_orders"));
                }
            }
        }

        return stats;
    }

    private List<Map<String, Object>> fetchTopItems(Connection conn, Integer branchId, boolean isAdmin) throws SQLException {
        List<Map<String, Object>> items = new ArrayList<>();
        StringBuilder query = new StringBuilder(
                "SELECT m.menu_id, m.name, SUM(oi.quantity) AS total_qty, " +
                "SUM(oi.price) AS total_sales " +
                "FROM OrderItem oi " +
                "INNER JOIN OrderTbl o ON oi.order_id = o.order_id " +
                "INNER JOIN Menu m ON oi.menu_id = m.menu_id " +
                "WHERE o.order_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
        if (!isAdmin) {
            query.append(" AND o.branch_id = ?");
        }
        query.append(" GROUP BY m.menu_id, m.name " +
                     "ORDER BY total_qty DESC " +
                     "LIMIT 5");

        try (PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            if (!isAdmin) {
                stmt.setInt(1, branchId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("menu_id", rs.getInt("menu_id"));
                    item.put("name", rs.getString("name"));
                    item.put("total_qty", rs.getInt("total_qty"));
                    item.put("total_sales", Math.round(rs.getDouble("total_sales") * 100.0) / 100.0);
                    items.add(item);
                }
            }
        }

        return items;
    }

    private List<Map<String, Object>> fetchPendingOrders(Connection conn, Integer branchId, boolean isAdmin) throws SQLException {
        List<Map<String, Object>> orders = new ArrayList<>();
        StringBuilder query = new StringBuilder(
                "SELECT o.order_id, o.order_date, o.total_amount, o.status, c.name AS customer_name " +
                "FROM OrderTbl o " +
                "LEFT JOIN Customer c ON o.customer_id = c.customer_id " +
                "WHERE o.status IN ('pending', 'confirmed')");
        if (!isAdmin) {
            query.append(" AND o.branch_id = ?");
        }
        query.append(" ORDER BY o.order_date DESC LIMIT 5");

        try (PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            if (!isAdmin) {
                stmt.setInt(1, branchId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> order = new HashMap<>();
                    order.put("order_id", rs.getInt("order_id"));
                    order.put("order_date", rs.getTimestamp("order_date").toString());
                    order.put("total_amount", Math.round(rs.getDouble("total_amount") * 100.0) / 100.0);
                    order.put("status", rs.getString("status"));
                    order.put("customer_name", rs.getString("customer_name"));
                    orders.add(order);
                }
            }
        }

        return orders;
    }
}


