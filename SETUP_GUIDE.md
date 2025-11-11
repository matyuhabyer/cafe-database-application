# The Waiting Room Cafe - Database Application Setup Guide

## Overview
This is a web-based cafe ordering and management system built with PHP (XAMPP) and MySQL. The system uses a remote MySQL database (not XAMPP's MySQL) and includes comprehensive stored procedures, triggers, and views.

## Prerequisites
- XAMPP installed (for Apache/PHP)
- MySQL Workbench (for database management)
- Access to remote MySQL database (school cloud)
- PHP 7.4 or higher

## Setup Instructions

### 1. Database Configuration

#### Step 1.1: Update Database Connection
Edit `config/database.php` and update the following constants:
```php
define('DB_HOST', 'your-remote-host');     // Your school cloud database host
define('DB_USER', 'your-username');         // Your database username
define('DB_PASS', 'your-password');         // Your database password
define('DB_NAME', 'cafe_db');               // Database name
```

#### Step 1.2: Create Database Schema
1. Open MySQL Workbench
2. Connect to your remote database
3. Run `models/cafe_db.sql` to create all tables
4. Run `models/cafe_db_data.sql` to insert initial data (categories, menu items, etc.)

#### Step 1.3: Update Schema for Authentication
Run `models/schema_updates.sql` in MySQL Workbench to add:
- `username` and `password` fields to `Customer` table
- `username` and `password` fields to `Employee` table
- Indexes for faster lookups

**Important:** Update employee passwords after first login. Default password is 'admin123' (MD5 hash).

#### Step 1.4: Create Stored Procedures and Triggers
Run `models/stored_procedures_triggers.sql` in MySQL Workbench. This will create:
- 15 triggers for data validation and automation
- 15 stored procedures for business logic
- SystemLog table for audit trails

#### Step 1.5: Create Views
Run `models/views.sql` in MySQL Workbench. This will create 10 views for reporting and data access.

### 2. XAMPP Configuration

#### Step 2.1: Place Files in XAMPP
Ensure all files are in: `C:\xampp\htdocs\cafe-database-application\`

#### Step 2.2: Start Apache
1. Open XAMPP Control Panel
2. Start Apache service
3. **Do NOT start MySQL** (we're using remote MySQL)

#### Step 2.3: Access Application
Open browser and navigate to: `http://localhost/cafe-database-application/`

### 3. Initial Setup

#### Step 3.1: Create Admin Account
After running `schema_updates.sql`, default admin accounts are created with:
- Username: Based on employee name (lowercase, no spaces)
- Password: `admin123` (MD5 hash)

**Change these passwords immediately after first login!**

#### Step 3.2: Test Database Connection
1. Go to `index.php`
2. Try logging in with an admin account
3. Check browser console for any connection errors

### 4. File Structure

```
cafe-database-application/
├── api/                          # Backend API endpoints
│   ├── auth/                    # Authentication endpoints
│   │   ├── login.php
│   │   ├── register.php
│   │   └── logout.php
│   ├── menu/                    # Menu endpoints
│   │   ├── get_menu.php
│   │   ├── get_item.php
│   │   ├── get_categories.php
│   │   └── get_currencies.php
│   ├── orders/                  # Order endpoints
│   │   ├── create_order.php
│   │   ├── get_orders.php
│   │   ├── get_order_details.php
│   │   └── update_order_status.php
│   ├── payments/                # Payment endpoints
│   │   └── create_transaction.php
│   ├── loyalty/                 # Loyalty endpoints
│   │   ├── get_loyalty.php
│   │   └── redeem_points.php
│   └── admin/                   # Admin endpoints
│       ├── get_branches.php
│       └── get_reports.php
├── config/
│   └── database.php             # Database configuration
├── models/                       # SQL files
│   ├── cafe_db.sql              # Base schema
│   ├── cafe_db_data.sql         # Initial data
│   ├── schema_updates.sql       # Authentication fields
│   ├── stored_procedures_triggers.sql  # Procedures & triggers
│   └── views.sql                # Database views
├── css/
│   └── style.css
├── js/
│   ├── products.js              # Menu/product functionality
│   ├── cart.js                  # Cart functionality
│   ├── header.js                # Header/navigation
│   └── admin.js                 # Admin panel
├── includes/
│   └── header.php               # Header component
├── index.php                     # Login page
├── products.php                 # Menu page
├── cart.php                      # Cart page
├── orders.php                    # Orders page
└── admin.php                     # Admin panel
```

### 5. API Endpoints Reference

#### Authentication
- `POST api/auth/login.php` - User login
- `POST api/auth/register.php` - Customer registration
- `POST api/auth/logout.php` - User logout

#### Menu
- `GET api/menu/get_menu.php?currency=PHP` - Get all menu items
- `GET api/menu/get_item.php?menu_id=1&currency=PHP` - Get single item
- `GET api/menu/get_categories.php` - Get all categories
- `GET api/menu/get_currencies.php` - Get available currencies

#### Orders
- `POST api/orders/create_order.php` - Create new order
- `GET api/orders/get_orders.php` - Get user's orders
- `GET api/orders/get_order_details.php?order_id=1` - Get order details
- `POST api/orders/update_order_status.php` - Update order status (employees)

#### Payments
- `POST api/payments/create_transaction.php` - Record payment

#### Loyalty
- `GET api/loyalty/get_loyalty.php` - Get loyalty card info
- `POST api/loyalty/redeem_points.php` - Redeem 100 points

#### Admin
- `GET api/admin/get_branches.php` - Get branches
- `GET api/admin/get_reports.php?start_date=2025-01-01&end_date=2025-01-31` - Get reports

### 6. Database Features

#### Triggers (15 total)
- Price validation
- Stock management
- Order total calculation
- Loyalty points automation
- Order history logging
- Payment locking
- And more...

#### Stored Procedures (15 total)
- `sp_CreateOrder` - Create order with items
- `sp_UpdateOrderItems` - Update order items
- `sp_CancelOrder` - Cancel order
- `sp_RecordPayment` - Record payment
- `sp_CalculateLoyaltyPoints` - Calculate loyalty points
- `sp_RedeemLoyaltyReward` - Redeem points
- `sp_UpdateOrderStatus` - Update order status
- `sp_ManageMenuItem` - Add/edit menu items
- `sp_DailySalesReport` - Daily sales report
- `sp_RevenueAndBestSellers` - Revenue and best sellers
- `sp_TopCustomers` - Top customers
- `sp_TransactionSummary` - Transaction summary
- `sp_BranchComparison` - Branch comparison
- `sp_InventoryReport` - Inventory report
- `sp_ConvertPrices` - Currency conversion

#### Views (10 total)
- `vw_AvailableMenuItems` - Available menu items
- `vw_ActiveOrders` - Active orders
- `vw_CustomerOrderPayment` - Customer order payment details
- `vw_OrderQueue` - Order queue for staff
- `vw_DailySalesSummary` - Daily sales summary
- `vw_LoyaltyPoints` - Loyalty points view
- `vw_TopSellingItems` - Top selling items
- `vw_CanceledOrders` - Canceled orders
- `vw_EmployeeActivity` - Employee activity log
- `vw_SystemTransactions` - System transactions

### 7. Testing Checklist

- [ ] Database connection successful
- [ ] Can login as admin
- [ ] Can register as customer
- [ ] Can view menu items
- [ ] Can add items to cart
- [ ] Can place order
- [ ] Can view orders
- [ ] Can process payment
- [ ] Loyalty points are calculated correctly
- [ ] Order status updates work
- [ ] Admin reports display correctly

### 8. Troubleshooting

#### Database Connection Issues
- Verify database credentials in `config/database.php`
- Check if remote database allows connections from your IP
- Verify database name is correct

#### Session Issues
- Ensure `session_start()` is called in PHP files
- Check PHP session configuration in `php.ini`

#### API Errors
- Check browser console for JavaScript errors
- Check PHP error logs in XAMPP
- Verify all API endpoints are accessible

### 9. Security Notes

1. **Password Storage**: Currently using MD5 (for development). In production, use `password_hash()` and `password_verify()`.

2. **SQL Injection**: All queries use prepared statements.

3. **Session Security**: Implement session timeout and secure session handling.

4. **Input Validation**: Add client-side and server-side validation.

5. **HTTPS**: Use HTTPS in production.

### 10. Next Steps

1. Update database credentials
2. Run all SQL files in order
3. Test all functionality
4. Update default passwords
5. Customize UI/UX as needed
6. Add additional features as required

## Support

For issues or questions, refer to the project documentation or contact the development team.

---

**Note**: This system uses a remote MySQL database. Do not use XAMPP's MySQL service. All database operations should be performed through MySQL Workbench connected to the remote database.

