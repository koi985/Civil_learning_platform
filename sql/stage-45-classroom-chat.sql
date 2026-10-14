CREATE TABLE IF NOT EXISTS classroom_chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  sender_role VARCHAR(32) NOT NULL,
  sender_name VARCHAR(64) NOT NULL,
  message_type VARCHAR(16) NOT NULL DEFAULT 'TEXT',
  content VARCHAR(1000),
  file_id BIGINT,
  file_url VARCHAR(255),
  file_name VARCHAR(255),
  file_type VARCHAR(128),
  file_size BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_classroom_chat_classroom_id_id (classroom_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
