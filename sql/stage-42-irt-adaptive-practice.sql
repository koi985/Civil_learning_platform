CREATE TABLE IF NOT EXISTS question_irt_stat (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  subject_name VARCHAR(64) NULL,
  knowledge_points VARCHAR(255) NULL,
  difficulty_b DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  sample_count INT NOT NULL DEFAULT 0,
  correct_rate DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  calibrated_at DATETIME NULL,
  calibration_version VARCHAR(32) NOT NULL DEFAULT 'rasch-v1',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_question_irt_stat_question (question_id),
  KEY idx_question_irt_stat_subject (subject_name)
);

CREATE TABLE IF NOT EXISTS user_ability_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  overall_theta DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  preferred_subject_theta DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  ability_confidence DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  calibrated_at DATETIME NULL,
  calibration_version VARCHAR(32) NOT NULL DEFAULT 'rasch-v1',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_ability_profile_user (user_id)
);

CREATE TABLE IF NOT EXISTS user_knowledge_ability (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  subject_name VARCHAR(64) NOT NULL,
  knowledge_point VARCHAR(128) NOT NULL,
  theta DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  theta_confidence DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  mastery_score_snapshot DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  calibrated_at DATETIME NULL,
  calibration_version VARCHAR(32) NOT NULL DEFAULT 'rasch-v1',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_knowledge_ability_user_kp (user_id, subject_name, knowledge_point),
  KEY idx_user_knowledge_ability_subject (subject_name)
);

CREATE TABLE IF NOT EXISTS recommend_practice_bundle (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  recommend_mode VARCHAR(32) NOT NULL,
  subject_name VARCHAR(64) NULL,
  province VARCHAR(32) NULL,
  knowledge_points VARCHAR(255) NULL,
  question_count INT NOT NULL DEFAULT 0,
  estimated_minutes INT NOT NULL DEFAULT 0,
  question_ids TEXT NOT NULL,
  theta_range VARCHAR(64) NULL,
  difficulty_range VARCHAR(64) NULL,
  ability_fit_score DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  explain_reasons TEXT NULL,
  source_strategy VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  generated_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_recommend_practice_bundle_user (user_id, status, generated_at),
  KEY idx_recommend_practice_bundle_mode (recommend_mode)
);
