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

@WebServlet("/api/menu/get_item")
public class GetItemServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String menuIdParam = request.getParameter("menu_id");
        if (menuIdParam == null || menuIdParam.isEmpty()) {
            ResponseUtil.sendErrorResponse(response, "Missing menu_id parameter", 400);
            return;
        }
        
        int menuId;
        try {
            menuId = Integer.parseInt(menuIdParam);
        } catch (NumberFormatException e) {
            ResponseUtil.sendErrorResponse(response, "Invalid menu_id parameter", 400);
            return;
        }
        
        String currencyCode = request.getParameter("currency");
        if (currencyCode == null || currencyCode.isEmpty()) {
            currencyCode = "PHP";
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            // Get currency rate
            String currencyQuery = "SELECT currency_id, code, symbol, rate FROM Currency WHERE code = ?";
            Map<String, Object> currency = null;
            try (PreparedStatement stmt = conn.prepareStatement(currencyQuery)) {
                stmt.setString(1, currencyCode);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        ResponseUtil.sendErrorResponse(response, "Currency not found", 404);
                        return;
                    }
                    currency = new HashMap<>();
                    currency.put("currency_id", rs.getInt("currency_id"));
                    currency.put("code", rs.getString("code"));
                    currency.put("symbol", rs.getString("symbol"));
                    currency.put("rate", rs.getDouble("rate"));
                }
            }
            
            double currencyRate = (Double) currency.get("rate");
            
            // Get menu item
            String menuQuery = "SELECT m.menu_id, m.category_id, m.name, m.description, " +
                              "m.image_url, m.price_amount, m.is_drink, m.is_available, " +
                              "c.name as category_name " +
                              "FROM Menu m " +
                              "INNER JOIN Category c ON m.category_id = c.category_id " +
                              "WHERE m.menu_id = ?";
            
            Map<String, Object> menuItem = null;
            try (PreparedStatement stmt = conn.prepareStatement(menuQuery)) {
                stmt.setInt(1, menuId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        ResponseUtil.sendErrorResponse(response, "Menu item not found", 404);
                        return;
                    }
                    
                    double basePrice = rs.getDouble("price_amount");
                    double convertedPrice = basePrice * currencyRate;
                    
                    menuItem = new HashMap<>();
                    menuItem.put("menu_id", rs.getInt("menu_id"));
                    menuItem.put("category_id", rs.getInt("category_id"));
                    menuItem.put("category_name", rs.getString("category_name"));
                    menuItem.put("name", rs.getString("name"));
                    menuItem.put("description", rs.getString("description"));
                    menuItem.put("image_url", rs.getString("image_url"));
                    menuItem.put("price_amount", Math.round(convertedPrice * 100.0) / 100.0);
                    menuItem.put("price_php", Math.round(basePrice * 100.0) / 100.0);
                    menuItem.put("currency_code", currency.get("code"));
                    menuItem.put("currency_symbol", currency.get("symbol"));
                    menuItem.put("is_drink", rs.getBoolean("is_drink"));
                    menuItem.put("is_available", rs.getBoolean("is_available"));
                    menuItem.put("drink_options", new ArrayList<>());
                    menuItem.put("available_extras", new ArrayList<>());
                    menuItem.put("legends", new ArrayList<>());
                }
            }
            
            // Get drink options if it's a drink
            if ((Boolean) menuItem.get("is_drink")) {
                String drinkOptionQuery = "SELECT drink_option_id, temperature, price_modifier " +
                                        "FROM DrinkOption WHERE menu_id = ?";
                try (PreparedStatement doStmt = conn.prepareStatement(drinkOptionQuery)) {
                    doStmt.setInt(1, menuId);
                    try (ResultSet doRs = doStmt.executeQuery()) {
                        List<Map<String, Object>> drinkOptions = new ArrayList<>();
                        while (doRs.next()) {
                            double modifier = doRs.getDouble("price_modifier");
                            double modifierPrice = modifier * currencyRate;
                            Map<String, Object> option = new HashMap<>();
                            option.put("drink_option_id", doRs.getInt("drink_option_id"));
                            option.put("temperature", doRs.getString("temperature"));
                            option.put("price_modifier", Math.round(modifierPrice * 100.0) / 100.0);
                            option.put("price_modifier_php", Math.round(modifier * 100.0) / 100.0);
                            drinkOptions.add(option);
                        }
                        menuItem.put("drink_options", drinkOptions);
                    }
                }
            }
            
            // Get available extras
            String extraQuery = "SELECT e.extra_id, e.name, e.price " +
                              "FROM Extra e " +
                              "INNER JOIN MenuExtra me ON e.extra_id = me.extra_id " +
                              "WHERE me.menu_id = ? ORDER BY e.name";
            try (PreparedStatement extraStmt = conn.prepareStatement(extraQuery)) {
                extraStmt.setInt(1, menuId);
                try (ResultSet extraRs = extraStmt.executeQuery()) {
                    List<Map<String, Object>> extras = new ArrayList<>();
                    while (extraRs.next()) {
                        double extraPrice = extraRs.getDouble("price");
                        double convertedExtraPrice = extraPrice * currencyRate;
                        Map<String, Object> extra = new HashMap<>();
                        extra.put("extra_id", extraRs.getInt("extra_id"));
                        extra.put("name", extraRs.getString("name"));
                        extra.put("price", Math.round(convertedExtraPrice * 100.0) / 100.0);
                        extra.put("price_php", Math.round(extraPrice * 100.0) / 100.0);
                        extras.add(extra);
                    }
                    menuItem.put("available_extras", extras);
                }
            }
            
            // Get legends
            String legendQuery = "SELECT l.legend_id, l.code, l.description " +
                               "FROM Legend l " +
                               "INNER JOIN MenuLegend ml ON l.legend_id = ml.legend_id " +
                               "WHERE ml.menu_id = ?";
            try (PreparedStatement legendStmt = conn.prepareStatement(legendQuery)) {
                legendStmt.setInt(1, menuId);
                try (ResultSet legendRs = legendStmt.executeQuery()) {
                    List<Map<String, Object>> legends = new ArrayList<>();
                    while (legendRs.next()) {
                        Map<String, Object> legend = new HashMap<>();
                        legend.put("legend_id", legendRs.getInt("legend_id"));
                        legend.put("code", legendRs.getString("code"));
                        legend.put("description", legendRs.getString("description"));
                        legends.add(legend);
                    }
                    menuItem.put("legends", legends);
                }
            }
            
            ResponseUtil.sendJSONResponse(response, menuItem, 200, "");
            
        } catch (SQLException e) {
            System.err.println("Get menu item error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while fetching menu item", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}





