-- =====================================================
-- DATABASE SCHEMA UPDATES
-- Add authentication fields to Employee and Customer tables
-- Run this in MySQL Workbench after creating the base schema
-- =====================================================

USE cafe_db;

-- Add username and password fields to Customer table
ALTER TABLE Customer
ADD COLUMN username VARCHAR(50) UNIQUE AFTER customer_id,
ADD COLUMN password VARCHAR(255) AFTER username;

-- Add username and password fields to Employee table
ALTER TABLE Employee
ADD COLUMN username VARCHAR(50) UNIQUE AFTER employee_id,
ADD COLUMN password VARCHAR(255) AFTER username;

-- Create indexes for faster lookups
CREATE INDEX idx_customer_username ON Customer(username);
CREATE INDEX idx_employee_username ON Employee(username);

-- Update existing employees with default credentials (change these in production!)
-- Password: 'admin123' (MD5 hash)
UPDATE Employee 
SET username = LOWER(REPLACE(name, ' ', '')), 
    password = MD5('admin123')
WHERE username IS NULL;

-- Note: For production, use password_hash() in PHP instead of MD5
-- This is just for initial setup. You should update passwords after first login.

-- =====================================================
-- Optional: Add email validation constraint
-- =====================================================

-- Add check constraint for email format (MySQL 8.0.16+)
-- ALTER TABLE Customer
-- ADD CONSTRAINT chk_email_format 
-- CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$');

-- =====================================================
-- Update TransactionTbl payment_method ENUM to include GCash
-- =====================================================

-- Note: The current schema has 'bank_transfer' which can represent GCash
-- If you want a separate GCash option, uncomment below:
-- ALTER TABLE TransactionTbl
-- MODIFY COLUMN payment_method ENUM('cash', 'card', 'gcash', 'bank_transfer', 'other') NOT NULL;

-- =====================================================
-- END OF SCHEMA UPDATES
-- =====================================================

