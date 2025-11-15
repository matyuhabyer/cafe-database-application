/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.api.loyalty;

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

@WebServlet("/api/loyalty/get_loyalty")
public class GetLoyaltyServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (!SessionUtil.isCustomerLoggedIn(session)) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Customer login required.", 401);
            return;
        }
        
        int customerId = SessionUtil.getUserId(session);
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            String query = "SELECT l.loyalty_id, l.card_number, l.points, l.last_redeemed, " +
                          "l.is_active, l.created_at, c.name as customer_name " +
                          "FROM LoyaltyCard l " +
                          "INNER JOIN Customer c ON l.customer_id = c.customer_id " +
                          "WHERE l.customer_id = ? AND l.is_active = 1";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        ResponseUtil.sendErrorResponse(response, "Loyalty card not found", 404);
                        return;
                    }
                    
                    int points = rs.getInt("points");
                    boolean canRedeem = points >= 100;
                    
                    Map<String, Object> loyaltyData = new HashMap<>();
                    loyaltyData.put("loyalty_id", rs.getInt("loyalty_id"));
                    loyaltyData.put("card_number", rs.getString("card_number"));
                    loyaltyData.put("points", points);
                    loyaltyData.put("can_redeem", canRedeem);
                    Timestamp lastRedeemed = rs.getTimestamp("last_redeemed");
                    loyaltyData.put("last_redeemed", lastRedeemed != null ? lastRedeemed.toString() : null);
                    loyaltyData.put("is_active", rs.getBoolean("is_active"));
                    loyaltyData.put("created_at", rs.getTimestamp("created_at").toString());
                    loyaltyData.put("customer_name", rs.getString("customer_name"));
                    
                    ResponseUtil.sendJSONResponse(response, loyaltyData, 200, "");
                }
            }
        } catch (SQLException e) {
            System.err.println("Get loyalty error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while fetching loyalty information", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}
