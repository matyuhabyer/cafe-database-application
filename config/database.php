<?php
/**
 * Database Configuration
 * Connects to remote MySQL database (not XAMPP MySQL)
 */

// Database connection settings
define('DB_HOST', 'ccscloud.dlsu.edu.ph:21013'); // Replace with your school cloud database host
define('DB_USER', 'student1');     // Replace with your database username
define('DB_PASS', 'Dlsu1234!');     // Replace with your database password
define('DB_NAME', 'cafe_db');           // Database name

/**
 * Create database connection
 * @return mysqli|false Returns mysqli connection object or false on failure
 */
function getDBConnection() {
    $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);
    
    // Check connection
    if ($conn->connect_error) {
        error_log("Database connection failed: " . $conn->connect_error);
        return false;
    }
    
    // Set charset to utf8mb4 for proper character encoding
    $conn->set_charset("utf8mb4");
    
    return $conn;
}

/**
 * Close database connection
 * @param mysqli $conn Database connection object
 */
function closeDBConnection($conn) {
    if ($conn) {
        $conn->close();
    }
}

/**
 * Execute a prepared statement and return result
 * @param mysqli $conn Database connection
 * @param string $query SQL query with placeholders
 * @param string $types Parameter types (i=int, d=double, s=string, b=blob)
 * @param array $params Parameters to bind
 * @return mysqli_stmt|false Prepared statement or false on failure
 */
function executeQuery($conn, $query, $types = '', $params = []) {
    $stmt = $conn->prepare($query);
    
    if (!$stmt) {
        error_log("Prepare failed: " . $conn->error);
        return false;
    }
    
    if (!empty($params)) {
        $stmt->bind_param($types, ...$params);
    }
    
    if (!$stmt->execute()) {
        error_log("Execute failed: " . $stmt->error);
        $stmt->close();
        return false;
    }
    
    return $stmt;
}

/**
 * Call a stored procedure and return results
 * @param mysqli $conn Database connection
 * @param string $procedureName Name of the stored procedure
 * @param array $inputParams Input parameters
 * @param array $outputParams Array of output parameter names (e.g., ['@order_id', '@result_message'])
 * @return array|false Returns array with output parameters or false on failure
 */
function callStoredProcedure($conn, $procedureName, $inputParams = [], $outputParams = []) {
    // Build parameter placeholders
    $placeholders = str_repeat('?,', count($inputParams)) . implode(',', $outputParams);
    
    // Build procedure call
    $query = "CALL $procedureName($placeholders)";
    
    $stmt = $conn->prepare($query);
    
    if (!$stmt) {
        error_log("Prepare failed: " . $conn->error);
        return false;
    }
    
    // Bind input parameters if any
    if (!empty($inputParams)) {
        $types = str_repeat('s', count($inputParams)); // Default to string, adjust as needed
        $stmt->bind_param($types, ...$inputParams);
    }
    
    if (!$stmt->execute()) {
        error_log("Execute failed: " . $stmt->error);
        $stmt->close();
        return false;
    }
    
    $stmt->close();
    
    // Get output parameters
    $result = [];
    if (!empty($outputParams)) {
        $outputQuery = "SELECT " . implode(',', $outputParams);
        $outputResult = $conn->query($outputQuery);
        if ($outputResult) {
            $result = $outputResult->fetch_assoc();
        }
    }
    
    return $result;
}

/**
 * Send JSON response
 * @param mixed $data Response data
 * @param int $statusCode HTTP status code
 * @param string $message Optional message
 */
function sendJSONResponse($data, $statusCode = 200, $message = '') {
    http_response_code($statusCode);
    header('Content-Type: application/json');
    
    $response = [
        'success' => $statusCode >= 200 && $statusCode < 300,
        'data' => $data,
        'message' => $message
    ];
    
    echo json_encode($response);
    exit;
}

/**
 * Send error response
 * @param string $message Error message
 * @param int $statusCode HTTP status code
 */
function sendErrorResponse($message, $statusCode = 400) {
    sendJSONResponse(null, $statusCode, $message);
}
