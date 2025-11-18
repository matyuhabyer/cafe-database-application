package itdbadm.cafedbapp.api.admin;

import itdbadm.cafedbapp.config.DatabaseConfig;
import itdbadm.cafedbapp.util.SessionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/api/admin/export_report")
public class ExportReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !SessionUtil.isAdminOrManager(session) || !"admin".equals(session.getAttribute("role"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Admin access required.");
            return;
        }

        String reportType = request.getParameter("report_type");
        if (reportType == null || reportType.isBlank()) {
            reportType = "sales_summary";
        }

        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed.");
            return;
        }

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + reportType + ".csv\"");

        try (PrintWriter writer = response.getWriter()) {
            switch (reportType) {
                case "sales_summary":
                    exportSalesSummary(conn, writer);
                    break;
                case "loyalty_trends":
                    exportLoyaltyTrends(conn, writer);
                    break;
                case "orders_by_status":
                    exportOrdersByStatus(conn, writer);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown report_type.");
            }
        } catch (SQLException e) {
            System.err.println("ExportReportServlet error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to export report.");
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }

    private void exportSalesSummary(Connection conn, PrintWriter writer) throws SQLException {
        writer.println("Date,Total Sales (PHP),Transactions");
        String query = "SELECT DATE(t.transaction_date) AS sale_date, " +
                "COALESCE(SUM(t.amount_paid * t.exchange_rate), 0) AS total_sales, " +
                "COUNT(t.transaction_id) AS total_transactions " +
                "FROM TransactionTbl t " +
                "WHERE t.transaction_date >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                "GROUP BY sale_date " +
                "ORDER BY sale_date DESC";

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                writer.printf("%s,%.2f,%d%n",
                        rs.getDate("sale_date"),
                        rs.getDouble("total_sales"),
                        rs.getInt("total_transactions"));
            }
        }
    }

    private void exportLoyaltyTrends(Connection conn, PrintWriter writer) throws SQLException {
        writer.println("Month,Active Cards,Total Points,Redeemed This Month");
        String query = "SELECT DATE_FORMAT(created_at, '%Y-%m') AS month_label, " +
                "COUNT(*) AS active_cards, " +
                "COALESCE(SUM(points), 0) AS total_points, " +
                "COALESCE(SUM(CASE WHEN last_redeemed BETWEEN DATE_FORMAT(NOW(), '%Y-%m-01') AND NOW() THEN 1 ELSE 0 END), 0) AS redeemed_this_month " +
                "FROM LoyaltyCard " +
                "GROUP BY month_label " +
                "ORDER BY month_label DESC " +
                "LIMIT 12";

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                writer.printf("%s,%d,%d,%d%n",
                        rs.getString("month_label"),
                        rs.getInt("active_cards"),
                        rs.getInt("total_points"),
                        rs.getInt("redeemed_this_month"));
            }
        }
    }

    private void exportOrdersByStatus(Connection conn, PrintWriter writer) throws SQLException {
        writer.println("Status,Order Count,Total Amount");
        String query = "SELECT status, COUNT(*) AS total_orders, COALESCE(SUM(total_amount), 0) AS total_amount " +
                "FROM OrderTbl " +
                "GROUP BY status";

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                writer.printf("%s,%d,%.2f%n",
                        rs.getString("status"),
                        rs.getInt("total_orders"),
                        rs.getDouble("total_amount"));
            }
        }
    }
}


