/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package itdbadm.cafedbapp.util;

/**
 *
 * @author Matthew Javier
 */
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for sending JSON responses
 */
public class ResponseUtil {
    
    private static final Gson gson = new Gson();
    
    /**
     * Send JSON response
     * @param response HttpServletResponse
     * @param data Response data
     * @param statusCode HTTP status code
     * @param message Optional message
     */
    public static void sendJSONResponse(HttpServletResponse response, Object data, 
                                       int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", statusCode >= 200 && statusCode < 300);
        responseMap.put("data", data);
        responseMap.put("message", message);
        
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(responseMap));
        out.flush();
    }
    
    /**
     * Send error response
     * @param response HttpServletResponse
     * @param message Error message
     * @param statusCode HTTP status code
     */
    public static void sendErrorResponse(HttpServletResponse response, String message, 
                                        int statusCode) throws IOException {
        sendJSONResponse(response, null, statusCode, message);
    }
}




