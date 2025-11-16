/*
 * Get Employees Servlet - Admin only
 * Returns list of all employees for admin management
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

@WebServlet("/api/admin/get_employees")
public class GetEmployeesServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || !"admin".equals(session.getAttribute("role"))) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Admin access required.", 401);
            return;
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            String query = "SELECT e.employee_id, e.username, e.name, e.role, e.contact_num, e.branch_id, " +
                          "b.name as branch_name " +
                          "FROM Employee e " +
                          "LEFT JOIN Branch b ON e.branch_id = b.branch_id " +
                          "ORDER BY e.role, e.name";
            
            List<Map<String, Object>> employees = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Map<String, Object> employee = new HashMap<>();
                    employee.put("employee_id", rs.getInt("employee_id"));
                    employee.put("username", rs.getString("username"));
                    employee.put("name", rs.getString("name"));
                    employee.put("role", rs.getString("role"));
                    employee.put("contact_num", rs.getString("contact_num"));
                    Object branchId = rs.getObject("branch_id");
                    employee.put("branch_id", branchId != null ? rs.getInt("branch_id") : null);
                    employee.put("branch_name", rs.getString("branch_name"));
                    employees.add(employee);
                }
            }
            
            ResponseUtil.sendJSONResponse(response, employees, 200, "");
            
        } catch (SQLException e) {
            System.err.println("Get employees error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while fetching employees", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}

