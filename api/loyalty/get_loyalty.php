<?php
/**
 * Get Loyalty Card Information API Endpoint
 */

require_once '../../config/database.php';

session_start();

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    sendErrorResponse('Method not allowed', 405);
}

// Check if user is logged in as customer
if (!isset($_SESSION['user_id']) || $_SESSION['user_type'] !== 'customer') {
    sendErrorResponse('Unauthorized. Customer login required.', 401);
}

$customer_id = $_SESSION['user_id'];

$conn = getDBConnection();
if (!$conn) {
    sendErrorResponse('Database connection failed', 500);
}

try {
    $query = "SELECT l.loyalty_id, l.card_number, l.points, l.last_redeemed, 
                     l.is_active, l.created_at,
                     c.name as customer_name
              FROM LoyaltyCard l
              INNER JOIN Customer c ON l.customer_id = c.customer_id
              WHERE l.customer_id = ? AND l.is_active = 1";
    
    $stmt = executeQuery($conn, $query, 'i', [$customer_id]);
    
    if (!$stmt) {
        sendErrorResponse('Failed to fetch loyalty card', 500);
    }
    
    $result = $stmt->get_result();
    
    if ($result->num_rows === 0) {
        $stmt->close();
        sendErrorResponse('Loyalty card not found', 404);
    }
    
    $loyalty = $result->fetch_assoc();
    $stmt->close();
    
    $points = (int)$loyalty['points'];
    $can_redeem = $points >= 100;
    
    sendJSONResponse([
        'loyalty_id' => (int)$loyalty['loyalty_id'],
        'card_number' => $loyalty['card_number'],
        'points' => $points,
        'can_redeem' => $can_redeem,
        'last_redeemed' => $loyalty['last_redeemed'],
        'is_active' => (bool)$loyalty['is_active'],
        'created_at' => $loyalty['created_at'],
        'customer_name' => $loyalty['customer_name']
    ], 200);
    
} catch (Exception $e) {
    error_log("Get loyalty error: " . $e->getMessage());
    sendErrorResponse('An error occurred while fetching loyalty information', 500);
} finally {
    closeDBConnection($conn);
}

