/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : market_db

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 13/06/2025 08:55:16
*/
CREATE DATABASE market_db;
USE market_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for cart_items
-- ----------------------------
DROP TABLE IF EXISTS `cart_items`;
CREATE TABLE `cart_items`  (
  `cart_item_id` int NOT NULL AUTO_INCREMENT,
  `cart_id` int NOT NULL,
  `item_id` int NOT NULL,
  `quantity` int NULL DEFAULT 1,
  `added_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_item_id`) USING BTREE,
  INDEX `cart_id`(`cart_id` ASC) USING BTREE,
  INDEX `item_id`(`item_id` ASC) USING BTREE,
  CONSTRAINT `cart_items_ibfk_1` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`cart_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `cart_items_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `items` (`item_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cart_items
-- ----------------------------

-- ----------------------------
-- Table structure for carts
-- ----------------------------
DROP TABLE IF EXISTS `carts`;
CREATE TABLE `carts`  (
  `cart_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `carts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of carts
-- ----------------------------
INSERT INTO `carts` VALUES (23, 2, '2025-06-10 23:39:06');
INSERT INTO `carts` VALUES (24, 1, '2025-06-11 00:27:46');
INSERT INTO `carts` VALUES (25, 3, '2025-06-11 14:11:29');
INSERT INTO `carts` VALUES (26, 30, '2025-06-12 08:59:17');

-- ----------------------------
-- Table structure for categories
-- ----------------------------
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories`  (
  `category_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`category_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of categories
-- ----------------------------
INSERT INTO `categories` VALUES (1, '服饰鞋帽', '2025-06-05 09:53:39', 'admin');
INSERT INTO `categories` VALUES (2, '生活用品', '2025-06-05 09:53:39', 'admin');
INSERT INTO `categories` VALUES (3, '数码电子', '2025-06-05 09:53:39', 'admin');
INSERT INTO `categories` VALUES (4, '图书教材', '2025-06-05 09:53:39', 'admin');

-- ----------------------------
-- Table structure for items
-- ----------------------------
DROP TABLE IF EXISTS `items`;
CREATE TABLE `items`  (
  `item_id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `price` decimal(10, 2) NOT NULL,
  `user_id` int NOT NULL,
  `category_id` int NOT NULL,
  `status` enum('ACTIVE','SOLD','INACTIVE') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'ACTIVE',
  `valid_until` datetime NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `item_image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`item_id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `category_id`(`category_id` ASC) USING BTREE,
  CONSTRAINT `items_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `items_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of items
-- ----------------------------
INSERT INTO `items` VALUES (7, '二手笔记本电脑', '华硕笔记本，i5处理器，8G内存，512G固态硬盘，九成新', 2500.00, 3, 3, 'ACTIVE', '2025-08-01 21:45:55', '2025-06-07 06:49:48', '2025-06-09 09:51:41', 'item-7');
INSERT INTO `items` VALUES (8, '大学数学教材', '高等数学上下册，微积分基础，九五成新', 50.00, 3, 4, 'ACTIVE', '2025-08-01 21:46:04', '2025-06-07 06:49:48', '2025-06-09 09:52:25', 'item-8');
INSERT INTO `items` VALUES (9, '运动鞋', '耐克运动鞋，43码，只穿过几次', 300.00, 1, 1, 'ACTIVE', '2025-08-01 21:46:09', '2025-06-07 06:49:48', '2025-06-09 09:53:07', 'item-9');
INSERT INTO `items` VALUES (10, '台灯', '宿舍学习LED台灯，亮度可调，带USB充电口', 80.00, 1, 2, 'ACTIVE', '2025-08-01 21:46:14', '2025-06-07 06:49:48', '2025-06-09 09:53:14', 'item-10');
INSERT INTO `items` VALUES (11, '平板电脑', 'iPad 2020款，128G，WiFi版，配有笔和保护套', 2300.00, 2, 3, 'ACTIVE', '2025-08-01 21:46:19', '2025-06-07 06:49:48', '2025-06-10 14:24:02', 'item-11');
INSERT INTO `items` VALUES (12, '耳机', '索尼无线蓝牙耳机，降噪效果好', 600.00, 3, 3, 'SOLD', '2025-08-01 21:46:23', '2025-06-07 06:49:48', '2025-06-11 06:10:34', 'item-12');
INSERT INTO `items` VALUES (22, '二手iPhone 12', '128GB，电池健康度88%，无拆修', 2200.00, 1, 3, 'ACTIVE', '2025-08-01 00:00:00', '2025-06-09 09:07:55', '2025-06-09 09:43:46', 'item-22');
INSERT INTO `items` VALUES (23, '耐克运动鞋', '42码，气垫缓震，仅穿三次', 350.00, 1, 1, 'ACTIVE', '2025-08-01 00:00:00', '2025-06-09 09:08:37', '2025-06-09 09:43:03', 'item-23');
INSERT INTO `items` VALUES (24, '考研英语真题集', '近十年真题+详解，重点已标注', 45.00, 1, 4, 'ACTIVE', '2025-08-01 00:00:00', '2025-06-09 09:09:20', '2025-06-09 09:42:38', 'item-24');
INSERT INTO `items` VALUES (25, 'USB小风扇', '三档风速，宿舍降温神器', 35.00, 1, 2, 'ACTIVE', '2025-08-01 00:00:00', '2025-06-09 09:11:33', '2025-06-10 15:33:12', 'item-25');
INSERT INTO `items` VALUES (26, '微积分教材', '同济第七版，课后习题全解', 30.00, 1, 4, 'ACTIVE', '2025-08-01 00:00:00', '2025-06-09 09:12:22', '2025-06-09 09:39:11', 'item-26');
INSERT INTO `items` VALUES (27, '双肩电脑包', '防水面料，专用电脑隔层', 90.00, 1, 2, 'ACTIVE', '2025-08-01 00:00:00', '2025-06-09 09:13:05', '2025-06-09 09:38:19', 'item-27');
INSERT INTO `items` VALUES (28, '机械键盘', '青轴手感，RGB背光', 180.00, 1, 3, 'ACTIVE', '2025-08-01 00:00:00', '2025-06-09 09:13:40', '2025-06-10 15:33:16', 'item-28');
INSERT INTO `items` VALUES (29, '羽绒服', '长款加厚，L码，保暖性好', 280.00, 1, 1, 'ACTIVE', '2025-08-01 00:00:00', '2025-06-09 09:14:28', '2025-06-09 09:34:54', 'item-29');
INSERT INTO `items` VALUES (30, 'Java编程教材', '配有实验指导，适合初学者', 50.00, 1, 4, 'ACTIVE', '2025-08-01 00:00:00', '2025-06-09 09:15:38', '2025-06-09 09:19:53', 'item-30');
INSERT INTO `items` VALUES (31, '升降椅', '人体工学设计，九成新', 200.00, 1, 2, 'ACTIVE', '2025-08-01 00:00:00', '2025-06-09 09:17:48', '2025-06-10 16:36:34', 'item-31');
INSERT INTO `items` VALUES (36, '李霖1', 'syf的son1111111111111111111111111111111', 0.01, 30, 2, 'ACTIVE', '2025-08-01 08:00:00', '2025-06-12 01:36:29', '2025-06-12 01:56:47', 'item-36');

-- ----------------------------
-- Table structure for order_items
-- ----------------------------
DROP TABLE IF EXISTS `order_items`;
CREATE TABLE `order_items`  (
  `order_item_id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `item_id` int NOT NULL,
  `price` decimal(10, 2) NOT NULL,
  `quantity` int NOT NULL,
  PRIMARY KEY (`order_item_id`) USING BTREE,
  INDEX `order_id`(`order_id` ASC) USING BTREE,
  INDEX `item_id`(`item_id` ASC) USING BTREE,
  CONSTRAINT `order_items_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `order_items_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `items` (`item_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_items
-- ----------------------------
INSERT INTO `order_items` VALUES (8, 8, 12, 600.00, 1);

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `order_id` int NOT NULL AUTO_INCREMENT,
  `buyer_id` int NOT NULL,
  `seller_id` int NOT NULL,
  `total_amount` decimal(10, 2) NOT NULL,
  `status` enum('PENDING','COMPLETED','CANCELLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`) USING BTREE,
  INDEX `buyer_id`(`buyer_id` ASC) USING BTREE,
  INDEX `seller_id`(`seller_id` ASC) USING BTREE,
  CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `orders_ibfk_2` FOREIGN KEY (`seller_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders
-- ----------------------------
INSERT INTO `orders` VALUES (8, 2, 3, 600.00, 'COMPLETED', '2025-06-11 06:10:34');

-- ----------------------------
-- Table structure for reviews
-- ----------------------------
DROP TABLE IF EXISTS `reviews`;
CREATE TABLE `reviews`  (
  `review_id` int NOT NULL AUTO_INCREMENT,
  `rating` tinyint NULL DEFAULT NULL,
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `item_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`review_id`) USING BTREE,
  INDEX `ibfk1`(`item_id` ASC) USING BTREE,
  INDEX `ibfk2`(`user_id` ASC) USING BTREE,
  CONSTRAINT `ibfk1` FOREIGN KEY (`item_id`) REFERENCES `items` (`item_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ibfk2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `reviews_chk_1` CHECK (`rating` between 1 and 5)
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of reviews
-- ----------------------------
INSERT INTO `reviews` VALUES (8, 5, '55555555555555555555555555', '2025-06-11 06:12:17', 12, 2);

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `role` enum('ADMIN','CATEGORY_MANAGER','USER') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'USER',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `user_image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avg_rating` float NULL DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'admin', '12345678', '3035295963@qq.com', '17386450433', 'ADMIN', '2025-06-05 13:55:41', 'user-1', NULL, '汉科2#221');
INSERT INTO `users` VALUES (2, 'user', '123456', '1111111111@qq.com', '15236998741', 'USER', '2025-06-06 17:22:38', 'user-2', 3, '汉科2#222');
INSERT INTO `users` VALUES (3, 'catey', '1234567', '2222222229@qq.com', '14789663878', 'CATEGORY_MANAGER', '2025-06-06 17:23:36', 'user-3', 5, '汉科2#223');
INSERT INTO `users` VALUES (13, 'user1', '1234567', 'user1@test.com', '13800138001', 'USER', '2025-06-09 08:46:34', 'user-13', NULL, '汉科2#225');
INSERT INTO `users` VALUES (14, 'user2', 'pass123', 'user2@test.com', '13800138002', 'USER', '2025-06-09 08:46:34', 'user-14', NULL, '汉科2#225');
INSERT INTO `users` VALUES (15, 'user3', 'pass123', 'user3@test.com', '13800138003', 'USER', '2025-06-09 08:46:34', 'user-15', NULL, '汉科2#226');
INSERT INTO `users` VALUES (16, 'user4', 'pass123', 'user4@test.com', '13800138004', 'USER', '2025-06-09 08:46:34', 'user-16', NULL, '汉科2#227');
INSERT INTO `users` VALUES (17, 'user5', 'pass123', 'user5@test.com', '13800138005', 'USER', '2025-06-09 08:46:34', 'user-17', NULL, '汉科2#228');
INSERT INTO `users` VALUES (18, 'user6', 'pass123', 'user6@test.com', '13800138006', 'USER', '2025-06-09 08:46:34', 'user-18', NULL, '汉科2#229');
INSERT INTO `users` VALUES (19, 'user7', 'pass123', 'user7@test.com', '13800138007', 'USER', '2025-06-09 08:46:34', 'user-19', NULL, '汉科2#230');
INSERT INTO `users` VALUES (20, 'user8', 'pass123', 'user8@test.com', '13800138008', 'USER', '2025-06-09 08:46:34', 'user-20', NULL, '汉科2#231');
INSERT INTO `users` VALUES (21, 'user9', 'pass123', 'user9@test.com', '13800138009', 'USER', '2025-06-09 08:46:34', 'user-21', NULL, '汉科2#232');
INSERT INTO `users` VALUES (22, 'user10', 'pass123', 'user10@test.com', '13800138010', 'USER', '2025-06-09 08:46:34', 'user-22', NULL, '汉科2#233');
INSERT INTO `users` VALUES (23, 'user11', 'pass123', 'user11@test.com', '13800138011', 'USER', '2025-06-09 08:46:34', 'user-23', NULL, '汉科2#320');
INSERT INTO `users` VALUES (24, 'user12', 'pass123', 'user12@test.com', '13800138012', 'USER', '2025-06-09 08:46:34', 'user-24', NULL, '汉科2#321');
INSERT INTO `users` VALUES (25, 'user13', 'pass123', 'user13@test.com', '13800138013', 'USER', '2025-06-09 08:46:34', 'user-25', NULL, '汉科2#322');
INSERT INTO `users` VALUES (26, 'user14', 'pass123', 'user14@test.com', '13800138014', 'USER', '2025-06-09 08:46:34', 'user-26', NULL, '汉科2#323');
INSERT INTO `users` VALUES (27, 'user15', 'pass123', 'user15@test.com', '13800138015', 'USER', '2025-06-09 08:46:34', 'user-27', NULL, '汉科2#324');
INSERT INTO `users` VALUES (30, 'test', '123456', '147@qq.com', '18727942397', 'USER', '2025-06-12 00:58:45', 'user-30', NULL, '4#321');

-- ----------------------------
-- Triggers structure for table reviews
-- ----------------------------
DROP TRIGGER IF EXISTS `update_user_rating_after_insert`;
delimiter ;;
CREATE TRIGGER `update_user_rating_after_insert` AFTER INSERT ON `reviews` FOR EACH ROW BEGIN
    DECLARE seller_id INT;
    
    -- 获取评价对应商品的卖家ID
    SELECT user_id INTO seller_id FROM items WHERE item_id = NEW.item_id;
    
    -- 更新卖家的平均评分
    UPDATE users 
    SET avg_rating = (
        SELECT AVG(r.rating) 
        FROM reviews r 
        JOIN items i ON r.item_id = i.item_id 
        WHERE i.user_id = seller_id
    )
    WHERE user_id = seller_id;
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table reviews
-- ----------------------------
DROP TRIGGER IF EXISTS `update_user_rating_after_update`;
delimiter ;;
CREATE TRIGGER `update_user_rating_after_update` AFTER UPDATE ON `reviews` FOR EACH ROW BEGIN
    DECLARE seller_id INT;
    
    -- 获取评价对应商品的卖家ID
    SELECT user_id INTO seller_id FROM items WHERE item_id = NEW.item_id;
    
    -- 更新卖家的平均评分
    UPDATE users 
    SET avg_rating = (
        SELECT AVG(r.rating) 
        FROM reviews r 
        JOIN items i ON r.item_id = i.item_id 
        WHERE i.user_id = seller_id
    )
    WHERE user_id = seller_id;
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table reviews
-- ----------------------------
DROP TRIGGER IF EXISTS `update_user_rating_after_delete`;
delimiter ;;
CREATE TRIGGER `update_user_rating_after_delete` AFTER DELETE ON `reviews` FOR EACH ROW BEGIN
    DECLARE seller_id INT;
    
    -- 获取评价对应商品的卖家ID
    SELECT user_id INTO seller_id FROM items WHERE item_id = OLD.item_id;
    
    -- 更新卖家的平均评分
    UPDATE users 
    SET avg_rating = (
        SELECT AVG(r.rating) 
        FROM reviews r 
        JOIN items i ON r.item_id = i.item_id 
        WHERE i.user_id = seller_id
    )
    WHERE user_id = seller_id;
    
    -- 如果该卖家没有评价了，则设置平均评分为NULL
    UPDATE users 
    SET avg_rating = NULL
    WHERE user_id = seller_id 
    AND NOT EXISTS (
        SELECT 1 FROM reviews r 
        JOIN items i ON r.item_id = i.item_id 
        WHERE i.user_id = seller_id
    );
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
