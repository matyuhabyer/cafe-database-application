-- =====================================================
-- DATABASE VIEWS
-- For MySQL Workbench - The Waiting Room Cafe Database
-- =====================================================

USE cafe_db;

-- =====================================================
-- VIEW 1: Available Menu Items View
-- Displays all available menu items with category, description, and current price
-- =====================================================

CREATE OR REPLACE VIEW vw_AvailableMenuItems AS
SELECT 
    m.menu_id,
    m.name,
    m.description,
    c.category_id,
    c.name as category_name,
    m.price_amount,
    m.is_drink,
    m.is_available,
    GROUP_CONCAT(DISTINCT l.code ORDER BY l.code SEPARATOR ', ') as legend_codes
FROM Menu m
INNER JOIN Category c ON m.category_id = c.category_id
LEFT JOIN MenuLegend ml ON m.menu_id = ml.menu_id
LEFT JOIN Legend l ON ml.legend_id = l.legend_id
WHERE m.is_available = 1
GROUP BY m.menu_id, m.name, m.description, c.category_id, c.name, m.price_amount, m.is_drink, m.is_available;

-- =====================================================
-- VIEW 2: Active Orders View
-- Shows all active orders with total amount, status, and assigned employee
-- =====================================================

CREATE OR REPLACE VIEW vw_ActiveOrders AS
SELECT 
    o.order_id,
    o.order_date,
    o.total_amount,
    o.status,
    o.earned_points,
    c.customer_id,
    c.name as customer_name,
    c.email as customer_email,
    c.phone_num as customer_phone,
    e.employee_id,
    e.name as employee_name,
    e.role as employee_role,
    b.branch_id,
    b.name as branch_name,
    l.card_number as loyalty_card_number,
    l.points as customer_points
FROM OrderTbl o
INNER JOIN Customer c ON o.customer_id = c.customer_id
LEFT JOIN Employee e ON o.employee_id = e.employee_id
LEFT JOIN Branch b ON o.branch_id = b.branch_id
LEFT JOIN LoyaltyCard l ON o.loyalty_id = l.loyalty_id
WHERE o.status IN ('pending', 'confirmed')
ORDER BY o.order_date DESC;

-- =====================================================
-- VIEW 3: Customer Order Payment View
-- Combines customer, order, and payment details for reporting dashboards
-- =====================================================

CREATE OR REPLACE VIEW vw_CustomerOrderPayment AS
SELECT 
    c.customer_id,
    c.name as customer_name,
    c.email,
    c.phone_num,
    o.order_id,
    o.order_date,
    o.total_amount as order_total,
    o.status as order_status,
    o.earned_points,
    t.transaction_id,
    t.payment_method,
    t.amount_paid,
    t.exchange_rate,
    (t.amount_paid * t.exchange_rate) as amount_paid_php,
    t.transaction_date,
    t.status as transaction_status,
    cur.code as currency_code,
    cur.symbol as currency_symbol,
    b.branch_id,
    b.name as branch_name,
    l.card_number,
    l.points as loyalty_points
FROM Customer c
INNER JOIN OrderTbl o ON c.customer_id = o.customer_id
LEFT JOIN TransactionTbl t ON o.order_id = t.order_id
LEFT JOIN Currency cur ON t.currency_id = cur.currency_id
LEFT JOIN Branch b ON o.branch_id = b.branch_id
LEFT JOIN LoyaltyCard l ON o.loyalty_id = l.loyalty_id
ORDER BY o.order_date DESC;

-- =====================================================
-- VIEW 4: Order Queue View
-- Displays real-time order queue for staff, sorted by preparation status
-- =====================================================

CREATE OR REPLACE VIEW vw_OrderQueue AS
SELECT 
    o.order_id,
    o.order_date,
    o.status,
    o.total_amount,
    c.name as customer_name,
    b.name as branch_name,
    e.name as assigned_employee,
    COUNT(DISTINCT oi.order_item_id) as item_count,
    SUM(oi.quantity) as total_items,
    TIMESTAMPDIFF(MINUTE, o.order_date, NOW()) as minutes_waiting
