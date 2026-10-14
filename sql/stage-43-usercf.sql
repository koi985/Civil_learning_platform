-- Stage 43: 真实 UserCF 协同过滤支撑表
-- 用户-用户相似度持久化（Redis 重建源、Top-50 截断）
DROP TABLE IF EXISTS `user_user_similarity`;
CREATE TABLE `user_user_similarity` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `similar_user_id` BIGINT NOT NULL,
  `similarity` DECIMAL(8,6) NOT NULL,
  `rank_no` INT NOT NULL,
  `computed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_similar` (`user_id`, `similar_user_id`),
  KEY `idx_user_rank` (`user_id`, `rank_no`)
) COMMENT='UserCF 相似用户矩阵（Top-50）';

-- 用户-物品交互聚合矩阵（统一向量空间：COURSE / EXAM / NEWS）
DROP TABLE IF EXISTS `user_item_interaction`;
CREATE TABLE `user_item_interaction` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `item_type` VARCHAR(16) NOT NULL,
  `item_id` BIGINT NOT NULL,
  `weight` DECIMAL(6,3) NOT NULL,
  `last_acted_at` DATETIME NOT NULL,
  UNIQUE KEY `uk_user_item` (`user_id`, `item_type`, `item_id`),
  KEY `idx_item` (`item_type`, `item_id`),
  KEY `idx_user_type` (`user_id`, `item_type`)
) COMMENT='用户对物品的加权交互（CF 输入）';

-- 物品流行度 + IDF 权重
DROP TABLE IF EXISTS `item_popularity`;
CREATE TABLE `item_popularity` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `item_type` VARCHAR(16) NOT NULL,
  `item_id` BIGINT NOT NULL,
  `user_count` INT NOT NULL,
  `idf_weight` DECIMAL(8,6) NOT NULL,
  `computed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_item` (`item_type`, `item_id`)
) COMMENT='物品流行度与 IDF 权重';
