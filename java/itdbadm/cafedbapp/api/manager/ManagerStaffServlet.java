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

@WebServlet("/api/manager/staff")
public class ManagerStaffServlet extends HttpServlet {

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
            List<Map<String, Object>> employees = fetchStaff(conn, branchId, isAdmin);
            ResponseUtil.sendJSONResponse(response, employees, 200, "Staff list loaded");
        } catch (SQLException e) {
            System.err.println("ManagerStaffServlet error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "Failed to load staff list", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }

    private List<Map<String, Object>> fetchStaff(Connection conn, Integer branchId, boolean isAdmin) throws SQLException {
        List<Map<String, Object>> employees = new ArrayList<>();
        StringBuilder query = new StringBuilder(
                "SELECT e.employee_id, e.username, e.name, e.role, e.contact_num, e.branch_id, " +
                "COALESCE(SUM(CASE WHEN oh.timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END), 0) AS orders_processed_last30, " +
                "MAX(oh.timestamp) AS last_activity " +
                "FROM Employee e " +
                "LEFT JOIN OrderHistory oh ON e.employee_id = oh.employee_id ");

        if (!isAdmin) {
            query.append("WHERE e.branch_id = ? ");
        } else {
            query.append("WHERE e.branch_id IS NOT NULL ");
        }

        query.append("GROUP BY e.employee_id, e.username, e.name, e.role, e.contact_num, e.branch_id " +
                     "ORDER BY e.role, e.name");

        try (PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            if (!isAdmin) {
                stmt.setInt(1, branchId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> employee = new HashMap<>();
                    employee.put("employee_id", rs.getInt("employee_id"));
                    employee.put("username", rs.getString("username"));
                    employee.put("name", rs.getString("name"));
                    employee.put("role", rs.getString("role"));
                    employee.put("contact_num", rs.getString("contact_num"));
                    employee.put("branch_id", rs.getObject("branch_id"));
                    employee.put("orders_processed_last30", rs.getInt("orders_processed_last30"));
                    employee.put("last_activity", rs.getTimestamp("last_activity") != null
                            ? rs.getTimestamp("last_activity").toString()
                            : null);
                    employees.add(employee);
                }
            }
        }

        return employees;
    }
}


