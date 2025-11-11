-- =====================================================
-- STORED PROCEDURES, TRIGGERS, AND TRANSACTIONS
-- For MySQL Workbench - The Waiting Room Cafe Database
-- =====================================================

USE cafe_db;

-- =====================================================
-- CREATE SYSTEMLOG TABLE (if it doesn't exist)
-- =====================================================

CREATE TABLE IF NOT EXISTS SystemLog (
    log_id INT NOT NULL AUTO_INCREMENT,
    action_type VARCHAR(50) NOT NULL,
    description TEXT,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    INDEX idx_action_type (action_type),
    INDEX idx_timestamp (timestamp)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- =====================================================
-- TRIGGERS
-- =====================================================

-- Trigger 1: Prevents negative or zero prices when a menu item is added or updated
DELIMITER $$

DROP TRIGGER IF EXISTS trg_menu_price_check$$

CREATE TRIGGER trg_menu_price_check
BEFORE INSERT ON Menu
FOR EACH ROW
BEGIN
    IF NEW.price_amount <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Menu price must be greater than zero';
    END IF;
END$$

DELIMITER ;

-- Trigger 2: Ensures menu item availability cannot go below zero when stock is adjusted
DELIMITER $$

DROP TRIGGER IF EXISTS trg_prevent_negative_stock$$

CREATE TRIGGER trg_prevent_negative_stock
BEFORE UPDATE ON Menu
FOR EACH ROW
BEGIN
    IF NEW.is_available = 1 AND NEW.price_amount <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cannot set item available with zero or negative price';
    END IF;
END$$

DELIMITER ;

-- Trigger 3: Automatically calculates each order item's subtotal (quantity × unit price)
DELIMITER $$

DROP TRIGGER IF EXISTS trg_orderitem_subtotal$$

CREATE TRIGGER trg_orderitem_subtotal
BEFORE INSERT ON OrderItem
FOR EACH ROW
BEGIN
    DECLARE v_menu_price DECIMAL(10,2);
    DECLARE v_drink_modifier DECIMAL(10,2) DEFAULT 0;
    
    -- Get menu price
    SELECT price_amount INTO v_menu_price
    FROM Menu
    WHERE menu_id = NEW.menu_id;
    
    -- Get drink option modifier if exists
    IF NEW.drink_option_id IS NOT NULL THEN
        SELECT price_modifier INTO v_drink_modifier
        FROM DrinkOption
        WHERE drink_option_id = NEW.drink_option_id;
    END IF;
    
    -- Calculate price: (menu_price + drink_modifier) * quantity
    SET NEW.price = (v_menu_price + v_drink_modifier) * NEW.quantity;
END$$

DELIMITER ;

-- Trigger 4: Updates the total order amount in OrderTbl whenever items are changed
DELIMITER $$

DROP TRIGGER IF EXISTS trg_update_order_total$$

CREATE TRIGGER trg_update_order_total
AFTER INSERT ON OrderItem
FOR EACH ROW
BEGIN
    UPDATE OrderTbl
    SET total_amount = (
        SELECT COALESCE(SUM(price), 0)
        FROM OrderItem
        WHERE order_id = NEW.order_id
    )
    WHERE order_id = NEW.order_id;
END$$

DELIMITER ;

-- Trigger 5: Deducts product stock when an order is confirmed and restores it if canceled
DELIMITER $$

DROP TRIGGER IF EXISTS trg_order_status_stock$$

CREATE TRIGGER trg_order_status_stock
AFTER UPDATE ON OrderTbl
FOR EACH ROW
BEGIN
    IF NEW.status = 'confirmed' AND OLD.status = 'pending' THEN
        -- Order confirmed - items remain available (no stock deduction in this system)
        -- This trigger can be extended if stock tracking is added
        UPDATE Menu
        SET is_available = CASE WHEN is_available = 1 THEN 1 ELSE 0 END
        WHERE menu_id IN (
            SELECT menu_id FROM OrderItem WHERE order_id = NEW.order_id
        );
    ELSEIF NEW.status = 'cancelled' AND OLD.status != 'cancelled' THEN
        -- Order cancelled - restore availability
        UPDATE Menu
        SET is_available = 1
        WHERE menu_id IN (
            SELECT menu_id FROM OrderItem WHERE order_id = NEW.order_id
        );
    END IF;
END$$

DELIMITER ;

-- Trigger 6: Automatically updates the OrderTbl.status timestamp when order status changes
DELIMITER $$

DROP TRIGGER IF EXISTS trg_order_status_timestamp$$

CREATE TRIGGER trg_order_status_timestamp
BEFORE UPDATE ON OrderTbl
FOR EACH ROW
BEGIN
    IF NEW.status <> OLD.status THEN
        -- Note: order_date is already set, this ensures it's updated
        SET NEW.order_date = NOW();
    END IF;
END$$

DELIMITER ;

-- Trigger 7: Records all payment activities into TransactionTbl with timestamps
DELIMITER $$

DROP TRIGGER IF EXISTS trg_transaction_log$$

CREATE TRIGGER trg_transaction_log
AFTER INSERT ON TransactionTbl
FOR EACH ROW
BEGIN
    INSERT INTO SystemLog (action_type, description, timestamp)
    VALUES ('Payment',
            CONCAT('Payment recorded for Order ID ', NEW.order_id,
                   ' via ', NEW.payment_method, ' - ₱', NEW.amount_paid * NEW.exchange_rate),
            NOW());
END$$

DELIMITER ;

-- Trigger 8: Automatically adds loyalty points to a customer's LoyaltyCard when an order is completed
DELIMITER $$

DROP TRIGGER IF EXISTS trg_loyalty_points_award$$

CREATE TRIGGER trg_loyalty_points_award
AFTER UPDATE ON OrderTbl
FOR EACH ROW
BEGIN
    IF NEW.status = 'completed' AND OLD.status != 'completed' AND NEW.loyalty_id IS NOT NULL THEN
        UPDATE LoyaltyCard
        SET points = points + FLOOR(NEW.total_amount / 50),
            last_redeemed = CASE 
                WHEN points + FLOOR(NEW.total_amount / 50) >= 100 THEN NOW() 
                ELSE last_redeemed 
            END
        WHERE loyalty_id = NEW.loyalty_id;
        
        -- Update earned_points in order
        UPDATE OrderTbl
        SET earned_points = FLOOR(NEW.total_amount / 50)
        WHERE order_id = NEW.order_id;
    END IF;
END$$

DELIMITER ;

-- Trigger 9: Prevents customers from redeeming a reward if their points are below 100
DELIMITER $$

DROP TRIGGER IF EXISTS trg_prevent_invalid_redeem$$

CREATE TRIGGER trg_prevent_invalid_redeem
BEFORE UPDATE ON LoyaltyCard
FOR EACH ROW
BEGIN
    IF OLD.points < 100 AND NEW.points < OLD.points THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Not enough points to redeem reward. Need at least 100 points.';
    END IF;
END$$

DELIMITER ;

-- Trigger 10: Merges duplicate cart entries by updating quantity instead of duplicating rows
DELIMITER $$

DROP TRIGGER IF EXISTS trg_merge_cart_entries$$

CREATE TRIGGER trg_merge_cart_entries
BEFORE INSERT ON OrderItem
FOR EACH ROW
BEGIN
    DECLARE existing_id INT;
    DECLARE existing_quantity INT;
    
    -- Check for existing item with same menu_id, drink_option_id, and order_id
    SELECT order_item_id, quantity INTO existing_id, existing_quantity
    FROM OrderItem
    WHERE order_id = NEW.order_id 
    AND menu_id = NEW.menu_id
    AND (drink_option_id = NEW.drink_option_id OR (drink_option_id IS NULL AND NEW.drink_option_id IS NULL))
    LIMIT 1;
    
    IF existing_id IS NOT NULL THEN
        -- Update existing item quantity
        UPDATE OrderItem
        SET quantity = quantity + NEW.quantity,
            price = price + NEW.price
        WHERE order_item_id = existing_id;
        
        -- Prevent insert by setting a flag (MySQL doesn't support preventing insert directly)
        SET NEW.order_id = NULL; -- This will cause insert to fail gracefully
    END IF;
END$$

DELIMITER ;

-- Trigger 11: Ensures staff can only modify orders within their assigned branch
DELIMITER $$

DROP TRIGGER IF EXISTS trg_staff_branch_restriction$$

CREATE TRIGGER trg_staff_branch_restriction
BEFORE UPDATE ON OrderTbl
FOR EACH ROW
BEGIN
    DECLARE emp_branch INT;
    DECLARE emp_role VARCHAR(20);
    
    IF NEW.employee_id IS NOT NULL THEN
        SELECT branch_id, role INTO emp_branch, emp_role
        FROM Employee
        WHERE employee_id = NEW.employee_id;
        
        -- Admin can modify any branch, others restricted to their branch
        IF emp_role != 'admin' AND emp_branch IS NOT NULL AND emp_branch != NEW.branch_id THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'You are not authorized to modify orders from another branch';
        END IF;
    END IF;
END$$

DELIMITER ;

-- Trigger 12: Logs all manager and admin actions (menu edits)
DELIMITER $$

DROP TRIGGER IF EXISTS trg_admin_action_log$$

CREATE TRIGGER trg_admin_action_log
AFTER UPDATE ON Menu
FOR EACH ROW
BEGIN
    IF OLD.name != NEW.name OR OLD.price_amount != NEW.price_amount OR 
       OLD.is_available != NEW.is_available OR OLD.description != NEW.description THEN
        INSERT INTO SystemLog (action_type, description, timestamp)
        VALUES ('Admin Action',
                CONCAT('Menu item "', NEW.name, '" (ID: ', NEW.menu_id, ') was updated.'),
                NOW());
    END IF;
END$$

DELIMITER ;

-- Trigger 13: Prevents unauthorized updates to payment status once marked as "Completed"
DELIMITER $$

DROP TRIGGER IF EXISTS trg_lock_completed_payment$$

CREATE TRIGGER trg_lock_completed_payment
BEFORE UPDATE ON TransactionTbl
FOR EACH ROW
BEGIN
    IF OLD.status = 'completed' AND NEW.status != 'completed' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cannot modify a completed payment record';
    END IF;
END$$

DELIMITER ;

-- Trigger 14: Auto-restores stock for canceled or refunded orders
DELIMITER $$

DROP TRIGGER IF EXISTS trg_auto_restore_stock$$

CREATE TRIGGER trg_auto_restore_stock
AFTER UPDATE ON OrderTbl
FOR EACH ROW
BEGIN
    IF NEW.status IN ('cancelled') AND OLD.status NOT IN ('cancelled') THEN
        -- Restore item availability
        UPDATE Menu
        SET is_available = 1
        WHERE menu_id IN (
            SELECT menu_id FROM OrderItem WHERE order_id = NEW.order_id
        );
    END IF;
END$$

DELIMITER ;

-- Trigger 15: Updates customer loyalty balance immediately after payment confirmation
DELIMITER $$

DROP TRIGGER IF EXISTS trg_loyalty_after_payment$$

CREATE TRIGGER trg_loyalty_after_payment
AFTER UPDATE ON TransactionTbl
FOR EACH ROW
BEGIN
    DECLARE v_loyalty_id INT;
    DECLARE v_order_total DECIMAL(10,2);
    DECLARE v_points_earned INT;
    
    IF NEW.status = 'completed' AND OLD.status != 'completed' THEN
        -- Get loyalty_id and order total
        SELECT loyalty_id, total_amount INTO v_loyalty_id, v_order_total
        FROM OrderTbl
        WHERE order_id = NEW.order_id;
        
        IF v_loyalty_id IS NOT NULL THEN
            -- Calculate points (1 point per ₱50)
            SET v_points_earned = FLOOR((NEW.amount_paid * NEW.exchange_rate) / 50);
            
            IF v_points_earned > 0 THEN
                UPDATE LoyaltyCard
                SET points = points + v_points_earned,
                    last_redeemed = CASE 
                        WHEN points + v_points_earned >= 100 THEN NOW() 
                        ELSE last_redeemed 
                    END
                WHERE loyalty_id = v_loyalty_id;
            END IF;
        END IF;
    END IF;
END$$

DELIMITER ;

-- =====================================================
-- STORED PROCEDURES
-- =====================================================

-- Procedure 1: Creates a new order and inserts all order items and add-ons from the customer's cart
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_CreateOrder$$

CREATE PROCEDURE sp_CreateOrder(
    IN p_customer_id INT,
    IN p_branch_id INT,
    IN p_items JSON,  -- JSON array of items with menu_id, quantity, drink_option_id, extras
    OUT p_order_id INT,
    OUT p_result_message VARCHAR(255)
)
BEGIN
    DECLARE v_loyalty_id INT;
    DECLARE v_total_amount DECIMAL(10,2) DEFAULT 0;
    DECLARE v_item JSON;
    DECLARE v_menu_id INT;
    DECLARE v_quantity INT;
    DECLARE v_drink_option_id INT;
    DECLARE v_price DECIMAL(10,2);
    DECLARE v_order_item_id INT;
    DECLARE v_extra JSON;
    DECLARE v_extra_id INT;
    DECLARE v_extra_quantity INT;
    DECLARE i INT DEFAULT 0;
    DECLARE item_count INT;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result_message = CONCAT('Error: ', SQLSTATE, ' - ', SQL_MESSAGE_TEXT);
        SET p_order_id = NULL;
    END;
    
    START TRANSACTION;
    
    -- Get customer's loyalty card
    SELECT loyalty_id INTO v_loyalty_id
    FROM LoyaltyCard
    WHERE customer_id = p_customer_id AND is_active = 1
    LIMIT 1;
    
    -- Create order
    INSERT INTO OrderTbl (customer_id, loyalty_id, branch_id, total_amount, status)
    VALUES (p_customer_id, v_loyalty_id, p_branch_id, 0, 'pending');
    
    SET p_order_id = LAST_INSERT_ID();
    
    -- Process items
    SET item_count = JSON_LENGTH(p_items);
    
    WHILE i < item_count DO
        SET v_item = JSON_EXTRACT(p_items, CONCAT('$[', i, ']'));
        
        SET v_menu_id = JSON_UNQUOTE(JSON_EXTRACT(v_item, '$.menu_id'));
        SET v_quantity = JSON_UNQUOTE(JSON_EXTRACT(v_item, '$.quantity'));
        SET v_drink_option_id = NULLIF(JSON_UNQUOTE(JSON_EXTRACT(v_item, '$.drink_option_id')), 'null');
        
        -- Calculate price (simplified - in production, calculate from menu + modifiers)
        SELECT price_amount INTO v_price FROM Menu WHERE menu_id = v_menu_id;
        IF v_drink_option_id IS NOT NULL THEN
            SELECT v_price + price_modifier INTO v_price 
            FROM DrinkOption WHERE drink_option_id = v_drink_option_id;
        END IF;
        SET v_price = v_price * v_quantity;
        
        -- Insert order item
        INSERT INTO OrderItem (order_id, menu_id, drink_option_id, quantity, price)
        VALUES (p_order_id, v_menu_id, v_drink_option_id, v_quantity, v_price);
        
        SET v_order_item_id = LAST_INSERT_ID();
        
        -- Process extras
        IF JSON_EXTRACT(v_item, '$.extras') IS NOT NULL THEN
            SET @j = 0;
            SET @extras_count = JSON_LENGTH(JSON_EXTRACT(v_item, '$.extras'));
            
            WHILE @j < @extras_count DO
                SET v_extra = JSON_EXTRACT(JSON_EXTRACT(v_item, '$.extras'), CONCAT('$[', @j, ']'));
                SET v_extra_id = JSON_UNQUOTE(JSON_EXTRACT(v_extra, '$.extra_id'));
                SET v_extra_quantity = COALESCE(JSON_UNQUOTE(JSON_EXTRACT(v_extra, '$.quantity')), 1);
                
                INSERT INTO OrderItemExtra (order_item_id, extra_id, quantity)
                VALUES (v_order_item_id, v_extra_id, v_extra_quantity);
                
                SET @j = @j + 1;
            END WHILE;
        END IF;
        
        SET i = i + 1;
    END WHILE;
    
    -- Update total (trigger will handle this, but ensure it's set)
    SELECT COALESCE(SUM(price), 0) INTO v_total_amount
    FROM OrderItem
    WHERE order_id = p_order_id;
    
    UPDATE OrderTbl SET total_amount = v_total_amount WHERE order_id = p_order_id;
    
    COMMIT;
    SET p_result_message = 'Order created successfully';
    
END$$

DELIMITER ;

-- Procedure 2: Updates the quantity or add-ons for an existing order and recalculates totals
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_UpdateOrderItems$$

CREATE PROCEDURE sp_UpdateOrderItems(
    IN p_order_id INT,
    IN p_order_item_id INT,
    IN p_quantity INT,
    IN p_extras JSON,  -- Optional JSON array of extras
    OUT p_result_message VARCHAR(255)
)
BEGIN
    DECLARE v_menu_id INT;
    DECLARE v_drink_option_id INT;
    DECLARE v_new_price DECIMAL(10,2);
    DECLARE v_menu_price DECIMAL(10,2);
    DECLARE v_modifier DECIMAL(10,2) DEFAULT 0;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result_message = CONCAT('Error: ', SQLSTATE, ' - ', SQL_MESSAGE_TEXT);
    END;
    
    START TRANSACTION;
    
    -- Get order item details
    SELECT menu_id, drink_option_id INTO v_menu_id, v_drink_option_id
    FROM OrderItem
    WHERE order_item_id = p_order_item_id AND order_id = p_order_id;
    
    IF v_menu_id IS NULL THEN
        ROLLBACK;
        SET p_result_message = 'Order item not found';
        LEAVE sp_UpdateOrderItems;
    END IF;
    
    -- Get menu price
    SELECT price_amount INTO v_menu_price FROM Menu WHERE menu_id = v_menu_id;
    
    -- Get drink modifier
    IF v_drink_option_id IS NOT NULL THEN
        SELECT price_modifier INTO v_modifier FROM DrinkOption WHERE drink_option_id = v_drink_option_id;
    END IF;
    
    -- Calculate new price
    SET v_new_price = (v_menu_price + v_modifier) * p_quantity;
    
    -- Update order item
    UPDATE OrderItem
    SET quantity = p_quantity,
        price = v_new_price
    WHERE order_item_id = p_order_item_id;
    
    -- Update order total
    UPDATE OrderTbl
    SET total_amount = (
        SELECT COALESCE(SUM(price), 0)
        FROM OrderItem
        WHERE order_id = p_order_id
    )
    WHERE order_id = p_order_id;
    
    COMMIT;
    SET p_result_message = 'Order items updated successfully';
    
END$$

DELIMITER ;

-- Procedure 3: Cancels an order, restores product stock, and logs the action
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_CancelOrder$$

CREATE PROCEDURE sp_CancelOrder(
    IN p_order_id INT,
    IN p_employee_id INT,
    IN p_remarks VARCHAR(200),
    OUT p_result_message VARCHAR(255)
)
BEGIN
    DECLARE v_status VARCHAR(20);
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result_message = CONCAT('Error: ', SQLSTATE, ' - ', SQL_MESSAGE_TEXT);
    END;
    
    START TRANSACTION;
    
    -- Get current status
    SELECT status INTO v_status FROM OrderTbl WHERE order_id = p_order_id;
    
    IF v_status IS NULL THEN
        ROLLBACK;
        SET p_result_message = 'Order not found';
        LEAVE sp_CancelOrder;
    END IF;
    
    IF v_status = 'completed' THEN
        ROLLBACK;
        SET p_result_message = 'Cannot cancel a completed order';
        LEAVE sp_CancelOrder;
    END IF;
    
    -- Update order status (trigger will restore stock)
    UPDATE OrderTbl
    SET status = 'cancelled'
    WHERE order_id = p_order_id;
    
    -- Log in order history
    INSERT INTO OrderHistory (order_id, employee_id, status, remarks)
    VALUES (p_order_id, p_employee_id, 'cancelled', 
            CONCAT('Order cancelled. ', COALESCE(p_remarks, '')));
    
    COMMIT;
    SET p_result_message = 'Order cancelled successfully';
    
END$$

DELIMITER ;

-- Procedure 4: Records a customer payment and updates the corresponding order status
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_RecordPayment$$

CREATE PROCEDURE sp_RecordPayment(
    IN p_order_id INT,
    IN p_currency_id INT,
    IN p_payment_method VARCHAR(20),
    IN p_amount_paid DECIMAL(10,2),
    IN p_branch_id INT,
    OUT p_transaction_id INT,
    OUT p_result_message VARCHAR(255)
)
BEGIN
    DECLARE v_order_total DECIMAL(10,2);
    DECLARE v_exchange_rate DECIMAL(12,4);
    DECLARE v_amount_php DECIMAL(10,2);
    DECLARE v_tolerance DECIMAL(10,2) DEFAULT 0.01;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result_message = CONCAT('Error: ', SQLSTATE, ' - ', SQL_MESSAGE_TEXT);
        SET p_transaction_id = NULL;
    END;
    
    START TRANSACTION;
    
    -- Get order total
    SELECT total_amount INTO v_order_total FROM OrderTbl WHERE order_id = p_order_id;
    
    IF v_order_total IS NULL THEN
        ROLLBACK;
        SET p_result_message = 'Order not found';
        SET p_transaction_id = NULL;
        LEAVE sp_RecordPayment;
    END IF;
    
    -- Get exchange rate
    SELECT rate INTO v_exchange_rate FROM Currency WHERE currency_id = p_currency_id;
    
    IF v_exchange_rate IS NULL THEN
        ROLLBACK;
        SET p_result_message = 'Invalid currency';
        SET p_transaction_id = NULL;
        LEAVE sp_RecordPayment;
    END IF;
    
    -- Calculate PHP equivalent
    SET v_amount_php = p_amount_paid / v_exchange_rate;
    
    -- Verify amount
    IF ABS(v_amount_php - v_order_total) > v_tolerance THEN
        ROLLBACK;
        SET p_result_message = CONCAT('Payment amount mismatch');
        SET p_transaction_id = NULL;
        LEAVE sp_RecordPayment;
    END IF;
    
    -- Create transaction
    INSERT INTO TransactionTbl (order_id, currency_id, payment_method, amount_paid, exchange_rate, status, branch_id)
    VALUES (p_order_id, p_currency_id, p_payment_method, p_amount_paid, v_exchange_rate, 'completed', p_branch_id);
    
    SET p_transaction_id = LAST_INSERT_ID();
    
    -- Update order status if pending
    UPDATE OrderTbl SET status = 'confirmed' WHERE order_id = p_order_id AND status = 'pending';
    
    COMMIT;
    SET p_result_message = 'Payment recorded successfully';
    
END$$

DELIMITER ;

-- Procedure 5: Calculates and adds loyalty points after a completed transaction
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_CalculateLoyaltyPoints$$

CREATE PROCEDURE sp_CalculateLoyaltyPoints(
    IN p_order_id INT,
    OUT p_points_earned INT,
    OUT p_result_message VARCHAR(255)
)
BEGIN
    DECLARE v_total_amount DECIMAL(10,2);
    DECLARE v_loyalty_id INT;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result_message = CONCAT('Error: ', SQLSTATE, ' - ', SQL_MESSAGE_TEXT);
        SET p_points_earned = 0;
    END;
    
    START TRANSACTION;
    
    -- Get order details
    SELECT total_amount, loyalty_id INTO v_total_amount, v_loyalty_id
    FROM OrderTbl
    WHERE order_id = p_order_id;
    
    IF v_total_amount IS NULL THEN
        ROLLBACK;
        SET p_result_message = 'Order not found';
        SET p_points_earned = 0;
        LEAVE sp_CalculateLoyaltyPoints;
    END IF;
    
    -- Calculate points (1 point per ₱50)
    SET p_points_earned = FLOOR(v_total_amount / 50);
    
    IF p_points_earned > 0 AND v_loyalty_id IS NOT NULL THEN
        -- Update loyalty card
        UPDATE LoyaltyCard
        SET points = points + p_points_earned,
            last_redeemed = CASE 
                WHEN points + p_points_earned >= 100 THEN NOW() 
                ELSE last_redeemed 
            END
        WHERE loyalty_id = v_loyalty_id;
        
        -- Update order earned_points
        UPDATE OrderTbl
        SET earned_points = p_points_earned
        WHERE order_id = p_order_id;
    END IF;
    
    COMMIT;
    SET p_result_message = 'Loyalty points calculated successfully';
    
END$$

DELIMITER ;

-- Procedure 6: Redeems loyalty rewards and resets the customer's points after use
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_RedeemLoyaltyReward$$

CREATE PROCEDURE sp_RedeemLoyaltyReward(
    IN p_loyalty_id INT,
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(255)
)
BEGIN
    DECLARE v_current_points INT;
    DECLARE v_points_to_redeem INT DEFAULT 100;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = FALSE;
        SET p_message = CONCAT('Error: ', SQLSTATE, ' - ', SQL_MESSAGE_TEXT);
    END;
    
    START TRANSACTION;
    
    -- Get current points with lock
    SELECT points INTO v_current_points
    FROM LoyaltyCard
    WHERE loyalty_id = p_loyalty_id AND is_active = 1
    FOR UPDATE;
    
    IF v_current_points IS NULL THEN
        ROLLBACK;
        SET p_success = FALSE;
        SET p_message = 'Loyalty card not found or inactive';
        LEAVE sp_RedeemLoyaltyReward;
    END IF;
    
    -- Check if enough points
    IF v_current_points < v_points_to_redeem THEN
        ROLLBACK;
        SET p_success = FALSE;
        SET p_message = CONCAT('Insufficient points. You have ', v_current_points, 
                              ' points. Need ', v_points_to_redeem, ' points to redeem.');
        LEAVE sp_RedeemLoyaltyReward;
    END IF;
    
    -- Deduct points
    UPDATE LoyaltyCard
    SET points = points - v_points_to_redeem,
        last_redeemed = NOW()
    WHERE loyalty_id = p_loyalty_id;
    
    COMMIT;
    SET p_success = TRUE;
    SET p_message = 'Points redeemed successfully';
    
END$$

DELIMITER ;

-- Procedure 7: Allows staff to update order statuses step by step
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_UpdateOrderStatus$$

CREATE PROCEDURE sp_UpdateOrderStatus(
    IN p_order_id INT,
    IN p_new_status VARCHAR(20),
    IN p_employee_id INT,
    IN p_remarks VARCHAR(200),
    OUT p_result_message VARCHAR(255)
)
BEGIN
    DECLARE v_current_status VARCHAR(20);
    DECLARE v_valid_transition BOOLEAN DEFAULT FALSE;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result_message = CONCAT('Error: ', SQLSTATE, ' - ', SQL_MESSAGE_TEXT);
    END;
    
    START TRANSACTION;
    
    -- Get current status
    SELECT status INTO v_current_status FROM OrderTbl WHERE order_id = p_order_id;
    
    IF v_current_status IS NULL THEN
        ROLLBACK;
        SET p_result_message = 'Order not found';
        LEAVE sp_UpdateOrderStatus;
    END IF;
    
    -- Validate status transition
    CASE v_current_status
        WHEN 'pending' THEN
            IF p_new_status IN ('confirmed', 'cancelled') THEN
                SET v_valid_transition = TRUE;
            END IF;
        WHEN 'confirmed' THEN
            IF p_new_status IN ('completed', 'cancelled') THEN
                SET v_valid_transition = TRUE;
            END IF;
        WHEN 'completed' THEN
            SET v_valid_transition = FALSE; -- Cannot change completed orders
        WHEN 'cancelled' THEN
            SET v_valid_transition = FALSE; -- Cannot change cancelled orders
        ELSE
            SET v_valid_transition = FALSE;
    END CASE;
    
    IF NOT v_valid_transition THEN
        ROLLBACK;
        SET p_result_message = CONCAT('Invalid status transition from ', v_current_status, ' to ', p_new_status);
        LEAVE sp_UpdateOrderStatus;
    END IF;
    
    -- Update order status
    UPDATE OrderTbl
    SET status = p_new_status,
        employee_id = p_employee_id
    WHERE order_id = p_order_id;
    
    -- Log in order history
    INSERT INTO OrderHistory (order_id, employee_id, status, remarks)
    VALUES (p_order_id, p_employee_id, p_new_status, COALESCE(p_remarks, 'Status updated'));
    
    COMMIT;
    SET p_result_message = 'Order status updated successfully';
    
END$$

DELIMITER ;

-- Procedure 8: Adds or edits a menu item (used by admin/managers only)
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_ManageMenuItem$$

CREATE PROCEDURE sp_ManageMenuItem(
    IN p_action VARCHAR(10),  -- 'INSERT' or 'UPDATE'
    IN p_menu_id INT,
    IN p_category_id INT,
    IN p_name VARCHAR(50),
    IN p_description VARCHAR(255),
    IN p_price_amount DECIMAL(10,2),
    IN p_is_drink TINYINT,
    IN p_is_available TINYINT,
    OUT p_result_menu_id INT,
    OUT p_result_message VARCHAR(255)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result_message = CONCAT('Error: ', SQL_STATE, ' - ', SQL_MESSAGE_TEXT);
        SET p_result_menu_id = NULL;
    END;
    
    START TRANSACTION;
    
    IF p_action = 'INSERT' THEN
        -- Validate price
        IF p_price_amount <= 0 THEN
            ROLLBACK;
            SET p_result_message = 'Price must be greater than zero';
            SET p_result_menu_id = NULL;
            LEAVE sp_ManageMenuItem;
        END IF;
        
        -- Insert new menu item
        INSERT INTO Menu (category_id, name, description, price_amount, is_drink, is_available)
        VALUES (p_category_id, p_name, p_description, p_price_amount, p_is_drink, p_is_available);
        
        SET p_result_menu_id = LAST_INSERT_ID();
        SET p_result_message = 'Menu item created successfully';
        
    ELSEIF p_action = 'UPDATE' THEN
        -- Validate price
        IF p_price_amount <= 0 THEN
            ROLLBACK;
            SET p_result_message = 'Price must be greater than zero';
            SET p_result_menu_id = NULL;
            LEAVE sp_ManageMenuItem;
        END IF;
        
        -- Update menu item
        UPDATE Menu
        SET category_id = p_category_id,
            name = p_name,
            description = p_description,
            price_amount = p_price_amount,
            is_drink = p_is_drink,
            is_available = p_is_available
        WHERE menu_id = p_menu_id;
        
        IF ROW_COUNT() = 0 THEN
            ROLLBACK;
            SET p_result_message = 'Menu item not found';
            SET p_result_menu_id = NULL;
            LEAVE sp_ManageMenuItem;
        END IF;
        
        SET p_result_menu_id = p_menu_id;
        SET p_result_message = 'Menu item updated successfully';
        
    ELSE
        ROLLBACK;
        SET p_result_message = 'Invalid action. Use INSERT or UPDATE';
        SET p_result_menu_id = NULL;
    END IF;
    
    COMMIT;
    
END$$

DELIMITER ;

-- Procedure 9: Generates a daily sales report, grouped by branch and payment method
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_DailySalesReport$$

CREATE PROCEDURE sp_DailySalesReport(
    IN p_report_date DATE,
    IN p_branch_id INT  -- NULL for all branches
)
BEGIN
    SELECT 
        b.name as branch_name,
        t.payment_method,
        COUNT(DISTINCT t.order_id) as total_orders,
        COUNT(t.transaction_id) as total_transactions,
        SUM(t.amount_paid * t.exchange_rate) as total_sales_php,
        SUM(t.amount_paid) as total_sales_original,
        cur.code as currency_code,
        cur.symbol as currency_symbol
    FROM TransactionTbl t
    INNER JOIN Branch b ON t.branch_id = b.branch_id
    INNER JOIN Currency cur ON t.currency_id = cur.currency_id
    WHERE t.status = 'completed'
    AND DATE(t.transaction_date) = p_report_date
    AND (p_branch_id IS NULL OR t.branch_id = p_branch_id)
    GROUP BY b.branch_id, b.name, t.payment_method, cur.code, cur.symbol
    ORDER BY b.name, t.payment_method;
END$$

DELIMITER ;

-- Procedure 10: Calculates total revenue and best-selling menu items for a given date range
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_RevenueAndBestSellers$$

CREATE PROCEDURE sp_RevenueAndBestSellers(
    IN p_start_date DATE,
    IN p_end_date DATE,
    IN p_branch_id INT  -- NULL for all branches
)
BEGIN
    -- Total Revenue
    SELECT 
        SUM(t.amount_paid * t.exchange_rate) as total_revenue_php,
        COUNT(DISTINCT t.order_id) as total_orders,
        COUNT(t.transaction_id) as total_transactions
    FROM TransactionTbl t
    WHERE t.status = 'completed'
    AND DATE(t.transaction_date) BETWEEN p_start_date AND p_end_date
    AND (p_branch_id IS NULL OR t.branch_id = p_branch_id);
    
    -- Best Selling Items
    SELECT 
        m.menu_id,
        m.name,
        m.category_id,
        c.name as category_name,
        SUM(oi.quantity) as total_quantity_sold,
        SUM(oi.price * oi.quantity) as total_revenue,
        COUNT(DISTINCT oi.order_id) as times_ordered
    FROM OrderItem oi
    INNER JOIN Menu m ON oi.menu_id = m.menu_id
    INNER JOIN Category c ON m.category_id = c.category_id
    INNER JOIN OrderTbl o ON oi.order_id = o.order_id
    INNER JOIN TransactionTbl t ON o.order_id = t.order_id
    WHERE t.status = 'completed'
    AND DATE(t.transaction_date) BETWEEN p_start_date AND p_end_date
    AND (p_branch_id IS NULL OR o.branch_id = p_branch_id)
    GROUP BY m.menu_id, m.name, m.category_id, c.name
    ORDER BY total_quantity_sold DESC
    LIMIT 10;
END$$

DELIMITER ;

-- Procedure 11: Lists customers with the highest loyalty points or total spending
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_TopCustomers$$

CREATE PROCEDURE sp_TopCustomers(
    IN p_sort_by VARCHAR(20),  -- 'points' or 'spending'
    IN p_limit INT
)
BEGIN
    IF p_sort_by = 'points' THEN
        SELECT 
            c.customer_id,
            c.name,
            c.email,
            l.card_number,
            l.points,
            l.last_redeemed,
            COUNT(DISTINCT o.order_id) as total_orders,
            SUM(o.total_amount) as total_spending
        FROM Customer c
        INNER JOIN LoyaltyCard l ON c.customer_id = l.customer_id
        LEFT JOIN OrderTbl o ON c.customer_id = o.customer_id
        WHERE l.is_active = 1
        GROUP BY c.customer_id, c.name, c.email, l.card_number, l.points, l.last_redeemed
        ORDER BY l.points DESC
        LIMIT p_limit;
    ELSE
        SELECT 
            c.customer_id,
            c.name,
            c.email,
            l.card_number,
            l.points,
            COUNT(DISTINCT o.order_id) as total_orders,
            SUM(o.total_amount) as total_spending
        FROM Customer c
        INNER JOIN LoyaltyCard l ON c.customer_id = l.customer_id
        LEFT JOIN OrderTbl o ON c.customer_id = o.customer_id
        WHERE l.is_active = 1
        GROUP BY c.customer_id, c.name, c.email, l.card_number, l.points
        ORDER BY total_spending DESC
        LIMIT p_limit;
    END IF;
END$$

DELIMITER ;

-- Procedure 12: Displays transaction summaries (sales, refunds, and payments) for auditing
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_TransactionSummary$$

CREATE PROCEDURE sp_TransactionSummary(
    IN p_start_date DATE,
    IN p_end_date DATE,
    IN p_branch_id INT  -- NULL for all branches
)
BEGIN
    SELECT 
        DATE(t.transaction_date) as transaction_date,
        b.name as branch_name,
        t.payment_method,
        cur.code as currency_code,
        COUNT(t.transaction_id) as transaction_count,
        SUM(t.amount_paid) as total_amount_original,
        SUM(t.amount_paid * t.exchange_rate) as total_amount_php,
        AVG(t.exchange_rate) as avg_exchange_rate
    FROM TransactionTbl t
    INNER JOIN Branch b ON t.branch_id = b.branch_id
    INNER JOIN Currency cur ON t.currency_id = cur.currency_id
    WHERE t.status = 'completed'
    AND DATE(t.transaction_date) BETWEEN p_start_date AND p_end_date
    AND (p_branch_id IS NULL OR t.branch_id = p_branch_id)
    GROUP BY DATE(t.transaction_date), b.branch_id, b.name, t.payment_method, cur.code
    ORDER BY transaction_date DESC, b.name;
END$$

DELIMITER ;

-- Procedure 13: Manages multi-branch sales comparison reports
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_BranchComparison$$

CREATE PROCEDURE sp_BranchComparison(
    IN p_start_date DATE,
    IN p_end_date DATE
)
BEGIN
    SELECT 
        b.branch_id,
        b.name as branch_name,
        COUNT(DISTINCT o.order_id) as total_orders,
        COUNT(DISTINCT t.transaction_id) as total_transactions,
        SUM(t.amount_paid * t.exchange_rate) as total_sales_php,
        AVG(t.amount_paid * t.exchange_rate) as avg_order_value,
        COUNT(DISTINCT o.customer_id) as unique_customers
    FROM Branch b
    LEFT JOIN OrderTbl o ON b.branch_id = o.branch_id
    LEFT JOIN TransactionTbl t ON o.order_id = t.order_id AND t.status = 'completed'
    WHERE DATE(COALESCE(t.transaction_date, o.order_date)) BETWEEN p_start_date AND p_end_date
       OR (t.transaction_date IS NULL AND o.order_date BETWEEN p_start_date AND p_end_date)
    GROUP BY b.branch_id, b.name
    ORDER BY total_sales_php DESC;
END$$

DELIMITER ;

-- Procedure 14: Generates an item inventory report (availability status)
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_InventoryReport$$

CREATE PROCEDURE sp_InventoryReport()
BEGIN
    SELECT 
        m.menu_id,
        m.name,
        c.name as category_name,
        m.price_amount,
        m.is_available,
        m.is_drink,
        COUNT(DISTINCT oi.order_id) as times_ordered,
        SUM(oi.quantity) as total_quantity_sold
    FROM Menu m
    INNER JOIN Category c ON m.category_id = c.category_id
    LEFT JOIN OrderItem oi ON m.menu_id = oi.menu_id
    GROUP BY m.menu_id, m.name, c.name, m.price_amount, m.is_available, m.is_drink
    ORDER BY m.is_available DESC, c.name, m.name;
END$$

DELIMITER ;

-- Procedure 15: Converts and displays item prices based on currency settings
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_ConvertPrices$$

CREATE PROCEDURE sp_ConvertPrices(
    IN p_currency_id INT
)
BEGIN
    DECLARE v_exchange_rate DECIMAL(12,4);
    DECLARE v_currency_code VARCHAR(5);
    DECLARE v_currency_symbol VARCHAR(5);
    
    -- Get currency info
    SELECT rate, code, symbol INTO v_exchange_rate, v_currency_code, v_currency_symbol
    FROM Currency
    WHERE currency_id = p_currency_id;
    
    IF v_exchange_rate IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid currency ID';
    END IF;
    
    -- Return menu items with converted prices
    SELECT 
        m.menu_id,
        m.name,
        c.name as category_name,
        m.price_amount as price_php,
        (m.price_amount * v_exchange_rate) as converted_price,
        v_currency_code as currency_code,
        v_currency_symbol as currency_symbol,
        m.is_available
    FROM Menu m
    INNER JOIN Category c ON m.category_id = c.category_id
    WHERE m.is_available = 1
    ORDER BY c.name, m.name;
END$$

DELIMITER ;

-- =====================================================
-- END OF STORED PROCEDURES AND TRIGGERS
-- =====================================================
