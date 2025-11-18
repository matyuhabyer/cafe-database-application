/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.api.menu;

import itdbadm.cafedbapp.config.DatabaseConfig;
import itdbadm.cafedbapp.util.ResponseUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public endpoint for customers to get all available branches
 * This endpoint is accessible to all users (including customers)
 */
@WebServlet("/api/menu/get_branches")
public class GetBranchesServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            // Get all branches - public information for customers
            String query = "SELECT b.branch_id, b.name, b.address, b.contact_num " +
                          "FROM Branch b " +
                          "ORDER BY b.name";
            
            List<Map<String, Object>> branches = new ArrayList<>();
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Map<String, Object> branch = new HashMap<>();
                    branch.put("branch_id", rs.getInt("branch_id"));
                    branch.put("name", rs.getString("name"));
                    branch.put("address", rs.getString("address"));
                    branch.put("contact_num", rs.getString("contact_num"));
                    branches.add(branch);
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