FROM OrderTbl o
INNER JOIN Customer c ON o.customer_id = c.customer_id
LEFT JOIN Branch b ON o.branch_id = b.branch_id
LEFT JOIN Employee e ON o.employee_id = e.employee_id
LEFT JOIN OrderItem oi ON o.order_id = oi.order_id
WHERE o.status IN ('pending', 'confirmed')
GROUP BY o.order_id, o.order_date, o.status, o.total_amount, c.name, b.name, e.name
ORDER BY 
    CASE o.status
        WHEN 'pending' THEN 1
        WHEN 'confirmed' THEN 2
        ELSE 3
    END,
    o.order_date ASC;

-- =====================================================
-- VIEW 5: Daily Sales Summary View
-- Provides summarized daily sales per branch and per payment type
-- =====================================================

CREATE OR REPLACE VIEW vw_DailySalesSummary AS
SELECT 
    DATE(t.transaction_date) as sale_date,
    b.branch_id,
    b.name as branch_name,
    t.payment_method,
    cur.code as currency_code,
    COUNT(DISTINCT t.order_id) as total_orders,
    COUNT(t.transaction_id) as total_transactions,
    SUM(t.amount_paid) as total_sales_original,
    SUM(t.amount_paid * t.exchange_rate) as total_sales_php,
    AVG(t.amount_paid * t.exchange_rate) as avg_order_value_php,
    MIN(t.amount_paid * t.exchange_rate) as min_order_value_php,
    MAX(t.amount_paid * t.exchange_rate) as max_order_value_php
FROM TransactionTbl t
INNER JOIN Branch b ON t.branch_id = b.branch_id
INNER JOIN Currency cur ON t.currency_id = cur.currency_id
WHERE t.status = 'completed'
GROUP BY DATE(t.transaction_date), b.branch_id, b.name, t.payment_method, cur.code
ORDER BY sale_date DESC, b.name, t.payment_method;

-- =====================================================
-- VIEW 6: Loyalty Points View
-- Shows loyalty point balances and redeemed rewards for each customer
-- =====================================================

CREATE OR REPLACE VIEW vw_LoyaltyPoints AS
SELECT 
    c.customer_id,
    c.name as customer_name,
    c.email,
    c.phone_num,
    l.loyalty_id,
    l.card_number,
    l.points,
    l.last_redeemed,
    l.is_active,
    l.created_at as card_created_at,
    COUNT(DISTINCT o.order_id) as total_orders,
    SUM(o.total_amount) as total_spending,
    SUM(o.earned_points) as total_points_earned,
    FLOOR(SUM(o.total_amount) / 50) as calculated_points,
    CASE 
        WHEN l.points >= 100 THEN 'Eligible for Redemption'
        ELSE CONCAT('Need ', 100 - l.points, ' more points')
    END as redemption_status
FROM Customer c
INNER JOIN LoyaltyCard l ON c.customer_id = l.customer_id
LEFT JOIN OrderTbl o ON c.customer_id = o.customer_id AND o.status = 'completed'
WHERE l.is_active = 1
GROUP BY c.customer_id, c.name, c.email, c.phone_num, l.loyalty_id, l.card_number, 
         l.points, l.last_redeemed, l.is_active, l.created_at
ORDER BY l.points DESC;

-- =====================================================
-- VIEW 7: Top Selling Items View
-- Displays top-selling items within a selected time period
-- =====================================================

CREATE OR REPLACE VIEW vw_TopSellingItems AS
SELECT 
    m.menu_id,
    m.name as menu_item_name,
    c.category_id,
    c.name as category_name,
    m.price_amount,
    SUM(oi.quantity) as total_quantity_sold,
    COUNT(DISTINCT oi.order_id) as times_ordered,
    SUM(oi.price * oi.quantity) as total_revenue,
    AVG(oi.price * oi.quantity) as avg_order_value,
    MIN(oi.price * oi.quantity) as min_order_value,
    MAX(oi.price * oi.quantity) as max_order_value
FROM Menu m
INNER JOIN Category c ON m.category_id = c.category_id
INNER JOIN OrderItem oi ON m.menu_id = oi.menu_id
INNER JOIN OrderTbl o ON oi.order_id = o.order_id
INNER JOIN TransactionTbl t ON o.order_id = t.order_id
WHERE t.status = 'completed'
GROUP BY m.menu_id, m.name, c.category_id, c.name, m.price_amount
ORDER BY total_quantity_sold DESC;

