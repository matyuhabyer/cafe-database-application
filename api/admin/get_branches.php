<?php
/**
 * Get Branches API Endpoint
 */

require_once '../../config/database.php';

session_start();

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    sendErrorResponse('Method not allowed', 405);
}

// Check authorization (admin or manager)
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
    
    if ($role === 'admin') {
        // Admin: see all branches
        $query = "SELECT b.branch_id, b.name, b.address, b.contact_num, b.manager_id,
                         e.name as manager_name
                  FROM Branch b
                  LEFT JOIN Employee e ON b.manager_id = e.employee_id
                  ORDER BY b.name";
        $result = $conn->query($query);
    } else {
        // Manager: see only their branch
        $query = "SELECT b.branch_id, b.name, b.address, b.contact_num, b.manager_id,
                         e.name as manager_name
                  FROM Branch b
                  LEFT JOIN Employee e ON b.manager_id = e.employee_id
                  WHERE b.branch_id = ?";
        $stmt = executeQuery($conn, $query, 'i', [$branch_id]);
        $result = $stmt->get_result();
    }
    
    $branches = [];
    while ($row = $result->fetch_assoc()) {
        $branches[] = [
            'branch_id' => (int)$row['branch_id'],
            'name' => $row['name'],
            'address' => $row['address'],
            'contact_num' => $row['contact_num'],
            'manager_id' => $row['manager_id'] ? (int)$row['manager_id'] : null,
            'manager_name' => $row['manager_name']
        ];
    }
    
    if (isset($stmt)) {
        $stmt->close();
    }
    
    sendJSONResponse($branches, 200);
    
} catch (Exception $e) {
    error_log("Get branches error: " . $e->getMessage());
    sendErrorResponse('An error occurred while fetching branches', 500);
} finally {
    closeDBConnection($conn);
}

