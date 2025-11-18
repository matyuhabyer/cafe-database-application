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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/admin/dashboard_summary")
public class DashboardSummaryServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !SessionUtil.isAdminOrManager(session) || !"admin".equals(session.getAttribute("role"))) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Admin access required.", 401);
            return;
        }

        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }

        try {
            Map<String, Object> summary = new HashMap<>();
            summary.put("totals", fetchTotals(conn));
            summary.put("active_users", fetchActiveUsers(conn));
            summary.put("branch_performance", fetchBranchPerformance(conn));
            summary.put("loyalty_metrics", fetchLoyaltyMetrics(conn));

            ResponseUtil.sendJSONResponse(response, summary, 200, "Dashboard summary loaded");
        } catch (SQLException e) {
            System.err.println("DashboardSummaryServlet error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "Failed to load dashboard summary", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }

    private Map<String, Object> fetchTotals(Connection conn) throws SQLException {
        Map<String, Object> totals = new HashMap<>();

        String totalsQuery = "SELECT " +
                "COALESCE(SUM(t.amount_paid * t.exchange_rate), 0) AS total_sales, " +
                "COUNT(DISTINCT t.transaction_id) AS total_transactions, " +
                "COALESCE(SUM(CASE WHEN o.status IN ('pending','confirmed') THEN 1 ELSE 0 END), 0) AS active_orders " +
                "FROM TransactionTbl t " +
                "RIGHT JOIN OrderTbl o ON o.order_id = t.order_id";

        try (PreparedStatement stmt = conn.prepareStatement(totalsQuery);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                totals.put("total_sales", Math.round(rs.getDouble("total_sales") * 100.0) / 100.0);
                totals.put("total_transactions", rs.getInt("total_transactions"));
                totals.put("active_orders", rs.getInt("active_orders"));
            }
        }

        return totals;
    }

    private Map<String, Object> fetchActiveUsers(Connection conn) throws SQLException {
        Map<String, Object> users = new HashMap<>();

        String customerQuery = "SELECT COUNT(*) AS total_customers FROM Customer";
        try (PreparedStatement stmt = conn.prepareStatement(customerQuery);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                users.put("customers", rs.getInt("total_customers"));
            }
        }

        String employeeQuery = "SELECT COUNT(*) AS total_employees FROM Employee";
        try (PreparedStatement stmt = conn.prepareStatement(employeeQuery);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                users.put("employees", rs.getInt("total_employees"));
            }
        }

        return users;
    }

    private List<Map<String, Object>> fetchBranchPerformance(Connection conn) throws SQLException {
        List<Map<String, Object>> branches = new ArrayList<>();
        String query = "SELECT b.branch_id, b.name, " +
                "COALESCE(SUM(t.amount_paid * t.exchange_rate), 0) AS total_sales, " +
                "COUNT(DISTINCT t.transaction_id) AS transactions, " +
                "COALESCE(SUM(CASE WHEN o.status IN ('pending','confirmed') THEN 1 ELSE 0 END), 0) AS pending_orders " +
                "FROM Branch b " +
                "LEFT JOIN OrderTbl o ON o.branch_id = b.branch_id " +
                "LEFT JOIN TransactionTbl t ON t.order_id = o.order_id " +
                "GROUP BY b.branch_id, b.name " +
                "ORDER BY total_sales DESC";

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> branch = new HashMap<>();
                branch.put("branch_id", rs.getInt("branch_id"));
                branch.put("name", rs.getString("name"));
                branch.put("total_sales", Math.round(rs.getDouble("total_sales") * 100.0) / 100.0);
                branch.put("transactions", rs.getInt("transactions"));
                branch.put("pending_orders", rs.getInt("pending_orders"));
                branches.add(branch);
            }
        }

        return branches;
    }

    private Map<String, Object> fetchLoyaltyMetrics(Connection conn) throws SQLException {
        Map<String, Object> loyalty = new HashMap<>();
        String query = "SELECT " +
                "COUNT(*) AS total_cards, " +
                "COALESCE(SUM(points), 0) AS total_points, " +
                "COALESCE(SUM(CASE WHEN last_redeemed >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END), 0) AS recent_redemptions " +
                "FROM LoyaltyCard";

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                loyalty.put("total_cards", rs.getInt("total_cards"));
                loyalty.put("total_points", rs.getInt("total_points"));
                loyalty.put("recent_redemptions", rs.getInt("recent_redemptions"));
            }
        }

        return loyalty;
    }
}


