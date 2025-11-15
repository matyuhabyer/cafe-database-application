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

@WebServlet("/api/loyalty/redeem_points")
public class RedeemPointsServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
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
            conn.setAutoCommit(false);
            
            // Get loyalty card with lock
            String loyaltyQuery = "SELECT loyalty_id, points, is_active FROM LoyaltyCard " +
                                "WHERE customer_id = ? AND is_active = 1 FOR UPDATE";
            
            Integer loyaltyId = null;
            int currentPoints = 0;
            try (PreparedStatement stmt = conn.prepareStatement(loyaltyQuery)) {
                stmt.setInt(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        ResponseUtil.sendErrorResponse(response, "Loyalty card not found", 404);
                        return;
                    }
                    loyaltyId = rs.getInt("loyalty_id");
                    currentPoints = rs.getInt("points");
                }
            }
            
            // Check if customer has enough points
            if (currentPoints < 100) {
                conn.rollback();
                ResponseUtil.sendErrorResponse(response, 
                    "Insufficient points. You have " + currentPoints + " points. Need 100 points to redeem.", 400);
                return;
            }
            
            // Deduct 100 points
            String updateQuery = "UPDATE LoyaltyCard SET points = points - 100, last_redeemed = NOW() WHERE loyalty_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                stmt.setInt(1, loyaltyId);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Failed to redeem points", 500);
                    return;
                }
            }
            
            // Get updated points
            int newPoints = 0;
            String newPointsQuery = "SELECT points FROM LoyaltyCard WHERE loyalty_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(newPointsQuery)) {
                stmt.setInt(1, loyaltyId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        newPoints = rs.getInt("points");
                    }
                }
            }
            
            conn.commit();
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("loyalty_id", loyaltyId);
            responseData.put("points_redeemed", 100);
            responseData.put("points_remaining", newPoints);
            responseData.put("redeemed_at", new Timestamp(System.currentTimeMillis()).toString());
            
            ResponseUtil.sendJSONResponse(response, responseData, 200, 
                "Points redeemed successfully. You can now order a free item!");
            
        } catch (SQLException e) {
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back: " + ex.getMessage());
            }
            System.err.println("Redeem points error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while redeeming points", 500);
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
}
