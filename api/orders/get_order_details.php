<?php
/**
 * Get Order Details API Endpoint
 * Returns detailed information about a specific order
 */

require_once '../../config/database.php';

session_start();

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    sendErrorResponse('Method not allowed', 405);
}

if (!isset($_GET['order_id'])) {
    sendErrorResponse('Missing order_id parameter');
}

$order_id = (int)$_GET['order_id'];

$conn = getDBConnection();
if (!$conn) {
    sendErrorResponse('Database connection failed', 500);
}

try {
    // Get order basic info
    $orderQuery = "SELECT o.order_id, o.order_date, o.total_amount, o.status, o.earned_points,
                          c.customer_id, c.name as customer_name, c.email, c.phone_num,
                          b.branch_id, b.name as branch_name,
                          l.card_number, l.points as loyalty_points
                   FROM OrderTbl o
                   LEFT JOIN Customer c ON o.customer_id = c.customer_id
                   LEFT JOIN Branch b ON o.branch_id = b.branch_id
                   LEFT JOIN LoyaltyCard l ON o.loyalty_id = l.loyalty_id
                   WHERE o.order_id = ?";
    
    $orderStmt = executeQuery($conn, $orderQuery, 'i', [$order_id]);
    
    if (!$orderStmt) {
        sendErrorResponse('Failed to fetch order', 500);
    }
    
    $orderResult = $orderStmt->get_result();
    
    if ($orderResult->num_rows === 0) {
        $orderStmt->close();
        sendErrorResponse('Order not found', 404);
    }
    
    $order = $orderResult->fetch_assoc();
    $orderStmt->close();
    
    // Check authorization
    if (isset($_SESSION['user_type'])) {
        if ($_SESSION['user_type'] === 'customer' && (int)$order['customer_id'] !== (int)$_SESSION['user_id']) {
            sendErrorResponse('Unauthorized access to this order', 403);
        }
    } else {
        sendErrorResponse('Unauthorized', 401);
    }
    
    // Get order items
    $itemsQuery = "SELECT oi.order_item_id, oi.menu_id, oi.quantity, oi.price,
                          m.name as menu_name, m.description,
                          do.temperature, do.price_modifier
                   FROM OrderItem oi
                   INNER JOIN Menu m ON oi.menu_id = m.menu_id
                   LEFT JOIN DrinkOption do ON oi.drink_option_id = do.drink_option_id
                   WHERE oi.order_id = ?";
    
    $itemsStmt = executeQuery($conn, $itemsQuery, 'i', [$order_id]);
    
    $items = [];
    if ($itemsStmt) {
        $itemsResult = $itemsStmt->get_result();
        
        while ($item = $itemsResult->fetch_assoc()) {
            $order_item_id = $item['order_item_id'];
            
            $orderItem = [
                'order_item_id' => (int)$order_item_id,
                'menu_id' => (int)$item['menu_id'],
                'menu_name' => $item['menu_name'],
                'description' => $item['description'],
                'quantity' => (int)$item['quantity'],
                'price' => round((float)$item['price'], 2),
                'temperature' => $item['temperature'],
                'extras' => []
            ];
            
            // Get extras for this order item
            $extrasQuery = "SELECT e.extra_id, e.name, e.price, oie.quantity
                            FROM OrderItemExtra oie
                            INNER JOIN Extra e ON oie.extra_id = e.extra_id
                            WHERE oie.order_item_id = ?";
            $extrasStmt = executeQuery($conn, $extrasQuery, 'i', [$order_item_id]);
            
            if ($extrasStmt) {
                $extrasResult = $extrasStmt->get_result();
                while ($extra = $extrasResult->fetch_assoc()) {
                    $orderItem['extras'][] = [
                        'extra_id' => (int)$extra['extra_id'],
                        'name' => $extra['name'],
                        'price' => round((float)$extra['price'], 2),
                        'quantity' => (int)$extra['quantity']
                    ];
                }
                $extrasStmt->close();
            }
            
            $items[] = $orderItem;
        }
        $itemsStmt->close();
    }
    
    // Get order history
    $historyQuery = "SELECT oh.history_id, oh.status, oh.timestamp, oh.remarks,
                            e.name as employee_name, e.role
                     FROM OrderHistory oh
                     LEFT JOIN Employee e ON oh.employee_id = e.employee_id
                     WHERE oh.order_id = ?
                     ORDER BY oh.timestamp ASC";
    
    $historyStmt = executeQuery($conn, $historyQuery, 'i', [$order_id]);
    
    $history = [];
    if ($historyStmt) {
        $historyResult = $historyStmt->get_result();
        while ($h = $historyResult->fetch_assoc()) {
            $history[] = [
                'history_id' => (int)$h['history_id'],
                'status' => $h['status'],
                'timestamp' => $h['timestamp'],
                'remarks' => $h['remarks'],
                'employee_name' => $h['employee_name'],
                'employee_role' => $h['role']
            ];
        }
        $historyStmt->close();
    }
    
    // Get transaction if exists
    $transactionQuery = "SELECT t.transaction_id, t.payment_method, t.amount_paid, 
                                t.exchange_rate, t.transaction_date, t.status,
                                cur.code as currency_code, cur.symbol as currency_symbol
                         FROM TransactionTbl t
                         INNER JOIN Currency cur ON t.currency_id = cur.currency_id
                         WHERE t.order_id = ?";
    
    $transactionStmt = executeQuery($conn, $transactionQuery, 'i', [$order_id]);
    
    $transaction = null;
    if ($transactionStmt) {
        $transactionResult = $transactionStmt->get_result();
        if ($transactionResult->num_rows > 0) {
            $t = $transactionResult->fetch_assoc();
            $transaction = [
                'transaction_id' => (int)$t['transaction_id'],
                'payment_method' => $t['payment_method'],
                'amount_paid' => round((float)$t['amount_paid'], 2),
                'exchange_rate' => round((float)$t['exchange_rate'], 4),
                'transaction_date' => $t['transaction_date'],
                'status' => $t['status'],
                'currency_code' => $t['currency_code'],
                'currency_symbol' => $t['currency_symbol']
            ];
        }
        $transactionStmt->close();
    }
    
    $orderDetails = [
        'order_id' => (int)$order['order_id'],
        'order_date' => $order['order_date'],
        'total_amount' => round((float)$order['total_amount'], 2),
        'status' => $order['status'],
        'earned_points' => (int)$order['earned_points'],
        'customer' => [
            'customer_id' => (int)$order['customer_id'],
            'name' => $order['customer_name'],
            'email' => $order['email'],
            'phone_num' => $order['phone_num']
        ],
        'branch' => [
            'branch_id' => (int)$order['branch_id'],
            'name' => $order['branch_name']
        ],
        'loyalty' => [
            'card_number' => $order['card_number'],
            'points' => (int)$order['loyalty_points']
        ],
        'items' => $items,
        'history' => $history,
        'transaction' => $transaction
    ];
    
    sendJSONResponse($orderDetails, 200);
    
} catch (Exception $e) {
    error_log("Get order details error: " . $e->getMessage());
    sendErrorResponse('An error occurred while fetching order details', 500);
} finally {
    closeDBConnection($conn);
}

