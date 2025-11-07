-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema cafe_db
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema cafe_db
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `cafe_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ;
USE `cafe_db` ;

-- -----------------------------------------------------
-- Table `cafe_db`.`Employee`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`Employee` (
  `employee_id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  `role` ENUM('staff', 'manager', 'admin') NOT NULL DEFAULT 'staff',
  `contact_num` VARCHAR(15) NULL DEFAULT NULL,
  `branch_id` INT NULL DEFAULT NULL,
  PRIMARY KEY (`employee_id`),
  INDEX `branch_id` (`branch_id` ASC) VISIBLE,
  CONSTRAINT `Employee_ibfk_1`
    FOREIGN KEY (`branch_id`)
    REFERENCES `cafe_db`.`Branch` (`branch_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`Branch`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`Branch` (
  `branch_id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(250) NOT NULL,
  `address` VARCHAR(255) NULL DEFAULT NULL,
  `contact_num` VARCHAR(20) NULL DEFAULT NULL,
  `manager_id` INT NULL DEFAULT NULL,
  PRIMARY KEY (`branch_id`),
  INDEX `manager_id` (`manager_id` ASC) VISIBLE,
  CONSTRAINT `Branch_ibfk_1`
    FOREIGN KEY (`manager_id`)
    REFERENCES `cafe_db`.`Employee` (`employee_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`Category`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`Category` (
  `category_id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NULL DEFAULT NULL,
  PRIMARY KEY (`category_id`),
  UNIQUE INDEX `name_UNIQUE` (`name` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`Currency`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`Currency` (
  `currency_id` INT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(5) NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `symbol` VARCHAR(5) NOT NULL,
  `rate` DECIMAL(12,4) NOT NULL DEFAULT '1.0000',
  `last_updated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`currency_id`),
  UNIQUE INDEX `code_UNIQUE` (`code` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`Customer`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`Customer` (
  `customer_id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(150) NOT NULL,
  `phone_num` VARCHAR(15) NULL DEFAULT NULL,
  `email` VARCHAR(150) NULL DEFAULT NULL,
  PRIMARY KEY (`customer_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`Menu`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`Menu` (
  `menu_id` INT NOT NULL AUTO_INCREMENT,
  `category_id` INT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) NULL DEFAULT NULL,
  `is_drink` TINYINT NOT NULL DEFAULT '0',
  `is_available` TINYINT NOT NULL DEFAULT '1',
  PRIMARY KEY (`menu_id`),
  INDEX `fk_Menu_Category_idx` (`category_id` ASC) VISIBLE,
  CONSTRAINT `fk_Menu_Category`
    FOREIGN KEY (`category_id`)
    REFERENCES `cafe_db`.`Category` (`category_id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`DrinkOption`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`DrinkOption` (
  `drink_option_id` INT NOT NULL AUTO_INCREMENT,
  `menu_id` INT NOT NULL,
  `temperature` ENUM('hot', 'iced') NOT NULL,
  `price_modifier` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`drink_option_id`),
  INDEX `fk_DrinkOption_Menu1_idx` (`menu_id` ASC) VISIBLE,
  CONSTRAINT `fk_DrinkOption_Menu1`
    FOREIGN KEY (`menu_id`)
    REFERENCES `cafe_db`.`Menu` (`menu_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`Extra`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`Extra` (
  `extra_id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`extra_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`Legend`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`Legend` (
  `legend_id` INT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(20) NULL DEFAULT NULL,
  `description` VARCHAR(150) NULL DEFAULT NULL,
  PRIMARY KEY (`legend_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`LoyaltyCard`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`LoyaltyCard` (
  `loyalty_id` INT NOT NULL AUTO_INCREMENT,
  `customer_id` INT NULL DEFAULT NULL,
  `card_number` VARCHAR(20) NOT NULL,
  `points` INT NOT NULL DEFAULT '0',
  `last_redeemed` DATETIME NULL DEFAULT NULL,
  `is_active` TINYINT NOT NULL DEFAULT '1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`loyalty_id`),
  INDEX `fk_LoyaltyCard_Customer1_idx` (`customer_id` ASC) VISIBLE,
  CONSTRAINT `fk_LoyaltyCard_Customer1`
    FOREIGN KEY (`customer_id`)
    REFERENCES `cafe_db`.`Customer` (`customer_id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`MenuExtra`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`MenuExtra` (
  `extra_id` INT NOT NULL,
  `menu_id` INT NOT NULL,
  PRIMARY KEY (`extra_id`, `menu_id`),
  INDEX `fk_MenuExtra_Extra1_idx` (`extra_id` ASC) VISIBLE,
  INDEX `fk_MenuExtra_Menu1_idx` (`menu_id` ASC) VISIBLE,
  CONSTRAINT `fk_MenuExtra_Extra1`
    FOREIGN KEY (`extra_id`)
    REFERENCES `cafe_db`.`Extra` (`extra_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_MenuExtra_Menu1`
    FOREIGN KEY (`menu_id`)
    REFERENCES `cafe_db`.`Menu` (`menu_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`MenuLegend`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`MenuLegend` (
  `menu_id` INT NOT NULL,
  `legend_id` INT NOT NULL,
  PRIMARY KEY (`menu_id`, `legend_id`),
  INDEX `fk_MenuLegend_Menu1_idx` (`menu_id` ASC) VISIBLE,
  INDEX `fk_MenuLegend_Legend1_idx` (`legend_id` ASC) VISIBLE,
  CONSTRAINT `fk_MenuLegend_Legend1`
    FOREIGN KEY (`legend_id`)
    REFERENCES `cafe_db`.`Legend` (`legend_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_MenuLegend_Menu1`
    FOREIGN KEY (`menu_id`)
    REFERENCES `cafe_db`.`Menu` (`menu_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`MenuPrice`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`MenuPrice` (
  `price_id` INT NOT NULL AUTO_INCREMENT,
  `menu_id` INT NOT NULL,
  `currency_id` INT NOT NULL,
  `price_amount` DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (`price_id`),
  INDEX `fk_MenuPrice_Menu1_idx` (`menu_id` ASC) VISIBLE,
  INDEX `fk_MenuPrice_Currency1_idx` (`currency_id` ASC) VISIBLE,
  CONSTRAINT `fk_MenuPrice_Currency1`
    FOREIGN KEY (`currency_id`)
    REFERENCES `cafe_db`.`Currency` (`currency_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_MenuPrice_Menu1`
    FOREIGN KEY (`menu_id`)
    REFERENCES `cafe_db`.`Menu` (`menu_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`OrderTbl`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`OrderTbl` (
  `order_id` INT NOT NULL AUTO_INCREMENT,
  `customer_id` INT NOT NULL,
  `employee_id` INT NULL DEFAULT NULL,
  `loyalty_id` INT NULL DEFAULT NULL,
  `order_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `earned_points` INT NULL DEFAULT '0',
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `status` ENUM('pending', 'confirmed', 'completed', 'cancelled') NOT NULL DEFAULT 'pending',
  `branch_id` INT NULL DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  INDEX `fk_OrderTbl_Customer1_idx` (`customer_id` ASC) VISIBLE,
  INDEX `fk_OrderTbl_Employee1_idx` (`employee_id` ASC) VISIBLE,
  INDEX `fk_OrderTbl_LoyaltyCard1_idx` (`loyalty_id` ASC) VISIBLE,
  INDEX `branch_id` (`branch_id` ASC) VISIBLE,
  CONSTRAINT `fk_OrderTbl_Customer1`
    FOREIGN KEY (`customer_id`)
    REFERENCES `cafe_db`.`Customer` (`customer_id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_OrderTbl_Employee1`
    FOREIGN KEY (`employee_id`)
    REFERENCES `cafe_db`.`Employee` (`employee_id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_OrderTbl_LoyaltyCard1`
    FOREIGN KEY (`loyalty_id`)
    REFERENCES `cafe_db`.`LoyaltyCard` (`loyalty_id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `OrderTbl_ibfk_1`
    FOREIGN KEY (`branch_id`)
    REFERENCES `cafe_db`.`Branch` (`branch_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`OrderHistory`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`OrderHistory` (
  `history_id` INT NOT NULL AUTO_INCREMENT,
  `order_id` INT NOT NULL,
  `employee_id` INT NULL DEFAULT NULL,
  `status` ENUM('pending', 'confirmed', 'completed', 'cancelled') NOT NULL,
  `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remarks` VARCHAR(200) NULL DEFAULT NULL,
  PRIMARY KEY (`history_id`),
  INDEX `fk_OrderHistory_Employee1_idx` (`employee_id` ASC) VISIBLE,
  INDEX `fk_OrderHistory_OrderTbl1_idx` (`order_id` ASC) VISIBLE,
  CONSTRAINT `fk_OrderHistory_Employee1`
    FOREIGN KEY (`employee_id`)
    REFERENCES `cafe_db`.`Employee` (`employee_id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_OrderHistory_OrderTbl1`
    FOREIGN KEY (`order_id`)
    REFERENCES `cafe_db`.`OrderTbl` (`order_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`OrderItem`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`OrderItem` (
  `order_item_id` INT NOT NULL AUTO_INCREMENT,
  `order_id` INT NOT NULL,
  `menu_id` INT NOT NULL,
  `drink_option_id` INT NULL DEFAULT NULL,
  `quantity` INT NOT NULL DEFAULT '1',
  `price` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`order_item_id`),
  INDEX `fk_OrderItem_Menu1_idx` (`menu_id` ASC) VISIBLE,
  INDEX `fk_OrderItem_DrinkOption1_idx` (`drink_option_id` ASC) VISIBLE,
  INDEX `fk_OrderItem_OrderTbl1_idx` (`order_id` ASC) VISIBLE,
  CONSTRAINT `fk_OrderItem_DrinkOption1`
    FOREIGN KEY (`drink_option_id`)
    REFERENCES `cafe_db`.`DrinkOption` (`drink_option_id`),
  CONSTRAINT `fk_OrderItem_Menu1`
    FOREIGN KEY (`menu_id`)
    REFERENCES `cafe_db`.`Menu` (`menu_id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_OrderItem_OrderTbl1`
    FOREIGN KEY (`order_id`)
    REFERENCES `cafe_db`.`OrderTbl` (`order_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`OrderItemExtra`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`OrderItemExtra` (
  `order_item_id` INT NOT NULL,
  `extra_id` INT NOT NULL,
  `quantity` INT NOT NULL DEFAULT '1',
  INDEX `fk_OrderItemExtra_OrderItem1_idx` (`order_item_id` ASC) VISIBLE,
  INDEX `fk_OrderItemExtra_Extra1_idx` (`extra_id` ASC) VISIBLE,
  CONSTRAINT `fk_OrderItemExtra_Extra1`
    FOREIGN KEY (`extra_id`)
    REFERENCES `cafe_db`.`Extra` (`extra_id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_OrderItemExtra_OrderItem1`
    FOREIGN KEY (`order_item_id`)
    REFERENCES `cafe_db`.`OrderItem` (`order_item_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `cafe_db`.`TransactionTbl`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `cafe_db`.`TransactionTbl` (
  `transaction_id` INT NOT NULL AUTO_INCREMENT,
  `order_id` INT NOT NULL,
  `currency_id` INT NOT NULL,
  `payment_method` ENUM('cash', 'card', 'bank_transfer', 'other') NOT NULL,
  `amount_paid` DECIMAL(10,2) NOT NULL,
  `exchange_rate` DECIMAL(12,4) NOT NULL DEFAULT '1.0000',
  `transaction_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` ENUM('pending', 'completed') NOT NULL DEFAULT 'pending',
  `branch_id` INT NULL DEFAULT NULL,
  PRIMARY KEY (`transaction_id`),
  INDEX `fk_Transaction_Currency1_idx` (`currency_id` ASC) VISIBLE,
  INDEX `fk_TransactionTbl_OrderTbl1_idx` (`order_id` ASC) VISIBLE,
  INDEX `idx_branch_id_transaction` (`branch_id` ASC) VISIBLE,
  CONSTRAINT `fk_Transaction_Currency1`
    FOREIGN KEY (`currency_id`)
    REFERENCES `cafe_db`.`Currency` (`currency_id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT `fk_TransactionTbl_OrderTbl1`
    FOREIGN KEY (`order_id`)
    REFERENCES `cafe_db`.`OrderTbl` (`order_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `TransactionTbl_ibfk_1`
    FOREIGN KEY (`branch_id`)
    REFERENCES `cafe_db`.`Branch` (`branch_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
