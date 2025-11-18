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

@WebServlet("/api/admin/get_branches")
public class GetBranchesServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (!SessionUtil.isAdminOrManager(session)) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Admin or Manager access required.", 401);
            return;
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            String role = (String) session.getAttribute("role");
            Object branchIdObj = session.getAttribute("branch_id");
            
            List<Map<String, Object>> branches = new ArrayList<>();
            
            if ("admin".equals(role)) {
                // Admin: see all branches
                String query = "SELECT b.branch_id, b.name, b.address, b.contact_num, b.manager_id, " +
                              "e.name as manager_name " +
                              "FROM Branch b " +
                              "LEFT JOIN Employee e ON b.manager_id = e.employee_id " +
                              "ORDER BY b.name";
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        Map<String, Object> branch = new HashMap<>();
                        branch.put("branch_id", rs.getInt("branch_id"));
                        branch.put("name", rs.getString("name"));
                        branch.put("address", rs.getString("address"));
                        branch.put("contact_num", rs.getString("contact_num"));
                        Object managerId = rs.getObject("manager_id");
                        branch.put("manager_id", managerId != null ? rs.getInt("manager_id") : null);
                        branch.put("manager_name", rs.getString("manager_name"));
                        branches.add(branch);
                    }
                }
            } else {
                // Manager: see only their branch
                if (branchIdObj == null) {
                    ResponseUtil.sendErrorResponse(response, "Branch ID not found", 400);
                    return;
                }
                int branchId = (Integer) branchIdObj;
                String query = "SELECT b.branch_id, b.name, b.address, b.contact_num, b.manager_id, " +
                              "e.name as manager_name " +
                              "FROM Branch b " +
                              "LEFT JOIN Employee e ON b.manager_id = e.employee_id " +
                              "WHERE b.branch_id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, branchId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> branch = new HashMap<>();
                            branch.put("branch_id", rs.getInt("branch_id"));
                            branch.put("name", rs.getString("name"));
                            branch.put("address", rs.getString("address"));
                            branch.put("contact_num", rs.getString("contact_num"));
                            Object managerId = rs.getObject("manager_id");
                            branch.put("manager_id", managerId != null ? rs.getInt("manager_id") : null);
                            branch.put("manager_name", rs.getString("manager_name"));
                            branches.add(branch);
                        }
                    }
                }
            }
            
            ResponseUtil.sendJSONResponse(response, branches, 200, "");
            
        } catch (SQLException e) {
            System.err.println("Get branches error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while fetching branches", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}