-- =====================================================
-- VIEW 8: Canceled/Refunded Orders View
-- Lists canceled or refunded orders for management review
-- =====================================================

CREATE OR REPLACE VIEW vw_CanceledOrders AS
SELECT 
    o.order_id,
    o.order_date,
    o.total_amount,
    o.status,
    c.customer_id,
    c.name as customer_name,
    c.email,
    c.phone_num,
    b.branch_id,
    b.name as branch_name,
    e.employee_id,
    e.name as employee_name,
    e.role as employee_role,
    oh.timestamp as status_changed_at,
    oh.remarks,
    COUNT(DISTINCT oi.order_item_id) as item_count
FROM OrderTbl o
INNER JOIN Customer c ON o.customer_id = c.customer_id
LEFT JOIN Branch b ON o.branch_id = b.branch_id
LEFT JOIN Employee e ON o.employee_id = e.employee_id
LEFT JOIN OrderHistory oh ON o.order_id = oh.order_id AND oh.status = o.status
LEFT JOIN OrderItem oi ON o.order_id = oi.order_id
WHERE o.status = 'cancelled'
GROUP BY o.order_id, o.order_date, o.total_amount, o.status, c.customer_id, c.name, 
         c.email, c.phone_num, b.branch_id, b.name, e.employee_id, e.name, e.role, 
         oh.timestamp, oh.remarks
ORDER BY o.order_date DESC;

-- =====================================================
-- VIEW 9: Employee Activity Log View
-- Shows employee activity logs (order updates, payments processed, etc.)
-- =====================================================

CREATE OR REPLACE VIEW vw_EmployeeActivity AS
SELECT 
    e.employee_id,
    e.name as employee_name,
    e.role,
    b.branch_id,
    b.name as branch_name,
    oh.history_id,
    oh.order_id,
    oh.status as order_status,
    oh.timestamp as activity_timestamp,
    oh.remarks,
    o.total_amount,
    o.customer_id,
    c.name as customer_name,
    CASE 
        WHEN oh.status = 'pending' THEN 'Order Created'
        WHEN oh.status = 'confirmed' THEN 'Order Confirmed'
        WHEN oh.status = 'completed' THEN 'Order Completed'
        WHEN oh.status = 'cancelled' THEN 'Order Cancelled'
        ELSE 'Status Updated'
    END as activity_type
FROM Employee e
INNER JOIN OrderHistory oh ON e.employee_id = oh.employee_id
INNER JOIN OrderTbl o ON oh.order_id = o.order_id
LEFT JOIN Customer c ON o.customer_id = c.customer_id
LEFT JOIN Branch b ON e.branch_id = b.branch_id
ORDER BY oh.timestamp DESC;

-- =====================================================
-- VIEW 10: System Transactions View
-- Provides a comprehensive view of system transactions for admins and managers
-- =====================================================

CREATE OR REPLACE VIEW vw_SystemTransactions AS
SELECT 
    t.transaction_id,
    t.transaction_date,
    t.status as transaction_status,
    o.order_id,
    o.order_date,
    o.status as order_status,
    o.total_amount as order_total,
    c.customer_id,
    c.name as customer_name,
    b.branch_id,
    b.name as branch_name,
    t.payment_method,
    t.amount_paid,
    t.exchange_rate,
    (t.amount_paid * t.exchange_rate) as amount_paid_php,
    cur.currency_id,
    cur.code as currency_code,
    cur.name as currency_name,
    cur.symbol as currency_symbol,
    e.employee_id,
    e.name as employee_name,
    e.role as employee_role,
    TIMESTAMPDIFF(MINUTE, o.order_date, t.transaction_date) as minutes_to_payment
FROM TransactionTbl t
INNER JOIN OrderTbl o ON t.order_id = o.order_id
INNER JOIN Customer c ON o.customer_id = c.customer_id
INNER JOIN Branch b ON t.branch_id = b.branch_id
INNER JOIN Currency cur ON t.currency_id = cur.currency_id
LEFT JOIN Employee e ON o.employee_id = e.employee_id
ORDER BY t.transaction_date DESC;

-- =====================================================
-- END OF VIEWS
-- =====================================================

