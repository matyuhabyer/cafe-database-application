-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: localhost    Database: cafe_db
-- ------------------------------------------------------
-- Server version	8.0.43-0ubuntu0.22.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `Branch`
--

LOCK TABLES `Branch` WRITE;
/*!40000 ALTER TABLE `Branch` DISABLE KEYS */;
INSERT INTO `Branch` VALUES (1,'Manila Branch','2401 Taft Avenue, Manila 1004, Philippines','(632) 8465 8900',NULL),(2,'Laguna Branch','Laguna Boulevard, LTI Spine Road, Barangays Biñan and Malamig, Biñan City, Laguna, Philippines','(632) 8524 4611',NULL);
/*!40000 ALTER TABLE `Branch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Category`
--

LOCK TABLES `Category` WRITE;
/*!40000 ALTER TABLE `Category` DISABLE KEYS */;
INSERT INTO `Category` VALUES (1,'Breakfast Rice Bowls'),(7,'Coffee-Based'),(10,'Fizzy Drinks'),(8,'Non-Coffee'),(6,'Other Plates'),(3,'Pasta'),(2,'Rice Plates'),(5,'Salads'),(4,'Sandwiches'),(9,'Tea-Based');
/*!40000 ALTER TABLE `Category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Currency`
--

LOCK TABLES `Currency` WRITE;
/*!40000 ALTER TABLE `Currency` DISABLE KEYS */;
INSERT INTO `Currency` VALUES (1,'PHP','Philippine Peso','₱',1.0000,'2025-11-07 06:32:48'),(2,'USD','US Dollars','$',0.0170,'2025-11-07 08:06:20'),(3,'KRW','Korean Won','₩',24.6600,'2025-11-07 08:07:13');
/*!40000 ALTER TABLE `Currency` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Customer`
--

LOCK TABLES `Customer` WRITE;
/*!40000 ALTER TABLE `Customer` DISABLE KEYS */;
/*!40000 ALTER TABLE `Customer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `DrinkOption`
--

LOCK TABLES `DrinkOption` WRITE;
/*!40000 ALTER TABLE `DrinkOption` DISABLE KEYS */;
INSERT INTO `DrinkOption` VALUES (1,26,'hot',0.00),(2,27,'hot',0.00),(3,27,'iced',10.00),(4,28,'iced',0.00),(5,29,'hot',0.00),(6,29,'iced',10.00),(7,30,'hot',0.00),(8,31,'hot',0.00),(9,31,'iced',20.00),(10,32,'hot',0.00),(11,32,'iced',10.00),(12,33,'hot',0.00),(13,33,'iced',10.00),(14,34,'hot',0.00),(15,34,'iced',15.00),(16,35,'hot',0.00),(17,35,'iced',15.00),(18,36,'iced',0.00),(19,37,'hot',0.00),(20,37,'iced',10.00),(21,38,'iced',0.00),(22,39,'iced',0.00),(23,40,'iced',0.00),(24,41,'iced',0.00),(25,42,'iced',0.00),(26,43,'hot',0.00),(27,43,'iced',30.00),(28,44,'hot',0.00),(29,44,'iced',20.00),(30,45,'hot',0.00),(31,45,'iced',10.00),(32,46,'iced',0.00),(33,47,'iced',0.00),(34,48,'iced',0.00),(35,49,'iced',0.00),(36,50,'iced',0.00),(37,51,'iced',0.00),(38,52,'iced',0.00),(39,53,'iced',0.00),(40,54,'iced',0.00),(41,55,'iced',0.00),(42,56,'iced',0.00),(43,57,'hot',0.00),(44,58,'hot',0.00),(45,59,'hot',0.00),(46,60,'hot',0.00),(47,61,'hot',0.00),(48,62,'hot',0.00),(49,63,'hot',0.00),(50,57,'hot',0.00),(51,58,'hot',0.00),(52,59,'hot',0.00),(53,60,'hot',0.00),(54,61,'hot',0.00),(55,62,'hot',0.00),(56,63,'hot',0.00),(57,64,'iced',0.00),(58,65,'iced',0.00),(59,66,'iced',0.00),(60,67,'iced',0.00),(61,68,'iced',0.00),(62,69,'iced',0.00),(63,70,'iced',0.00),(64,71,'iced',0.00),(65,72,'iced',0.00),(66,73,'iced',0.00),(67,74,'iced',0.00);
/*!40000 ALTER TABLE `DrinkOption` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Employee`
--

LOCK TABLES `Employee` WRITE;
/*!40000 ALTER TABLE `Employee` DISABLE KEYS */;
INSERT INTO `Employee` VALUES (1,'Matthew Javier','admin',NULL,NULL),(2,'Alec Dela Cruz','admin',NULL,NULL),(3,'Margaux Miranda','admin',NULL,NULL),(4,'Macy Estabillo','admin',NULL,NULL);
/*!40000 ALTER TABLE `Employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Extra`
--

LOCK TABLES `Extra` WRITE;
/*!40000 ALTER TABLE `Extra` DISABLE KEYS */;
INSERT INTO `Extra` VALUES (1,'Plain Rice',60.00),(2,'Garlic Rice',110.00),(3,'Extra Egg',45.00),(4,'Garlic Bread',55.00),(5,'A Hash Brown',70.00),(6,'Espresso Shot',65.00),(7,'Whole Milk',55.00),(8,'Soy Milk',60.00),(9,'Oat Milk',60.00),(10,'Caramel Syrup',60.00),(11,'Chocolate Syrup',60.00),(12,'Vanilla Syrup',60.00),(13,'Whipped Cream',50.00);
/*!40000 ALTER TABLE `Extra` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Legend`
--

LOCK TABLES `Legend` WRITE;
/*!40000 ALTER TABLE `Legend` DISABLE KEYS */;
INSERT INTO `Legend` VALUES (1,'SPCL','T.W.R. Specialty'),(2,'SPCY','Spicy');
/*!40000 ALTER TABLE `Legend` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `LoyaltyCard`
--

LOCK TABLES `LoyaltyCard` WRITE;
/*!40000 ALTER TABLE `LoyaltyCard` DISABLE KEYS */;
/*!40000 ALTER TABLE `LoyaltyCard` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Menu`
--

LOCK TABLES `Menu` WRITE;
/*!40000 ALTER TABLE `Menu` DISABLE KEYS */;
INSERT INTO `Menu` VALUES (1,1,'Tocino Bowl','Filipino-style sweet cured pork, Fried Rice, and Fried Egg',290.00,0,1),(2,1,'Tapa Bowl','Filipino-style tenderised beef, Fried Rice, and Fried Egg',380.00,0,1),(3,1,'Bacon Bowl','Bacon strips, Hash Brown, Fried Rice, and Fried Egg',350.00,0,1),(4,1,'Spam Bowl','Spam slices, Hash Brown, Fried Rice, and Fried Egg',350.00,0,1),(5,1,'Bangus Bowl','Homemade Spanish-Style Bangus, Fried Rice, and Fried Egg',350.00,0,1),(6,1,'Longa Bowl','\"Naked\" Filipino Longanissa, Fried Rice, and Fried Egg',290.00,0,1),(7,2,'Bistek Tagalog','Traditional Pinoy-Beef Steak, Red Onion, Fried White Onion, and Fried Rice',390.00,0,1),(8,2,'Burger Bowl','130g of Pure Beef Patty served in a pool of Gravy, Slice of Cheese, topped with a Fried Egg on Rice',395.00,0,1),(9,2,'Fried Chicken','Half Spring Chicken Fried Golden, Served with Sweet Chili Sauce, Potato Wedges, and Plain Rice',420.00,0,1),(10,2,'Herbed Chicken','Pan-Grilled Chicken, and Plain Rice with a side of Gravy',420.00,0,1),(11,3,'Aligue Pasta','Fettucini, Peeled Shrimp, and Creamy Aligue Sauce',375.00,0,1),(12,3,'Puttanesca','Spaghetti, Tomato-based Sauce, and Parmesan',350.00,0,1),(13,3,'Arrabiata','Penne, Tomato-based Sauce, and Parmesan',310.00,0,1),(14,3,'Truffle Pasta','Fettucini, Creamy Truffle-based Pasta',310.00,0,1),(15,3,'Baked Spaghetti','Spaghetti, Sweet-Creamy Tomato-based Sauce and Beef',390.00,0,1),(16,4,'B.L.T. Sammie','Classic Sandwich with Bacon, Lettuce, and Tomatoes',350.00,0,1),(17,4,'The Clubhouse','Bacon, Lettuce, Tomatoes, Egg, Chicken & Ham, Served with Coleslaw on the side',380.00,0,1),(18,4,'Tuna Melt Sammie','Creamy Tuna and Melted Cheese',320.00,0,1),(19,4,'Bacon & Egg Sammie','Crispy Bacon and Cooked Egg',350.00,0,1),(20,5,'Salata Verde','Sweet-Tangy Balsamic Dressing, Topped with Figs, Walnuts, and Parmesan',330.00,0,1),(21,5,'Coleslaw','Shredded Raw Cabbage mixed with Creamy Dressing',170.00,0,1),(22,6,'Chicken & Fries','Juicy Fried Chicken with Salted French Fries',360.00,0,1),(23,6,'Loaded Nachos','Tortilla Chips with Melted Cheese, Meat, Beans, Jalapenos',300.00,0,1),(24,6,'Truffle Fries','Served with homemade Truffle Aioli Sauce',275.00,0,1),(25,6,'Seasoned Fries','French Fries with Salt, Herbs, and Spices',200.00,0,1),(26,7,'Espresso',NULL,130.00,1,1),(27,7,'Americano',NULL,140.00,1,1),(28,7,'Espresso Tonic',NULL,195.00,1,1),(29,7,'Latte',NULL,160.00,1,1),(30,7,'Flat White',NULL,165.00,1,1),(31,7,'Cappuccino',NULL,165.00,1,1),(32,7,'Caramel Macchiato',NULL,185.00,1,1),(33,7,'Caramel Latte',NULL,185.00,1,1),(34,7,'Dirty Chai',NULL,195.00,1,1),(35,7,'Mocha Latte',NULL,180.00,1,1),(36,7,'Pink Espresso',NULL,195.00,1,1),(37,7,'Boss \"A\" Spanish Latte',NULL,185.00,1,1),(38,7,'Dirty Matcha',NULL,245.00,1,1),(39,7,'Dirty Berry Choco',NULL,210.00,1,1),(40,7,'Mocha Crunch',NULL,215.00,1,1),(41,7,'Nutty Toffee',NULL,195.00,1,1),(42,7,'Strawberry Mocha',NULL,210.00,1,1),(43,8,'Matcha Latte',NULL,195.00,1,1),(44,8,'Chocolate',NULL,195.00,1,1),(45,8,'Chai Latte',NULL,185.00,1,1),(46,8,'Strawberry Matcha',NULL,215.00,1,1),(47,8,'Earl Grey Matcha',NULL,215.00,1,1),(48,8,'Berry Choco',NULL,195.00,1,1),(49,8,'Chocolate (Blended)',NULL,210.00,1,1),(50,8,'Caramel Shortbread',NULL,195.00,1,1),(51,8,'Strawberry Cheesecake',NULL,210.00,1,1),(52,8,'Matcha (Blended)',NULL,220.00,1,1),(53,8,'Frosty After 8',NULL,195.00,1,1),(54,8,'Fairy Floss',NULL,195.00,1,1),(55,8,'Banana Matcha',NULL,240.00,1,1),(56,8,'Choco Banana',NULL,220.00,1,1),(57,9,'English Breakfast',NULL,105.00,1,1),(58,9,'Earl Grey',NULL,105.00,1,1),(59,9,'Jasmine Tea',NULL,105.00,1,1),(60,9,'Lemon N Ginger',NULL,105.00,1,1),(61,9,'Chamomile',NULL,105.00,1,1),(62,9,'Red Fruits',NULL,105.00,1,1),(63,9,'Blue Pea Tea',NULL,105.00,1,1),(64,9,'House Blend Iced Tea',NULL,185.00,1,1),(65,9,'Passion Fruit Tea',NULL,185.00,1,1),(66,9,'Watermelon Fruit Tea',NULL,185.00,1,1),(67,9,'Strawberry Fruit Tea',NULL,185.00,1,1),(68,9,'Mango Chamomile',NULL,185.00,1,1),(69,9,'Lemon Earl Grey',NULL,185.00,1,1),(70,10,'Purple Fizz',NULL,195.00,1,1),(71,10,'Blue Lemon Fizz',NULL,195.00,1,1),(72,10,'Aroha Fizz',NULL,195.00,1,1),(73,10,'Regular Coke',NULL,110.00,1,1),(74,10,'Coke No Sugar',NULL,110.00,1,1);
/*!40000 ALTER TABLE `Menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `MenuExtra`
--

LOCK TABLES `MenuExtra` WRITE;
/*!40000 ALTER TABLE `MenuExtra` DISABLE KEYS */;
INSERT INTO `MenuExtra` VALUES (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),(1,11),(1,12),(1,13),(1,14),(1,15),(1,16),(1,17),(1,18),(1,19),(1,20),(1,21),(1,22),(1,23),(1,24),(1,25),(2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),(2,8),(2,9),(2,10),(2,11),(2,12),(2,13),(2,14),(2,15),(2,16),(2,17),(2,18),(2,19),(2,20),(2,21),(2,22),(2,23),(2,24),(2,25),(3,1),(3,2),(3,3),(3,4),(3,5),(3,6),(3,7),(3,8),(3,9),(3,10),(3,11),(3,12),(3,13),(3,14),(3,15),(3,16),(3,17),(3,18),(3,19),(3,20),(3,21),(3,22),(3,23),(3,24),(3,25),(4,1),(4,2),(4,3),(4,4),(4,5),(4,6),(4,7),(4,8),(4,9),(4,10),(4,11),(4,12),(4,13),(4,14),(4,15),(4,16),(4,17),(4,18),(4,19),(4,20),(4,21),(4,22),(4,23),(4,24),(4,25),(5,1),(5,2),(5,3),(5,4),(5,5),(5,6),(5,7),(5,8),(5,9),(5,10),(5,11),(5,12),(5,13),(5,14),(5,15),(5,16),(5,17),(5,18),(5,19),(5,20),(5,21),(5,22),(5,23),(5,24),(5,25),(6,26),(6,27),(6,28),(6,29),(6,30),(6,31),(6,32),(6,33),(6,34),(6,35),(6,36),(6,37),(6,38),(6,39),(6,40),(6,41),(6,42),(6,43),(6,44),(6,45),(6,46),(6,47),(6,48),(6,49),(6,50),(6,51),(6,52),(6,53),(6,54),(6,55),(6,56),(6,57),(6,58),(6,59),(6,60),(6,61),(6,62),(6,63),(6,64),(6,65),(6,66),(6,67),(6,68),(6,69),(6,70),(6,71),(6,72),(6,73),(6,74),(7,26),(7,27),(7,28),(7,29),(7,30),(7,31),(7,32),(7,33),(7,34),(7,35),(7,36),(7,37),(7,38),(7,39),(7,40),(7,41),(7,42),(7,43),(7,44),(7,45),(7,46),(7,47),(7,48),(7,49),(7,50),(7,51),(7,52),(7,53),(7,54),(7,55),(7,56),(7,57),(7,58),(7,59),(7,60),(7,61),(7,62),(7,63),(7,64),(7,65),(7,66),(7,67),(7,68),(7,69),(7,70),(7,71),(7,72),(7,73),(7,74),(8,26),(8,27),(8,28),(8,29),(8,30),(8,31),(8,32),(8,33),(8,34),(8,35),(8,36),(8,37),(8,38),(8,39),(8,40),(8,41),(8,42),(8,43),(8,44),(8,45),(8,46),(8,47),(8,48),(8,49),(8,50),(8,51),(8,52),(8,53),(8,54),(8,55),(8,56),(8,57),(8,58),(8,59),(8,60),(8,61),(8,62),(8,63),(8,64),(8,65),(8,66),(8,67),(8,68),(8,69),(8,70),(8,71),(8,72),(8,73),(8,74),(9,26),(9,27),(9,28),(9,29),(9,30),(9,31),(9,32),(9,33),(9,34),(9,35),(9,36),(9,37),(9,38),(9,39),(9,40),(9,41),(9,42),(9,43),(9,44),(9,45),(9,46),(9,47),(9,48),(9,49),(9,50),(9,51),(9,52),(9,53),(9,54),(9,55),(9,56),(9,57),(9,58),(9,59),(9,60),(9,61),(9,62),(9,63),(9,64),(9,65),(9,66),(9,67),(9,68),(9,69),(9,70),(9,71),(9,72),(9,73),(9,74),(10,26),(10,27),(10,28),(10,29),(10,30),(10,31),(10,32),(10,33),(10,34),(10,35),(10,36),(10,37),(10,38),(10,39),(10,40),(10,41),(10,42),(10,43),(10,44),(10,45),(10,46),(10,47),(10,48),(10,49),(10,50),(10,51),(10,52),(10,53),(10,54),(10,55),(10,56),(10,57),(10,58),(10,59),(10,60),(10,61),(10,62),(10,63),(10,64),(10,65),(10,66),(10,67),(10,68),(10,69),(10,70),(10,71),(10,72),(10,73),(10,74),(11,26),(11,27),(11,28),(11,29),(11,30),(11,31),(11,32),(11,33),(11,34),(11,35),(11,36),(11,37),(11,38),(11,39),(11,40),(11,41),(11,42),(11,43),(11,44),(11,45),(11,46),(11,47),(11,48),(11,49),(11,50),(11,51),(11,52),(11,53),(11,54),(11,55),(11,56),(11,57),(11,58),(11,59),(11,60),(11,61),(11,62),(11,63),(11,64),(11,65),(11,66),(11,67),(11,68),(11,69),(11,70),(11,71),(11,72),(11,73),(11,74),(12,26),(12,27),(12,28),(12,29),(12,30),(12,31),(12,32),(12,33),(12,34),(12,35),(12,36),(12,37),(12,38),(12,39),(12,40),(12,41),(12,42),(12,43),(12,44),(12,45),(12,46),(12,47),(12,48),(12,49),(12,50),(12,51),(12,52),(12,53),(12,54),(12,55),(12,56),(12,57),(12,58),(12,59),(12,60),(12,61),(12,62),(12,63),(12,64),(12,65),(12,66),(12,67),(12,68),(12,69),(12,70),(12,71),(12,72),(12,73),(12,74),(13,26),(13,27),(13,28),(13,29),(13,30),(13,31),(13,32),(13,33),(13,34),(13,35),(13,36),(13,37),(13,38),(13,39),(13,40),(13,41),(13,42),(13,43),(13,44),(13,45),(13,46),(13,47),(13,48),(13,49),(13,50),(13,51),(13,52),(13,53),(13,54),(13,55),(13,56),(13,57),(13,58),(13,59),(13,60),(13,61),(13,62),(13,63),(13,64),(13,65),(13,66),(13,67),(13,68),(13,69),(13,70),(13,71),(13,72),(13,73),(13,74);
/*!40000 ALTER TABLE `MenuExtra` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `MenuLegend`
--

LOCK TABLES `MenuLegend` WRITE;
/*!40000 ALTER TABLE `MenuLegend` DISABLE KEYS */;
INSERT INTO `MenuLegend` VALUES (1,1),(2,1),(7,1),(8,1),(12,2),(13,1),(13,2),(14,1),(15,1),(18,1),(22,1),(23,1),(23,2);
/*!40000 ALTER TABLE `MenuLegend` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `OrderHistory`
--

LOCK TABLES `OrderHistory` WRITE;
/*!40000 ALTER TABLE `OrderHistory` DISABLE KEYS */;
/*!40000 ALTER TABLE `OrderHistory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `OrderItem`
--

LOCK TABLES `OrderItem` WRITE;
/*!40000 ALTER TABLE `OrderItem` DISABLE KEYS */;
/*!40000 ALTER TABLE `OrderItem` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `OrderItemExtra`
--

LOCK TABLES `OrderItemExtra` WRITE;
/*!40000 ALTER TABLE `OrderItemExtra` DISABLE KEYS */;
/*!40000 ALTER TABLE `OrderItemExtra` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `OrderTbl`
--

LOCK TABLES `OrderTbl` WRITE;
/*!40000 ALTER TABLE `OrderTbl` DISABLE KEYS */;
/*!40000 ALTER TABLE `OrderTbl` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `TransactionTbl`
--

LOCK TABLES `TransactionTbl` WRITE;
/*!40000 ALTER TABLE `TransactionTbl` DISABLE KEYS */;
/*!40000 ALTER TABLE `TransactionTbl` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-11-08  3:57:17
