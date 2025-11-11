<?php
/**
 * Get Categories API Endpoint
 */

require_once '../../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    sendErrorResponse('Method not allowed', 405);
}

$conn = getDBConnection();
if (!$conn) {
    sendErrorResponse('Database connection failed', 500);
}

try {
    $query = "SELECT category_id, name FROM Category ORDER BY name";
    $result = $conn->query($query);
    
    $categories = [];
    while ($row = $result->fetch_assoc()) {
        $categories[] = [
            'category_id' => (int)$row['category_id'],
            'name' => $row['name']
        ];
    }
    
    sendJSONResponse($categories, 200);
    
} catch (Exception $e) {
    error_log("Get categories error: " . $e->getMessage());
    sendErrorResponse('An error occurred while fetching categories', 500);
} finally {
    closeDBConnection($conn);
}

