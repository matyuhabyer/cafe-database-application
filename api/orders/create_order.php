<?php
/**
 * Create Order API Endpoint
 * Creates a new order from cart items
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

$input = json_decode(file_get_contents('php://input'), true);

// Validate input
if (!isset($input['items']) || !is_array($input['items']) || empty($input['items'])) {
    sendErrorResponse('Order must contain at least one item');
}

if (!isset($input['branch_id'])) {
    sendErrorResponse('Branch ID is required');
}

$customer_id = $_SESSION['user_id'];
$branch_id = (int)$input['branch_id'];
$items = $input['items'];

$conn = getDBConnection();
if (!$conn) {
    sendErrorResponse('Database connection failed', 500);
}

try {
    // Start transaction
    $conn->begin_transaction();
    
    // Get customer's loyalty card
    $loyaltyQuery = "SELECT loyalty_id FROM LoyaltyCard WHERE customer_id = ? AND is_active = 1";
    $loyaltyStmt = executeQuery($conn, $loyaltyQuery, 'i', [$customer_id]);
    
    $loyalty_id = null;
    if ($loyaltyStmt) {
        $loyaltyResult = $loyaltyStmt->get_result();
        if ($loyaltyResult->num_rows > 0) {
            $loyalty = $loyaltyResult->fetch_assoc();
            $loyalty_id = $loyalty['loyalty_id'];
        }
        $loyaltyStmt->close();
    }
    
    // Calculate total amount
    $total_amount = 0;
    
    foreach ($items as $item) {
        if (!isset($item['menu_id']) || !isset($item['quantity']) || !isset($item['price'])) {
            $conn->rollback();
            sendErrorResponse('Invalid item data');
        }
        
        $itemTotal = (float)$item['price'] * (int)$item['quantity'];
        $total_amount += $itemTotal;
    }
    
    // Create order
    $orderQuery = "INSERT INTO OrderTbl (customer_id, loyalty_id, branch_id, total_amount, status) 
                   VALUES (?, ?, ?, ?, 'pending')";
    $orderStmt = executeQuery($conn, $orderQuery, 'iiid', [
        $customer_id,
        $loyalty_id,
        $branch_id,
        $total_amount
    ]);
    
    if (!$orderStmt) {
        $conn->rollback();
        sendErrorResponse('Failed to create order', 500);
    }
    
    $order_id = $conn->insert_id;
    $orderStmt->close();
    
    // Insert order items
    foreach ($items as $item) {
        $menu_id = (int)$item['menu_id'];
        $quantity = (int)$item['quantity'];
        $price = (float)$item['price'];
        $drink_option_id = isset($item['drink_option_id']) ? (int)$item['drink_option_id'] : null;
        
        // Insert order item
        $itemQuery = "INSERT INTO OrderItem (order_id, menu_id, drink_option_id, quantity, price) 
                      VALUES (?, ?, ?, ?, ?)";
        $itemStmt = executeQuery($conn, $itemQuery, 'iiiid', [
            $order_id,
            $menu_id,
            $drink_option_id,
            $quantity,
            $price
        ]);
        
        if (!$itemStmt) {
            $conn->rollback();
            sendErrorResponse('Failed to create order item', 500);
        }
        
        $order_item_id = $conn->insert_id;
        $itemStmt->close();
        
        // Insert order item extras if any
        if (isset($item['extras']) && is_array($item['extras'])) {
            foreach ($item['extras'] as $extra) {
                if (isset($extra['extra_id']) && isset($extra['quantity'])) {
                    $extraQuery = "INSERT INTO OrderItemExtra (order_item_id, extra_id, quantity) 
                                   VALUES (?, ?, ?)";
                    $extraStmt = executeQuery($conn, $extraQuery, 'iii', [
                        $order_item_id,
                        (int)$extra['extra_id'],
                        (int)$extra['quantity']
                    ]);
                    
                    if (!$extraStmt) {
                        $conn->rollback();
                        sendErrorResponse('Failed to add extra to order item', 500);
                    }
                    $extraStmt->close();
                }
            }
        }
    }
    
    // Create initial order history entry
    $historyQuery = "INSERT INTO OrderHistory (order_id, status, remarks) 
                     VALUES (?, 'pending', 'Order created by customer')";
    $historyStmt = executeQuery($conn, $historyQuery, 'is', [$order_id, 'pending']);
    
    if (!$historyStmt) {
        $conn->rollback();
        sendErrorResponse('Failed to create order history', 500);
    }
    $historyStmt->close();
    
    // Commit transaction
    $conn->commit();
    
    sendJSONResponse([
        'order_id' => $order_id,
        'total_amount' => round($total_amount, 2),
        'status' => 'pending'
    ], 201, 'Order created successfully');
    
} catch (Exception $e) {
    if ($conn->in_transaction) {
        $conn->rollback();
    }
    error_log("Create order error: " . $e->getMessage());
    sendErrorResponse('An error occurred while creating order', 500);
} finally {
    closeDBConnection($conn);
}

