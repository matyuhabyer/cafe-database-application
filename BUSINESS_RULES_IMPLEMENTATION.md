# Business Rules Implementation Summary

This document outlines the business rules that have been implemented in The Waiting Room Café system.

## ✅ Implemented Business Rules

### 1. Order Management

**Rule**: Each order is created by the customer through the platform. Orders consist of one or more menu items with optional extras. Each order is tagged to the branch and employee that handled it.

**Implementation**:
- ✅ Orders are created by customers via `/api/orders/create_order`
- ✅ Orders support multiple menu items with quantities
- ✅ Orders support drink options (temperature) and extras
- ✅ Orders are tagged with `branch_id` when created
- ✅ Employee tracking is done via `OrderHistory` table when employees process orders

**Location**: `CreateOrderServlet.java`, `UpdateOrderStatusServlet.java`

---

### 2. Menu and Pricing

**Rule**: Menu items are grouped under categories. Some drinks have temperature options (hot or iced) with price differences. Prices are stored in PHP to support multi-currency pricing. Extras have additional costs and are linked to drinks.

**Implementation**:
- ✅ Menu items are organized by categories
- ✅ Drink options (hot/iced) with different prices
- ✅ Multi-currency support (USD, KRW, PHP) with exchange rates
- ✅ Extras can be added to drinks with additional costs

**Location**: `GetMenuServlet.java`, `GetItemServlet.java`, `products.html`, `products.js`

---

### 3. Payment and Transactions

**Rule**: Each order must have at least one corresponding transaction record. The TransactionTbl stores the transactions containing the payment method (Cash, Card, Bank Transfer) and currency used (USD, PHP, KRW). Transactions are recorded with exchange_rate for accurate conversions.

**Implementation**:
- ✅ **ENFORCED**: Orders cannot be marked as "completed" without at least one completed transaction
- ✅ Payment methods validated: `cash`, `card`, `bank_transfer`, `others`
- ✅ Multi-currency transactions with exchange rate tracking
- ✅ Transaction amount validation against order total

**Location**: 
- `CreateTransactionServlet.java` - Payment method validation and transaction creation
- `UpdateOrderStatusServlet.java` - Transaction requirement check before completion

**Key Code**:
```java
// Business Rule: Orders must have at least one transaction before being marked as "completed"
if ("completed".equals(status)) {
    // Check for completed transaction
    if (transactionCount == 0) {
        // Reject completion
    }
}
```

---

### 4. Loyalty Program

**Rule**: 
- Customers earn 1 point per ₱50.00 spent on completed transactions
- When a customer reaches 100 points, they are eligible for one free drink or meal
- After redemption, their current points are deducted by 100

**Implementation**:
- ✅ Points calculation: `Math.floor(totalAmount / 50)` - 1 point per ₱50.00 (PHP amount)
- ✅ Points are awarded automatically when order status changes to "completed"
- ✅ Redemption requires exactly 100 points
- ✅ 100 points are deducted upon redemption

**Location**: 
- `UpdateOrderStatusServlet.java` - Points earning on order completion
- `RedeemPointsServlet.java` - Points redemption logic

**Key Code**:
```java
// Business Rule: Loyalty Points - Customers earn 1 point per ₱50.00 spent
double totalAmount = (Double) order.get("total_amount");
pointsEarned = (int) Math.floor(totalAmount / 50); // 1 point per ₱50.00
```

---

### 5. Branch Management

**Rule**: Employee records include branch_id to identify their assigned location. OrderTbl and TransactionTbl store branch_id for branch-based reporting. Each branch has one manager (manager_id in Branch) and multiple staff members.

**Implementation**:
- ✅ Employees have `branch_id` in their records
- ✅ Orders store `branch_id`
- ✅ Transactions store `branch_id`
- ✅ Branch-specific access restrictions for managers and staff

**Location**: Database schema, `GetBranchesServlet.java`, `GetReportsServlet.java`

---

### 6. Role-Based Access Control

**Rule**: 
- Customers can browse the menu, place orders, make payments, and earn loyalty points
- Staff can process orders, record payments, and update statuses
- Managers can view reports, manage menu items, and oversee staff activity
- Admin has full control of system configuration, branches, and data integrity

