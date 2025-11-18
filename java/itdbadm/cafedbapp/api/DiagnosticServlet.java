/*
 * Diagnostic Servlet to test database connection
 * Access at: /api/diagnostic
 */
package itdbadm.cafedbapp.api;

import itdbadm.cafedbapp.config.DatabaseConfig;
import itdbadm.cafedbapp.util.ResponseUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/diagnostic")
public class DiagnosticServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        Map<String, Object> diagnostics = new HashMap<>();
        
        // Test 1: Check if MySQL Driver is available
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            diagnostics.put("mysql_driver", "✅ Found");
        } catch (ClassNotFoundException e) {
            diagnostics.put("mysql_driver", "❌ Not Found: " + e.getMessage());
        }
        
        // Test 2: Try to get database connection
        Connection conn = null;
        try {
            conn = DatabaseConfig.getDBConnection();
            if (conn != null) {
                diagnostics.put("connection_status", "✅ Connected");
                
                // Test 3: Get database metadata
                DatabaseMetaData metaData = conn.getMetaData();
                diagnostics.put("database_url", metaData.getURL());
                diagnostics.put("database_name", conn.getCatalog());
                diagnostics.put("mysql_version", metaData.getDatabaseProductVersion());
                diagnostics.put("driver_name", metaData.getDriverName());
                diagnostics.put("driver_version", metaData.getDriverVersion());
                
                // Test 4: Execute a simple query
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1 as test, DATABASE() as db_name, USER() as user")) {
                    if (rs.next()) {
                        diagnostics.put("query_test", "✅ Success");
                        diagnostics.put("current_database", rs.getString("db_name"));
                        diagnostics.put("current_user", rs.getString("user"));
                    }
                }
                
                // Test 5: Check if tables exist
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
                    int tableCount = 0;
                    StringBuilder tables = new StringBuilder();
                    while (rs.next()) {
                        tableCount++;
                        if (tables.length() > 0) tables.append(", ");
                        tables.append(rs.getString(1));
                    }
                    diagnostics.put("tables_found", tableCount);
                    diagnostics.put("table_list", tables.toString());
                }
                
            } else {
                diagnostics.put("connection_status", "❌ Failed - Connection returned null");
            }
        } catch (SQLException e) {
            diagnostics.put("connection_status", "❌ Failed");
            diagnostics.put("connection_error", e.getMessage());
            diagnostics.put("error_code", e.getErrorCode());
            diagnostics.put("sql_state", e.getSQLState());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    diagnostics.put("connection_closed", "✅ Successfully closed");
                } catch (SQLException e) {
                    diagnostics.put("connection_closed", "❌ Error closing: " + e.getMessage());
                }
            }
        }
        
        // Test 6: Check SSH tunnel (port 3307)
        diagnostics.put("ssh_tunnel_note", "Check if port 3307 is listening: netstat -an | findstr 3307");
        
        // Overall status
        boolean allGood = diagnostics.get("mysql_driver").toString().contains("✅") &&
                         diagnostics.get("connection_status").toString().contains("✅");
        
        if (allGood) {
            ResponseUtil.sendJSONResponse(response, diagnostics, 200, "All diagnostics passed");
        } else {
            ResponseUtil.sendJSONResponse(response, diagnostics, 500, "Some diagnostics failed");
        }
    }
}

