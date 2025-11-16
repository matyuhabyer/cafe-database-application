/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.api.auth;

import itdbadm.cafedbapp.util.ResponseUtil;
import itdbadm.cafedbapp.util.SessionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Servlet to handle user logout
 * Invalidates the user's session and clears session data
 */
@WebServlet("/api/auth/logout")
public class LogoutServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            // Optional: Log who is logging out (for debugging/auditing)
            Integer userId = SessionUtil.getUserId(session);
            String userType = (String) session.getAttribute("user_type");
            String username = (String) session.getAttribute("username");
            
            // Invalidate the session
            session.invalidate();
            
            // Optional: Log logout event (you can add logging here if needed)
            if (userId != null) {
                System.out.println("User logged out - ID: " + userId + 
                    (username != null ? ", Username: " + username : "") + 
                    (userType != null ? ", Type: " + userType : ""));
            }
        }
        
        // Always return success, even if no session existed
        // This prevents information leakage about session state
        ResponseUtil.sendJSONResponse(response, null, 200, "Logout successful");
    }
}

