<?php
/**
 * Get Available Currencies API Endpoint
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
    $query = "SELECT currency_id, code, name, symbol, rate, last_updated 
              FROM Currency 
              ORDER BY code";
    $result = $conn->query($query);
    
    $currencies = [];
    while ($row = $result->fetch_assoc()) {
        $currencies[] = [
            'currency_id' => (int)$row['currency_id'],
            'code' => $row['code'],
            'name' => $row['name'],
            'symbol' => $row['symbol'],
            'rate' => (float)$row['rate'],
            'last_updated' => $row['last_updated']
        ];
    }
    
    sendJSONResponse($currencies, 200);
    
} catch (Exception $e) {
    error_log("Get currencies error: " . $e->getMessage());
    sendErrorResponse('An error occurred while fetching currencies', 500);
} finally {
    closeDBConnection($conn);
}

