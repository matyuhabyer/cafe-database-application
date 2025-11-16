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
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet to verify if user session is valid
 * Returns current user's role and user_type
 */
@WebServlet("/api/auth/verify_session")
public class VerifySessionServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            ResponseUtil.sendJSONResponse(response, null, 200, "");
            return;
        }
        
        String userType = (String) session.getAttribute("user_type");
        String role = (String) session.getAttribute("role");
        Integer userId = SessionUtil.getUserId(session);
        
        if (userType == null && role == null && userId == null) {
            // Session exists but no user data - invalid session
            session.invalidate();
            ResponseUtil.sendJSONResponse(response, null, 200, "");
            return;
        }
        
        // Return session data
        Map<String, Object> sessionData = new HashMap<>();
        if (userType != null) {
            sessionData.put("user_type", userType);
        }
        if (role != null) {
            sessionData.put("role", role);
        } else if (userType != null) {
            // If userType is set but role is not, set role based on userType
            if ("customer".equals(userType)) {
                sessionData.put("role", "customer");
            }
        }
        if (userId != null) {
            sessionData.put("user_id", userId);
        }
        
        ResponseUtil.sendJSONResponse(response, sessionData, 200, "");
    }
}