**Implementation**:
- ✅ **ENFORCED**: Staff/Managers can only process orders from their assigned branch
- ✅ **ENFORCED**: Staff/Managers can only record payments for orders from their assigned branch
- ✅ Admin can process orders and record payments for any branch
- ✅ Branch-specific filtering in reports and order views

**Location**: 
- `UpdateOrderStatusServlet.java` - Branch validation for order processing
- `CreateTransactionServlet.java` - Branch validation for payment recording
- `GetOrdersServlet.java` - Branch filtering for order views
- `GetReportsServlet.java` - Branch filtering for reports

**Key Code**:
```java
// Business Rule: Branch restriction - Staff/Managers can only process orders from their branch
if (!"admin".equals(role) && branchIdObj != null) {
    Integer orderBranchId = (Integer) order.get("branch_id");
    Integer employeeBranchId = (Integer) branchIdObj;
    
    if (orderBranchId == null || !orderBranchId.equals(employeeBranchId)) {
        // Reject unauthorized access
    }
}
```

---

### 7. Data Integrity and Security

**Rule**: All data changes (orders, payments) are tracked via OrderHistory. Only authenticated users can access customer or employee dashboards. Passwords are encrypted using secure hashing algorithms.

**Implementation**:
- ✅ All order status changes are logged in `OrderHistory` with employee_id, status, and remarks
- ✅ Session-based authentication required for all API endpoints
- ✅ Password encryption using BCrypt hashing
- ✅ Role-based API endpoint protection

**Location**: 
- `UpdateOrderStatusServlet.java` - OrderHistory logging
- `CreateTransactionServlet.java` - OrderHistory logging on payment
- `LoginServlet.java`, `RegisterServlet.java` - Password hashing
- `SessionUtil.java` - Authentication checks

---

## Business Scenario Compliance

### The Waiting Room Cafe operates 2 branches (Manila and Laguna)

✅ **Supported**: System supports multiple branches with branch-specific data isolation

### Customers order drinks or meals through the cafe's platform

✅ **Implemented**: Full online ordering system with menu browsing, cart, and checkout

### Employees process orders and record payments

✅ **Implemented**: 
- Staff can update order statuses
- Staff can record payments (with branch restrictions)
- All actions are logged with employee_id

### Managers monitor branch performance and transactions

✅ **Implemented**: 
- Branch-specific reports for managers
- Sales reports, top-selling items, order status reports
- Managers can only see data from their assigned branch

### Admins oversee operations and generate reports

✅ **Implemented**: 
- Admins can see all branches and all orders
- Full system access for configuration and data management

---

## Validation Summary

| Business Rule | Status | Enforcement Location |
|--------------|--------|---------------------|
| Order must have transaction before completion | ✅ Enforced | `UpdateOrderStatusServlet.java` |
| Branch restrictions for staff/managers | ✅ Enforced | `UpdateOrderStatusServlet.java`, `CreateTransactionServlet.java` |
| Payment method validation | ✅ Enforced | `CreateTransactionServlet.java` |
| Loyalty points calculation (₱50 = 1 point) | ✅ Verified | `UpdateOrderStatusServlet.java` |
| Loyalty points redemption (100 points) | ✅ Verified | `RedeemPointsServlet.java` |
| OrderHistory tracking | ✅ Implemented | All order/payment servlets |
| Password encryption | ✅ Implemented | `PasswordUtil.java` |
| Role-based access control | ✅ Implemented | All servlets via `SessionUtil` |

---

## Notes

1. **Currency**: All prices in the database are stored in PHP. Multi-currency support is handled through exchange rates in the `Currency` table.

2. **Payment Methods**: The system accepts `cash`, `card`, `bank_transfer`, and `others`.

3. **Transaction Requirement**: The system now enforces that orders cannot be marked as "completed" without at least one completed transaction, ensuring data integrity.

4. **Branch Isolation**: Staff and managers are restricted to their assigned branch for all operations (order processing, payment recording, report viewing). Only admins have cross-branch access.

