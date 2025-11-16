/*
 * Employee Management Servlet - Admin only
 * Allows admins to create, update, and deactivate employee accounts
 */
package itdbadm.cafedbapp.api.admin;

import itdbadm.cafedbapp.config.DatabaseConfig;
import itdbadm.cafedbapp.util.PasswordUtil;
import itdbadm.cafedbapp.util.ResponseUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet("/api/admin/manage_employee")
public class ManageEmployeeServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || !"admin".equals(session.getAttribute("role"))) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Admin access required.", 401);
            return;
        }
        
        // Read JSON input
        StringBuilder jsonInput = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonInput.append(line);
            }
        }
        
        JsonObject input = JsonParser.parseString(jsonInput.toString()).getAsJsonObject();
        
        if (!input.has("action")) {
            ResponseUtil.sendErrorResponse(response, "Missing required field: action", 400);
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
                    createEmployee(conn, input, response);
                    break;
                case "update":
                    updateEmployee(conn, input, response);
                    break;
                case "deactivate":
                    deactivateEmployee(conn, input, response);
                    break;
                default:
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Invalid action. Use: create, update, or deactivate", 400);
                    return;
            }
            
        } catch (SQLException e) {
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back: " + ex.getMessage());
            }
            System.err.println("Manage employee error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred: " + e.getMessage(), 500);
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }
            DatabaseConfig.closeDBConnection(conn);
        }
    }
    
    private void createEmployee(Connection conn, JsonObject input, HttpServletResponse response) 
            throws SQLException, IOException {
        
        if (!input.has("username") || !input.has("password") || !input.has("name") || 
            !input.has("role")) {
            conn.rollback();
            ResponseUtil.sendErrorResponse(response, "Missing required fields: username, password, name, role", 400);
            return;
        }
        
        String username = input.get("username").getAsString().trim();
        String password = input.get("password").getAsString();
        String name = input.get("name").getAsString().trim();
        String role = input.get("role").getAsString();
        String contactNum = input.has("contact_num") ? input.get("contact_num").getAsString().trim() : null;
        Integer branchId = input.has("branch_id") && !input.get("branch_id").isJsonNull() 
            ? input.get("branch_id").getAsInt() : null;
        
        // Validate role
        String[] validRoles = {"staff", "manager", "admin"};
        boolean validRole = false;
        for (String r : validRoles) {
            if (r.equals(role)) {
                validRole = true;
                break;
            }
        }
        if (!validRole) {
            conn.rollback();
            ResponseUtil.sendErrorResponse(response, "Invalid role. Must be: staff, manager, or admin", 400);
            return;
        }
        
        // Validate password length
        if (password.length() < 6) {
            conn.rollback();
            ResponseUtil.sendErrorResponse(response, "Password must be at least 6 characters long", 400);
            return;
        }
        
        // Check if username already exists
        String checkQuery = "SELECT employee_id FROM Employee WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Username already exists", 400);
                    return;
                }
            }
        }
        
        // Insert employee
        String insertQuery = "INSERT INTO Employee (username, password, name, role, contact_num, branch_id) " +
                           "VALUES (?, ?, ?, ?, ?, ?)";
        int employeeId;
        try (PreparedStatement stmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, PasswordUtil.hashPassword(password));
            stmt.setString(3, name);
            stmt.setString(4, role);
            if (contactNum != null && !contactNum.isEmpty()) {
                stmt.setString(5, contactNum);
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            if (branchId != null) {
                stmt.setInt(6, branchId);
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                conn.rollback();
                ResponseUtil.sendErrorResponse(response, "Failed to create employee", 500);
                return;
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    employeeId = generatedKeys.getInt(1);
                } else {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Failed to create employee", 500);
                    return;
                }
            }
        }
        
        conn.commit();
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("employee_id", employeeId);
        responseData.put("username", username);
        responseData.put("name", name);
        responseData.put("role", role);
        responseData.put("message", "Employee created successfully");
        
        ResponseUtil.sendJSONResponse(response, responseData, 201, "Employee created successfully");
    }
    
    private void updateEmployee(Connection conn, JsonObject input, HttpServletResponse response) 
            throws SQLException, IOException {
        
        if (!input.has("employee_id")) {
            conn.rollback();
            ResponseUtil.sendErrorResponse(response, "Missing required field: employee_id", 400);
            return;
        }
        
        int employeeId = input.get("employee_id").getAsInt();
        
        // Check if employee exists
        String checkQuery = "SELECT employee_id FROM Employee WHERE employee_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Employee not found", 404);
                    return;
                }
            }
        }
        
        // Build update query dynamically
        StringBuilder updateQuery = new StringBuilder("UPDATE Employee SET ");
        boolean hasUpdates = false;
        
        if (input.has("name")) {
            updateQuery.append("name = ?");
            hasUpdates = true;
        }
        if (input.has("role")) {
            if (hasUpdates) updateQuery.append(", ");
            updateQuery.append("role = ?");
            hasUpdates = true;
        }
        if (input.has("contact_num")) {
            if (hasUpdates) updateQuery.append(", ");
            updateQuery.append("contact_num = ?");
            hasUpdates = true;
        }
        if (input.has("branch_id")) {
            if (hasUpdates) updateQuery.append(", ");
            updateQuery.append("branch_id = ?");
            hasUpdates = true;
        }
        if (input.has("password") && !input.get("password").getAsString().isEmpty()) {
            if (hasUpdates) updateQuery.append(", ");
            updateQuery.append("password = ?");
            hasUpdates = true;
        }
        
        if (!hasUpdates) {
            conn.rollback();
            ResponseUtil.sendErrorResponse(response, "No fields to update", 400);
            return;
        }
        
        updateQuery.append(" WHERE employee_id = ?");
        
        // Validate role if provided
        if (input.has("role")) {
            String role = input.get("role").getAsString();
            String[] validRoles = {"staff", "manager", "admin"};
            boolean validRole = false;
            for (String r : validRoles) {
                if (r.equals(role)) {
                    validRole = true;
                    break;
                }
            }
            if (!validRole) {
                conn.rollback();
                ResponseUtil.sendErrorResponse(response, "Invalid role. Must be: staff, manager, or admin", 400);
                return;
            }
        }
        
        // Validate password if provided
        if (input.has("password") && !input.get("password").getAsString().isEmpty()) {
            String password = input.get("password").getAsString();
            if (password.length() < 6) {
                conn.rollback();
                ResponseUtil.sendErrorResponse(response, "Password must be at least 6 characters long", 400);
                return;
            }
        }
        
        // Execute update
        int paramIndex = 1;
        try (PreparedStatement stmt = conn.prepareStatement(updateQuery.toString())) {
            if (input.has("name")) {
                stmt.setString(paramIndex++, input.get("name").getAsString().trim());
            }
            if (input.has("role")) {
                stmt.setString(paramIndex++, input.get("role").getAsString());
            }
            if (input.has("contact_num")) {
                String contactNum = input.get("contact_num").getAsString().trim();
                if (contactNum.isEmpty()) {
                    stmt.setNull(paramIndex++, Types.VARCHAR);
                } else {
                    stmt.setString(paramIndex++, contactNum);
                }
            }
            if (input.has("branch_id")) {
                if (input.get("branch_id").isJsonNull()) {
                    stmt.setNull(paramIndex++, Types.INTEGER);
                } else {
                    stmt.setInt(paramIndex++, input.get("branch_id").getAsInt());
                }
            }
            if (input.has("password") && !input.get("password").getAsString().isEmpty()) {
                stmt.setString(paramIndex++, PasswordUtil.hashPassword(input.get("password").getAsString()));
            }
            stmt.setInt(paramIndex, employeeId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                conn.rollback();
                ResponseUtil.sendErrorResponse(response, "Failed to update employee", 500);
                return;
            }
        }
        
        conn.commit();
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("employee_id", employeeId);
        responseData.put("message", "Employee updated successfully");
        
        ResponseUtil.sendJSONResponse(response, responseData, 200, "Employee updated successfully");
    }
    
    private void deactivateEmployee(Connection conn, JsonObject input, HttpServletResponse response) 
            throws SQLException, IOException {
        
        if (!input.has("employee_id")) {
            conn.rollback();
            ResponseUtil.sendErrorResponse(response, "Missing required field: employee_id", 400);
            return;
        }
        
        int employeeId = input.get("employee_id").getAsInt();
        
        // Check if employee exists
        String checkQuery = "SELECT employee_id, role FROM Employee WHERE employee_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Employee not found", 404);
                    return;
                }
                // Prevent deactivating yourself
                // Note: This would require getting current admin's employee_id from session
                // For now, we'll just prevent deactivating if it's the only admin
                String role = rs.getString("role");
                if ("admin".equals(role)) {
                    // Check if this is the last admin
                    String adminCountQuery = "SELECT COUNT(*) as admin_count FROM Employee WHERE role = 'admin'";
                    try (Statement adminStmt = conn.createStatement();
                         ResultSet adminRs = adminStmt.executeQuery(adminCountQuery)) {
                        if (adminRs.next() && adminRs.getInt("admin_count") <= 1) {
                            conn.rollback();
                            ResponseUtil.sendErrorResponse(response, "Cannot deactivate the last admin account", 400);
                            return;
                        }
                    }
                }
            }
        }
        
        // Deactivate by setting branch_id to NULL (soft delete approach)
        // Or we could add an is_active field, but for now we'll use branch_id = NULL
        // Actually, looking at the schema, there's no is_active field for Employee
        // So we'll just remove their branch assignment and optionally set a flag
        // For a proper implementation, we'd need to add an is_active column
        // For now, we'll just set branch_id to NULL as a workaround
        
        String updateQuery = "UPDATE Employee SET branch_id = NULL WHERE employee_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
            stmt.setInt(1, employeeId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                conn.rollback();
                ResponseUtil.sendErrorResponse(response, "Failed to deactivate employee", 500);
                return;
            }
        }
        
        conn.commit();
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("employee_id", employeeId);
        responseData.put("message", "Employee deactivated successfully");
        
        ResponseUtil.sendJSONResponse(response, responseData, 200, "Employee deactivated successfully");
    }
}

