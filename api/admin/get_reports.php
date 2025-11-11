<?php
/**
 * Get Reports API Endpoint
 * Returns sales and performance reports
 */

require_once '../../config/database.php';

session_start();

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    sendErrorResponse('Method not allowed', 405);
}

// Check authorization
if (!isset($_SESSION['user_type']) || 
    ($_SESSION['user_type'] !== 'employee' || 
     !in_array($_SESSION['role'], ['admin', 'manager']))) {
    sendErrorResponse('Unauthorized. Admin or Manager access required.', 401);
}

$conn = getDBConnection();
if (!$conn) {
    sendErrorResponse('Database connection failed', 500);
}

try {
    $role = $_SESSION['role'];
    $branch_id = isset($_SESSION['branch_id']) ? $_SESSION['branch_id'] : null;
    
    // Get date range from query params
    $start_date = isset($_GET['start_date']) ? $_GET['start_date'] : date('Y-m-01'); // First day of current month
    $end_date = isset($_GET['end_date']) ? $_GET['end_date'] : date('Y-m-t'); // Last day of current month
    
    $reports = [];
    
    // Total Sales
    if ($role === 'admin') {
        $salesQuery = "SELECT SUM(amount_paid * exchange_rate) as total_sales_php,
                              COUNT(*) as total_transactions
                       FROM TransactionTbl
                       WHERE status = 'completed' 
                       AND DATE(transaction_date) BETWEEN ? AND ?";
        $salesStmt = executeQuery($conn, $salesQuery, 'ss', [$start_date, $end_date]);
    } else {
        $salesQuery = "SELECT SUM(amount_paid * exchange_rate) as total_sales_php,
                              COUNT(*) as total_transactions
                       FROM TransactionTbl
                       WHERE status = 'completed' 
                       AND branch_id = ?
                       AND DATE(transaction_date) BETWEEN ? AND ?";
        $salesStmt = executeQuery($conn, $salesQuery, 'iss', [$branch_id, $start_date, $end_date]);
    }
    
    if ($salesStmt) {
        $salesResult = $salesStmt->get_result();
        $sales = $salesResult->fetch_assoc();
        $reports['sales'] = [
            'total_sales_php' => round((float)($sales['total_sales_php'] ?? 0), 2),
            'total_transactions' => (int)($sales['total_transactions'] ?? 0)
        ];
        $salesStmt->close();
    }
    
    // Top Selling Items
    if ($role === 'admin') {
        $topItemsQuery = "SELECT m.name, SUM(oi.quantity) as total_quantity, 
                                 SUM(oi.price * oi.quantity) as total_revenue
                          FROM OrderItem oi
                          INNER JOIN Menu m ON oi.menu_id = m.menu_id
                          INNER JOIN OrderTbl o ON oi.order_id = o.order_id
                          INNER JOIN TransactionTbl t ON o.order_id = t.order_id
                          WHERE t.status = 'completed'
                          AND DATE(t.transaction_date) BETWEEN ? AND ?
                          GROUP BY m.menu_id, m.name
                          ORDER BY total_quantity DESC
                          LIMIT 10";
        $topItemsStmt = executeQuery($conn, $topItemsQuery, 'ss', [$start_date, $end_date]);
    } else {
        $topItemsQuery = "SELECT m.name, SUM(oi.quantity) as total_quantity, 
                                 SUM(oi.price * oi.quantity) as total_revenue
                          FROM OrderItem oi
                          INNER JOIN Menu m ON oi.menu_id = m.menu_id
                          INNER JOIN OrderTbl o ON oi.order_id = o.order_id
                          INNER JOIN TransactionTbl t ON o.order_id = t.order_id
                          WHERE t.status = 'completed'
                          AND t.branch_id = ?
                          AND DATE(t.transaction_date) BETWEEN ? AND ?
                          GROUP BY m.menu_id, m.name
                          ORDER BY total_quantity DESC
                          LIMIT 10";
        $topItemsStmt = executeQuery($conn, $topItemsQuery, 'iss', [$branch_id, $start_date, $end_date]);
    }
    
    $reports['top_items'] = [];
    if ($topItemsStmt) {
        $topItemsResult = $topItemsStmt->get_result();
        while ($item = $topItemsResult->fetch_assoc()) {
            $reports['top_items'][] = [
                'name' => $item['name'],
                'total_quantity' => (int)$item['total_quantity'],
                'total_revenue' => round((float)$item['total_revenue'], 2)
            ];
        }
        $topItemsStmt->close();
    }
    
    // Orders by Status
    if ($role === 'admin') {
        $statusQuery = "SELECT status, COUNT(*) as count
                        FROM OrderTbl
                        WHERE DATE(order_date) BETWEEN ? AND ?
                        GROUP BY status";
        $statusStmt = executeQuery($conn, $statusQuery, 'ss', [$start_date, $end_date]);
    } else {
        $statusQuery = "SELECT status, COUNT(*) as count
                        FROM OrderTbl
                        WHERE branch_id = ?
                        AND DATE(order_date) BETWEEN ? AND ?
                        GROUP BY status";
        $statusStmt = executeQuery($conn, $statusQuery, 'iss', [$branch_id, $start_date, $end_date]);
    }
    
    $reports['orders_by_status'] = [];
    if ($statusStmt) {
        $statusResult = $statusStmt->get_result();
        while ($status = $statusResult->fetch_assoc()) {
            $reports['orders_by_status'][$status['status']] = (int)$status['count'];
        }
        $statusStmt->close();
    }
    
    sendJSONResponse([
        'period' => [
            'start_date' => $start_date,
            'end_date' => $end_date
        ],
        'reports' => $reports
    ], 200);
    
} catch (Exception $e) {
    error_log("Get reports error: " . $e->getMessage());
    sendErrorResponse('An error occurred while fetching reports', 500);
} finally {
    closeDBConnection($conn);
}

