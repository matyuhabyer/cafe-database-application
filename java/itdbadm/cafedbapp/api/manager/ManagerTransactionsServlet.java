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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/manager/transactions")
public class ManagerTransactionsServlet extends HttpServlet {

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

        String startDateParam = request.getParameter("start_date");
        String endDateParam = request.getParameter("end_date");
        String paymentMethod = request.getParameter("payment_method");
        String status = request.getParameter("status");
        String minAmountParam = request.getParameter("min_amount");
        String maxAmountParam = request.getParameter("max_amount");

        LocalDate startDate = null;
        LocalDate endDate = null;
        Double minAmount = null;
        Double maxAmount = null;

        try {
            if (startDateParam != null && !startDateParam.isBlank()) {
                startDate = LocalDate.parse(startDateParam);
            }
            if (endDateParam != null && !endDateParam.isBlank()) {
                endDate = LocalDate.parse(endDateParam);
            }
            if (minAmountParam != null && !minAmountParam.isBlank()) {
                minAmount = Double.parseDouble(minAmountParam);
            }
            if (maxAmountParam != null && !maxAmountParam.isBlank()) {
                maxAmount = Double.parseDouble(maxAmountParam);
            }
        } catch (Exception e) {
            ResponseUtil.sendErrorResponse(response, "Invalid filter values.", 400);
            return;
        }

        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }

        try {
            List<Map<String, Object>> transactions = fetchTransactions(
                    conn, branchId, isAdmin, startDate, endDate, paymentMethod, status, minAmount, maxAmount);
            ResponseUtil.sendJSONResponse(response, transactions, 200, "Transactions loaded");
        } catch (SQLException e) {
            System.err.println("ManagerTransactionsServlet error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "Failed to load transactions", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }

    private List<Map<String, Object>> fetchTransactions(
            Connection conn,
            Integer branchId,
            boolean isAdmin,
            LocalDate startDate,
            LocalDate endDate,
            String paymentMethod,
            String status,
            Double minAmount,
            Double maxAmount) throws SQLException {

        List<Map<String, Object>> transactions = new ArrayList<>();
        StringBuilder query = new StringBuilder(
                "SELECT t.transaction_id, t.order_id, t.payment_method, t.amount_paid, t.exchange_rate, " +
                "t.transaction_date, t.status, c.code AS currency_code, c.symbol AS currency_symbol, " +
                "o.status AS order_status, o.branch_id " +
                "FROM TransactionTbl t " +
                "INNER JOIN OrderTbl o ON t.order_id = o.order_id " +
                "INNER JOIN Currency c ON t.currency_id = c.currency_id " +
                "WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (!isAdmin) {
            query.append(" AND o.branch_id = ?");
            params.add(branchId);
        }

        if (startDate != null) {
            query.append(" AND t.transaction_date >= ?");
            params.add(Timestamp.valueOf(startDate.atStartOfDay()));
        }

        if (endDate != null) {
            query.append(" AND t.transaction_date <= ?");
            params.add(Timestamp.valueOf(endDate.plusDays(1).atStartOfDay().minusSeconds(1)));
        }

        if (paymentMethod != null && !paymentMethod.isBlank()) {
            query.append(" AND t.payment_method = ?");
            params.add(paymentMethod.toLowerCase());
        }

        if (status != null && !status.isBlank()) {
            query.append(" AND t.status = ?");
            params.add(status.toLowerCase());
        }

        if (minAmount != null) {
            query.append(" AND t.amount_paid >= ?");
            params.add(minAmount);
        }

        if (maxAmount != null) {
            query.append(" AND t.amount_paid <= ?");
            params.add(maxAmount);
        }

        query.append(" ORDER BY t.transaction_date DESC LIMIT 200");

        try (PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Timestamp) {
                    stmt.setTimestamp(i + 1, (Timestamp) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof Double) {
                    stmt.setDouble(i + 1, (Double) param);
                } else {
                    stmt.setString(i + 1, param.toString());
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> trx = new HashMap<>();
                    trx.put("transaction_id", rs.getInt("transaction_id"));
                    trx.put("order_id", rs.getInt("order_id"));
                    trx.put("payment_method", rs.getString("payment_method"));
                    trx.put("amount_paid", Math.round(rs.getDouble("amount_paid") * 100.0) / 100.0);
                    trx.put("amount_paid_php", Math.round(rs.getDouble("amount_paid") * rs.getDouble("exchange_rate") * 100.0) / 100.0);
                    trx.put("exchange_rate", rs.getDouble("exchange_rate"));
                    trx.put("transaction_date", rs.getTimestamp("transaction_date").toString());
                    trx.put("status", rs.getString("status"));
                    trx.put("currency_code", rs.getString("currency_code"));
                    trx.put("currency_symbol", rs.getString("currency_symbol"));
                    trx.put("order_status", rs.getString("order_status"));
                    trx.put("branch_id", rs.getInt("branch_id"));
                    transactions.add(trx);
                }
            }
        }

        return transactions;
    }
}


