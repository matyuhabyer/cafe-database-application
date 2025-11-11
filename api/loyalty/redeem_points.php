<?php
/**
 * Redeem Loyalty Points API Endpoint
 * Redeems 100 points for a free item
 */

require_once '../../config/database.php';

session_start();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
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
    // Start transaction
    $conn->begin_transaction();
    
    // Get loyalty card
    $loyaltyQuery = "SELECT loyalty_id, points, is_active 
                     FROM LoyaltyCard 
                     WHERE customer_id = ? AND is_active = 1
                     FOR UPDATE";
    
    $loyaltyStmt = executeQuery($conn, $loyaltyQuery, 'i', [$customer_id]);
    
    if (!$loyaltyStmt) {
        $conn->rollback();
        sendErrorResponse('Failed to fetch loyalty card', 500);
    }
    
    $loyaltyResult = $loyaltyStmt->get_result();
    
    if ($loyaltyResult->num_rows === 0) {
        $loyaltyStmt->close();
        $conn->rollback();
        sendErrorResponse('Loyalty card not found', 404);
    }
    
    $loyalty = $loyaltyResult->fetch_assoc();
    $loyalty_id = $loyalty['loyalty_id'];
    $current_points = (int)$loyalty['points'];
    $loyaltyStmt->close();
    
    // Check if customer has enough points
    if ($current_points < 100) {
        $conn->rollback();
        sendErrorResponse("Insufficient points. You have {$current_points} points. Need 100 points to redeem.", 400);
    }
    
    // Deduct 100 points
    $updateQuery = "UPDATE LoyaltyCard 
                    SET points = points - 100, 
                        last_redeemed = NOW() 
                    WHERE loyalty_id = ?";
    
    $updateStmt = executeQuery($conn, $updateQuery, 'i', [$loyalty_id]);
    
    if (!$updateStmt) {
        $conn->rollback();
        sendErrorResponse('Failed to redeem points', 500);
    }
    
    $updateStmt->close();
    
    // Get updated points
    $newPointsQuery = "SELECT points FROM LoyaltyCard WHERE loyalty_id = ?";
    $newPointsStmt = executeQuery($conn, $newPointsQuery, 'i', [$loyalty_id]);
    
    $newPoints = 0;
    if ($newPointsStmt) {
        $newPointsResult = $newPointsStmt->get_result();
        if ($newPointsResult->num_rows > 0) {
            $newPointsRow = $newPointsResult->fetch_assoc();
            $newPoints = (int)$newPointsRow['points'];
        }
        $newPointsStmt->close();
    }
    
    // Commit transaction
    $conn->commit();
    
    sendJSONResponse([
        'loyalty_id' => $loyalty_id,
        'points_redeemed' => 100,
        'points_remaining' => $newPoints,
        'redeemed_at' => date('Y-m-d H:i:s')
    ], 200, 'Points redeemed successfully. You can now order a free item!');
    
} catch (Exception $e) {
    if ($conn->in_transaction) {
        $conn->rollback();
    }
    error_log("Redeem points error: " . $e->getMessage());
    sendErrorResponse('An error occurred while redeeming points', 500);
} finally {
    closeDBConnection($conn);
}

