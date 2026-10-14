USE civil_learning;

CREATE TABLE IF NOT EXISTS user_news_category_subscription (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_news_category (user_id, category_id)
);

INSERT IGNORE INTO user_news_category_subscription (user_id, category_id, created_at) VALUES
(1, 1, NOW()),
(1, 3, NOW()),
(2, 2, NOW()),
(5, 5, NOW());
