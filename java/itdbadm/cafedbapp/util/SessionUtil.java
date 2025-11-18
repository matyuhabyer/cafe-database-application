/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.util;

import jakarta.servlet.http.HttpSession;

/**
 * Utility class for session management
 */
public class SessionUtil {
    
    /**
     * Check if user is logged in as customer
     * @param session HttpSession
     * @return true if customer is logged in
     */
    public static boolean isCustomerLoggedIn(HttpSession session) {
        if (session == null) return false;
        String userType = (String) session.getAttribute("user_type");
        return "customer".equals(userType);
    }
    
    /**
     * Check if user is logged in as employee
     * @param session HttpSession
     * @return true if employee is logged in
     */
    public static boolean isEmployeeLoggedIn(HttpSession session) {
        if (session == null) return false;
        String userType = (String) session.getAttribute("user_type");
        return "employee".equals(userType);
    }
    
    /**
     * Check if user is admin or manager
     * @param session HttpSession
     * @return true if user is admin or manager
     */
    public static boolean isAdminOrManager(HttpSession session) {
        if (session == null) return false;
        String userType = (String) session.getAttribute("user_type");
        String role = (String) session.getAttribute("role");
        return "employee".equals(userType) && 
               (role != null && (role.equals("admin") || role.equals("manager")));
    }
    
    /**
     * Get user ID from session
     * @param session HttpSession
     * @return user ID or null
     */
    public static Integer getUserId(HttpSession session) {
        if (session == null) return null;
        Object userId = session.getAttribute("user_id");
        if (userId instanceof Integer) {
            return (Integer) userId;
        } else if (userId instanceof String) {
            try {
                return Integer.parseInt((String) userId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}





