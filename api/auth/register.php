<?php
/**
 * Customer Registration API Endpoint
 * Creates new customer account and loyalty card
 */

require_once '../../config/database.php';

// Only allow POST requests
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendErrorResponse('Method not allowed', 405);
}

// Get JSON input
$input = json_decode(file_get_contents('php://input'), true);

// Validate input
$required = ['username', 'password', 'name', 'email', 'phone_num'];
foreach ($required as $field) {
    if (!isset($input[$field]) || empty(trim($input[$field]))) {
        sendErrorResponse("Missing required field: $field");
    }
}

$username = trim($input['username']);
$password = trim($input['password']);
$name = trim($input['name']);
$email = trim($input['email']);
$phone_num = trim($input['phone_num']);

// Validate email format
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    sendErrorResponse('Invalid email format');
}

// Validate password strength (minimum 6 characters)
if (strlen($password) < 6) {
    sendErrorResponse('Password must be at least 6 characters long');
}

$conn = getDBConnection();
if (!$conn) {
    sendErrorResponse('Database connection failed', 500);
}

try {
    // Start transaction
    $conn->begin_transaction();
    
    // Check if username already exists
    $checkQuery = "SELECT customer_id FROM Customer WHERE username = ?";
    $checkStmt = executeQuery($conn, $checkQuery, 's', [$username]);
    
    if ($checkStmt) {
        $result = $checkStmt->get_result();
        if ($result->num_rows > 0) {
            $conn->rollback();
            sendErrorResponse('Username already exists');
        }
        $checkStmt->close();
    }
    
    // Check if email already exists
    $checkQuery = "SELECT customer_id FROM Customer WHERE email = ?";
    $checkStmt = executeQuery($conn, $checkQuery, 's', [$email]);
    
    if ($checkStmt) {
        $result = $checkStmt->get_result();
        if ($result->num_rows > 0) {
            $conn->rollback();
            sendErrorResponse('Email already registered');
        }
        $checkStmt->close();
    }
    
    // Insert customer
    $insertCustomer = "INSERT INTO Customer (username, password, name, email, phone_num) 
                        VALUES (?, ?, ?, ?, ?)";
    $stmt = executeQuery($conn, $insertCustomer, 'sssss', [
        $username,
        md5($password), // In production, use password_hash() and password_verify()
        $name,
        $email,
        $phone_num
    ]);
    
    if (!$stmt) {
        $conn->rollback();
        sendErrorResponse('Failed to create customer account', 500);
    }
    
    $customer_id = $conn->insert_id;
    $stmt->close();
    
    // Generate loyalty card number
    $card_number = 'LC-' . str_pad($customer_id, 6, '0', STR_PAD_LEFT);
    
    // Create loyalty card
    $insertLoyalty = "INSERT INTO LoyaltyCard (customer_id, card_number, points, is_active) 
                       VALUES (?, ?, 0, 1)";
    $stmt = executeQuery($conn, $insertLoyalty, 'is', [$customer_id, $card_number]);
    
    if (!$stmt) {
        $conn->rollback();
        sendErrorResponse('Failed to create loyalty card', 500);
    }
    
    $loyalty_id = $conn->insert_id;
    $stmt->close();
    
    // Commit transaction
    $conn->commit();
    
    // Start session
    session_start();
    $_SESSION['user_id'] = $customer_id;
    $_SESSION['user_type'] = 'customer';
    $_SESSION['username'] = $username;
    $_SESSION['name'] = $name;
    
    sendJSONResponse([
        'customer_id' => $customer_id,
        'name' => $name,
        'email' => $email,
        'phone_num' => $phone_num,
        'loyalty_id' => $loyalty_id,
        'card_number' => $card_number,
        'points' => 0,
        'role' => 'customer'
    ], 201, 'Registration successful');
    
} catch (Exception $e) {
    if ($conn->in_transaction) {
        $conn->rollback();
    }
    error_log("Registration error: " . $e->getMessage());
    sendErrorResponse('An error occurred during registration', 500);
} finally {
    closeDBConnection($conn);
}

