<?php
/**
 * Get Orders API Endpoint
 * Returns orders based on user role
 */

require_once '../../config/database.php';

session_start();

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    sendErrorResponse('Method not allowed', 405);
}

$conn = getDBConnection();
if (!$conn) {
    sendErrorResponse('Database connection failed', 500);
}

try {
    // Check user role and build query accordingly
    if (isset($_SESSION['user_type']) && $_SESSION['user_type'] === 'customer') {
        // Customer: get their own orders
        $customer_id = $_SESSION['user_id'];
        $query = "SELECT o.order_id, o.order_date, o.total_amount, o.status, o.earned_points,
                         b.name as branch_name
                  FROM OrderTbl o
                  LEFT JOIN Branch b ON o.branch_id = b.branch_id
                  WHERE o.customer_id = ?
                  ORDER BY o.order_date DESC";
        
        $stmt = executeQuery($conn, $query, 'i', [$customer_id]);
        
    } elseif (isset($_SESSION['user_type']) && $_SESSION['user_type'] === 'employee') {
        // Employee/Manager/Admin: get orders based on role and branch
        $branch_id = isset($_SESSION['branch_id']) ? $_SESSION['branch_id'] : null;
        $role = isset($_SESSION['role']) ? $_SESSION['role'] : 'staff';
        
        if ($role === 'admin') {
            // Admin: see all orders
            $query = "SELECT o.order_id, o.order_date, o.total_amount, o.status, o.earned_points,
                             c.name as customer_name,
                             b.name as branch_name
                      FROM OrderTbl o
                      LEFT JOIN Customer c ON o.customer_id = c.customer_id
                      LEFT JOIN Branch b ON o.branch_id = b.branch_id
                      ORDER BY o.order_date DESC";
            $stmt = $conn->prepare($query);
            $stmt->execute();
        } else {
            // Manager/Staff: see orders for their branch
            if ($branch_id) {
                $query = "SELECT o.order_id, o.order_date, o.total_amount, o.status, o.earned_points,
                                 c.name as customer_name,
                                 b.name as branch_name
                          FROM OrderTbl o
                          LEFT JOIN Customer c ON o.customer_id = c.customer_id
                          LEFT JOIN Branch b ON o.branch_id = b.branch_id
                          WHERE o.branch_id = ?
                          ORDER BY o.order_date DESC";
                $stmt = executeQuery($conn, $query, 'i', [$branch_id]);
            } else {
                sendErrorResponse('Branch ID not found', 400);
            }
        }
    } else {
        sendErrorResponse('Unauthorized', 401);
    }
    
    if (!isset($stmt) || !$stmt) {
        sendErrorResponse('Failed to fetch orders', 500);
    }
    
    $result = $stmt->get_result();
    $orders = [];
    
    while ($row = $result->fetch_assoc()) {
        $orders[] = [
            'order_id' => (int)$row['order_id'],
            'order_date' => $row['order_date'],
            'total_amount' => round((float)$row['total_amount'], 2),
            'status' => $row['status'],
            'earned_points' => (int)$row['earned_points'],
            'customer_name' => isset($row['customer_name']) ? $row['customer_name'] : null,
            'branch_name' => $row['branch_name']
        ];
    }
    
    $stmt->close();
    
    sendJSONResponse($orders, 200);
    
} catch (Exception $e) {
    error_log("Get orders error: " . $e->getMessage());
    sendErrorResponse('An error occurred while fetching orders', 500);
} finally {
    closeDBConnection($conn);
}

