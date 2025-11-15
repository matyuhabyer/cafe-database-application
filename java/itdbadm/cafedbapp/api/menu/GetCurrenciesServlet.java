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
import java.util.*;

@WebServlet("/api/menu/get_currencies")
public class GetCurrenciesServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            String query = "SELECT currency_id, code, name, symbol, rate, last_updated " +
                          "FROM Currency ORDER BY code";
            List<Map<String, Object>> currencies = new ArrayList<>();
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Map<String, Object> currency = new HashMap<>();
                    currency.put("currency_id", rs.getInt("currency_id"));
                    currency.put("code", rs.getString("code"));
                    currency.put("name", rs.getString("name"));
                    currency.put("symbol", rs.getString("symbol"));
                    currency.put("rate", rs.getDouble("rate"));
                    currency.put("last_updated", rs.getTimestamp("last_updated").toString());
                    currencies.add(currency);
                }
            }
            
            ResponseUtil.sendJSONResponse(response, currencies, 200, "");
            
        } catch (SQLException e) {
            System.err.println("Get currencies error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while fetching currencies", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}
