<?php
/**
 * Login API Endpoint
 * Handles authentication for customers and employees
 */

require_once '../../config/database.php';

// Only allow POST requests
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendErrorResponse('Method not allowed', 405);
}

// Get JSON input
$input = json_decode(file_get_contents('php://input'), true);

// Validate input
if (!isset($input['username']) || !isset($input['password']) || !isset($input['role'])) {
    sendErrorResponse('Missing required fields: username, password, role');
}

$username = trim($input['username']);
$password = $input['password'];
$role = $input['role'];

// Validate role
$validRoles = ['customer', 'staff', 'manager', 'admin'];
if (!in_array($role, $validRoles)) {
    sendErrorResponse('Invalid role specified');
}

$conn = getDBConnection();
if (!$conn) {
    sendErrorResponse('Database connection failed', 500);
}

try {
    if ($role === 'customer') {
        // Customer login
        $query = "SELECT c.customer_id, c.name, c.email, c.phone_num, 
                         l.loyalty_id, l.card_number, l.points, l.is_active
                  FROM Customer c
                  LEFT JOIN LoyaltyCard l ON c.customer_id = l.customer_id
                  WHERE c.username = ? AND c.password = ?";
        
        $stmt = executeQuery($conn, $query, 'ss', [$username, md5($password)]);
        
        if (!$stmt) {
            sendErrorResponse('Login failed', 500);
        }
        
        $result = $stmt->get_result();
        
        if ($result->num_rows === 0) {
            sendErrorResponse('Invalid username or password', 401);
        }
        
        $user = $result->fetch_assoc();
        
        // Start session
        session_start();
        $_SESSION['user_id'] = $user['customer_id'];
        $_SESSION['user_type'] = 'customer';
        $_SESSION['username'] = $username;
        $_SESSION['name'] = $user['name'];
        
        sendJSONResponse([
            'user_id' => $user['customer_id'],
            'name' => $user['name'],
            'email' => $user['email'],
            'phone_num' => $user['phone_num'],
            'loyalty_id' => $user['loyalty_id'],
            'card_number' => $user['card_number'],
            'points' => (int)$user['points'],
            'role' => 'customer'
        ], 200, 'Login successful');
        
    } else {
        // Employee login (staff, manager, admin)
        $query = "SELECT e.employee_id, e.name, e.role, e.contact_num, e.branch_id,
                         b.name as branch_name
                  FROM Employee e
                  LEFT JOIN Branch b ON e.branch_id = b.branch_id
                  WHERE e.username = ? AND e.password = ? AND e.role = ?";
        
        $stmt = executeQuery($conn, $query, 'sss', [$username, md5($password), $role]);
        
        if (!$stmt) {
            sendErrorResponse('Login failed', 500);
        }
        
        $result = $stmt->get_result();
        
        if ($result->num_rows === 0) {
            sendErrorResponse('Invalid username, password, or role', 401);
        }
        
        $user = $result->fetch_assoc();
        
        // Start session
        session_start();
        $_SESSION['user_id'] = $user['employee_id'];
        $_SESSION['user_type'] = 'employee';
        $_SESSION['username'] = $username;
        $_SESSION['name'] = $user['name'];
        $_SESSION['role'] = $user['role'];
        $_SESSION['branch_id'] = $user['branch_id'];
        
        sendJSONResponse([
            'employee_id' => $user['employee_id'],
            'name' => $user['name'],
            'role' => $user['role'],
            'contact_num' => $user['contact_num'],
            'branch_id' => $user['branch_id'],
            'branch_name' => $user['branch_name']
        ], 200, 'Login successful');
    }
    
} catch (Exception $e) {
    error_log("Login error: " . $e->getMessage());
    sendErrorResponse('An error occurred during login', 500);
} finally {
    closeDBConnection($conn);
}

