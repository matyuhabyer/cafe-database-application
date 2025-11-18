/*
 * Menu Management Servlet - Admin and Manager
 * Allows admins and managers to add, update, and remove menu items
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
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet("/api/admin/manage_menu")
public class ManageMenuServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (!SessionUtil.isAdminOrManager(session)) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Admin or Manager access required.", 401);
            return;
        }
        
        // Read JSON input
        StringBuilder jsonInput = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonInput.append(line);
            }
        }
        
        JsonObject input = JsonParser.parseString(jsonInput.toString()).getAsJsonObject();
        
        if (!input.has("action")) {
            ResponseUtil.sendErrorResponse(response, "Missing required field: action", 400);
            return;
        }
        
        String action = input.get("action").getAsString();
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            conn.setAutoCommit(false);
            
            switch (action) {
                case "create":
                    createMenuItem(conn, input, response);
                    break;
                case "update":
                    updateMenuItem(conn, input, response);
                    break;
                case "delete":
                    deleteMenuItem(conn, input, response);
                    break;
                default:
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Invalid action. Use: create, update, or delete", 400);
                    return;
            }
            
        } catch (SQLException e) {
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back: " + ex.getMessage());
            }
            System.err.println("Manage menu error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred: " + e.getMessage(), 500);
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
    
    private void createMenuItem(Connection conn, JsonObject input, HttpServletResponse response) 
            throws SQLException, IOException {
        
        if (!input.has("category_id") || !input.has("name") || !input.has("price_amount")) {
            conn.rollback();
            ResponseUtil.sendErrorResponse(response, "Missing required fields: category_id, name, price_amount", 400);
            return;
        }
        
        int categoryId = input.get("category_id").getAsInt();
        String name = input.get("name").getAsString().trim();
        double priceAmount = input.get("price_amount").getAsDouble();
        String description = input.has("description") ? input.get("description").getAsString().trim() : null;
        boolean isDrink = input.has("is_drink") ? input.get("is_drink").getAsBoolean() : false;
        boolean isAvailable = input.has("is_available") ? input.get("is_available").getAsBoolean() : true;
        String imageUrl = input.has("image_url") ? input.get("image_url").getAsString().trim() : null;
        
        // Validate price
        if (priceAmount < 0) {
            conn.rollback();
            ResponseUtil.sendErrorResponse(response, "Price cannot be negative", 400);
            return;
        }
        
        // Validate category exists
        String checkCategoryQuery = "SELECT category_id FROM Category WHERE category_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkCategoryQuery)) {
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Category not found", 404);
                    return;
                }
            }
        }
        
        // Insert menu item
        String insertQuery = "INSERT INTO Menu (category_id, name, description, image_url, price_amount, is_drink, is_available) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?)";
        int menuId;
        try (PreparedStatement stmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, categoryId);
            stmt.setString(2, name);
            if (description != null && !description.isEmpty()) {
                stmt.setString(3, description);
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }
            if (imageUrl != null && !imageUrl.isEmpty()) {
                stmt.setString(4, imageUrl);
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }
            stmt.setDouble(5, priceAmount);
            stmt.setBoolean(6, isDrink);
            stmt.setBoolean(7, isAvailable);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                conn.rollback();
                ResponseUtil.sendErrorResponse(response, "Failed to create menu item", 500);
                return;
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    menuId = generatedKeys.getInt(1);
                } else {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Failed to create menu item", 500);
                    return;
                }
            }
        }
        
        // Add drink options if it's a drink
        if (isDrink && input.has("drink_options")) {
            JsonArray drinkOptions = input.getAsJsonArray("drink_options");
            for (int i = 0; i < drinkOptions.size(); i++) {
                JsonObject option = drinkOptions.get(i).getAsJsonObject();
                String temperature = option.get("temperature").getAsString();
                double priceModifier = option.has("price_modifier") ? option.get("price_modifier").getAsDouble() : 0.0;
                
                String insertDrinkOptionQuery = "INSERT INTO DrinkOption (menu_id, temperature, price_modifier) " +
                                             "VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertDrinkOptionQuery)) {
                    stmt.setInt(1, menuId);
                    stmt.setString(2, temperature);
                    stmt.setDouble(3, priceModifier);
                    stmt.executeUpdate();
                }
            }
        }
        
        // Add extras if provided
        if (input.has("extras")) {
            JsonArray extras = input.getAsJsonArray("extras");
            for (int i = 0; i < extras.size(); i++) {
                int extraId = extras.get(i).getAsInt();
                
                String insertMenuExtraQuery = "INSERT INTO MenuExtra (menu_id, extra_id) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertMenuExtraQuery)) {
                    stmt.setInt(1, menuId);
                    stmt.setInt(2, extraId);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    // Ignore duplicate key errors
                    if (!e.getSQLState().equals("23000")) {
                        throw e;
                    }
                }
            }
        }
        
        // Add legends if provided
        if (input.has("legends")) {
            JsonArray legends = input.getAsJsonArray("legends");
            for (int i = 0; i < legends.size(); i++) {
                int legendId = legends.get(i).getAsInt();
                
                String insertMenuLegendQuery = "INSERT INTO MenuLegend (menu_id, legend_id) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertMenuLegendQuery)) {
                    stmt.setInt(1, menuId);
                    stmt.setInt(2, legendId);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    // Ignore duplicate key errors
                    if (!e.getSQLState().equals("23000")) {
                        throw e;
                    }
                }
            }
        }
        
        conn.commit();
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("menu_id", menuId);
        responseData.put("name", name);
        responseData.put("message", "Menu item created successfully");
        
        ResponseUtil.sendJSONResponse(response, responseData, 201, "Menu item created successfully");
    }
    
    private void updateMenuItem(Connection conn, JsonObject input, HttpServletResponse response) 
            throws SQLException, IOException {
        
        if (!input.has("menu_id")) {
            conn.rollback();
            ResponseUtil.sendErrorResponse(response, "Missing required field: menu_id", 400);
            return;
        }
        
        int menuId = input.get("menu_id").getAsInt();
        
        // Check if menu item exists
        String checkQuery = "SELECT menu_id FROM Menu WHERE menu_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
            stmt.setInt(1, menuId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Menu item not found", 404);
                    return;
                }
            }
        }
        
        // Build update query dynamically
        StringBuilder updateQuery = new StringBuilder("UPDATE Menu SET ");
        boolean hasUpdates = false;
        List<Object> params = new ArrayList<>();
        
        if (input.has("category_id")) {
            updateQuery.append("category_id = ?");
            params.add(input.get("category_id").getAsInt());
            hasUpdates = true;
        }
        if (input.has("name")) {
            if (hasUpdates) updateQuery.append(", ");
            updateQuery.append("name = ?");
            params.add(input.get("name").getAsString().trim());
            hasUpdates = true;
        }
        if (input.has("description")) {
            if (hasUpdates) updateQuery.append(", ");
            updateQuery.append("description = ?");
            String desc = input.get("description").getAsString().trim();
            params.add(desc.isEmpty() ? null : desc);
            hasUpdates = true;
        }
        if (input.has("image_url")) {
            if (hasUpdates) updateQuery.append(", ");
            updateQuery.append("image_url = ?");
            String img = input.get("image_url").getAsString().trim();
            params.add(img.isEmpty() ? null : img);
            hasUpdates = true;
        }
        if (input.has("price_amount")) {
            if (hasUpdates) updateQuery.append(", ");
            updateQuery.append("price_amount = ?");
            double price = input.get("price_amount").getAsDouble();
            if (price < 0) {
                conn.rollback();
                ResponseUtil.sendErrorResponse(response, "Price cannot be negative", 400);
                return;
            }
            params.add(price);
            hasUpdates = true;
        }
        if (input.has("is_drink")) {
            if (hasUpdates) updateQuery.append(", ");
            updateQuery.append("is_drink = ?");
            params.add(input.get("is_drink").getAsBoolean());
            hasUpdates = true;
        }
        if (input.has("is_available")) {
            if (hasUpdates) updateQuery.append(", ");
            updateQuery.append("is_available = ?");
            params.add(input.get("is_available").getAsBoolean());
            hasUpdates = true;
        }
        
        if (!hasUpdates) {
            conn.rollback();
            ResponseUtil.sendErrorResponse(response, "No fields to update", 400);
            return;
        }
        
        updateQuery.append(" WHERE menu_id = ?");
        params.add(menuId);
        
        // Execute update
        try (PreparedStatement stmt = conn.prepareStatement(updateQuery.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param == null) {
                    stmt.setNull(i + 1, Types.VARCHAR);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof Double) {
                    stmt.setDouble(i + 1, (Double) param);
                } else if (param instanceof Boolean) {
                    stmt.setBoolean(i + 1, (Boolean) param);
                } else {
                    stmt.setString(i + 1, (String) param);
                }
            }
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                conn.rollback();
                ResponseUtil.sendErrorResponse(response, "Failed to update menu item", 500);
                return;
            }
        }
        
        // Update drink options if provided
        if (input.has("drink_options")) {
            // Delete existing drink options
            String deleteDrinkOptionsQuery = "DELETE FROM DrinkOption WHERE menu_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteDrinkOptionsQuery)) {
                stmt.setInt(1, menuId);
                stmt.executeUpdate();
            }
            
            // Insert new drink options
            JsonArray drinkOptions = input.getAsJsonArray("drink_options");
            for (int i = 0; i < drinkOptions.size(); i++) {
                JsonObject option = drinkOptions.get(i).getAsJsonObject();
                String temperature = option.get("temperature").getAsString();
                double priceModifier = option.has("price_modifier") ? option.get("price_modifier").getAsDouble() : 0.0;
                
                String insertDrinkOptionQuery = "INSERT INTO DrinkOption (menu_id, temperature, price_modifier) " +
                                             "VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertDrinkOptionQuery)) {
                    stmt.setInt(1, menuId);
                    stmt.setString(2, temperature);
                    stmt.setDouble(3, priceModifier);
                    stmt.executeUpdate();
                }
            }
        }
        
        // Update extras if provided
        if (input.has("extras")) {
            // Delete existing extras
            String deleteExtrasQuery = "DELETE FROM MenuExtra WHERE menu_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteExtrasQuery)) {
                stmt.setInt(1, menuId);
                stmt.executeUpdate();
            }
            
            // Insert new extras
            JsonArray extras = input.getAsJsonArray("extras");
            for (int i = 0; i < extras.size(); i++) {
                int extraId = extras.get(i).getAsInt();
                
                String insertMenuExtraQuery = "INSERT INTO MenuExtra (menu_id, extra_id) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertMenuExtraQuery)) {
                    stmt.setInt(1, menuId);
                    stmt.setInt(2, extraId);
                    stmt.executeUpdate();
                }
            }
        }
        
        // Update legends if provided
        if (input.has("legends")) {
            // Delete existing legends
            String deleteLegendsQuery = "DELETE FROM MenuLegend WHERE menu_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteLegendsQuery)) {
                stmt.setInt(1, menuId);
                stmt.executeUpdate();
            }
            
            // Insert new legends
            JsonArray legends = input.getAsJsonArray("legends");
            for (int i = 0; i < legends.size(); i++) {
                int legendId = legends.get(i).getAsInt();
                
                String insertMenuLegendQuery = "INSERT INTO MenuLegend (menu_id, legend_id) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertMenuLegendQuery)) {
                    stmt.setInt(1, menuId);
                    stmt.setInt(2, legendId);
                    stmt.executeUpdate();
                }
            }
        }
        
        conn.commit();
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("menu_id", menuId);
        responseData.put("message", "Menu item updated successfully");
        
        ResponseUtil.sendJSONResponse(response, responseData, 200, "Menu item updated successfully");
    }
    
    private void deleteMenuItem(Connection conn, JsonObject input, HttpServletResponse response) 
            throws SQLException, IOException {
        
        if (!input.has("menu_id")) {
            conn.rollback();
            ResponseUtil.sendErrorResponse(response, "Missing required field: menu_id", 400);
            return;
        }
        
        int menuId = input.get("menu_id").getAsInt();
        
        // Check if menu item exists
        String checkQuery = "SELECT menu_id FROM Menu WHERE menu_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
            stmt.setInt(1, menuId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    conn.rollback();
                    ResponseUtil.sendErrorResponse(response, "Menu item not found", 404);
                    return;
                }
            }
        }
        
        // Check if menu item is used in any orders
        String checkOrdersQuery = "SELECT COUNT(*) as order_count FROM OrderItem WHERE menu_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkOrdersQuery)) {
            stmt.setInt(1, menuId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("order_count") > 0) {
                    // Instead of deleting, mark as unavailable
                    String updateQuery = "UPDATE Menu SET is_available = 0 WHERE menu_id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                        updateStmt.setInt(1, menuId);
                        updateStmt.executeUpdate();
                    }
                    conn.commit();
                    
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("menu_id", menuId);
                    responseData.put("message", "Menu item marked as unavailable (cannot delete items with order history)");
                    
                    ResponseUtil.sendJSONResponse(response, responseData, 200, 
                        "Menu item marked as unavailable (cannot delete items with order history)");
                    return;
                }
            }
        }
        
        // Delete menu item (cascade will handle related records)
        String deleteQuery = "DELETE FROM Menu WHERE menu_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
            stmt.setInt(1, menuId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                conn.rollback();
                ResponseUtil.sendErrorResponse(response, "Failed to delete menu item", 500);
                return;
            }
        }
        
        conn.commit();
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("menu_id", menuId);
        responseData.put("message", "Menu item deleted successfully");
        
        ResponseUtil.sendJSONResponse(response, responseData, 200, "Menu item deleted successfully");
    }
}

