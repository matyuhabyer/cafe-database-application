package itdbadm.cafedbapp.api.admin;

import itdbadm.cafedbapp.config.DatabaseConfig;
import itdbadm.cafedbapp.util.ResponseUtil;
import itdbadm.cafedbapp.util.SessionUtil;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/admin/system_settings")
public class SystemSettingsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !SessionUtil.isAdminOrManager(session) || !"admin".equals(session.getAttribute("role"))) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Admin access required.", 401);
            return;
        }

        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }

        try {
            ensureSettingsTable(conn);
            Map<String, Object> settings = readSettings(conn);
            ResponseUtil.sendJSONResponse(response, settings, 200, "System settings loaded");
        } catch (SQLException e) {
            System.err.println("SystemSettingsServlet (GET) error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "Failed to load system settings", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !SessionUtil.isAdminOrManager(session) || !"admin".equals(session.getAttribute("role"))) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Admin access required.", 401);
            return;
        }

        StringBuilder jsonInput = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonInput.append(line);
            }
        }

        JsonObject input;
        try {
            input = JsonParser.parseString(jsonInput.toString()).getAsJsonObject();
        } catch (Exception e) {
            ResponseUtil.sendErrorResponse(response, "Invalid JSON payload", 400);
            return;
        }

        if (input == null || !input.has("settings")) {
            ResponseUtil.sendErrorResponse(response, "Missing field: settings", 400);
            return;
        }

        JsonObject settingsPayload = input.getAsJsonObject("settings");
        if (settingsPayload == null || settingsPayload.entrySet().isEmpty()) {
            ResponseUtil.sendErrorResponse(response, "No settings provided", 400);
            return;
        }

        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }

        try {
            ensureSettingsTable(conn);
            writeSettings(conn, settingsPayload);
            Map<String, Object> updated = readSettings(conn);
            ResponseUtil.sendJSONResponse(response, updated, 200, "System settings updated");
        } catch (SQLException e) {
            System.err.println("SystemSettingsServlet (POST) error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "Failed to update settings: " + e.getMessage(), 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }

    private void ensureSettingsTable(Connection conn) throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS SystemSetting (" +
                "setting_key VARCHAR(100) PRIMARY KEY," +
                "setting_value TEXT," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        }
    }

    private Map<String, Object> readSettings(Connection conn) throws SQLException {
        Map<String, Object> settings = new HashMap<>();
        String query = "SELECT setting_key, setting_value FROM SystemSetting";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        }
        return settings;
    }

    private void writeSettings(Connection conn, JsonObject payload) throws SQLException {
        String upsert = "INSERT INTO SystemSetting (setting_key, setting_value) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (PreparedStatement stmt = conn.prepareStatement(upsert)) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry : payload.entrySet()) {
                stmt.setString(1, entry.getKey());
                stmt.setString(2, entry.getValue().isJsonNull() ? null : entry.getValue().getAsString());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}


