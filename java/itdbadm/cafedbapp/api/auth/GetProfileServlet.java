/*
 * Get Profile Servlet
 * Returns current user's profile information (customer or employee)
 */
package itdbadm.cafedbapp.api.auth;

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
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth/get_profile")
public class GetProfileServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Please login.", 401);
            return;
        }
        
        String userType = (String) session.getAttribute("user_type");
        Integer userId = SessionUtil.getUserId(session);
        
        if (userId == null) {
            ResponseUtil.sendErrorResponse(response, "User ID not found in session", 401);
            return;
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            Map<String, Object> profileData = new HashMap<>();
            
            if ("customer".equals(userType)) {
                // Get customer profile with loyalty card
                String query = "SELECT c.customer_id, c.username, c.name, c.email, c.phone_num, " +
                              "l.loyalty_id, l.card_number, l.points, l.is_active, l.last_redeemed, l.created_at " +
                              "FROM Customer c " +
                              "LEFT JOIN LoyaltyCard l ON c.customer_id = l.customer_id AND l.is_active = 1 " +
                              "WHERE c.customer_id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            ResponseUtil.sendErrorResponse(response, "Customer not found", 404);
                            return;
                        }
                        
                        profileData.put("user_type", "customer");
                        profileData.put("customer_id", rs.getInt("customer_id"));
                        profileData.put("username", rs.getString("username"));
                        profileData.put("name", rs.getString("name"));
                        profileData.put("email", rs.getString("email"));
                        profileData.put("phone_num", rs.getString("phone_num"));
                        
                        // Loyalty card information
                        Object loyaltyId = rs.getObject("loyalty_id");
                        if (loyaltyId != null) {
                            Map<String, Object> loyaltyCard = new HashMap<>();
                            loyaltyCard.put("loyalty_id", rs.getInt("loyalty_id"));
                            loyaltyCard.put("card_number", rs.getString("card_number"));
                            loyaltyCard.put("points", rs.getInt("points"));
                            loyaltyCard.put("is_active", rs.getBoolean("is_active"));
                            loyaltyCard.put("can_redeem", rs.getInt("points") >= 100);
                            
                            Timestamp lastRedeemed = rs.getTimestamp("last_redeemed");
                            loyaltyCard.put("last_redeemed", lastRedeemed != null ? lastRedeemed.toString() : null);
                            
                            Timestamp createdAt = rs.getTimestamp("created_at");
                            loyaltyCard.put("created_at", createdAt != null ? createdAt.toString() : null);
                            
                            profileData.put("loyalty_card", loyaltyCard);
                        } else {
                            profileData.put("loyalty_card", null);
                        }
                    }
                }
            } else if ("employee".equals(userType)) {
                // Get employee profile
                String query = "SELECT e.employee_id, e.username, e.name, e.role, e.contact_num, e.branch_id, " +
                              "b.name as branch_name, b.address as branch_address, b.contact_num as branch_contact " +
                              "FROM Employee e " +
                              "LEFT JOIN Branch b ON e.branch_id = b.branch_id " +
                              "WHERE e.employee_id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            ResponseUtil.sendErrorResponse(response, "Employee not found", 404);
                            return;
                        }
                        
                        profileData.put("user_type", "employee");
                        profileData.put("employee_id", rs.getInt("employee_id"));
                        profileData.put("username", rs.getString("username"));
                        profileData.put("name", rs.getString("name"));
                        profileData.put("role", rs.getString("role"));
                        profileData.put("contact_num", rs.getString("contact_num"));
                        
                        Object branchId = rs.getObject("branch_id");
                        if (branchId != null) {
                            Map<String, Object> branch = new HashMap<>();
                            branch.put("branch_id", rs.getInt("branch_id"));
                            branch.put("name", rs.getString("branch_name"));
                            branch.put("address", rs.getString("branch_address"));
                            branch.put("contact_num", rs.getString("branch_contact"));
                            profileData.put("branch", branch);
                        } else {
                            profileData.put("branch", null);
                        }
                    }
                }
            } else {
                ResponseUtil.sendErrorResponse(response, "Invalid user type", 400);
                return;
            }
            
            ResponseUtil.sendJSONResponse(response, profileData, 200, "");
            
        } catch (SQLException e) {
            System.err.println("Get profile error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while fetching profile", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}
