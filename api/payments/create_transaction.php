<?php
/**
 * Create Transaction API Endpoint
 * Records payment for an order
 */

require_once '../../config/database.php';

session_start();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendErrorResponse('Method not allowed', 405);
}

$input = json_decode(file_get_contents('php://input'), true);

// Validate input
if (!isset($input['order_id']) || !isset($input['currency_id']) || 
    !isset($input['payment_method']) || !isset($input['amount_paid'])) {
    sendErrorResponse('Missing required fields: order_id, currency_id, payment_method, amount_paid');
}

$order_id = (int)$input['order_id'];
$currency_id = (int)$input['currency_id'];
$payment_method = $input['payment_method'];
$amount_paid = (float)$input['amount_paid'];

// Validate payment method
$validMethods = ['cash', 'card', 'bank_transfer', 'other'];
// Map GCash to bank_transfer
if (strtolower($payment_method) === 'gcash') {
    $payment_method = 'bank_transfer';
}

if (!in_array($payment_method, $validMethods)) {
    sendErrorResponse('Invalid payment method');
}

$conn = getDBConnection();
if (!$conn) {
    sendErrorResponse('Database connection failed', 500);
}

try {
    // Start transaction
    $conn->begin_transaction();
    
    // Verify order exists and get details
    $orderQuery = "SELECT order_id, total_amount, status, branch_id 
                   FROM OrderTbl 
                   WHERE order_id = ?";
    $orderStmt = executeQuery($conn, $orderQuery, 'i', [$order_id]);
    
    if (!$orderStmt) {
        $conn->rollback();
        sendErrorResponse('Failed to verify order', 500);
    }
    
    $orderResult = $orderStmt->get_result();
    if ($orderResult->num_rows === 0) {
        $orderStmt->close();
        $conn->rollback();
        sendErrorResponse('Order not found', 404);
    }
    
    $order = $orderResult->fetch_assoc();
    $orderStmt->close();
    
    // Check if order already has a completed transaction
    $existingQuery = "SELECT transaction_id, status FROM TransactionTbl WHERE order_id = ? AND status = 'completed'";
    $existingStmt = executeQuery($conn, $existingQuery, 'i', [$order_id]);
    
    if ($existingStmt) {
        $existingResult = $existingStmt->get_result();
        if ($existingResult->num_rows > 0) {
            $existingStmt->close();
            $conn->rollback();
            sendErrorResponse('Order already has a completed payment', 400);
        }
        $existingStmt->close();
    }
    
    // Get currency exchange rate
    $currencyQuery = "SELECT rate FROM Currency WHERE currency_id = ?";
    $currencyStmt = executeQuery($conn, $currencyQuery, 'i', [$currency_id]);
    
    if (!$currencyStmt) {
        $conn->rollback();
        sendErrorResponse('Invalid currency', 400);
    }
    
    $currencyResult = $currencyStmt->get_result();
    if ($currencyResult->num_rows === 0) {
        $currencyStmt->close();
        $conn->rollback();
        sendErrorResponse('Currency not found', 404);
    }
    
    $currency = $currencyResult->fetch_assoc();
    $exchange_rate = (float)$currency['rate'];
    $currencyStmt->close();
    
    // Calculate PHP equivalent
    $amount_php = $amount_paid / $exchange_rate;
    
    // Verify payment amount (allow small tolerance for rounding)
    $order_total = (float)$order['total_amount'];
    $tolerance = 0.01;
    
    if (abs($amount_php - $order_total) > $tolerance) {
        $conn->rollback();
        sendErrorResponse("Payment amount mismatch. Expected: ₱{$order_total}, Received: ₱{$amount_php}", 400);
    }
    
    // Create transaction
    $transactionQuery = "INSERT INTO TransactionTbl 
                        (order_id, currency_id, payment_method, amount_paid, exchange_rate, status, branch_id) 
                        VALUES (?, ?, ?, ?, ?, 'completed', ?)";
    
    $transactionStmt = executeQuery($conn, $transactionQuery, 'iisddi', [
        $order_id,
        $currency_id,
        $payment_method,
        $amount_paid,
        $exchange_rate,
        $order['branch_id']
    ]);
    
    if (!$transactionStmt) {
        $conn->rollback();
        sendErrorResponse('Failed to create transaction', 500);
    }
    
    $transaction_id = $conn->insert_id;
    $transactionStmt->close();
    
    // Update order status to confirmed if it's still pending
    if ($order['status'] === 'pending') {
        $updateOrderQuery = "UPDATE OrderTbl SET status = 'confirmed' WHERE order_id = ?";
        $updateOrderStmt = executeQuery($conn, $updateOrderQuery, 'i', [$order_id]);
        
        if (!$updateOrderStmt) {
            $conn->rollback();
            sendErrorResponse('Failed to update order status', 500);
        }
        $updateOrderStmt->close();
        
        // Log status change
        $employee_id = isset($_SESSION['user_id']) && $_SESSION['user_type'] === 'employee' 
                      ? $_SESSION['user_id'] : null;
        
        $historyQuery = "INSERT INTO OrderHistory (order_id, employee_id, status, remarks) 
                         VALUES (?, ?, 'confirmed', 'Payment received')";
        $historyStmt = executeQuery($conn, $historyQuery, 'iis', [$order_id, $employee_id, 'confirmed']);
        
        if ($historyStmt) {
            $historyStmt->close();
        }
    }
    
    // Commit transaction
    $conn->commit();
    
    sendJSONResponse([
        'transaction_id' => $transaction_id,
        'order_id' => $order_id,
        'amount_paid' => round($amount_paid, 2),
        'amount_php' => round($amount_php, 2),
        'exchange_rate' => round($exchange_rate, 4),
        'payment_method' => $payment_method,
        'status' => 'completed'
    ], 201, 'Transaction created successfully');
    
} catch (Exception $e) {
    if ($conn->in_transaction) {
        $conn->rollback();
    }
    error_log("Create transaction error: " . $e->getMessage());
    sendErrorResponse('An error occurred while creating transaction', 500);
} finally {
    closeDBConnection($conn);
}

