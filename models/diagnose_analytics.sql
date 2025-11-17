-- Diagnostic queries to check why analytics is showing no data

-- 1. Check if stored procedures exist
SELECT 
    ROUTINE_NAME as procedure_name,
    ROUTINE_TYPE,
    CREATED,
    LAST_ALTERED
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'cafe_db' 
  AND ROUTINE_NAME IN (
    'sp_weekly_sales_per_branch',
    'sp_monthly_sales_per_branch',
    'sp_annual_sales_per_branch'
  );

-- 2. Check if branches exist
SELECT 
    COUNT(*) as total_branches,
    GROUP_CONCAT(name) as branch_names
FROM Branch;

-- 3. Check if completed transactions exist
SELECT 
    COUNT(*) as total_completed_transactions,
    COUNT(DISTINCT branch_id) as branches_with_transactions,
    MIN(transaction_date) as earliest_transaction,
    MAX(transaction_date) as latest_transaction
FROM TransactionTbl
WHERE status = 'completed';

-- 4. Check transactions with branch_id NULL
SELECT 
    COUNT(*) as transactions_with_null_branch,
    GROUP_CONCAT(DISTINCT DATE(transaction_date)) as transaction_dates
FROM TransactionTbl
WHERE status = 'completed' AND branch_id IS NULL;

-- 5. Check transactions per branch
SELECT 
    b.branch_id,
    b.name as branch_name,
    COUNT(t.transaction_id) as transaction_count,
    COALESCE(SUM(t.amount_paid * t.exchange_rate), 0) as total_sales
FROM Branch b
LEFT JOIN TransactionTbl t ON t.branch_id = b.branch_id AND t.status = 'completed'
GROUP BY b.branch_id, b.name
ORDER BY total_sales DESC;

-- 6. Test weekly sales procedure manually (replace date with Monday of current week)
-- Example: CALL sp_weekly_sales_per_branch('2025-01-20');
-- Run this after installing the stored procedures

-- 7. Check current date ranges
SELECT 
    CURDATE() as today,
    DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE())) DAY) as week_start_monday,
    DATE_FORMAT(CURDATE(), '%Y-%m-01') as month_start,
    YEAR(CURDATE()) as current_year;

