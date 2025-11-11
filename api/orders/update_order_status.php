<?php
/**
 * Update Order Status API Endpoint
 * Allows employees to update order status
 */

require_once '../../config/database.php';

session_start();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendErrorResponse('Method not allowed', 405);
}

// Check if user is logged in as employee
if (!isset($_SESSION['user_id']) || $_SESSION['user_type'] !== 'employee') {
    sendErrorResponse('Unauthorized. Employee login required.', 401);
}

$input = json_decode(file_get_contents('php://input'), true);

if (!isset($input['order_id']) || !isset($input['status'])) {
    sendErrorResponse('Missing required fields: order_id, status');
}

$order_id = (int)$input['order_id'];
$status = $input['status'];
$remarks = isset($input['remarks']) ? trim($input['remarks']) : '';
$employee_id = $_SESSION['user_id'];

// Validate status
$validStatuses = ['pending', 'confirmed', 'completed', 'cancelled'];
if (!in_array($status, $validStatuses)) {
    sendErrorResponse('Invalid status. Must be one of: ' . implode(', ', $validStatuses));
}

$conn = getDBConnection();
if (!$conn) {
    sendErrorResponse('Database connection failed', 500);
}

try {
    // Start transaction
    $conn->begin_transaction();
    
    // Verify order exists
    $checkQuery = "SELECT order_id, status, customer_id, loyalty_id, total_amount 
                   FROM OrderTbl 
                   WHERE order_id = ?";
    $checkStmt = executeQuery($conn, $checkQuery, 'i', [$order_id]);
    
    if (!$checkStmt) {
        $conn->rollback();
        sendErrorResponse('Failed to verify order', 500);
    }
    
    $orderResult = $checkStmt->get_result();
    if ($orderResult->num_rows === 0) {
        $checkStmt->close();
        $conn->rollback();
        sendErrorResponse('Order not found', 404);
    }
    
    $order = $orderResult->fetch_assoc();
    $checkStmt->close();
    
    // Update order status
    $updateQuery = "UPDATE OrderTbl SET status = ? WHERE order_id = ?";
    $updateStmt = executeQuery($conn, $updateQuery, 'si', [$status, $order_id]);
    
    if (!$updateStmt) {
        $conn->rollback();
        sendErrorResponse('Failed to update order status', 500);
    }
    $updateStmt->close();
    
    // Log in order history
    $historyQuery = "INSERT INTO OrderHistory (order_id, employee_id, status, remarks) 
                     VALUES (?, ?, ?, ?)";
    $historyStmt = executeQuery($conn, $historyQuery, 'iiss', [
        $order_id,
        $employee_id,
        $status,
        $remarks ?: "Status updated to $status"
    ]);
    
    if (!$historyStmt) {
        $conn->rollback();
        sendErrorResponse('Failed to log order history', 500);
    }
    $historyStmt->close();
    
    // If order is completed, update loyalty points
    if ($status === 'completed' && $order['loyalty_id']) {
        // Calculate points: 1 point per ₱50
        $total_amount = (float)$order['total_amount'];
        $points_earned = floor($total_amount / 50);
        
        if ($points_earned > 0) {
            // Update loyalty card points
            $pointsQuery = "UPDATE LoyaltyCard 
                           SET points = points + ?, 
                               last_redeemed = CASE WHEN points + ? >= 100 THEN NOW() ELSE last_redeemed END
                           WHERE loyalty_id = ?";
            $pointsStmt = executeQuery($conn, $pointsQuery, 'iii', [
                $points_earned,
                $points_earned,
                $order['loyalty_id']
            ]);
            
            if (!$pointsStmt) {
                $conn->rollback();
                sendErrorResponse('Failed to update loyalty points', 500);
            }
            $pointsStmt->close();
            
            // Update order earned_points
            $earnedQuery = "UPDATE OrderTbl SET earned_points = ? WHERE order_id = ?";
            $earnedStmt = executeQuery($conn, $earnedQuery, 'ii', [$points_earned, $order_id]);
            
            if (!$earnedStmt) {
                $conn->rollback();
                sendErrorResponse('Failed to update order earned points', 500);
            }
            $earnedStmt->close();
        }
    }
    
    // Commit transaction
    $conn->commit();
    
    sendJSONResponse([
        'order_id' => $order_id,
        'status' => $status,
        'points_earned' => isset($points_earned) ? $points_earned : 0
    ], 200, 'Order status updated successfully');
    
} catch (Exception $e) {
    if ($conn->in_transaction) {
        $conn->rollback();
    }
    error_log("Update order status error: " . $e->getMessage());
    sendErrorResponse('An error occurred while updating order status', 500);
} finally {
    closeDBConnection($conn);
}

