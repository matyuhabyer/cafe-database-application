/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/admin/get_reports")
public class GetReportsServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (!SessionUtil.isAdminOrManager(session)) {
            ResponseUtil.sendErrorResponse(response, "Unauthorized. Admin or Manager access required.", 401);
            return;
        }
        
        String role = (String) session.getAttribute("role");
        Object branchIdObj = session.getAttribute("branch_id");
        
        // Get analytics period from query params (weekly, monthly, annual)
        String analyticsPeriod = request.getParameter("analytics_period"); // weekly, monthly, annual
        String reportType = request.getParameter("report_type"); // items, foods, drinks
        
        // Get date range from query params (defaults to current month)
        String startDate = request.getParameter("start_date");
        if (startDate == null || startDate.isEmpty()) {
            startDate = java.time.LocalDate.now().withDayOfMonth(1).toString();
        }
        String endDate = request.getParameter("end_date");
        if (endDate == null || endDate.isEmpty()) {
            endDate = java.time.LocalDate.now().withDayOfMonth(
                java.time.LocalDate.now().lengthOfMonth()).toString();
        }
        
        Connection conn = DatabaseConfig.getDBConnection();
        if (conn == null) {
            ResponseUtil.sendErrorResponse(response, "Database connection failed", 500);
            return;
        }
        
        try {
            Map<String, Object> reports = new HashMap<>();
            
            // Total Sales
            String salesQuery;
            if ("admin".equals(role)) {
                salesQuery = "SELECT SUM(o.total_amount * t.exchange_rate) AS total_sales_php, " +
                            "       COUNT(*) AS total_transactions " +
                            "FROM TransactionTbl t " +
                            "JOIN OrderTbl o ON t.order_id = o.order_id " +
                            "WHERE o.status <> 'cancelled' " +         // order takes priority
                            "  AND t.status = 'completed' " +          // only count completed transactions
                            "  AND DATE(t.transaction_date) BETWEEN ? AND ?";
            } else {
                if (branchIdObj == null) {
                    ResponseUtil.sendErrorResponse(response, "Branch ID not found", 400);
                    return; 
                }
                salesQuery = "SELECT SUM(o.total_amount * t.exchange_rate) AS total_sales_php, " +
                            "       COUNT(*) AS total_transactions " +
                            "FROM TransactionTbl t " +
                            "JOIN OrderTbl o ON t.order_id = o.order_id " +
                            "WHERE o.status <> 'cancelled' " +         // order status overrides
                            "  AND t.status = 'completed' " +
                            "  AND t.branch_id = ? " +
                            "  AND DATE(t.transaction_date) BETWEEN ? AND ?";
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(salesQuery)) {
                if ("admin".equals(role)) {
                    stmt.setString(1, startDate);
                    stmt.setString(2, endDate);
                } else {
                    int branchId = (Integer) branchIdObj;
                    stmt.setInt(1, branchId);
                    stmt.setString(2, startDate);
                    stmt.setString(3, endDate);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> sales = new HashMap<>();
                        sales.put("total_sales_php", Math.round(rs.getDouble("total_sales_php") * 100.0) / 100.0);
                        sales.put("total_transactions", rs.getInt("total_transactions"));
                        reports.put("sales", sales);
                    }
                }
            }
            
            // Top Selling Items/Foods/Drinks - Use stored procedures, fallback to direct query
            List<Map<String, Object>> topItems = new ArrayList<>();
            String storedProcName = null;
            boolean useStoredProc = true;
            
            // Determine which stored procedure to call based on report_type
            if (reportType == null || reportType.isEmpty() || "items".equals(reportType)) {
                storedProcName = "sp_top_selling_items";
            } else if ("foods".equals(reportType)) {
                storedProcName = "sp_top_selling_foods";
            } else if ("drinks".equals(reportType)) {
                storedProcName = "sp_top_selling_drinks";
            } else {
                storedProcName = "sp_top_selling_items"; // Default to all items
            }
            
            // Call stored procedure for top items
            String callQuery = "{CALL " + storedProcName + "(?, ?, ?)}";
            try (CallableStatement cstmt = conn.prepareCall(callQuery)) {
                System.out.println("Calling stored procedure: " + storedProcName + " with startDate=" + startDate + ", endDate=" + endDate);
                cstmt.setString(1, startDate);  // p_start
                cstmt.setString(2, endDate);    // p_end
                cstmt.setInt(3, 10);            // p_limit (top 10)
                
                boolean hasResultSet = cstmt.execute();
                if (hasResultSet) {
                    try (ResultSet rs = cstmt.getResultSet()) {
                        int count = 0;
                        while (rs.next()) {
                            count++;
                            Map<String, Object> item = new HashMap<>();
                            item.put("name", rs.getString("name"));
                            item.put("total_quantity", rs.getInt("total_qty"));
                            item.put("total_revenue", Math.round(rs.getDouble("total_sales") * 100.0) / 100.0);
                            topItems.add(item);
                            System.out.println("  - Item: " + rs.getString("name") + ", Qty: " + rs.getInt("total_qty"));
                        }
                        System.out.println("Stored procedure returned " + count + " items");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error calling stored procedure " + storedProcName + ": " + e.getMessage());
                e.printStackTrace();
                useStoredProc = false;
                
                // Fallback: Use direct SQL query if stored procedure doesn't exist
                System.out.println("Falling back to direct SQL query for report_type: " + reportType);
                String fallbackQuery;
                if ("foods".equals(reportType)) {
                    fallbackQuery = "SELECT m.name, SUM(oi.quantity) as total_qty, " +
                                  "SUM(oi.price) as total_sales " +
                                  "FROM OrderItem oi " +
                                  "INNER JOIN Menu m ON oi.menu_id = m.menu_id " +
                                  "INNER JOIN OrderTbl ord ON oi.order_id = ord.order_id " +
                                  "INNER JOIN TransactionTbl t ON ord.order_id = t.order_id " +
                                  "WHERE DATE(t.transaction_date) BETWEEN ? AND ? " +
                                  "AND t.status = 'completed' " +
                                  "AND m.is_drink = 0 " +
                                  "GROUP BY m.menu_id, m.name ORDER BY total_qty DESC LIMIT 10";
                } else if ("drinks".equals(reportType)) {
                    fallbackQuery = "SELECT m.name, SUM(oi.quantity) as total_qty, " +
                                  "SUM(oi.price) as total_sales " +
                                  "FROM OrderItem oi " +
                                  "INNER JOIN Menu m ON oi.menu_id = m.menu_id " +
                                  "INNER JOIN OrderTbl ord ON oi.order_id = ord.order_id " +
                                  "INNER JOIN TransactionTbl t ON ord.order_id = t.order_id " +
                                  "WHERE DATE(t.transaction_date) BETWEEN ? AND ? " +
                                  "AND t.status = 'completed' " +
                                  "AND m.is_drink = 1 " +
                                  "GROUP BY m.menu_id, m.name ORDER BY total_qty DESC LIMIT 10";
                } else {
                    fallbackQuery = "SELECT m.name, SUM(oi.quantity) as total_qty, " +
                                  "SUM(oi.price) as total_sales " +
                                  "FROM OrderItem oi " +
                                  "INNER JOIN Menu m ON oi.menu_id = m.menu_id " +
                                  "INNER JOIN OrderTbl ord ON oi.order_id = ord.order_id " +
                                  "INNER JOIN TransactionTbl t ON ord.order_id = t.order_id " +
                                  "WHERE DATE(t.transaction_date) BETWEEN ? AND ? " +
                                  "AND t.status = 'completed' " +
                                  "GROUP BY m.menu_id, m.name ORDER BY total_qty DESC LIMIT 10";
                }
                
                try (PreparedStatement stmt = conn.prepareStatement(fallbackQuery)) {
                    stmt.setString(1, startDate);
                    stmt.setString(2, endDate);
                    try (ResultSet rs = stmt.executeQuery()) {
                        int count = 0;
                        while (rs.next()) {
                            count++;
                            Map<String, Object> item = new HashMap<>();
                            item.put("name", rs.getString("name"));
                            item.put("total_quantity", rs.getInt("total_qty"));
                            item.put("total_revenue", Math.round(rs.getDouble("total_sales") * 100.0) / 100.0);
                            topItems.add(item);
                            System.out.println("  - Fallback Item: " + rs.getString("name") + ", Qty: " + rs.getInt("total_qty") + ", is_drink filter: " + reportType);
                        }
                        System.out.println("Fallback query returned " + count + " items");
                    }
                } catch (SQLException fallbackError) {
                    System.err.println("Error in fallback query: " + fallbackError.getMessage());
                    fallbackError.printStackTrace();
                }
            }
            reports.put("top_items", topItems);
            
            // Orders by Status
            String statusQuery;
            if ("admin".equals(role)) {
                statusQuery = "SELECT status, COUNT(*) as count FROM OrderTbl " +
                            "WHERE DATE(order_date) BETWEEN ? AND ? GROUP BY status";
            } else {
                statusQuery = "SELECT status, COUNT(*) as count FROM OrderTbl " +
                            "WHERE branch_id = ? AND DATE(order_date) BETWEEN ? AND ? GROUP BY status";
            }
            
            Map<String, Integer> ordersByStatus = new HashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement(statusQuery)) {
                if ("admin".equals(role)) {
                    stmt.setString(1, startDate);
                    stmt.setString(2, endDate);
                } else {
                    int branchId = (Integer) branchIdObj;
                    stmt.setInt(1, branchId);
                    stmt.setString(2, startDate);
                    stmt.setString(3, endDate);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ordersByStatus.put(rs.getString("status"), rs.getInt("count"));
                    }
                }
            }
            reports.put("orders_by_status", ordersByStatus);
            
            // Analytics: Sales per Branch (Weekly/Monthly/Annual) - Only for admin
            Map<String, Object> analytics = new HashMap<>();
            if ("admin".equals(role) && analyticsPeriod != null && !analyticsPeriod.isEmpty()) {
                List<Map<String, Object>> branchSales = new ArrayList<>();
                
                // First, verify that branches exist in the database
                try {
                    String branchCheckQuery = "SELECT COUNT(*) as branch_count FROM Branch";
                    try (PreparedStatement stmt = conn.prepareStatement(branchCheckQuery);
                         ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int branchCount = rs.getInt("branch_count");
                            System.out.println("Total branches in database: " + branchCount);
                            if (branchCount == 0) {
                                analytics.put("period", analyticsPeriod);
                                analytics.put("branch_sales", new ArrayList<>());
                                analytics.put("error", "No branches found in the database. Please add branches first.");
                                reports.put("analytics", analytics);
                                // Continue to return response instead of skipping
                            }
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error checking branches: " + e.getMessage());
                }
                
                try {
                    String analyticsProcName = null;
                    java.sql.Date dateParam = null;
                    
                    if ("weekly".equals(analyticsPeriod)) {
                        analyticsProcName = "sp_weekly_sales_per_branch";
                        // Get start of current week (Monday)
                        java.time.LocalDate today = java.time.LocalDate.now();
                        // Get the Monday of the current week
                        java.time.DayOfWeek dayOfWeek = today.getDayOfWeek();
                        int daysToSubtract = dayOfWeek.getValue() - 1; // Monday = 1, so subtract (dayValue - 1)
                        java.time.LocalDate weekStart = today.minusDays(daysToSubtract);
                        dateParam = java.sql.Date.valueOf(weekStart);
                    } else if ("monthly".equals(analyticsPeriod)) {
                        analyticsProcName = "sp_monthly_sales_per_branch";
                        // Get start of current month
                        java.time.LocalDate today = java.time.LocalDate.now();
                        java.time.LocalDate monthStart = today.withDayOfMonth(1);
                        dateParam = java.sql.Date.valueOf(monthStart);
                    } else if ("annual".equals(analyticsPeriod)) {
                        analyticsProcName = "sp_annual_sales_per_branch";
                        // Get current year
                        int currentYear = java.time.LocalDate.now().getYear();
                        // For annual, we'll need to use a different approach
                        // since it takes INT year, not DATE
                        String annualCallQuery = "{CALL sp_annual_sales_per_branch(?)}";
                        boolean storedProcSuccess = false;
                        
                        try (CallableStatement cstmt = conn.prepareCall(annualCallQuery)) {
                            cstmt.setInt(1, currentYear);
                            boolean hasResultSet = cstmt.execute();
                            if (hasResultSet) {
                                try (ResultSet rs = cstmt.getResultSet()) {
                                    while (rs.next()) {
                                        Map<String, Object> branch = new HashMap<>();
                                        branch.put("branch_id", rs.getInt("branch_id"));
                                        branch.put("branch_name", rs.getString("branch_name"));
                                        branch.put("total_sales", Math.round(rs.getDouble("total_sales") * 100.0) / 100.0);
                                        branch.put("transaction_count", rs.getInt("transaction_count"));
                                        branchSales.add(branch);
                                        System.out.println("Annual Branch: " + rs.getString("branch_name") + ", Sales: " + rs.getDouble("total_sales"));
                                    }
                                    storedProcSuccess = true;
                                }
                            }
                        } catch (SQLException e) {
                            System.err.println("Annual stored procedure failed: " + e.getMessage());
                            e.printStackTrace();
                            
                            // Fallback: Use direct SQL query
                            java.time.LocalDate yearStart = java.time.LocalDate.of(currentYear, 1, 1);
                            java.time.LocalDate yearEnd = java.time.LocalDate.of(currentYear, 12, 31);
                            
                            String fallbackQuery = "SELECT b.branch_id, b.name AS branch_name, " +
                                                    "  COALESCE(SUM(CASE WHEN o.status <> 'cancelled' " +
                                                    "                   THEN (o.total_amount * t.exchange_rate) " +
                                                    "                   ELSE 0 END), 0.00) AS total_sales, " +
                                                    "  COALESCE(COUNT(DISTINCT CASE WHEN o.status <> 'cancelled' " +
                                                    "                              THEN t.transaction_id END), 0) AS transaction_count " +
                                                    "FROM Branch b " +
                                                    "LEFT JOIN OrderTbl o ON o.branch_id = b.branch_id " +
                                                    "LEFT JOIN TransactionTbl t ON t.order_id = o.order_id " +
                                                    "  AND DATE(t.transaction_date) BETWEEN ? AND ? " +
                                                    "  AND t.status = 'completed' " +
                                                    "GROUP BY b.branch_id, b.name " +
                                                    "ORDER BY total_sales DESC";
                            
                            try (PreparedStatement stmt = conn.prepareStatement(fallbackQuery)) {
                                stmt.setDate(1, java.sql.Date.valueOf(yearStart));
                                stmt.setDate(2, java.sql.Date.valueOf(yearEnd));
                                try (ResultSet rs = stmt.executeQuery()) {
                                    while (rs.next()) {
                                        Map<String, Object> branch = new HashMap<>();
                                        branch.put("branch_id", rs.getInt("branch_id"));
                                        branch.put("branch_name", rs.getString("branch_name"));
                                        branch.put("total_sales", Math.round(rs.getDouble("total_sales") * 100.0) / 100.0);
                                        branch.put("transaction_count", rs.getInt("transaction_count"));
                                        branchSales.add(branch);
                                        System.out.println("Annual Fallback Branch: " + rs.getString("branch_name") + ", Sales: " + rs.getDouble("total_sales"));
                                    }
                                }
                            } catch (SQLException fallbackError) {
                                System.err.println("Annual fallback query failed: " + fallbackError.getMessage());
                                fallbackError.printStackTrace();
                            }
                        }
                        
                        analytics.put("period", analyticsPeriod);
                        analytics.put("year", currentYear);
                        analytics.put("branch_sales", branchSales);
                        reports.put("analytics", analytics);
                    }
                    
                    if (analyticsProcName != null && !"annual".equals(analyticsPeriod) && dateParam != null) {
                        String weeklyMonthlyCallQuery = "{CALL " + analyticsProcName + "(?)}";
                        System.out.println("Calling stored procedure: " + analyticsProcName + " with date: " + dateParam.toString());
                        boolean storedProcSuccess = false;
                        
                        try (CallableStatement cstmt = conn.prepareCall(weeklyMonthlyCallQuery)) {
                            cstmt.setDate(1, dateParam);
                            boolean hasResultSet = cstmt.execute();
                            if (hasResultSet) {
                                try (ResultSet rs = cstmt.getResultSet()) {
                                    int rowCount = 0;
                                    while (rs.next()) {
                                        rowCount++;
                                        Map<String, Object> branch = new HashMap<>();
                                        branch.put("branch_id", rs.getInt("branch_id"));
                                        branch.put("branch_name", rs.getString("branch_name"));
                                        branch.put("total_sales", Math.round(rs.getDouble("total_sales") * 100.0) / 100.0);
                                        branch.put("transaction_count", rs.getInt("transaction_count"));
                                        branchSales.add(branch);
                                        System.out.println("Branch: " + rs.getString("branch_name") + ", Sales: " + rs.getDouble("total_sales") + ", Transactions: " + rs.getInt("transaction_count"));
                                    }
                                    System.out.println("Total rows returned: " + rowCount);
                                    storedProcSuccess = true;
                                    if (rowCount == 0) {
                                        System.out.println("WARNING: No data returned from stored procedure. Check if transactions exist for the date range.");
                                    }
                                }
                            } else {
                                System.out.println("WARNING: Stored procedure did not return a result set.");
                            }
                        } catch (SQLException e) {
                            System.err.println("Stored procedure failed: " + e.getMessage());
                            e.printStackTrace();
                            
                            // Fallback: Use direct SQL query if stored procedure doesn't exist
                            System.out.println("Falling back to direct SQL query for analytics");
                            String fallbackQuery;
                            if ("weekly".equals(analyticsPeriod)) {
                                java.time.LocalDate today = java.time.LocalDate.now();
                                java.time.DayOfWeek dayOfWeek = today.getDayOfWeek();
                                int daysToSubtract = dayOfWeek.getValue() - 1;
                                java.time.LocalDate weekStart = today.minusDays(daysToSubtract);
                                java.time.LocalDate weekEnd = weekStart.plusDays(6);
                                
                                fallbackQuery = "SELECT b.branch_id, b.name AS branch_name, " +
                                                "  COALESCE(SUM(CASE WHEN o.status <> 'cancelled' " +
                                                "                   THEN (o.total_amount * t.exchange_rate) " +
                                                "                   ELSE 0 END), 0.00) AS total_sales, " +
                                                "  COALESCE(COUNT(DISTINCT CASE WHEN o.status <> 'cancelled' " +
                                                "                              THEN t.transaction_id END), 0) AS transaction_count " +
                                                "FROM Branch b " +
                                                "LEFT JOIN OrderTbl o ON o.branch_id = b.branch_id " +
                                                "LEFT JOIN TransactionTbl t ON t.order_id = o.order_id " +
                                                "  AND DATE(t.transaction_date) BETWEEN ? AND ? " +
                                                "  AND t.status = 'completed' " +
                                                "GROUP BY b.branch_id, b.name " +
                                                "ORDER BY total_sales DESC";

                                
                                
                                try (PreparedStatement stmt = conn.prepareStatement(fallbackQuery)) {
                                    stmt.setDate(1, java.sql.Date.valueOf(weekStart));
                                    stmt.setDate(2, java.sql.Date.valueOf(weekEnd));
                                    try (ResultSet rs = stmt.executeQuery()) {
                                        while (rs.next()) {
                                            Map<String, Object> branch = new HashMap<>();
                                            branch.put("branch_id", rs.getInt("branch_id"));
                                            branch.put("branch_name", rs.getString("branch_name"));
                                            branch.put("total_sales", Math.round(rs.getDouble("total_sales") * 100.0) / 100.0);
                                            branch.put("transaction_count", rs.getInt("transaction_count"));
                                            branchSales.add(branch);
                                            System.out.println("Fallback Branch: " + rs.getString("branch_name") + ", Sales: " + rs.getDouble("total_sales"));
                                        }
                                    }
                                }
                            } else if ("monthly".equals(analyticsPeriod)) {
                                java.time.LocalDate today = java.time.LocalDate.now();
                                java.time.LocalDate monthStart = today.withDayOfMonth(1);
                                java.time.LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                                
                                fallbackQuery = "SELECT b.branch_id, b.name AS branch_name, " +
                                                "  COALESCE(SUM(CASE WHEN o.status <> 'cancelled' " +
                                                "                   THEN (o.total_amount * t.exchange_rate) " +
                                                "                   ELSE 0 END), 0.00) AS total_sales, " +
                                                "  COALESCE(COUNT(DISTINCT CASE WHEN o.status <> 'cancelled' " +
                                                "                              THEN t.transaction_id END), 0) AS transaction_count " +
                                                "FROM Branch b " +
                                                "LEFT JOIN OrderTbl o ON o.branch_id = b.branch_id " +
                                                "LEFT JOIN TransactionTbl t ON t.order_id = o.order_id " +
                                                "  AND DATE(t.transaction_date) BETWEEN ? AND ? " +
                                                "  AND t.status = 'completed' " +
                                                "GROUP BY b.branch_id, b.name " +
                                                "ORDER BY total_sales DESC";
                                
                                try (PreparedStatement stmt = conn.prepareStatement(fallbackQuery)) {
                                    stmt.setDate(1, java.sql.Date.valueOf(monthStart));
                                    stmt.setDate(2, java.sql.Date.valueOf(monthEnd));
                                    try (ResultSet rs = stmt.executeQuery()) {
                                        while (rs.next()) {
                                            Map<String, Object> branch = new HashMap<>();
                                            branch.put("branch_id", rs.getInt("branch_id"));
                                            branch.put("branch_name", rs.getString("branch_name"));
                                            branch.put("total_sales", Math.round(rs.getDouble("total_sales") * 100.0) / 100.0);
                                            branch.put("transaction_count", rs.getInt("transaction_count"));
                                            branchSales.add(branch);
                                            System.out.println("Fallback Branch: " + rs.getString("branch_name") + ", Sales: " + rs.getDouble("total_sales"));
                                        }
                                    }
                                }
                            }
                        }
                        
                        analytics.put("period", analyticsPeriod);
                        analytics.put("start_date", dateParam.toString());
                        analytics.put("branch_sales", branchSales);
                        reports.put("analytics", analytics);
                    }
                } catch (SQLException e) {
                    System.err.println("Error fetching analytics: " + e.getMessage());
                    e.printStackTrace();
                    // If stored procedure doesn't exist, analytics will be empty
                    // Frontend will show appropriate message
                    analytics.put("period", analyticsPeriod);
                    analytics.put("branch_sales", new ArrayList<>());
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("doesn't exist")) {
                        analytics.put("error", "Analytics stored procedures are not installed. Please ensure sp_weekly_sales_per_branch, sp_monthly_sales_per_branch, and sp_annual_sales_per_branch are created in the database.");
                    } else {
                        analytics.put("error", "Error fetching analytics: " + errorMsg);
                    }
                    reports.put("analytics", analytics);
                }
            }
            
            Map<String, Object> responseData = new HashMap<>();
            Map<String, String> period = new HashMap<>();
            period.put("start_date", startDate);
            period.put("end_date", endDate);
            responseData.put("period", period);
            responseData.put("reports", reports);
            
            ResponseUtil.sendJSONResponse(response, responseData, 200, "");
            
        } catch (SQLException e) {
            System.err.println("Get reports error: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.sendErrorResponse(response, "An error occurred while fetching reports", 500);
        } finally {
            DatabaseConfig.closeDBConnection(conn);
        }
    }
}





