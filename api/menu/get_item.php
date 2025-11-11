<?php
/**
 * Get Single Menu Item API Endpoint
 * Returns detailed information about a specific menu item
 */

require_once '../../config/database.php';

// Allow GET requests
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    sendErrorResponse('Method not allowed', 405);
}

if (!isset($_GET['menu_id'])) {
    sendErrorResponse('Missing menu_id parameter');
}

$menu_id = (int)$_GET['menu_id'];
$currencyCode = isset($_GET['currency']) ? $_GET['currency'] : 'PHP';

$conn = getDBConnection();
if (!$conn) {
    sendErrorResponse('Database connection failed', 500);
}

try {
    // Get currency rate
    $currencyQuery = "SELECT currency_id, code, symbol, rate FROM Currency WHERE code = ?";
    $currencyStmt = executeQuery($conn, $currencyQuery, 's', [$currencyCode]);
    
    if (!$currencyStmt) {
        sendErrorResponse('Invalid currency', 400);
    }
    
    $currencyResult = $currencyStmt->get_result();
    if ($currencyResult->num_rows === 0) {
        $currencyStmt->close();
        sendErrorResponse('Currency not found', 404);
    }
    
    $currency = $currencyResult->fetch_assoc();
    $currencyStmt->close();
    
    // Get menu item
    $menuQuery = "SELECT m.menu_id, m.category_id, m.name, m.description, 
                         m.price_amount, m.is_drink, m.is_available,
                         c.name as category_name
                  FROM Menu m
                  INNER JOIN Category c ON m.category_id = c.category_id
                  WHERE m.menu_id = ?";
    
    $menuStmt = executeQuery($conn, $menuQuery, 'i', [$menu_id]);
    
    if (!$menuStmt) {
        sendErrorResponse('Failed to fetch menu item', 500);
    }
    
    $menuResult = $menuStmt->get_result();
    
    if ($menuResult->num_rows === 0) {
        $menuStmt->close();
        sendErrorResponse('Menu item not found', 404);
    }
    
    $menu = $menuResult->fetch_assoc();
    $menuStmt->close();
    
    // Convert price
    $basePrice = (float)$menu['price_amount'];
    $convertedPrice = $basePrice * (float)$currency['rate'];
    
    $menuItem = [
        'menu_id' => (int)$menu['menu_id'],
        'category_id' => (int)$menu['category_id'],
        'category_name' => $menu['category_name'],
        'name' => $menu['name'],
        'description' => $menu['description'],
        'price_amount' => round($convertedPrice, 2),
        'price_php' => round($basePrice, 2),
        'currency_code' => $currency['code'],
        'currency_symbol' => $currency['symbol'],
        'is_drink' => (bool)$menu['is_drink'],
        'is_available' => (bool)$menu['is_available'],
        'drink_options' => [],
        'available_extras' => [],
        'legends' => []
    ];
    
    // Get drink options
    if ($menu['is_drink']) {
        $drinkOptionQuery = "SELECT drink_option_id, temperature, price_modifier 
                             FROM DrinkOption 
                             WHERE menu_id = ?";
        $doStmt = executeQuery($conn, $drinkOptionQuery, 'i', [$menu_id]);
        
        if ($doStmt) {
            $doResult = $doStmt->get_result();
            while ($do = $doResult->fetch_assoc()) {
                $modifierPrice = (float)$do['price_modifier'] * (float)$currency['rate'];
                $menuItem['drink_options'][] = [
                    'drink_option_id' => (int)$do['drink_option_id'],
                    'temperature' => $do['temperature'],
                    'price_modifier' => round($modifierPrice, 2),
                    'price_modifier_php' => round((float)$do['price_modifier'], 2)
                ];
            }
            $doStmt->close();
        }
    }
    
    // Get available extras
    $extraQuery = "SELECT e.extra_id, e.name, e.price
                   FROM Extra e
                   INNER JOIN MenuExtra me ON e.extra_id = me.extra_id
                   WHERE me.menu_id = ?
                   ORDER BY e.name";
    $extraStmt = executeQuery($conn, $extraQuery, 'i', [$menu_id]);
    
    if ($extraStmt) {
        $extraResult = $extraStmt->get_result();
        while ($extra = $extraResult->fetch_assoc()) {
            $extraPrice = (float)$extra['price'] * (float)$currency['rate'];
            $menuItem['available_extras'][] = [
                'extra_id' => (int)$extra['extra_id'],
                'name' => $extra['name'],
                'price' => round($extraPrice, 2),
                'price_php' => round((float)$extra['price'], 2)
            ];
        }
        $extraStmt->close();
    }
    
    // Get legends
    $legendQuery = "SELECT l.legend_id, l.code, l.description
                    FROM Legend l
                    INNER JOIN MenuLegend ml ON l.legend_id = ml.legend_id
                    WHERE ml.menu_id = ?";
    $legendStmt = executeQuery($conn, $legendQuery, 'i', [$menu_id]);
    
    if ($legendStmt) {
        $legendResult = $legendStmt->get_result();
        while ($legend = $legendResult->fetch_assoc()) {
            $menuItem['legends'][] = [
                'legend_id' => (int)$legend['legend_id'],
                'code' => $legend['code'],
                'description' => $legend['description']
            ];
        }
        $legendStmt->close();
    }
    
    sendJSONResponse($menuItem, 200);
    
} catch (Exception $e) {
    error_log("Get menu item error: " . $e->getMessage());
    sendErrorResponse('An error occurred while fetching menu item', 500);
} finally {
    closeDBConnection($conn);
}

