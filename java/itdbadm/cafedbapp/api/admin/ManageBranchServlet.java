package itdbadm.cafedbapp.api.admin;

import itdbadm.cafedbapp.config.DatabaseConfig;
import itdbadm.cafedbapp.util.ResponseUtil;
import itdbadm.cafedbapp.util.SessionUtil;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/api/admin/manage_branch")
public class ManageBranchServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !SessionUtil.isAdminOrManager(session) || !"admin".equals(session.getAttribute("role"))) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Admin access required.", 401);
            return;
        }

        StringBuilder jsonInput = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonInput.append(line);
            }
        }

        JsonObject input;
        try {
            input = JsonParser.parseString(jsonInput.toString()).getAsJsonObject();
        } catch (Exception e) {
            ResponseUtil.sendErrorResponse(response, "Invalid JSON payload", 400);
            return;
        }

        if (input == null || !input.has("action")) {
            ResponseUtil.sendErrorResponse(response, "Missing field: action", 400);
            return;
        }

        String action = input.get("action").getAsString();
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }

        try {
            conn.setAutoCommit(false);
            switch (action) {
                case "create":
                    createBranch(conn, input);
                    ResponseUtil.sendJSONResponse(response, null, 201, "Branch created successfully");
                    break;
                case "update":
                    updateBranch(conn, input);
                    ResponseUtil.sendJSONResponse(response, null, 200, "Branch updated successfully");
                    break;
                case "delete":
                    deleteBranch(conn, input);
                    ResponseUtil.sendJSONResponse(response, null, 200, "Branch deleted successfully");
                    break;
                default:
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Invalid action. Use create, update, or delete.", 400);
                    return;
            }
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Failed rolling back branch transaction: " + rollbackEx.getMessage());
            }
            System.err.println("ManageBranchServlet error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "Branch operation failed: " + e.getMessage(), 500);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignore) {}
            DatabaseConfig.closeDBConnection(conn);
        }
    }

    private void createBranch(Connection conn, JsonObject input) throws SQLException {
        if (!input.has("name")) {
            throw new SQLException("Missing field: name");
        }
        String name = input.get("name").getAsString().trim();
        String address = input.has("address") ? input.get("address").getAsString().trim() : null;
        String contact = input.has("contact_num") ? input.get("contact_num").getAsString().trim() : null;
        Integer managerId = input.has("manager_id") && !input.get("manager_id").isJsonNull()
                ? input.get("manager_id").getAsInt() : null;

        int branchId;
        String insertQuery = "INSERT INTO Branch (name, address, contact_num, manager_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, address);
            stmt.setString(3, contact);
            if (managerId != null) {
                ensureManagerEligibility(conn, managerId);
                stmt.setInt(4, managerId);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to retrieve new branch ID");
                }
                branchId = keys.getInt(1);
            }
        }

        if (managerId != null) {
            assignManagerToBranch(conn, managerId, branchId);
        }
    }

    private void updateBranch(Connection conn, JsonObject input) throws SQLException {
        if (!input.has("branch_id")) {
            throw new SQLException("Missing field: branch_id");
        }
        int branchId = input.get("branch_id").getAsInt();
        ensureBranchExists(conn, branchId);

        String updateQuery = "UPDATE Branch SET name = COALESCE(?, name), address = COALESCE(?, address), " +
                "contact_num = COALESCE(?, contact_num), manager_id = ? WHERE branch_id = ?";

        Integer managerId = input.has("manager_id") && !input.get("manager_id").isJsonNull()
                ? input.get("manager_id").getAsInt() : null;

        if (managerId != null) {
            ensureManagerEligibility(conn, managerId);
        }

        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
            stmt.setString(1, input.has("name") ? input.get("name").getAsString().trim() : null);
            stmt.setString(2, input.has("address") ? input.get("address").getAsString().trim() : null);
            stmt.setString(3, input.has("contact_num") ? input.get("contact_num").getAsString().trim() : null);
            if (managerId != null) {
                stmt.setInt(4, managerId);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            stmt.setInt(5, branchId);
            stmt.executeUpdate();
        }

        if (managerId != null) {
            assignManagerToBranch(conn, managerId, branchId);
        }
    }

    private void deleteBranch(Connection conn, JsonObject input) throws SQLException {
        if (!input.has("branch_id")) {
            throw new SQLException("Missing field: branch_id");
        }
        int branchId = input.get("branch_id").getAsInt();
        ensureBranchExists(conn, branchId);

        // Prevent deletion if orders or employees depend on this branch
        String orderCheck = "SELECT COUNT(*) AS cnt FROM OrderTbl WHERE branch_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(orderCheck)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("cnt") > 0) {
                    throw new SQLException("Cannot delete branch with existing orders.");
                }
            }
        }

        String updateEmployees = "UPDATE Employee SET branch_id = NULL WHERE branch_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateEmployees)) {
            stmt.setInt(1, branchId);
            stmt.executeUpdate();
        }

        String deleteQuery = "DELETE FROM Branch WHERE branch_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
            stmt.setInt(1, branchId);
            stmt.executeUpdate();
        }
    }

    private void ensureBranchExists(Connection conn, int branchId) throws SQLException {
        String query = "SELECT branch_id FROM Branch WHERE branch_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Branch not found.");
                }
            }
        }
    }

    private void ensureManagerEligibility(Connection conn, int managerId) throws SQLException {
        String query = "SELECT role FROM Employee WHERE employee_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, managerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Manager not found.");
                }
                String role = rs.getString("role");
                if (!"manager".equals(role) && !"admin".equals(role)) {
                    throw new SQLException("Assigned employee must be a manager or admin.");
                }
            }
        }
    }

    private void assignManagerToBranch(Connection conn, int managerId, int branchId) throws SQLException {
        String updateEmployee = "UPDATE Employee SET branch_id = ? WHERE employee_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateEmployee)) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, managerId);
            stmt.executeUpdate();
        }
    }
}


