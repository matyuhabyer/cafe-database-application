# How Triggers and Stored Procedures Work

## 📋 Overview

### **Triggers** = Automatic actions that fire when database events happen
### **Stored Procedures** = Reusable code blocks you can call from PHP or MySQL Workbench

---

## 🔧 SETUP PROCESS (One-Time)

### Step 1: Create Triggers & Procedures in MySQL Workbench

1. **Open MySQL Workbench**
2. **Connect to your remote database** (`ccscloud.dlsu.edu.ph:21013`)
3. **Open the SQL file**: `models/stored_procedures_triggers.sql`
4. **Run the entire file** (Execute button or F5)
   - This creates ALL triggers and stored procedures in your database
   - You only need to do this ONCE

**That's it!** They're now stored in your database and will work automatically.

---

## ⚡ HOW TRIGGERS WORK (Automatic)

Triggers fire **automatically** when certain database events occur. You don't need to call them from PHP.

### Example: `trg_loyalty_points_award`

**What it does:**
- Automatically adds loyalty points when an order status changes to "completed"

**How it works:**
1. PHP code updates order status: `UPDATE OrderTbl SET status = 'completed' WHERE order_id = 1`
2. **Trigger automatically fires** (you don't call it!)
3. Trigger checks: "Is status now 'completed'?"
4. If yes, trigger updates loyalty points automatically
5. Done!

**You don't need to do anything in PHP** - the trigger handles it automatically.

### Example: `trg_menu_price_check`

**What it does:**
- Prevents inserting menu items with zero or negative prices

**How it works:**
1. PHP tries to insert: `INSERT INTO Menu (name, price_amount) VALUES ('Coffee', -10)`
2. **Trigger automatically fires BEFORE insert**
3. Trigger checks: "Is price_amount <= 0?"
4. If yes, trigger **blocks the insert** and throws an error
5. PHP receives the error

**You don't call the trigger** - it automatically validates every insert/update.

---

## 🔄 HOW STORED PROCEDURES WORK (Called from PHP)

Stored procedures are **functions stored in the database** that you can call from PHP code.

### Current Situation

Right now, your PHP code uses **direct SQL queries** like this:

```php
// Direct SQL query (current approach)
$query = "INSERT INTO OrderTbl (customer_id, branch_id, total_amount, status) 
          VALUES (?, ?, ?, 'pending')";
$stmt = executeQuery($conn, $query, 'iiid', [$customer_id, $branch_id, $total_amount]);
```

### Alternative: Using Stored Procedures

You can call stored procedures from PHP instead:

```php
// Call stored procedure
$query = "CALL sp_CreateOrder(?, ?, ?, @order_id, @result_message)";
$stmt = executeQuery($conn, $query, 'iis', [$customer_id, $branch_id, $items_json]);

// Get output parameters
$result = $conn->query("SELECT @order_id, @result_message");
$row = $result->fetch_assoc();
$order_id = $row['@order_id'];
```

---

## 🎯 TWO APPROACHES

### Approach 1: Direct SQL (Current - Works Fine)
- PHP code contains all the SQL logic
- More flexible, easier to debug
- **This is what your current code does**

### Approach 2: Stored Procedures (Optional)
- Business logic stored in database
- Called from PHP with `CALL procedure_name()`
- More centralized, but less flexible

**Both approaches work!** Your current code (direct SQL) is perfectly fine.

---

## 📝 PRACTICAL EXAMPLE

### Scenario: Customer places an order

**What happens automatically (Triggers):**

1. **PHP inserts order**: `INSERT INTO OrderTbl ...`
   - ✅ `trg_OrderCreated` automatically creates OrderHistory entry

2. **PHP inserts order items**: `INSERT INTO OrderItem ...`
   - ✅ `trg_orderitem_subtotal` automatically calculates item price
   - ✅ `trg_update_order_total` automatically updates order total

3. **PHP updates order status**: `UPDATE OrderTbl SET status = 'completed'`
   - ✅ `trg_order_status_timestamp` automatically updates timestamp
   - ✅ `trg_loyalty_points_award` automatically adds loyalty points
   - ✅ `trg_order_status_stock` automatically manages stock

**You don't write code for any of this!** Triggers handle it automatically.

---

## 🛠️ HOW TO USE STORED PROCEDURES (Optional)

If you want to use stored procedures instead of direct SQL, here's how:

### Example: Using `sp_CreateOrder` instead of direct SQL

**Current code (Direct SQL):**
```php
$orderQuery = "INSERT INTO OrderTbl (customer_id, loyalty_id, branch_id, total_amount, status) 
               VALUES (?, ?, ?, ?, 'pending')";
```

**Using stored procedure:**
```php
// Convert items to JSON
$items_json = json_encode($items);

// Call stored procedure
$query = "CALL sp_CreateOrder(?, ?, ?, @order_id, @result_message)";
$stmt = executeQuery($conn, $query, 'iis', [
    $customer_id,
    $branch_id,
    $items_json
]);

// Get output parameters
$result = $conn->query("SELECT @order_id, @result_message");
$row = $result->fetch_assoc();
$order_id = $row['@order_id'];
$message = $row['@result_message'];
```

---

## ✅ SUMMARY

1. **Run SQL file in MySQL Workbench** → Creates triggers & procedures (one-time)
2. **Triggers work automatically** → No PHP code needed, they fire on database events
3. **Stored procedures are optional** → You can call them from PHP, or use direct SQL (current approach)
4. **Your current code works fine** → Direct SQL is a valid approach

---

## 🧪 TESTING TRIGGERS

To test if triggers are working:

1. **Test price validation trigger:**
   ```sql
   INSERT INTO Menu (category_id, name, price_amount, is_drink) 
   VALUES (1, 'Test Item', -10, 0);
   ```
   Should fail with error: "Menu price must be greater than zero"

2. **Test loyalty points trigger:**
   ```sql
   UPDATE OrderTbl SET status = 'completed' WHERE order_id = 1;
   ```
   Check if loyalty points were automatically added.

---

## 💡 RECOMMENDATION

**Keep your current approach (direct SQL)** - it's working fine and is easier to maintain.

**Triggers will still work automatically** - they don't need to be called from PHP.

**Stored procedures are optional** - use them if you want centralized business logic, but it's not required.

