-- =====================================================
-- FINALIZED TRIGGERS FOR CAFE DATABASE
-- Compatible with cafe_db.sql schema
-- =====================================================

USE cafe_db;

-- =====================================================
-- CREATE SYSTEMLOG TABLE (if it doesn't exist)
-- Required by triggers 4 and 8
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

-- Trigger 1: Prevents negative or zero prices when a menu item is added
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

-- Trigger 2: Automatically calculates each order item's subtotal (quantity × unit price + drink modifier)
-- FIXED: Now includes drink modifier in calculation
DELIMITER $$

DROP TRIGGER IF EXISTS trg_orderitem_subtotal$$

CREATE TRIGGER trg_orderitem_subtotal
BEFORE INSERT ON OrderItem
FOR EACH ROW
BEGIN
    DECLARE v_menu_price DECIMAL(10,2);
    DECLARE v_drink_modifier DECIMAL(10,2) DEFAULT 0;
    
    SELECT price_amount INTO v_menu_price
    FROM Menu
    WHERE menu_id = NEW.menu_id;
    
    IF NEW.drink_option_id IS NOT NULL THEN
        SELECT price_modifier INTO v_drink_modifier
        FROM DrinkOption
        WHERE drink_option_id = NEW.drink_option_id;
    END IF;
    
    SET NEW.price = (v_menu_price + COALESCE(v_drink_modifier, 0)) * NEW.quantity;
END$$

DELIMITER ;

-- Trigger 3: Updates the total order amount in OrderTbl whenever items are inserted
-- 3a. Update order total after item insert
DELIMITER $$

DROP TRIGGER IF EXISTS trg_update_order_total_insert$$

CREATE TRIGGER trg_update_order_total_insert
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

-- 3b. Update order total after item update
DELIMITER $$

DROP TRIGGER IF EXISTS trg_update_order_total_update$$

CREATE TRIGGER trg_update_order_total_update
AFTER UPDATE ON OrderItem
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

-- 3c. Update order total after item delete
DELIMITER $$

DROP TRIGGER IF EXISTS trg_update_order_total_delete$$

CREATE TRIGGER trg_update_order_total_delete
AFTER DELETE ON OrderItem
FOR EACH ROW
BEGIN
    UPDATE OrderTbl
    SET total_amount = (
        SELECT COALESCE(SUM(price), 0)
        FROM OrderItem
        WHERE order_id = OLD.order_id
    )
    WHERE order_id = OLD.order_id;
END$$

DELIMITER ;

-- Trigger 4: Records all payment activities into TransactionTbl with timestamps and method used
DELIMITER $$

DROP TRIGGER IF EXISTS trg_transaction_log$$

CREATE TRIGGER trg_transaction_log
AFTER INSERT ON TransactionTbl
FOR EACH ROW
BEGIN
    INSERT INTO SystemLog (action_type, description, timestamp)
    VALUES (
        'Payment',
        CONCAT('Payment recorded for Order ID ', NEW.order_id,
               ' via ', NEW.payment_method, ' - ₱', NEW.amount_paid * NEW.exchange_rate),
        NOW()
    );
END$$

DELIMITER ;

-- Trigger 5: Automatically adds loyalty points to a customer's LoyaltyCard when an order is completed
DELIMITER $$

DROP TRIGGER IF EXISTS trg_loyalty_points_award$$

CREATE TRIGGER trg_loyalty_points_award
BEFORE UPDATE ON OrderTbl
FOR EACH ROW
BEGIN
    IF NEW.status = 'completed'
       AND OLD.status <> 'completed'
       AND NEW.loyalty_id IS NOT NULL THEN
        UPDATE LoyaltyCard
        SET points = points + FLOOR(NEW.total_amount / 50),
            last_redeemed = CASE
                WHEN points + FLOOR(NEW.total_amount / 50) >= 100 THEN NOW()
                ELSE last_redeemed
            END
        WHERE loyalty_id = NEW.loyalty_id;
        
        SET NEW.earned_points = FLOOR(NEW.total_amount / 50);
    END IF;
END$$

DELIMITER ;

-- Trigger 6: Prevents customers from redeeming a reward if their points are below 100
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

-- Trigger 7: Ensures staff can only modify orders and menu data within their assigned branch
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
        
        IF emp_role <> 'admin'
           AND emp_branch IS NOT NULL
           AND emp_branch <> NEW.branch_id THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'You are not authorized to modify orders from another branch';
        END IF;
    END IF;
END$$

DELIMITER ;

-- Trigger 8: Logs all manager and admin actions (menu edits) into a SystemLog table
-- NOTE: Requires SystemLog table (defined in stored_procedures_triggers.sql)
DELIMITER $$

DROP TRIGGER IF EXISTS trg_admin_action_log$$

CREATE TRIGGER trg_admin_action_log
AFTER UPDATE ON Menu
FOR EACH ROW
BEGIN
    IF OLD.name <> NEW.name
       OR OLD.price_amount <> NEW.price_amount
       OR OLD.is_available <> NEW.is_available
       OR OLD.description <> NEW.description THEN
        INSERT INTO SystemLog (action_type, description, timestamp)
        VALUES (
            'Admin Action',
            CONCAT('Menu item "', NEW.name, '" (ID: ', NEW.menu_id, ') was updated.'),
            NOW()
        );
    END IF;
END$$

DELIMITER ;

-- Trigger 9: Prevents unauthorized updates to payment status once marked as "Completed"
-- NOTE: TransactionTbl.status ENUM is ('pending', 'completed') - no 'Paid' status
DELIMITER $$

DROP TRIGGER IF EXISTS trg_lock_completed_payment$$

CREATE TRIGGER trg_lock_completed_payment
BEFORE UPDATE ON TransactionTbl
FOR EACH ROW
BEGIN
    IF OLD.status = 'completed' AND NEW.status <> 'completed' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cannot modify a completed payment record';
    END IF;
END$$

DELIMITER ;

-- Trigger 10: Log every order status change into OrderHistory (audit + notifications)
DELIMITER $$

DROP TRIGGER IF EXISTS trg_order_status_history$$

CREATE TRIGGER trg_order_status_history
AFTER UPDATE ON OrderTbl
FOR EACH ROW
BEGIN
    IF NEW.status <> OLD.status THEN
        INSERT INTO OrderHistory (order_id, employee_id, status, remarks)
        VALUES (
            NEW.order_id,
            NEW.employee_id,
            NEW.status,
            CONCAT('Status changed from ', OLD.status, ' to ', NEW.status)
        );
    END IF;
END$$

DELIMITER ;

-- Trigger 11: Keep TransactionTbl.branch_id aligned with its OrderTbl branch
DELIMITER $$

DROP TRIGGER IF EXISTS trg_transaction_branch_alignment$$

CREATE TRIGGER trg_transaction_branch_alignment
BEFORE INSERT ON TransactionTbl
FOR EACH ROW
BEGIN
    DECLARE v_branch_id INT;
    DECLARE v_order_exists TINYINT DEFAULT 1;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_order_exists = 0;
    
    SELECT branch_id INTO v_branch_id
    FROM OrderTbl
    WHERE order_id = NEW.order_id;
    
    IF v_order_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Order does not exist for this transaction';
    ELSE
        IF v_branch_id IS NULL THEN
            IF NEW.branch_id IS NULL THEN
                SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Assign the order to a branch before recording a payment';
            END IF;
        ELSE
            IF NEW.branch_id IS NULL THEN
                SET NEW.branch_id = v_branch_id;
            ELSEIF NEW.branch_id <> v_branch_id THEN
                SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Transaction branch must match the order branch';
            END IF;
        END IF;
    END IF;
END$$

DELIMITER ;

-- Trigger 12: Ensure extras added to an order item are allowed for that menu item
DELIMITER $$

DROP TRIGGER IF EXISTS trg_validate_orderitem_extra$$

CREATE TRIGGER trg_validate_orderitem_extra
BEFORE INSERT ON OrderItemExtra
FOR EACH ROW
BEGIN
    DECLARE v_menu_id INT;
    DECLARE v_allowed INT DEFAULT 0;
    DECLARE v_item_exists TINYINT DEFAULT 1;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_item_exists = 0;
    
    SELECT menu_id INTO v_menu_id
    FROM OrderItem
    WHERE order_item_id = NEW.order_item_id;
    
    IF v_item_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Order item does not exist for the requested extra';
    ELSE
        SELECT COUNT(*)
        INTO v_allowed
        FROM MenuExtra
        WHERE menu_id = v_menu_id
          AND extra_id = NEW.extra_id;
        
        IF v_allowed = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Selected extra is not available for this menu item';
        END IF;
        
        IF NEW.quantity <= 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Extra quantity must be greater than zero';
        END IF;
    END IF;
END$$

DELIMITER ;

-- =====================================================
-- END OF TRIGGERS
-- =====================================================
