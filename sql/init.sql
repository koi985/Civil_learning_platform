CREATE DATABASE IF NOT EXISTS civil_learning DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE civil_learning;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS exam_answer;
DROP TABLE IF EXISTS exam_record;
DROP TABLE IF EXISTS classroom_exam_answer;
DROP TABLE IF EXISTS classroom_exam_submission;
DROP TABLE IF EXISTS classroom_exam_question;
DROP TABLE IF EXISTS classroom_exam_task;
DROP TABLE IF EXISTS classroom_homework_answer;
DROP TABLE IF EXISTS classroom_homework_submission;
DROP TABLE IF EXISTS classroom_homework_question;
DROP TABLE IF EXISTS classroom_homework;
DROP TABLE IF EXISTS exam_analysis_course;
DROP TABLE IF EXISTS user_daily_checkin;
DROP TABLE IF EXISTS user_question_favorite;
DROP TABLE IF EXISTS user_wrong_question;
DROP TABLE IF EXISTS practice_answer;
DROP TABLE IF EXISTS practice_record;
DROP TABLE IF EXISTS payment_log;
DROP TABLE IF EXISTS course_order_refund_log;
DROP TABLE IF EXISTS course_order_refund;
DROP TABLE IF EXISTS course_order_item;
DROP TABLE IF EXISTS course_order;
DROP TABLE IF EXISTS shopping_cart_item;
DROP TABLE IF EXISTS user_test_paper_question;
DROP TABLE IF EXISTS user_test_paper;
DROP TABLE IF EXISTS admin_growth_focus_follow;
DROP TABLE IF EXISTS teacher_growth_focus_follow;
DROP TABLE IF EXISTS admin_growth_dashboard_preference;
DROP TABLE IF EXISTS teacher_growth_dashboard_preference;
DROP TABLE IF EXISTS admin_growth_focus_follow_history;
DROP TABLE IF EXISTS teacher_growth_focus_follow_history;
DROP TABLE IF EXISTS user_growth_diagnosis_report;
DROP TABLE IF EXISTS user_growth_assessment_session;
DROP TABLE IF EXISTS user_growth_achievement;
DROP TABLE IF EXISTS user_knowledge_mastery;
DROP TABLE IF EXISTS user_daily_task;
DROP TABLE IF EXISTS user_study_plan;
DROP TABLE IF EXISTS user_course_learning_record;
DROP TABLE IF EXISTS user_course_chapter_progress;
DROP TABLE IF EXISTS user_course_study_progress;
DROP TABLE IF EXISTS user_course;
DROP TABLE IF EXISTS exam_question;
DROP TABLE IF EXISTS exam;
DROP TABLE IF EXISTS question_option;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS classroom_join_request;
DROP TABLE IF EXISTS classroom_material;
DROP TABLE IF EXISTS classroom_chat_message;
DROP TABLE IF EXISTS classroom_member;
DROP TABLE IF EXISTS classroom;
DROP TABLE IF EXISTS news_share_log;
DROP TABLE IF EXISTS news_comment;
DROP TABLE IF EXISTS user_news_category_subscription;
DROP TABLE IF EXISTS news;
DROP TABLE IF EXISTS news_category;
DROP TABLE IF EXISTS course_comment;
DROP TABLE IF EXISTS course_material;
DROP TABLE IF EXISTS course_chapter;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS recommend_pinned_resource;
DROP TABLE IF EXISTS recommend_feedback;
DROP TABLE IF EXISTS recommend_refresh_log;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS banner;
DROP TABLE IF EXISTS recommend_config;
DROP TABLE IF EXISTS system_config;
DROP TABLE IF EXISTS system_broadcast_message;
DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS sys_file_record;
DROP TABLE IF EXISTS teacher_profile;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  description_text VARCHAR(255),
  sort_no INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  nickname VARCHAR(64) NOT NULL,
  real_name VARCHAR(64),
  avatar_url VARCHAR(255),
  email VARCHAR(128),
  phone VARCHAR(32),
  province VARCHAR(32),
  exam_type VARCHAR(64),
  preferred_subject VARCHAR(64),
  vip_expire_at DATETIME,
  enabled TINYINT NOT NULL DEFAULT 1,
  locked TINYINT NOT NULL DEFAULT 0,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id)
);

CREATE TABLE teacher_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  gender VARCHAR(16),
  email VARCHAR(128),
  certificate_file_id BIGINT,
  intro TEXT,
  audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  audit_remark VARCHAR(255),
  audited_by BIGINT,
  audited_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_file_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_type VARCHAR(64),
  file_size BIGINT NOT NULL,
  biz_type VARCHAR(64),
  uploader_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  module_name VARCHAR(64) NOT NULL,
  operation_name VARCHAR(128) NOT NULL,
  request_method VARCHAR(16),
  request_uri VARCHAR(255),
  operator_id BIGINT,
  operator_name VARCHAR(64),
  operator_role VARCHAR(64),
  request_body TEXT,
  response_body TEXT,
  success TINYINT NOT NULL DEFAULT 1,
  error_message VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE banner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  link_url VARCHAR(255),
  sort_no INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  summary VARCHAR(255),
  content TEXT NOT NULL,
  read_count INT NOT NULL DEFAULT 0,
  published_at DATETIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE recommend_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  resource_id BIGINT NOT NULL,
  feedback_type VARCHAR(32) NOT NULL DEFAULT 'NOT_INTERESTED',
  source_page VARCHAR(32) NOT NULL DEFAULT 'RECOMMEND',
  reason_text VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recommend_pinned_resource (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_type VARCHAR(32) NOT NULL,
  resource_id BIGINT NOT NULL,
  display_title VARCHAR(255),
  display_reason VARCHAR(255),
  display_tags VARCHAR(255),
  action_text VARCHAR(64),
  region_scope VARCHAR(32) NOT NULL DEFAULT 'ALL',
  province VARCHAR(32),
  fallback_enabled TINYINT NOT NULL DEFAULT 1,
  sort_no INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  start_time DATETIME,
  end_time DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE system_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_group VARCHAR(32) NOT NULL,
  config_key VARCHAR(64) NOT NULL,
  config_name VARCHAR(128) NOT NULL,
  config_value VARCHAR(1000),
  value_type VARCHAR(32) NOT NULL DEFAULT 'STRING',
  description_text VARCHAR(255),
  sort_no INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_system_config_group_key (config_group, config_key)
);

CREATE TABLE system_broadcast_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  scope VARCHAR(32) NOT NULL DEFAULT 'ALL',
  sender_id BIGINT,
  sender_name VARCHAR(64),
  receiver_count INT NOT NULL DEFAULT 0,
  sent_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE recommend_refresh_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  refresh_source VARCHAR(32) NOT NULL DEFAULT 'RECOMMEND',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  cover_file_id BIGINT,
  cover_url VARCHAR(255) NOT NULL,
  teacher_name VARCHAR(64) NOT NULL,
  teacher_avatar VARCHAR(255),
  region_scope VARCHAR(32) NOT NULL DEFAULT 'NATIONAL',
  province VARCHAR(32) NOT NULL,
  subject_name VARCHAR(64) NOT NULL,
  exam_type VARCHAR(64) NOT NULL,
  course_type VARCHAR(32) NOT NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  original_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  tags VARCHAR(255),
  learn_count INT NOT NULL DEFAULT 0,
  description TEXT,
  highlights TEXT,
  target_users VARCHAR(255),
  validity_days INT NOT NULL DEFAULT 365,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE course_chapter (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  chapter_type VARCHAR(32) NOT NULL DEFAULT 'RECORDED',
  duration_minutes INT NOT NULL DEFAULT 0,
  sort_no INT NOT NULL DEFAULT 0,
  live_start_time DATETIME,
  video_file_id BIGINT NULL,
  video_url VARCHAR(255),
  trial_available TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE course_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  material_type VARCHAR(32) NOT NULL,
  file_id BIGINT NOT NULL,
  sort_no INT NOT NULL DEFAULT 0
);

CREATE TABLE course_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  rating INT NOT NULL,
  content VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE news_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL UNIQUE,
  sort_no INT NOT NULL DEFAULT 0
);

CREATE TABLE news (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  summary VARCHAR(255),
  cover_file_id BIGINT,
  cover_url VARCHAR(255),
  source_name VARCHAR(128),
  author_name VARCHAR(64),
  content LONGTEXT NOT NULL,
  tags VARCHAR(255),
  read_count INT NOT NULL DEFAULT 0,
  share_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  published_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE news_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  news_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE news_share_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  news_id BIGINT NOT NULL,
  user_id BIGINT,
  share_channel VARCHAR(32) NOT NULL DEFAULT 'LINK',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE user_news_category_subscription (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_news_category (user_id, category_id)
);

CREATE TABLE classroom (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  subject_name VARCHAR(64) NOT NULL,
  teacher_id BIGINT NOT NULL,
  teacher_name VARCHAR(64) NOT NULL,
  cover_file_id BIGINT,
  cover_url VARCHAR(255) NOT NULL,
  intro TEXT,
  capacity INT NOT NULL DEFAULT 100,
  is_public TINYINT NOT NULL DEFAULT 1,
  invite_code VARCHAR(6),
  member_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_classroom_invite_code (invite_code)
);

CREATE TABLE classroom_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_code VARCHAR(32) NOT NULL DEFAULT 'STUDENT',
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_classroom_member (classroom_id, user_id)
);

CREATE TABLE classroom_chat_message (
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
);

CREATE TABLE classroom_join_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  remark VARCHAR(255),
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE classroom_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_id BIGINT NOT NULL,
  material_category VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL,
  file_id BIGINT NOT NULL,
  description VARCHAR(500),
  sort_no INT NOT NULL DEFAULT 1,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE classroom_homework (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  description TEXT,
  attachment_file_id BIGINT,
  question_count INT NOT NULL DEFAULT 0,
  total_score INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  deadline_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE classroom_homework_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  homework_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  score INT NOT NULL DEFAULT 1,
  sort_no INT NOT NULL DEFAULT 0
);

CREATE TABLE classroom_homework_submission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  homework_id BIGINT NOT NULL,
  classroom_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  score INT NOT NULL DEFAULT 0,
  objective_score INT NOT NULL DEFAULT 0,
  question_count INT NOT NULL DEFAULT 0,
  answered_count INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  duration_seconds INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
  review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  review_remark VARCHAR(255),
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  started_at DATETIME,
  submitted_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE classroom_homework_answer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  homework_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  user_answer TEXT,
  correct_answer TEXT,
  is_correct TINYINT NOT NULL DEFAULT 0,
  score INT NOT NULL DEFAULT 0,
  teacher_comment VARCHAR(500)
);

CREATE TABLE classroom_exam_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  description TEXT,
  question_count INT NOT NULL DEFAULT 0,
  total_score INT NOT NULL DEFAULT 0,
  duration_minutes INT NOT NULL DEFAULT 60,
  anti_cheat_enabled TINYINT NOT NULL DEFAULT 0,
  anti_cheat_notice VARCHAR(500),
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  start_time DATETIME,
  end_time DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE classroom_exam_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_exam_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  score INT NOT NULL DEFAULT 1,
  sort_no INT NOT NULL DEFAULT 0
);

CREATE TABLE classroom_exam_submission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_exam_id BIGINT NOT NULL,
  classroom_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  score INT NOT NULL DEFAULT 0,
  question_count INT NOT NULL DEFAULT 0,
  answered_count INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  duration_seconds INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
  review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  review_remark VARCHAR(255),
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  started_at DATETIME,
  submitted_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE classroom_exam_answer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  classroom_exam_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  user_answer TEXT,
  correct_answer TEXT,
  is_correct TINYINT NOT NULL DEFAULT 0,
  score INT NOT NULL DEFAULT 0,
  teacher_comment VARCHAR(500)
);

CREATE TABLE question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  subject_name VARCHAR(64) NOT NULL,
  category_name VARCHAR(64) NOT NULL,
  province VARCHAR(32) NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  difficulty_level VARCHAR(32) NOT NULL,
  score INT NOT NULL DEFAULT 1,
  source_name VARCHAR(64) NOT NULL,
  material TEXT,
  stem TEXT NOT NULL,
  analysis TEXT NOT NULL,
  answer_value VARCHAR(255) NOT NULL,
  knowledge_points VARCHAR(255),
  video_explain_title VARCHAR(128),
  video_explain_url VARCHAR(255),
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_daily_checkin (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  checkin_date DATE NOT NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  source_record_id BIGINT,
  streak_days INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_daily_checkin (user_id, checkin_date),
  KEY idx_user_daily_checkin_user_created (user_id, created_at)
);

CREATE TABLE user_question_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_question_favorite (user_id, question_id)
);

CREATE TABLE question_option (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  option_key VARCHAR(8) NOT NULL,
  option_content VARCHAR(500) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0
);

CREATE TABLE exam (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  intro TEXT,
  province VARCHAR(32) NOT NULL,
  exam_type VARCHAR(64) NOT NULL,
  season_name VARCHAR(64) NOT NULL,
  subject_name VARCHAR(64) NOT NULL,
  duration_minutes INT NOT NULL DEFAULT 120,
  total_score INT NOT NULL DEFAULT 100,
  question_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  ranking_enabled TINYINT NOT NULL DEFAULT 1,
  start_time DATETIME,
  end_time DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE exam_analysis_course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  recommend_reason VARCHAR(255),
  sort_no INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_exam_analysis_course (exam_id, course_id)
);

CREATE TABLE exam_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  score INT NOT NULL DEFAULT 2,
  sort_no INT NOT NULL DEFAULT 0
);

CREATE TABLE user_course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  source_order_id BIGINT,
  expire_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_course (user_id, course_id)
);

CREATE TABLE user_course_study_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  current_chapter_id BIGINT,
  completed_chapter_count INT NOT NULL DEFAULT 0,
  progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  total_study_seconds INT NOT NULL DEFAULT 0,
  last_study_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_course_study (user_id, course_id)
);

CREATE TABLE user_course_chapter_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  chapter_id BIGINT NOT NULL,
  study_seconds INT NOT NULL DEFAULT 0,
  progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  completed TINYINT NOT NULL DEFAULT 0,
  first_started_at DATETIME,
  last_study_at DATETIME,
  completed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_course_chapter_progress (user_id, course_id, chapter_id)
);

CREATE TABLE user_course_learning_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  chapter_id BIGINT NOT NULL,
  watch_seconds INT NOT NULL DEFAULT 0,
  progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  completed TINYINT NOT NULL DEFAULT 0,
  action_type VARCHAR(32) NOT NULL DEFAULT 'STUDY',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_study_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_name VARCHAR(128) NOT NULL,
  target_exam_type VARCHAR(64) NOT NULL,
  subject_name VARCHAR(64) NOT NULL,
  daily_study_minutes INT NOT NULL DEFAULT 60,
  daily_question_count INT NOT NULL DEFAULT 20,
  weekly_exam_count INT NOT NULL DEFAULT 1,
  duration_days INT NOT NULL DEFAULT 30,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  focus_knowledge_points VARCHAR(500),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  archived_at DATETIME,
  total_task_count INT NOT NULL DEFAULT 0,
  completed_task_count INT NOT NULL DEFAULT 0,
  completion_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_user_study_plan_user_status (user_id, status)
);

CREATE TABLE user_daily_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  task_date DATE NOT NULL,
  task_type VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL,
  description_text VARCHAR(255),
  target_value INT NOT NULL DEFAULT 0,
  completed_value INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  action_path VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_user_daily_task_user_date (user_id, task_date),
  KEY idx_user_daily_task_plan (plan_id)
);

CREATE TABLE user_knowledge_mastery (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  subject_name VARCHAR(64) NOT NULL,
  knowledge_point VARCHAR(128) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  wrong_count INT NOT NULL DEFAULT 0,
  pending_wrong_count INT NOT NULL DEFAULT 0,
  accuracy_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  mastery_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  mastery_confidence DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  priority_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  mastery_level VARCHAR(32) NOT NULL DEFAULT 'WEAK',
  knowledge_status VARCHAR(32) NOT NULL DEFAULT 'TO_DIAGNOSE',
  last_practiced_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_knowledge_mastery (user_id, subject_name, knowledge_point)
);

CREATE TABLE user_growth_achievement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_id BIGINT,
  task_id BIGINT,
  achievement_type VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL,
  description_text VARCHAR(255),
  achievement_value INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_growth_achievement_user (user_id),
  KEY idx_user_growth_achievement_plan (plan_id),
  KEY idx_user_growth_achievement_task (task_id)
);

CREATE TABLE user_growth_diagnosis_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_id BIGINT,
  diagnosis_date DATE NOT NULL,
  diagnosis_stage VARCHAR(32) NOT NULL DEFAULT 'TRACKING',
  vitality_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  plan_completion_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  knowledge_mastery_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  test_paper_average_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  predicted_pass_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  prediction_confidence DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  formula_version VARCHAR(64) NOT NULL DEFAULT 'growth-pass-v2',
  weak_knowledge_points VARCHAR(500),
  recommendation_text VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_growth_diagnosis (user_id, plan_id, diagnosis_date),
  KEY idx_user_growth_diagnosis_user_date (user_id, diagnosis_date)
);

CREATE TABLE user_growth_assessment_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_id BIGINT,
  practice_record_id BIGINT NOT NULL,
  assessment_type VARCHAR(32) NOT NULL DEFAULT 'ENTRY',
  question_count INT NOT NULL DEFAULT 20,
  predicted_pass_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  predicted_confidence DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  prediction_stage VARCHAR(32) NOT NULL DEFAULT 'TRACKING',
  weak_knowledge_points VARCHAR(500),
  recommendation_text VARCHAR(500),
  assessment_status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_growth_assessment_record (practice_record_id),
  KEY idx_user_growth_assessment_user_created (user_id, created_at),
  KEY idx_user_growth_assessment_user_status (user_id, assessment_status)
);

CREATE TABLE admin_growth_focus_follow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_id BIGINT,
  latest_diagnosis_id BIGINT,
  follow_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  priority_level VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
  remark VARCHAR(500),
  next_follow_up_at DATETIME,
  created_by BIGINT,
  updated_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_growth_focus_follow (user_id, plan_id),
  KEY idx_admin_growth_focus_follow_status (follow_status)
);

CREATE TABLE teacher_growth_focus_follow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  plan_id BIGINT,
  latest_diagnosis_id BIGINT,
  follow_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  priority_level VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
  remark VARCHAR(500),
  next_follow_up_at DATETIME,
  created_by BIGINT,
  updated_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_teacher_growth_focus_follow (classroom_id, user_id, plan_id),
  KEY idx_teacher_growth_focus_follow_status (follow_status)
);

CREATE TABLE admin_growth_dashboard_preference (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  segment VARCHAR(32),
  min_completion_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  follow_status VARCHAR(32),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE teacher_growth_dashboard_preference (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  classroom_id BIGINT,
  segment VARCHAR(32),
  follow_status VARCHAR(32),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE admin_growth_focus_follow_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_follow_id BIGINT,
  user_id BIGINT NOT NULL,
  plan_id BIGINT,
  latest_diagnosis_id BIGINT,
  follow_status VARCHAR(32) NOT NULL,
  priority_level VARCHAR(32) NOT NULL,
  remark VARCHAR(500),
  next_follow_up_at DATETIME,
  operator_id BIGINT,
  action_type VARCHAR(32) NOT NULL DEFAULT 'SAVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_admin_growth_focus_follow_history_user (user_id, created_at)
);

CREATE TABLE teacher_growth_focus_follow_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_follow_id BIGINT,
  classroom_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  plan_id BIGINT,
  latest_diagnosis_id BIGINT,
  follow_status VARCHAR(32) NOT NULL,
  priority_level VARCHAR(32) NOT NULL,
  remark VARCHAR(500),
  next_follow_up_at DATETIME,
  operator_id BIGINT,
  action_type VARCHAR(32) NOT NULL DEFAULT 'SAVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_teacher_growth_focus_follow_history_classroom (classroom_id, created_at)
);

CREATE TABLE user_test_paper (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  subject_name VARCHAR(64),
  category_name VARCHAR(64),
  difficulty_level VARCHAR(32),
  question_count INT NOT NULL DEFAULT 0,
  generate_mode VARCHAR(32) NOT NULL DEFAULT 'WEAKNESS',
  paper_type VARCHAR(32) NOT NULL DEFAULT 'WEAKNESS',
  knowledge_points VARCHAR(255),
  target_knowledge_points VARCHAR(255),
  recommendation_reason VARCHAR(255),
  generation_snapshot TEXT,
  difficulty_distribution VARCHAR(255),
  source_strategy VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_test_paper_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  test_paper_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  question_score INT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_test_paper_question (test_paper_id, question_id)
);

CREATE TABLE shopping_cart_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  selected TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_cart_user_course (user_id, course_id)
);

CREATE TABLE course_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  item_count INT NOT NULL DEFAULT 0,
  order_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  payment_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
  payment_channel VARCHAR(32),
  remark VARCHAR(255),
  paid_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE course_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  course_title VARCHAR(128) NOT NULL,
  course_cover_url VARCHAR(255),
  unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  quantity INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  payment_channel VARCHAR(32) NOT NULL,
  payment_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  payment_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
  transaction_no VARCHAR(64) NOT NULL,
  paid_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE course_order_refund (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  refund_no VARCHAR(64) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  refund_reason VARCHAR(500) NOT NULL,
  refund_status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
  requested_at DATETIME NOT NULL,
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  finished_at DATETIME,
  review_remark VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_course_order_refund_status (refund_status),
  KEY idx_course_order_refund_user (user_id)
);

CREATE TABLE course_order_refund_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  refund_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  from_status VARCHAR(32),
  to_status VARCHAR(32) NOT NULL,
  operator_id BIGINT,
  operator_role VARCHAR(32),
  operator_name VARCHAR(64),
  remark VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_course_order_refund_log_refund (refund_id, created_at)
);

CREATE TABLE practice_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  practice_type VARCHAR(32) NOT NULL DEFAULT 'QUESTION_BANK',
  practice_mode VARCHAR(32) NOT NULL DEFAULT 'RANDOM',
  title VARCHAR(128) NOT NULL,
  subject_name VARCHAR(64),
  province VARCHAR(32),
  question_count INT NOT NULL DEFAULT 0,
  answered_count INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  score INT NOT NULL DEFAULT 0,
  accuracy_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  duration_seconds INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
  submitted_at DATETIME,
  source_snapshot VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE practice_answer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  practice_record_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  user_answer VARCHAR(255),
  correct_answer VARCHAR(255) NOT NULL,
  is_correct TINYINT NOT NULL DEFAULT 0,
  marked TINYINT NOT NULL DEFAULT 0,
  spent_seconds INT NOT NULL DEFAULT 0,
  question_score INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_wrong_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  latest_source_type VARCHAR(32) NOT NULL,
  latest_record_id BIGINT NOT NULL,
  latest_answer TEXT,
  removed TINYINT NOT NULL DEFAULT 0,
  wrong_count INT NOT NULL DEFAULT 1,
  last_wrong_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_wrong_question (user_id, question_id)
);

CREATE TABLE exam_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  total_score INT NOT NULL DEFAULT 0,
  question_count INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  score INT NOT NULL DEFAULT 0,
  duration_seconds INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
  ranking_enabled TINYINT NOT NULL DEFAULT 1,
  started_at DATETIME NOT NULL,
  submitted_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE exam_answer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_record_id BIGINT NOT NULL,
  exam_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  user_answer VARCHAR(255),
  correct_answer VARCHAR(255) NOT NULL,
  is_correct TINYINT NOT NULL DEFAULT 0,
  score INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recommend_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(64) NOT NULL UNIQUE,
  config_name VARCHAR(128) NOT NULL,
  config_value DECIMAL(10,4) NOT NULL,
  description_text VARCHAR(255),
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO sys_role (id, code, name, description_text, sort_no, enabled) VALUES
(1, 'USER', '普通用户', '平台默认学员角色', 1, 1),
(2, 'TEACHER', '教师', '审核通过的教师角色', 2, 1),
(3, 'ADMIN', '管理员', '平台后台管理角色', 3, 1),
(4, 'OPERATOR', '运营专员', '用于平台治理与内容运营的扩展角色', 4, 1);

INSERT INTO sys_user (id, username, password, nickname, real_name, avatar_url, email, phone, province, exam_type, preferred_subject, vip_expire_at, enabled, locked, deleted) VALUES
(1, 'admin', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '系统管理员', '平台管理员', 'https://dummyimage.com/120x120/1d4ed8/ffffff&text=A', 'admin@civil.local', '13800000001', '全国', '管理端', '平台治理', '2030-12-31 23:59:59', 1, 0, 0),
(2, 'student1', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '上岸冲刺生', '李同学', 'https://dummyimage.com/120x120/f97316/ffffff&text=U1', 'student1@civil.local', '13800000002', '江苏', '省考', '行测', '2027-12-31 23:59:59', 1, 0, 0),
(3, 'teacher1', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '言语金牌讲师', '王老师', 'https://dummyimage.com/120x120/0f766e/ffffff&text=T1', 'teacher1@civil.local', '13800000003', '浙江', '国考', '言语理解', '2028-12-31 23:59:59', 1, 0, 0),
(4, 'teacher2', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '申论高分老师', '陈老师', 'https://dummyimage.com/120x120/7c3aed/ffffff&text=T2', 'teacher2@civil.local', '13800000004', '江苏', '省考', '申论', '2028-12-31 23:59:59', 1, 0, 0),
(5, 'student2', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '模考进阶生', '张同学', 'https://dummyimage.com/120x120/2563eb/ffffff&text=U2', 'student2@civil.local', '13800000005', '广东', '事业编', '判断推理', '2027-06-30 23:59:59', 1, 0, 0);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 3),
(2, 1),
(3, 2),
(4, 2),
(5, 1);

INSERT INTO sys_file_record (id, original_name, stored_name, file_path, file_url, file_type, file_size, biz_type, uploader_id) VALUES
(1, 'teacher1-certificate.pdf', 'teacher1-certificate.pdf', 'D:/Civil_learning_platform/upload/certificate/teacher1-certificate.pdf', '/api/files/view/1', 'application/pdf', 123456, 'TEACHER_CERTIFICATE', 3),
(2, 'teacher2-certificate.pdf', 'teacher2-certificate.pdf', 'D:/Civil_learning_platform/upload/certificate/teacher2-certificate.pdf', '/api/files/view/2', 'application/pdf', 123456, 'TEACHER_CERTIFICATE', 4),
(3, 'course-material-1.pdf', 'course-material-1.pdf', 'D:/Civil_learning_platform/upload/course/course-material-1.pdf', '/api/files/view/3', 'application/pdf', 45213, 'COURSE_MATERIAL', 1),
(4, 'course-material-2.pdf', 'course-material-2.pdf', 'D:/Civil_learning_platform/upload/course/course-material-2.pdf', '/api/files/view/4', 'application/pdf', 38567, 'COURSE_MATERIAL', 1),
(5, 'homework-guide.pdf', 'homework-guide.pdf', 'D:/Civil_learning_platform/upload/homework/homework-guide.pdf', '/api/files/view/5', 'application/pdf', 29876, 'HOMEWORK', 3),
(6, 'teacher-demo-cover.jpg', 'teacher-demo-cover.jpg', 'D:/Civil_learning_platform/upload/classroom/teacher-demo-cover.jpg', '/api/files/view/6', 'image/jpeg', 22134, 'CLASSROOM', 3),
(7, 'classroom-trial-video.mp4', 'classroom-trial-video.mp4', 'D:/Civil_learning_platform/upload/classroom/classroom-trial-video.mp4', '/api/files/view/7', 'video/mp4', 1024000, 'CLASSROOM_MATERIAL', 3);

INSERT INTO teacher_profile (user_id, gender, email, certificate_file_id, intro, audit_status, audit_remark, audited_by, audited_at) VALUES
(3, '男', 'teacher1@civil.local', 1, '专注言语理解与表达十年，擅长体系化提分。', 'APPROVED', '资质审核通过', 1, NOW()),
(4, '女', 'teacher2@civil.local', 2, '主讲申论写作与综合分析，注重实战训练。', 'PENDING', NULL, NULL, NULL);

INSERT INTO banner (title, image_url, link_url, sort_no, enabled) VALUES
('2026 国考系统班', 'https://dummyimage.com/1200x360/1d4ed8/ffffff&text=国考系统班', '/course/detail/1', 1, 1),
('省考刷题冲刺营', 'https://dummyimage.com/1200x360/f97316/ffffff&text=省考冲刺营', '/course/detail/2', 2, 1),
('万人模考季', 'https://dummyimage.com/1200x360/0f766e/ffffff&text=模考季', '/exam', 3, 1);

INSERT INTO notice (title, summary, content, read_count, published_at, status, created_by) VALUES
('系统上线公告', '平台基础功能已开放体验', '当前版本已支持课程、题库、模考、班级与教师申请等主流程。', 186, NOW(), 'PUBLISHED', 1),
('五一模考报名提醒', '本周开放全国联考模拟报名', '请在模考专区关注报名开放时间，建议提前完成账户实名认证。', 132, DATE_SUB(NOW(), INTERVAL 1 DAY), 'PUBLISHED', 1),
('课程资料下载说明', '资料统一通过上传中心管理', '已购课程用户可在课程详情页和学习页下载讲义、题本与资料。', 96, DATE_SUB(NOW(), INTERVAL 2 DAY), 'PUBLISHED', 1),
('阶段六运营公告', '推荐刷新、不感兴趣反馈和公告中心已上线', '本轮已补齐公告列表/详情消费链路，并支持推荐刷新与不感兴趣反馈真实入库。', 48, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'PUBLISHED', 1);

INSERT INTO course (id, title, cover_url, teacher_name, teacher_avatar, province, subject_name, exam_type, course_type, price, original_price, tags, learn_count, description, highlights, target_users, validity_days, status) VALUES
(1, '2026 国考行测系统班', 'https://dummyimage.com/600x360/1d4ed8/ffffff&text=行测系统班', '王老师', 'https://dummyimage.com/120x120/0f766e/ffffff&text=T1', '全国', '行测', '国考', 'LIVE', 1999.00, 2599.00, '直播,行测,系统班', 1280, '围绕常识、言语、判断、数量、资料分析搭建完整提分路径。', '直播陪学 + 讲义题本 + 阶段测评', '适合零基础到冲刺学员', 365, 'PUBLISHED'),
(2, '江苏省考申论高分班', 'https://dummyimage.com/600x360/f97316/ffffff&text=申论高分班', '陈老师', 'https://dummyimage.com/120x120/7c3aed/ffffff&text=T2', '江苏', '申论', '省考', 'RECORDED', 899.00, 1299.00, '录播,申论,写作提升', 860, '聚焦归纳概括、综合分析、提出对策和大作文。', '录播精讲 + 范文素材 + 批改模板', '适合有一定基础的提分学员', 180, 'PUBLISHED'),
(3, '广东事业单位面试冲刺营', 'https://dummyimage.com/600x360/0f766e/ffffff&text=面试冲刺', '王老师', 'https://dummyimage.com/120x120/0f766e/ffffff&text=T1', '广东', '面试', '事业单位', 'LIVE', 499.00, 699.00, '直播,事业单位,面试,冲刺', 530, '结构化面试热点拆解与答题框架训练。', '热点预测 + 模拟答辩', '适合即将进面的考生', 90, 'PUBLISHED'),
(4, '时政热点公开课', 'https://dummyimage.com/600x360/2563eb/ffffff&text=时政公开课', '王老师', 'https://dummyimage.com/120x120/0f766e/ffffff&text=T1', '全国', '时政', '通用', 'RECORDED', 0.00, 0.00, '免费,时政,试听', 2160, '高频时政热点快速梳理。', '免费试听 + 随堂练习', '适合所有备考用户', 30, 'PUBLISHED'),
(5, '2026 选调生综合能力提升班', 'https://dummyimage.com/600x360/7c3aed/ffffff&text=选调提升班', '李老师', 'https://dummyimage.com/120x120/7c3aed/ffffff&text=T3', '全国', '综合能力', '选调', 'LIVE', 1299.00, 1699.00, '直播,选调,综合能力', 742, '围绕选调生笔试高频模块与基层治理写作能力做系统训练。', '政策梳理 + 写作训练 + 阶段测评', '适合备考选调生的应届与基层项目考生', 240, 'PUBLISHED'),
(6, '结构化面试全真演练课', 'https://dummyimage.com/600x360/db2777/ffffff&text=面试演练课', '赵老师', 'https://dummyimage.com/120x120/db2777/ffffff&text=T4', '全国', '面试', '面试', 'LIVE', 699.00, 999.00, '直播,面试,全真演练', 1180, '覆盖结构化面试审题、破题、表达与追问应对。', '小班演练 + 点评复盘 + 热点题库', '适合笔试通过后进入面试备考阶段的学员', 120, 'PUBLISHED'),
(7, '军队文职公共科目系统班', 'https://dummyimage.com/600x360/0f766e/ffffff&text=军队文职系统班', '周老师', 'https://dummyimage.com/120x120/0f766e/ffffff&text=T5', '全国', '公共科目', '军队文职', 'RECORDED', 799.00, 1199.00, '录播,军队文职,公共科目', 624, '系统讲解军队文职公共科目基础知识、岗位能力与考前冲刺。', '知识框架 + 真题精讲 + 冲刺清单', '适合第一次备考军队文职公共科目的学员', 180, 'PUBLISHED');

INSERT INTO course_chapter (course_id, title, chapter_type, duration_minutes, sort_no, live_start_time, video_url, trial_available) VALUES
(1, '开营直播与备考规划', 'LIVE', 120, 1, DATE_ADD(NOW(), INTERVAL 2 DAY), 'https://example.com/video/course1-1', 1),
(1, '言语理解高频技巧', 'RECORDED', 95, 2, NULL, 'https://example.com/video/course1-2', 0),
(1, '资料分析速算专题', 'RECORDED', 90, 3, NULL, 'https://example.com/video/course1-3', 0),
(2, '归纳概括高分模板', 'RECORDED', 80, 1, NULL, 'https://example.com/video/course2-1', 1),
(2, '综合分析案例拆解', 'RECORDED', 88, 2, NULL, 'https://example.com/video/course2-2', 0),
(3, '面试答题结构搭建', 'LIVE', 100, 1, DATE_ADD(NOW(), INTERVAL 5 DAY), 'https://example.com/video/course3-1', 1),
(4, '时政热点速览', 'RECORDED', 45, 1, NULL, 'https://example.com/video/course4-1', 1),
(5, '选调考试政策与备考路径', 'LIVE', 90, 1, DATE_ADD(NOW(), INTERVAL 3 DAY), 'https://example.com/video/course5-1', 1),
(5, '基层治理写作专题', 'RECORDED', 85, 2, NULL, 'https://example.com/video/course5-2', 0),
(6, '结构化面试答题框架', 'LIVE', 100, 1, DATE_ADD(NOW(), INTERVAL 4 DAY), 'https://example.com/video/course6-1', 1),
(6, '追问与现场表达训练', 'RECORDED', 75, 2, NULL, 'https://example.com/video/course6-2', 0),
(7, '军队文职公共科目导学', 'RECORDED', 70, 1, NULL, 'https://example.com/video/course7-1', 1),
(7, '岗位能力真题精讲', 'RECORDED', 95, 2, NULL, 'https://example.com/video/course7-2', 0);

INSERT INTO course_material (course_id, title, material_type, file_id, sort_no) VALUES
(1, '行测讲义 PDF', 'PDF', 3, 1),
(1, '阶段刷题题本', 'WORD', 4, 2),
(2, '申论高分素材包', 'PDF', 3, 1),
(3, '面试金句整理', 'PDF', 4, 1),
(5, '选调综合能力讲义', 'PDF', 3, 1),
(6, '面试热点题本', 'PDF', 4, 1),
(7, '军队文职公共科目清单', 'PDF', 3, 1);

INSERT INTO course_comment (course_id, user_id, rating, content) VALUES
(1, 2, 5, '老师讲得很系统，题型拆解非常清楚。'),
(1, 5, 4, '适合打基础，直播陪练很有帮助。'),
(2, 2, 5, '申论模板和范文素材非常实用。'),
(4, 5, 4, '免费课内容紧凑，适合快速复习。'),
(5, 2, 5, '选调政策讲得清楚，写作训练很实用。'),
(6, 5, 5, '面试演练节奏很接近真实考场。'),
(7, 2, 4, '公共科目框架清晰，适合系统复习。');

INSERT INTO news_category (id, name, sort_no) VALUES
(1, '国考', 1),
(2, '省考', 2),
(3, '选调', 3),
(4, '事业单位', 4),
(5, '军队文职', 5),
(6, '面试', 6);

INSERT INTO news (id, category_id, title, summary, cover_url, source_name, author_name, content, tags, read_count, share_count, status, published_at) VALUES
(1, 1, '2026 国考公告发布时间预测', '结合历年节奏分析公告发布时间节点。', 'https://dummyimage.com/600x360/1d4ed8/ffffff&text=国考公告', '平台原创', '编辑部', '<p>根据近五年国考节奏，公告通常会在十月中下旬发布。</p><p>建议考生提前完成岗位方向筛选与专项提升。</p>', '国考,公告,时间线', 1280, 86, 'PUBLISHED', NOW()),
(2, 1, '国考行测五大模块复习优先级', '按常识、言语、判断、资料、数量拆解复习投入。', 'https://dummyimage.com/600x360/2563eb/ffffff&text=国考行测', '平台原创', '教研中心', '<p>国考行测备考要先稳定高频模块，再用套卷训练速度。</p><p>建议资料分析和判断推理优先形成稳定得分区间。</p>', '国考,行测,备考', 1120, 73, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(3, 1, '国考申论材料阅读的三个层次', '从主题、问题、对策三个层次提升材料处理效率。', 'https://dummyimage.com/600x360/1e40af/ffffff&text=国考申论', '平台原创', '申论组', '<p>申论材料阅读不是逐句摘抄，而是围绕作答任务提炼信息。</p>', '国考,申论,材料阅读', 840, 44, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(4, 2, '省考联考时间线与复习节奏', '梳理省考联考常见节点，帮助安排阶段复习。', 'https://dummyimage.com/600x360/f97316/ffffff&text=省考时间线', '平台原创', '资讯组', '<p>省考联考备考要兼顾本省考情和全国联考共性。</p><p>报名、资格审核、笔试和成绩发布节点需要提前关注。</p>', '省考,联考,时间线', 980, 52, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 2, '省考申论高频题型答题框架', '归纳概括、综合分析、提出对策和文章写作框架整理。', 'https://dummyimage.com/600x360/ea580c/ffffff&text=省考申论', '平台原创', '陈老师', '<p>省考申论要把题型框架练熟，再结合本省热点补充表达。</p>', '省考,申论,框架', 760, 37, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 2, '省考行测资料分析提速清单', '从列式、估算、定位三个维度讲清提速路径。', 'https://dummyimage.com/600x360/f59e0b/ffffff&text=资料分析', '平台原创', '王老师', '<p>资料分析最怕无序，先定位材料，再估算，再校验。</p>', '省考,行测,资料分析', 780, 51, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(7, 3, '选调生考试政策与报名条件梳理', '面向应届生和基层项目人员整理常见报考条件。', 'https://dummyimage.com/600x360/7c3aed/ffffff&text=选调政策', '平台原创', '选调研究组', '<p>选调生考试要重点关注身份条件、院校范围、党员和学生干部经历等要求。</p>', '选调,政策,报名', 690, 32, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(8, 3, '选调综合能力写作素材怎么积累', '围绕基层治理、乡村振兴和公共服务建立素材库。', 'https://dummyimage.com/600x360/6d28d9/ffffff&text=选调写作', '平台原创', '内容组', '<p>选调写作素材应贴近基层场景，避免只堆砌宏观表述。</p>', '选调,写作,基层治理', 620, 28, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(9, 3, '选调面谈常见问题与准备方向', '从自我认知、岗位理解和基层适应三个方向准备。', 'https://dummyimage.com/600x360/8b5cf6/ffffff&text=选调面谈', '平台原创', '面试组', '<p>选调面谈更看重价值认同和基层服务意愿，回答要结合真实经历。</p>', '选调,面谈,面试', 560, 19, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(10, 4, '事业单位联考职测备考策略', '常识、言语、判断和资料分析的稳定提分方案。', 'https://dummyimage.com/600x360/0f766e/ffffff&text=事业单位职测', '平台原创', '事业单位组', '<p>事业单位职测备考要重视题型稳定性，并通过限时练习提升速度。</p>', '事业单位,职测,联考', 740, 33, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(11, 4, '事业单位综合应用能力作答要点', '按不同类别梳理材料处理和任务表达重点。', 'https://dummyimage.com/600x360/059669/ffffff&text=综合应用', '平台原创', '教研中心', '<p>综合应用能力作答要先识别角色和任务，再组织条理化答案。</p>', '事业单位,综合应用能力,备考', 650, 24, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(12, 4, '江苏省事业单位招考信息汇总', '汇总最近一周可报名岗位及考试节点。', 'https://dummyimage.com/600x360/2563eb/ffffff&text=事业单位招考', '平台原创', '资讯组', '<p>本周江苏多个地区发布事业单位招录计划，建议考生关注报名截止时间。</p>', '事业单位,江苏,招考', 540, 27, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(13, 5, '军队文职公共科目考试内容拆解', '梳理基础知识和岗位能力两个主要部分。', 'https://dummyimage.com/600x360/155e75/ffffff&text=军队文职', '平台原创', '军队文职组', '<p>军队文职公共科目复习应兼顾公共基础与岗位能力，避免单点突击。</p>', '军队文职,公共科目,考试内容', 710, 35, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(14, 5, '军队文职岗位表筛选方法', '从专业、学历、工作地点和岗位条件筛选适配岗位。', 'https://dummyimage.com/600x360/0e7490/ffffff&text=岗位筛选', '平台原创', '资讯组', '<p>岗位筛选要先锁定硬性条件，再比较竞争热度和发展方向。</p>', '军队文职,岗位表,报名', 590, 22, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 11 DAY)),
(15, 5, '军队文职考前冲刺复盘清单', '用清单方式整理公共科目考前复盘重点。', 'https://dummyimage.com/600x360/0891b2/ffffff&text=文职冲刺', '平台原创', '教研中心', '<p>考前冲刺要回到错题、公式、常识高频点和时间分配。</p>', '军队文职,冲刺,复盘', 520, 18, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(16, 6, '结构化面试答题框架搭建', '从审题、破题、分点和收束四步建立稳定表达。', 'https://dummyimage.com/600x360/db2777/ffffff&text=结构化面试', '平台原创', '赵老师', '<p>结构化面试要先保证逻辑清楚，再逐步提升表达亮点。</p>', '面试,结构化,答题框架', 900, 46, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 13 DAY)),
(17, 6, '面试热点：基层治理现代化案例积累', '适合申论与面试共用的高频素材整理。', 'https://dummyimage.com/600x360/be185d/ffffff&text=面试热点', '平台原创', '内容组', '<p>基层治理案例可从数字治理、社区服务、青年参与等方向积累。</p>', '面试,热点,基层治理', 960, 42, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(18, 6, '无领导小组讨论入门训练要点', '围绕角色定位、发言节奏和总结陈词进行训练。', 'https://dummyimage.com/600x360/e11d48/ffffff&text=无领导面试', '平台原创', '面试组', '<p>无领导讨论要避免抢话和沉默，重点体现推动讨论的能力。</p>', '面试,无领导小组,表达', 610, 25, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 15 DAY));

INSERT INTO news_comment (news_id, user_id, content) VALUES
(1, 2, '时间线整理得很清楚，方便规划复习节奏。'),
(2, 5, '这些素材对申论和面试都很有帮助。'),
(3, 2, '希望后续多出一些真题拆解。');

INSERT INTO news_share_log (news_id, user_id, share_channel, created_at) VALUES
(1, 2, 'WECHAT', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 5, 'LINK', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 2, 'LINK', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, NULL, 'LINK', DATE_SUB(NOW(), INTERVAL 8 HOUR));

INSERT INTO user_news_category_subscription (user_id, category_id, created_at) VALUES
(1, 1, NOW()),
(1, 3, NOW()),
(2, 2, NOW()),
(5, 6, NOW());

INSERT INTO classroom (id, name, subject_name, teacher_id, teacher_name, cover_file_id, cover_url, intro, capacity, is_public, member_count, status) VALUES
(1, '2026 国考行测陪跑班', '行测', 3, '王老师', NULL, '/classroom-assets/covers/classroom-xingce.png', '每周直播答疑，日常刷题打卡。', 200, 1, 58, 'OPEN'),
(2, '江苏申论写作精进班', '申论', 4, '陈老师', NULL, '/classroom-assets/covers/classroom-shenlun.png', '重点训练写作框架与素材应用。', 120, 1, 43, 'OPEN'),
(3, '结构化面试模拟班', '面试', 3, '王老师', NULL, '/classroom-assets/covers/classroom-mianshi.png', '线上模拟答辩与即时点评。', 80, 1, 21, 'OPEN');

INSERT INTO classroom_member (classroom_id, user_id, role_code) VALUES
(1, 3, 'TEACHER'),
(1, 2, 'STUDENT'),
(1, 5, 'STUDENT'),
(2, 4, 'TEACHER'),
(2, 2, 'STUDENT'),
(3, 3, 'TEACHER'),
(3, 5, 'STUDENT');

INSERT INTO classroom_join_request (classroom_id, user_id, status, remark, reviewed_by, reviewed_at) VALUES
  (1, 5, 'APPROVED', '欢迎进班', 3, NOW()),
  (2, 5, 'PENDING', '想重点提升申论写作', NULL, NULL),
  (3, 2, 'PENDING', '希望训练面试表达', NULL, NULL);

INSERT INTO classroom_material (classroom_id, material_category, title, file_id, description, sort_no, status) VALUES
  (1, 'TRIAL_VIDEO', '行测陪跑班试听导学', 6, '班级试看导学视频，帮助学生确认班级节奏与授课方式。', 1, 'ENABLED'),
  (1, 'TRIAL_DOCUMENT', '行测陪跑班试用讲义', 5, '开放部分课前讲义与作业样例，供学员预览。', 2, 'ENABLED'),
  (3, 'TRIAL_VIDEO', '面试模拟班试听片段', 7, '展示结构化面试模拟答题片段。', 1, 'ENABLED');

INSERT INTO classroom_homework (id, classroom_id, teacher_id, title, description, attachment_file_id, question_count, total_score, status, deadline_at, created_at) VALUES
  (1, 1, 3, '行测周测一', '完成言语与资料分析基础题，提交后自动评分，老师二次批改。', 5, 4, 5, 'PUBLISHED', DATE_ADD(NOW(), INTERVAL 3 DAY), NOW()),
  (2, 1, 3, '错题强化作业', '围绕判断推理常错点进行巩固训练。', NULL, 2, 2, 'PUBLISHED', DATE_ADD(NOW(), INTERVAL 5 DAY), NOW());

INSERT INTO classroom_homework_question (homework_id, question_id, score, sort_no) VALUES
(1, 1, 1, 1),
(1, 2, 1, 2),
(1, 3, 1, 3),
(1, 6, 2, 4),
(2, 4, 1, 1),
(2, 5, 1, 2);

INSERT INTO classroom_homework_submission (id, homework_id, classroom_id, user_id, score, objective_score, question_count, answered_count, correct_count, duration_seconds, status, review_status, review_remark, reviewed_by, reviewed_at, started_at, submitted_at, created_at) VALUES
(1, 1, 1, 2, 3, 2, 4, 4, 2, 780, 'SUBMITTED', 'REVIEWED', '基础不错，继续加强资料分析。', 3, NOW(), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 1, 1, 5, 1, 1, 4, 4, 1, 860, 'SUBMITTED', 'AUTO_GRADED', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO classroom_homework_answer (submission_id, homework_id, question_id, user_answer, correct_answer, is_correct, score, teacher_comment) VALUES
(1, 1, 1, 'A', 'A', 1, 1, NULL),
(1, 1, 2, 'C', 'C', 1, 1, NULL),
(1, 1, 3, 'B', 'D', 0, 0, NULL),
(1, 1, 6, '我会先核对题干单位和同比、环比口径，再建立速算步骤，避免只凭直觉选项。', '回答应覆盖审题、口径统一、列式核验与复盘四个维度。', 0, 1, '思路完整，但可以再补充“错因复盘”的动作。'),
(2, 1, 1, 'A', 'A', 1, 1, NULL),
(2, 1, 2, 'B', 'C', 0, 0, NULL),
(2, 1, 3, 'B', 'D', 0, 0, NULL),
(2, 1, 6, '先看选项再猜。', '回答应覆盖审题、口径统一、列式核验与复盘四个维度。', 0, 0, NULL);

INSERT INTO classroom_exam_task (id, classroom_id, teacher_id, title, description, question_count, total_score, duration_minutes, anti_cheat_enabled, anti_cheat_notice, status, start_time, end_time, created_at) VALUES
(1, 1, 3, '班级月考一', '班级阶段测验，系统自动判分并生成班级排名。', 5, 6, 45, 1, '考试过程中请勿切屏、刷新页面或与他人共享答题内容，系统会记录异常行为。', 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), NOW());

INSERT INTO classroom_exam_question (classroom_exam_id, question_id, score, sort_no) VALUES
(1, 1, 1, 1),
(1, 2, 1, 2),
(1, 4, 1, 3),
(1, 5, 1, 4),
(1, 6, 2, 5);

INSERT INTO classroom_exam_submission (id, classroom_exam_id, classroom_id, user_id, score, question_count, answered_count, correct_count, duration_seconds, status, review_status, review_remark, reviewed_by, reviewed_at, started_at, submitted_at, created_at) VALUES
(1, 1, 1, 2, 4, 5, 5, 3, 1560, 'SUBMITTED', 'REVIEWED', '判断推理提升明显，继续保持。', 3, NOW(), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 1, 1, 5, 2, 5, 5, 2, 1700, 'SUBMITTED', 'AUTO_GRADED', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR));

INSERT INTO classroom_exam_answer (submission_id, classroom_exam_id, question_id, user_answer, correct_answer, is_correct, score, teacher_comment) VALUES
(1, 1, 1, 'A', 'A', 1, 1, NULL),
(1, 1, 2, 'C', 'C', 1, 1, NULL),
(1, 1, 4, 'B', 'B', 1, 1, NULL),
(1, 1, 5, 'A', 'D', 0, 0, NULL),
(1, 1, 6, '先划分数据区间，再列出关键比较维度，最后写出结论和复盘。', '回答应覆盖审题、口径统一、列式核验与复盘四个维度。', 0, 1, '有步骤感，但结论表达可以再更明确。'),
(2, 1, 1, 'A', 'A', 1, 1, NULL),
(2, 1, 2, 'B', 'C', 0, 0, NULL),
(2, 1, 4, 'B', 'B', 1, 1, NULL),
(2, 1, 5, 'A', 'D', 0, 0, NULL),
(2, 1, 6, '不知道。', '回答应覆盖审题、口径统一、列式核验与复盘四个维度。', 0, 0, NULL);

INSERT INTO question (id, subject_name, category_name, province, question_type, difficulty_level, source_name, stem, analysis, answer_value, knowledge_points, video_explain_title, video_explain_url, enabled) VALUES
(1, '行测', '言语理解与表达', '全国', 'SINGLE', '中等', '平台自建', '下列词语中使用最恰当的一项是？', '重点考查词语辨析能力。', 'B', '言语理解,词语辨析', '言语理解高频辨析讲解', 'https://example.com/video/explain-question-1', 1),
(2, '行测', '资料分析', '江苏', 'SINGLE', '中等', '平台自建', '根据材料，下列说法正确的是？', '先定位数据，再进行比较。', 'C', '资料分析,增长率', NULL, NULL, 1),
(3, '申论', '归纳概括', '全国', 'SINGLE', '基础', '平台自建', '请概括材料反映的主要问题。', '注意主体、问题、影响的提炼。', 'A', '申论,归纳概括', '申论归纳概括拆解课', 'https://example.com/video/explain-question-3', 1),
(4, '行测', '判断推理', '广东', 'SINGLE', '较难', '平台自建', '下列推理最符合逻辑的是？', '识别论证结构与削弱点。', 'D', '判断推理,逻辑', '判断推理逻辑强化', 'https://example.com/video/explain-question-4', 1),
(5, '行测', '数量关系', '全国', 'SINGLE', '中等', '平台自建', '若某数列满足题干条件，则下一项为多少？', '先找递推规律，再代入验证。', 'A', '数量关系,数列', NULL, NULL, 1),
(6, '行测', '资料分析', '全国', 'ESSAY', '中等', '平台自建', '请简述你在资料分析题中避免粗心失分的三步做法。', '主观题重点看是否覆盖审题、口径统一、列式核验与复盘。', '回答应覆盖审题、口径统一、列式核验与复盘四个维度。', '资料分析,复盘方法', NULL, NULL, 1),
(7, '行测', '常识判断', '全国', 'SINGLE', '中等', '平台自建', '下列关于基层治理的表述，正确的是？', '基层治理强调协同共治与资源整合。', 'B', '常识判断,基层治理', NULL, NULL, 1),
(8, '行测', '言语理解与表达', '江苏', 'SINGLE', '中等', '平台自建', '在句子中填入最恰当的词语是？', '根据语境与搭配关系确定词语。', 'C', '言语理解,词语填空', NULL, NULL, 1),
(9, '行测', '判断推理', '全国', 'SINGLE', '较难', '平台自建', '下列选项中，最能削弱论证的是？', '削弱题重点看是否切断论据与结论的联系。', 'D', '判断推理,削弱', NULL, NULL, 1),
(10, '行测', '资料分析', '广东', 'SINGLE', '中等', '平台自建', '根据图表，下列说法正确的是？', '资料分析先定位，再比值，再核验单位。', 'A', '资料分析,图表阅读', NULL, NULL, 1),
(11, '行测', '数量关系', '全国', 'SINGLE', '较难', '平台自建', '某工程按计划完成，剩余工作量为原计划的多少？', '先算已完成比例，再求剩余比例。', 'B', '数量关系,工程问题', NULL, NULL, 1),
(12, '行测', '常识判断', '全国', 'SINGLE', '基础', '平台自建', '下列关于公文格式的说法，正确的是？', '公文格式强调规范、简明与准确。', 'C', '常识判断,公文格式', NULL, NULL, 1),
(13, '行测', '言语理解与表达', '全国', 'SINGLE', '中等', '平台自建', '选择最能概括文段主旨的一项。', '主旨题优先看首尾和高频词。', 'A', '言语理解,主旨概括', NULL, NULL, 1),
(14, '行测', '判断推理', '江苏', 'SINGLE', '中等', '平台自建', '类比推理中，最合适的一组关系是？', '先找词语之间的对应关系再验证。', 'D', '判断推理,类比', NULL, NULL, 1),
(15, '行测', '资料分析', '全国', 'SINGLE', '较难', '平台自建', '某地区同比增长率是多少？', '增长率题先找基期与现期。', 'B', '资料分析,增长率', NULL, NULL, 1),
(16, '行测', '数量关系', '广东', 'SINGLE', '中等', '平台自建', '若两数之和与差满足条件，则较大数是多少？', '代入方程组后即可求解。', 'C', '数量关系,方程问题', NULL, NULL, 1),
(17, '行测', '常识判断', '江苏', 'SINGLE', '中等', '平台自建', '以下关于依法行政的说法，正确的是？', '依法行政强调法定职责必须为、法无授权不可为。', 'A', '常识判断,依法行政', NULL, NULL, 1),
(18, '行测', '言语理解与表达', '全国', 'SINGLE', '中等', '平台自建', '句子排序最合理的是？', '排序题要抓首句、指代和时间顺序。', 'D', '言语理解,句子排序', NULL, NULL, 1),
(19, '行测', '判断推理', '全国', 'SINGLE', '较难', '平台自建', '图形推理中，规律最可能是？', '图形推理常看位置、数量、样式变化。', 'B', '判断推理,图形推理', NULL, NULL, 1),
(20, '行测', '资料分析', '广东', 'SINGLE', '中等', '平台自建', '下列对增长趋势的判断，正确的是？', '趋势题一般看同比和环比的方向。', 'C', '资料分析,趋势判断', NULL, NULL, 1),
(21, '行测', '数量关系', '全国', 'SINGLE', '基础', '平台自建', '一个数的 20% 是 48，这个数是多少？', '直接用已知比例反推原数。', 'A', '数量关系,百分数', NULL, NULL, 1);

INSERT INTO question_option (question_id, option_key, option_content, sort_no) VALUES
(1, 'A', '词义扩大', 1),
(1, 'B', '词义准确', 2),
(1, 'C', '搭配不当', 3),
(1, 'D', '语义重复', 4),
(2, 'A', '增速最高的是甲行业', 1),
(2, 'B', '总量最高的是乙行业', 2),
(2, 'C', '丙行业同比增长快于乙行业', 3),
(2, 'D', '丁行业占比最低', 4),
(3, 'A', '问题集中在服务效率、协同不足与群众体验不佳', 1),
(3, 'B', '材料主要介绍治理成绩', 2),
(3, 'C', '材料体现人才建设问题', 3),
(3, 'D', '材料重点是财税制度改革', 4),
(4, 'A', '论据加强了结论', 1),
(4, 'B', '论据与结论无关', 2),
(4, 'C', '前提必然推出结论', 3),
(4, 'D', '论据不能充分支持结论', 4),
(5, 'A', '34', 1),
(5, 'B', '36', 2),
(5, 'C', '38', 3),
(5, 'D', '40', 4),
(7, 'A', '权责不清、资源分散', 1),
(7, 'B', '协同共治、资源整合', 2),
(7, 'C', '单一部门独立负责', 3),
(7, 'D', '减少公共参与', 4),
(8, 'A', '细小', 1),
(8, 'B', '笼统', 2),
(8, 'C', '精准', 3),
(8, 'D', '随意', 4),
(9, 'A', '论据增加了结论的可信度', 1),
(9, 'B', '论据与结论完全一致', 2),
(9, 'C', '论据补充了更多案例', 3),
(9, 'D', '出现了反例，削弱了结论', 4),
(10, 'A', '甲指标占比最高且增长稳定', 1),
(10, 'B', '乙指标同比下降', 2),
(10, 'C', '丙指标绝对值最低', 3),
(10, 'D', '丁指标没有变化', 4),
(11, 'A', '40%', 1),
(11, 'B', '30%', 2),
(11, 'C', '20%', 3),
(11, 'D', '10%', 4),
(12, 'A', '公文标题可以使用口语化表达', 1),
(12, 'B', '公文落款可省略发文机关', 2),
(12, 'C', '公文格式应当规范统一', 3),
(12, 'D', '公文附件无需编号', 4),
(13, 'A', '文段主要强调治理应兼顾效率与温度', 1),
(13, 'B', '文段仅介绍政策背景', 2),
(13, 'C', '文段批评所有改革方案', 3),
(13, 'D', '文段着重说明资金投入', 4),
(14, 'A', '医生：手术 = 教师：课堂', 1),
(14, 'B', '汽车：轮胎 = 书本：封面', 2),
(14, 'C', '春天：温暖 = 秋天：收获', 3),
(14, 'D', '种子：发芽 = 机器：运转', 4),
(15, 'A', '1%', 1),
(15, 'B', '5%', 2),
(15, 'C', '8%', 3),
(15, 'D', '12%', 4),
(16, 'A', '12', 1),
(16, 'B', '15', 2),
(16, 'C', '18', 3),
(16, 'D', '21', 4),
(17, 'A', '行政机关应当在法定权限内履职', 1),
(17, 'B', '行政机关可随意扩大权限', 2),
(17, 'C', '行政机关无需公开信息', 3),
(17, 'D', '行政机关可拒绝履行法定义务', 4),
(18, 'A', '①②③④', 1),
(18, 'B', '②①④③', 2),
(18, 'C', '④③②①', 3),
(18, 'D', '③①④②', 4),
(19, 'A', '图形颜色数量不变但位置随机变化', 1),
(19, 'B', '图形中的元素按照固定方向递增', 2),
(19, 'C', '图形全部保持静止', 3),
(19, 'D', '图形只发生大小变化', 4),
(20, 'A', '增长趋势持续上升', 1),
(20, 'B', '增长趋势先升后降', 2),
(20, 'C', '增长趋势保持平稳上升', 3),
(20, 'D', '增长趋势明显回落', 4),
(21, 'A', '240', 1),
(21, 'B', '220', 2),
(21, 'C', '200', 3),
(21, 'D', '180', 4);


-- stage 23 extra question bank start
DELETE FROM question_option WHERE question_id BETWEEN 22 AND 121;
DELETE FROM question WHERE id BETWEEN 22 AND 121;

INSERT INTO question (id, subject_name, category_name, province, question_type, difficulty_level, source_name, stem, analysis, answer_value, knowledge_points, video_explain_title, video_explain_url, enabled) VALUES
(22, '行测', '常识判断', '全国', 'SINGLE', '中等', '平台自建', '推进数字政府建设，以下做法最符合提升政务服务协同性要求的是：', '协同性强调跨部门业务联动和数据共享，减少重复提交与重复审核。', 'B', '常识判断,数字政府', NULL, NULL, 1),
(23, '行测', '常识判断', '江苏', 'SINGLE', '中等', '平台自建', '根据行政处罚法，当事人对拟作出的较重行政处罚有异议时，依法享有的程序性权利是：', '对较重行政处罚，当事人依法有权要求举行听证。', 'C', '常识判断,行政处罚法', NULL, NULL, 1),
(24, '行测', '常识判断', '广东', 'SINGLE', '基础', '平台自建', '市民在路边拾得他人遗失的钱包后，较为妥当的做法是：', '拾得遗失物应及时返还权利人，无法联系时可交公安等有关部门。', 'A', '常识判断,民法典', NULL, NULL, 1),
(25, '行测', '常识判断', '浙江', 'SINGLE', '中等', '平台自建', '关于家庭食品安全，下列说法正确的是：', '冷冻食品反复解冻会加快微生物繁殖，增加食品安全风险。', 'D', '常识判断,食品安全', NULL, NULL, 1),
(26, '行测', '常识判断', '全国', 'SINGLE', '基础', '平台自建', '地震发生时，若身处室内且来不及撤离，较合适的避险方式是：', '应尽量就近躲避在结实家具旁，远离玻璃、吊灯等坠落物。', 'B', '常识判断,应急避险', NULL, NULL, 1),
(27, '行测', '常识判断', '江苏', 'SINGLE', '中等', '平台自建', '关于碳达峰与碳中和，下列理解正确的是：', '二者都强调通过优化能源结构和提高效率减少温室气体排放。', 'A', '常识判断,双碳', NULL, NULL, 1),
(28, '行测', '常识判断', '广东', 'SINGLE', '中等', '平台自建', '政府信息公开工作中，下列属于应当主动公开内容的是：', '涉及公众利益、需要社会广泛知晓的政府信息，应当依法主动公开。', 'C', '常识判断,政府信息公开', NULL, NULL, 1),
(29, '行测', '常识判断', '浙江', 'SINGLE', '中等', '平台自建', '关于网络购物七日无理由退货制度，下列说法正确的是：', '鲜活易腐等不宜退货的商品，不适用七日无理由退货。', 'D', '常识判断,消费者权益保护', NULL, NULL, 1),
(30, '行测', '常识判断', '全国', 'SINGLE', '基础', '平台自建', '家庭厨房中油锅起火，较为妥当的处置方式是：', '油锅起火不宜泼水，可用锅盖覆盖隔绝空气。', 'B', '常识判断,消防安全', NULL, NULL, 1),
(31, '行测', '常识判断', '江苏', 'SINGLE', '较难', '平台自建', '劳动合同期限一年以上不满三年的，试用期依法不得超过：', '劳动合同法规定，此类合同试用期不得超过二个月。', 'C', '常识判断,劳动合同法', NULL, NULL, 1),
(32, '行测', '常识判断', '广东', 'SINGLE', '中等', '平台自建', '关于发明专利的保护期限，下列说法正确的是：', '发明专利权的期限为二十年，自申请日起计算。', 'A', '常识判断,知识产权', NULL, NULL, 1),
(33, '行测', '常识判断', '浙江', 'SINGLE', '中等', '平台自建', '关于公文处理规则，下列说法正确的是：', '请示应当一文一事，以便上级机关及时、准确作出批复。', 'B', '常识判断,公文处理', NULL, NULL, 1),
(34, '行测', '常识判断', '全国', 'SINGLE', '中等', '平台自建', '根据行政复议法的一般规则，下列说法正确的是：', '行政复议期间原具体行政行为原则上不停止执行，但法律另有规定的除外。', 'D', '常识判断,行政复议法', NULL, NULL, 1),
(35, '行测', '常识判断', '江苏', 'SINGLE', '基础', '平台自建', '关于心肺复苏胸外按压，下列说法正确的是：', '成人胸外按压频率一般为每分钟100到120次。', 'A', '常识判断,急救常识', NULL, NULL, 1),
(36, '行测', '常识判断', '广东', 'SINGLE', '中等', '平台自建', '关于个人信息保护，下列做法符合最小必要原则的是：', '收集个人信息应限于实现处理目的的最小范围。', 'C', '常识判断,个人信息保护', NULL, NULL, 1),
(37, '行测', '常识判断', '浙江', 'SINGLE', '基础', '平台自建', '闪电发生后通常先看到光后听到雷声，主要原因是：', '光在空气中的传播速度远高于声音。', 'B', '常识判断,物理常识', NULL, NULL, 1),
(38, '行测', '常识判断', '全国', 'SINGLE', '中等', '平台自建', '关于乡村振兴战略，下列说法正确的是：', '乡村振兴强调产业、人才、文化、生态、组织全面振兴。', 'D', '常识判断,乡村振兴', NULL, NULL, 1),
(39, '行测', '常识判断', '江苏', 'SINGLE', '中等', '平台自建', '预算管理中强调过紧日子，主要体现的要求是：', '过紧日子强调节用为民、压减一般性支出，把资金更多用于民生和重点领域。', 'A', '常识判断,财政管理', NULL, NULL, 1),
(40, '行测', '常识判断', '广东', 'SINGLE', '基础', '平台自建', '湿地常被称为地球之肾，主要是因为其具有较强的：', '湿地具有涵养水源、净化水体、维护生物多样性等生态功能。', 'C', '常识判断,生态环境', NULL, NULL, 1),
(41, '行测', '常识判断', '浙江', 'SINGLE', '中等', '平台自建', '完善公共文化服务体系，重点应当体现的方向是：', '公共文化服务强调标准化、均等化，重点向基层和薄弱地区延伸。', 'B', '常识判断,公共文化服务', NULL, NULL, 1),
(42, '行测', '言语理解与表达', '全国', 'SINGLE', '中等', '平台自建', '乡村建设既要重视硬件改善，也要尊重村庄肌理与生活方式，避免千村一面。只有把改善人居环境与延续乡土文化______起来，乡村才能真正留得住人、记得住乡愁。填入画横线部分最恰当的一项是：', '语境强调把两个目标统一起来，“统筹”最贴切。', 'A', '言语理解,实词辨析', NULL, NULL, 1),
(43, '行测', '言语理解与表达', '江苏', 'SINGLE', '中等', '平台自建', '治理城市内涝，不能只在暴雨来临时仓促应对，而应从排水管网、海绵设施、风险预警等方面进行系统谋划。这样的治理思路，体现的是从被动抢险走向主动______。填入画横线部分最恰当的一项是：', '与“被动抢险”对应，“主动防治”最符合语义。', 'D', '言语理解,语境填空', NULL, NULL, 1),
(44, '行测', '言语理解与表达', '广东', 'SINGLE', '基础', '平台自建', '基层干部既要把政策讲清楚，也要把群众诉求听明白。沟通不是单向灌输，而是双向互动、彼此理解的过程。因此，基层工作尤其需要______。填入画横线部分最恰当的一项是：', '前文强调双向互动、彼此理解，因此“换位思考”最贴切。', 'B', '言语理解,语境填空', NULL, NULL, 1),
(45, '行测', '言语理解与表达', '浙江', 'SINGLE', '中等', '平台自建', '制度执行不能停留在纸面上。再完善的制度，如果缺少监督问责和执行反馈，也难免在落实过程中出现______。填入画横线部分最恰当的一项是：', '语境强调落实走样，“变形走样”最常见也最准确。', 'C', '言语理解,成语辨析', NULL, NULL, 1),
(46, '行测', '言语理解与表达', '全国', 'SINGLE', '中等', '平台自建', '人工智能工具可以提高信息处理效率，但若完全依赖其输出而不加核验，也容易造成事实错误被不断复制放大。因此，使用新技术的前提仍然是保持必要的______。填入画横线部分最恰当的一项是：', '语境强调对输出进行核验，需保持“审慎”态度。', 'A', '言语理解,态度词', NULL, NULL, 1),
(47, '行测', '言语理解与表达', '江苏', 'SINGLE', '较难', '平台自建', '推动科技成果转化，既要让科研人员看到回报，也要让企业能够承接应用，还要让金融服务及时跟上。只有打通创新链、产业链、资金链之间的堵点，成果转化才能真正形成______。填入画横线部分最恰当的一项是：', '前文强调链条打通，形成“闭环”最准确。', 'D', '言语理解,语境填空', NULL, NULL, 1),
(48, '行测', '言语理解与表达', '广东', 'SINGLE', '基础', '平台自建', '读书的意义，并不只是为了记住多少知识点，更在于通过持续阅读形成独立思考和判断能力。对读书来说，数量固然重要，但更重要的是阅读之后的______。填入画横线部分最恰当的一项是：', '前文强调独立思考和判断，因此应是“沉淀”。', 'B', '言语理解,实词辨析', NULL, NULL, 1),
(49, '行测', '言语理解与表达', '浙江', 'SINGLE', '中等', '平台自建', '公共政策的制定要讲求时度效。节奏太慢，可能错失窗口；推进太急，也可能引发执行层面的摩擦。由此可见，政策推进尤其需要把握______。填入画横线部分最恰当的一项是：', '前文强调节奏与力度的平衡，因此“分寸”最贴切。', 'C', '言语理解,语境填空', NULL, NULL, 1),
(50, '行测', '言语理解与表达', '全国', 'SINGLE', '中等', '平台自建', '评价一项改革不能只看短期数据，也要看其是否改善了制度供给、激发了主体活力、增强了发展后劲。也就是说，判断改革成效不能停留于一时一地，而要有更加______的视角。填入画横线部分最恰当的一项是：', '语境强调时间和空间上的拉长放宽，“长远”最合适。', 'A', '言语理解,实词辨析', NULL, NULL, 1),
(51, '行测', '言语理解与表达', '江苏', 'SINGLE', '中等', '平台自建', '历史街区更新若只追求外观翻新，忽视原有生活场景和社区关系，往往会让街区失去真实气息。城市更新不是简单复制，而应在保护中更新、在延续中______。填入画横线部分最恰当的一项是：', '与“延续”搭配，最贴切的是“焕新”。', 'D', '言语理解,词语搭配', NULL, NULL, 1),
(52, '行测', '言语理解与表达', '广东', 'SINGLE', '中等', '平台自建', '有些地方把“一网通办”理解为把线下表格搬到线上，但群众办事仍需反复填报、重复上传。真正的一网通办，不是流程的简单上网，而是业务的深度重构和数据的高效协同。这段文字意在说明：', '文段先指出误区，再强调真正含义，主旨是数字化要落实到流程重构。', 'B', '言语理解,主旨概括', NULL, NULL, 1),
(53, '行测', '言语理解与表达', '浙江', 'SINGLE', '中等', '平台自建', '短视频传播速度快、覆盖面广，能够帮助传统文化更快触达年轻人。但如果只追求热度和流量，忽视内容出处和文化语境，也可能让传统文化被碎片化、娱乐化。对这段文字理解最准确的是：', '文段持审慎肯定态度，强调传播传统文化既要借助新媒介也要避免浅表化。', 'C', '言语理解,文意理解', NULL, NULL, 1),
(54, '行测', '言语理解与表达', '全国', 'SINGLE', '较难', '平台自建', '从共享单车到共享充电宝，共享经济一度给人以万物皆可共享的想象。然而实践表明，共享模式是否成立，取决于使用频率、维护成本、调度效率等多重因素。离开具体场景谈共享，难免会落入概念先行。上述文字主要想表达的是：', '文段核心在于共享模式要看具体条件，不能抽象迷信概念。', 'A', '言语理解,主旨概括', NULL, NULL, 1),
(55, '行测', '言语理解与表达', '江苏', 'SINGLE', '中等', '平台自建', '一个人能够专注多久，往往不仅取决于意志力，还与任务是否清晰、反馈是否及时、环境是否干扰过多有关。与其一味责怪自己不够自律，不如先从优化任务拆分和工作环境开始。由此可以推出，作者更认同：', '作者强调专注力受环境和任务设计影响，不只是个人意志问题。', 'D', '言语理解,推断理解', NULL, NULL, 1),
(56, '行测', '言语理解与表达', '广东', 'SINGLE', '中等', '平台自建', '博物馆热并不意味着人们只想打卡拍照。越来越多年轻观众愿意为了一个展览多次进馆，认真听讲解、做笔记、看图录。这说明，文化消费升级不是单纯追逐新奇，而是更加重视体验的深度和获得感。下列最适合作为这段文字标题的是：', '标题应突出“文化消费升级重在深度体验”的主旨。', 'B', '言语理解,标题选择', NULL, NULL, 1),
(57, '行测', '言语理解与表达', '浙江', 'SINGLE', '中等', '平台自建', '很多时候，问题久拖不决，并不是因为无解，而是因为不同部门各有目标、各有考量，谁都不愿先迈一步。破解这种僵局，需要建立跨部门协调机制，明确责任边界和协同流程。对这段文字概括最准确的是：', '文段指出问题根源并给出对策，核心是通过机制建设破除协同僵局。', 'C', '言语理解,主旨概括', NULL, NULL, 1),
(58, '行测', '言语理解与表达', '全国', 'SINGLE', '中等', '平台自建', '有的企业把“用户需要什么”理解为“什么都满足”，结果产品越做越重、功能越堆越多，反而让用户更难使用。真正的用户导向，不是无边界地增加功能，而是在理解核心需求后做出取舍。根据这段文字，作者认为产品设计的关键在于：', '作者强调产品设计要识别核心需求并敢于取舍。', 'A', '言语理解,文意理解', NULL, NULL, 1),
(59, '行测', '言语理解与表达', '江苏', 'SINGLE', '基础', '平台自建', '阅读速度快，并不必然意味着理解得深。面对信息密集的长文章，适当放慢节奏、回看关键段落，往往更有助于建立完整的理解框架。这段文字主要强调：', '文段强调阅读质量重于单纯速度。', 'D', '言语理解,主旨概括', NULL, NULL, 1),
(60, '行测', '言语理解与表达', '广东', 'SINGLE', '较难', '平台自建', '如果把基层创新简单理解为“从无到有”，就容易忽视很多改进其实来自对既有流程的细微调整。恰恰是这些看似不起眼的小改动，往往最能直接改善群众体验。由此可知，作者想提醒我们：', '作者强调创新不只是颠覆式创造，也包括渐进式优化。', 'B', '言语理解,推断理解', NULL, NULL, 1),
(61, '行测', '言语理解与表达', '浙江', 'SINGLE', '中等', '平台自建', '一些地方发展夜间经济时，容易把重点放在延长营业时间和增加活动场次上，却忽略了交通接驳、治安巡逻、卫生保洁等配套服务。结果看似热闹，体验却并不理想。这段文字意在强调：', '文段强调发展夜间经济要重视配套保障，而非只做表面热闹。', 'C', '言语理解,主旨概括', NULL, NULL, 1),
(62, '行测', '判断推理', '全国', 'SINGLE', '中等', '平台自建', '某地推行“午间不打烊”服务后，群众满意度明显提高。因此，只要延长办公时间，就一定能提升政务服务满意度。以下哪项最能削弱上述结论？', '指出满意度提高可能还与窗口整合、人员培训等因素有关，最能削弱因果关系。', 'B', '判断推理,削弱', NULL, NULL, 1),
(63, '行测', '判断推理', '江苏', 'SINGLE', '中等', '平台自建', '某社区认为，只要增加活动场次，居民参与率就会持续上升。以下哪项如果为真，最能削弱该观点？', '活动数量增加不等于参与率上升，若内容雷同、居民兴趣不足，结论就被削弱。', 'D', '判断推理,削弱', NULL, NULL, 1),
(64, '行测', '判断推理', '广东', 'SINGLE', '较难', '平台自建', '某企业认为，引入智能客服后人工成本下降，因此客户问题解决效率一定提高。以下哪项最能削弱该结论？', '成本下降并不必然导致效率提高，若复杂问题转人工更慢，则削弱最强。', 'A', '判断推理,削弱', NULL, NULL, 1),
(65, '行测', '判断推理', '浙江', 'SINGLE', '中等', '平台自建', '研究发现，经常参加阅读活动的学生成绩更好。因此，阅读活动是提高成绩的决定性因素。以下哪项最能削弱上述推断？', '指出相关不等于因果，成绩好可能源于家庭阅读环境更好。', 'C', '判断推理,削弱', NULL, NULL, 1),
(66, '行测', '判断推理', '全国', 'SINGLE', '中等', '平台自建', '某地景区推出预约分时入园后，游客投诉量下降。由此可见，分时预约能够解决景区管理中的所有问题。以下哪项最能削弱该观点？', '“所有问题”表述过强，只需指出其他问题依然存在即可削弱。', 'B', '判断推理,削弱', NULL, NULL, 1),
(67, '行测', '判断推理', '江苏', 'SINGLE', '较难', '平台自建', '某机构认为，线上培训比线下培训成本低，所以线上培训一定更值得推广。以下哪项最能削弱该结论？', '若培训目标需要现场操作和即时反馈，单纯成本低不足以推出更值得推广。', 'A', '判断推理,削弱', NULL, NULL, 1),
(68, '行测', '判断推理', '广东', 'SINGLE', '中等', '平台自建', '某团队认为，只要增加项目会议次数，就能减少沟通失误。以下哪项最能削弱这一观点？', '会议次数增加若缺少明确分工，反而可能制造新的信息噪音。', 'D', '判断推理,削弱', NULL, NULL, 1),
(69, '行测', '判断推理', '浙江', 'SINGLE', '中等', '平台自建', '某地将公共自行车站点全部换成电子围栏后，认为骑行秩序一定会明显改善。以下哪项最能削弱该判断？', '若停放引导和执法维护不到位，仅换技术手段不足以改善秩序。', 'C', '判断推理,削弱', NULL, NULL, 1),
(70, '行测', '判断推理', '全国', 'SINGLE', '中等', '平台自建', '某校计划增加体育课时，校长认为此举有助于提升学生整体状态。以下哪项如果为真，最能支持校长的观点？', '直接表明增加体育活动能改善精力和课堂专注度，支持最强。', 'B', '判断推理,加强', NULL, NULL, 1),
(71, '行测', '判断推理', '江苏', 'SINGLE', '中等', '平台自建', '某地计划在社区设置共享工具柜，认为此举能减少家庭重复购置低频工具。以下哪项如果为真，最能支持该观点？', '若居民对低频工具需求分散且工具使用时段错开，就能有效支持共享。', 'A', '判断推理,加强', NULL, NULL, 1),
(72, '行测', '判断推理', '广东', 'SINGLE', '较难', '平台自建', '某企业主张将报销流程简化为线上审批，认为这会缩短员工等待时间。以下哪项如果为真，最能支持该主张？', '直接说明线上审批减少流转环节和重复签字，支持最强。', 'D', '判断推理,加强', NULL, NULL, 1),
(73, '行测', '判断推理', '浙江', 'SINGLE', '中等', '平台自建', '某博物馆推出分层讲解服务，认为能提高观众满意度。以下哪项如果为真，最能支持该判断？', '如果不同观众群体对信息深度需求不同，分层讲解能更好匹配需求。', 'C', '判断推理,加强', NULL, NULL, 1),
(74, '行测', '判断推理', '全国', 'SINGLE', '中等', '平台自建', '共享岗位：指两个或两个以上劳动者在约定时间内共同完成一个全时岗位职责的用工安排。根据上述定义，下列属于共享岗位的是：', '定义强调多人共同完成同一全时岗位职责，只有B符合。', 'B', '判断推理,定义判断', NULL, NULL, 1),
(75, '行测', '判断推理', '江苏', 'SINGLE', '中等', '平台自建', '情绪劳动：指服务提供者为了满足职业角色要求，有意识地管理、调节并展示特定情绪的劳动。根据上述定义，下列属于情绪劳动的是：', '空乘在服务过程中主动维持礼貌、稳定情绪，符合定义。', 'A', '判断推理,定义判断', NULL, NULL, 1),
(76, '行测', '判断推理', '广东', 'SINGLE', '较难', '平台自建', '算法歧视：指自动化决策系统因训练数据、规则设计或使用方式存在偏差，导致特定群体遭受不合理区别对待。根据上述定义，下列属于算法歧视的是：', '系统因历史数据偏差频繁拒绝某类群体申请，属于不合理区别对待。', 'D', '判断推理,定义判断', NULL, NULL, 1),
(77, '行测', '判断推理', '浙江', 'SINGLE', '中等', '平台自建', '影子价格：在资源约束条件下，某种资源增加一个单位所带来的目标函数增量。根据上述定义，下列体现影子价格的是：', '在产能受限时，多一小时机器时间带来利润增加，符合影子价格定义。', 'C', '判断推理,定义判断', NULL, NULL, 1),
(78, '行测', '判断推理', '全国', 'SINGLE', '基础', '平台自建', '责任：落实 相当于 制度：', '责任通过落实体现，制度通过执行体现，逻辑关系一致。', 'A', '判断推理,类比推理', NULL, NULL, 1),
(79, '行测', '判断推理', '江苏', 'SINGLE', '中等', '平台自建', '医生：诊疗 相当于 教师：', '医生从事诊疗，教师从事授课。', 'D', '判断推理,类比推理', NULL, NULL, 1),
(80, '行测', '判断推理', '广东', 'SINGLE', '中等', '平台自建', '种子 对于 发芽 相当于 方案 对于 ：', '种子经过条件具备而发芽，方案经过实施而落地。', 'B', '判断推理,类比推理', NULL, NULL, 1),
(81, '行测', '判断推理', '浙江', 'SINGLE', '中等', '平台自建', '如果一个项目要么延期，要么增加预算。已知该项目没有延期，则可以推出：', '根据选言推理，否定前项可推出肯定后项，即增加预算。', 'C', '判断推理,逻辑推理', NULL, NULL, 1),
(82, '行测', '资料分析', '全国', 'SINGLE', '基础', '平台自建', '某市2024年社区食堂接待老年人就餐72万人次，同比增长20%。则2023年该市社区食堂接待老年人就餐约为多少万人次？', '72÷1.2=60。', 'A', '资料分析,增长率', NULL, NULL, 1),
(83, '行测', '资料分析', '江苏', 'SINGLE', '中等', '平台自建', '某县2024年新增市场主体1.44万户，比上年增加0.24万户。则该县2024年新增市场主体同比增长约为：', '增长率=0.24÷1.20=20%。', 'B', '资料分析,增长率', NULL, NULL, 1),
(84, '行测', '资料分析', '广东', 'SINGLE', '中等', '平台自建', '某地2024年新能源公交车数量为1350辆，比2023年增加150辆。2024年新能源公交车数量约是2023年的多少倍？', '2023年为1200辆，1350÷1200=1.125倍。', 'C', '资料分析,倍数', NULL, NULL, 1),
(85, '行测', '资料分析', '浙江', 'SINGLE', '基础', '平台自建', '某市2024年公共图书馆到馆读者为480万人次，其中青少年读者120万人次。青少年读者占比约为：', '120÷480=25%。', 'D', '资料分析,比重', NULL, NULL, 1),
(86, '行测', '资料分析', '全国', 'SINGLE', '较难', '平台自建', '某园区2024年规上工业企业研发投入总额为52亿元，同比增长30%。则2023年该园区规上工业企业研发投入总额约为多少亿元？', '52÷1.3=40亿元。', 'A', '资料分析,基期量', NULL, NULL, 1),
(87, '行测', '资料分析', '江苏', 'SINGLE', '中等', '平台自建', '某市2024年新增充电桩8.4万个，其中公共充电桩2.1万个。公共充电桩占新增充电桩的比重约为：', '2.1÷8.4=25%。', 'B', '资料分析,比重', NULL, NULL, 1),
(88, '行测', '资料分析', '广东', 'SINGLE', '中等', '平台自建', '某区2024年政务服务热线受理工单12万件，办结11.4万件。该区2024年政务服务热线工单办结率约为：', '11.4÷12=95%。', 'C', '资料分析,比率', NULL, NULL, 1),
(89, '行测', '资料分析', '浙江', 'SINGLE', '较难', '平台自建', '某市2024年接待游客2640万人次，比上年增长10%；旅游收入330亿元，比上年增长20%。则旅游收入增速比游客接待量增速高多少个百分点？', '20%-10%=10个百分点。', 'D', '资料分析,增速比较', NULL, NULL, 1),
(90, '行测', '资料分析', '全国', 'SINGLE', '基础', '平台自建', '某企业2024年线上销售额为4.8亿元，线下销售额为7.2亿元。线上销售额占总销售额的比重约为：', '4.8÷12=40%。', 'A', '资料分析,比重', NULL, NULL, 1),
(91, '行测', '资料分析', '江苏', 'SINGLE', '中等', '平台自建', '某地2024年新增城镇就业9万人，同比增加0.9万人。则2023年该地新增城镇就业人数约为：', '9-0.9=8.1万人。', 'C', '资料分析,基期量', NULL, NULL, 1),
(92, '行测', '资料分析', '广东', 'SINGLE', '中等', '平台自建', '某高校2024年本科毕业生共6000人，其中继续深造人数1500人。继续深造人数占本科毕业生总数的比例约为：', '1500÷6000=25%。', 'B', '资料分析,比重', NULL, NULL, 1),
(93, '行测', '资料分析', '浙江', 'SINGLE', '中等', '平台自建', '某地2024年农村电商交易额为18亿元，同比增长50%。则2023年农村电商交易额约为多少亿元？', '18÷1.5=12亿元。', 'A', '资料分析,基期量', NULL, NULL, 1),
(94, '行测', '资料分析', '全国', 'SINGLE', '较难', '平台自建', '某医院2024年门诊量为360万人次，同比上升8%；住院量为24万人次，同比上升20%。则下列说法正确的是：', '住院量增速20%高于门诊量增速8%。', 'D', '资料分析,比较', NULL, NULL, 1),
(95, '行测', '资料分析', '江苏', 'SINGLE', '基础', '平台自建', '某县2024年粮食总产量为84万吨，其中小麦产量21万吨。小麦产量占粮食总产量的比重约为：', '21÷84=25%。', 'C', '资料分析,比重', NULL, NULL, 1),
(96, '行测', '资料分析', '广东', 'SINGLE', '中等', '平台自建', '某市2024年地铁客运量为18亿人次，比上年增加3亿人次。该市2024年地铁客运量同比增长约为：', '3÷15=20%。', 'B', '资料分析,增长率', NULL, NULL, 1),
(97, '行测', '资料分析', '浙江', 'SINGLE', '中等', '平台自建', '某企业2024年利润总额为2.4亿元，成本总额为9.6亿元。利润总额约占成本总额的：', '2.4÷9.6=25%。', 'A', '资料分析,比率', NULL, NULL, 1),
(98, '行测', '资料分析', '全国', 'SINGLE', '较难', '平台自建', '某地2024年绿色建筑面积为960万平方米，比2022年增加60%。若2023年较2022年增长20%，则2024年较2023年增长约为：', '2022为600，2023为720，2024为960，增长率为240÷720=33.3%。', 'D', '资料分析,增长率', NULL, NULL, 1),
(99, '行测', '资料分析', '江苏', 'SINGLE', '基础', '平台自建', '某校图书馆共有藏书48万册，其中电子图书12万册。纸质图书约有多少万册？', '48-12=36万册。', 'C', '资料分析,简单计算', NULL, NULL, 1),
(100, '行测', '资料分析', '广东', 'SINGLE', '中等', '平台自建', '某平台2024年活跃商户为3.6万户，比上年增长12.5%。则2023年活跃商户约为多少万户？', '3.6÷1.125=3.2万户。', 'B', '资料分析,基期量', NULL, NULL, 1),
(101, '行测', '资料分析', '浙江', 'SINGLE', '中等', '平台自建', '某市2024年新建口袋公园48个，比2023年多建12个。2024年新建口袋公园数量比2023年约增长：', '12÷36=33.3%。', 'D', '资料分析,增长率', NULL, NULL, 1),
(102, '行测', '数量关系', '全国', 'SINGLE', '基础', '平台自建', '一件商品按原价打八折后售价为160元，则原价是多少元？', '原价=160÷0.8=200元。', 'A', '数量关系,百分数', NULL, NULL, 1),
(103, '行测', '数量关系', '江苏', 'SINGLE', '基础', '平台自建', '某班共有45名学生，男生人数比女生人数多3人。该班男生有多少人？', '设男生x，女生x-3，则2x-3=45，x=24。', 'C', '数量关系,方程', NULL, NULL, 1),
(104, '行测', '数量关系', '广东', 'SINGLE', '中等', '平台自建', '甲、乙两人同时从两地相向而行，甲每小时走5千米，乙每小时走7千米，3小时后相遇。两地相距多少千米？', '路程和=(5+7)×3=36千米。', 'B', '数量关系,行程', NULL, NULL, 1),
(105, '行测', '数量关系', '浙江', 'SINGLE', '中等', '平台自建', '某工程队单独完成一项工程，甲需12天，乙需18天。两队合作完成该工程需要多少天？', '效率和=1/12+1/18=5/36，所需时间=36/5=7.2天。', 'D', '数量关系,工程问题', NULL, NULL, 1),
(106, '行测', '数量关系', '全国', 'SINGLE', '中等', '平台自建', '一个长方形长增加20%，宽减少20%，则面积变化情况是：', '面积变为1.2×0.8=0.96，减少4%。', 'A', '数量关系,几何与比例', NULL, NULL, 1),
(107, '行测', '数量关系', '江苏', 'SINGLE', '基础', '平台自建', '某数的25%等于30，则该数的40%等于多少？', '原数=30÷25%=120，40%=48。', 'C', '数量关系,百分数', NULL, NULL, 1),
(108, '行测', '数量关系', '广东', 'SINGLE', '较难', '平台自建', '把1至9这9个数字分别填入3×3方格中，每行每列数字和都相等，则每行数字和应为多少？', '1到9和为45，平均分成3行，每行和为15。', 'B', '数量关系,数字推理', NULL, NULL, 1),
(109, '行测', '数量关系', '浙江', 'SINGLE', '中等', '平台自建', '商店购进一批商品，按定价售出可获利25%，若按定价的九折售出，利润率为：', '定价=1.25成本，九折售价=1.125成本，利润率为12.5%。', 'D', '数量关系,利润', NULL, NULL, 1),
(110, '行测', '数量关系', '全国', 'SINGLE', '基础', '平台自建', '一个水池单开进水管6小时注满，单开排水管9小时排空。若同时打开两管，注满水池需要多少小时？', '效率和=1/6-1/9=1/18，故需18小时。', 'A', '数量关系,工程问题', NULL, NULL, 1),
(111, '行测', '数量关系', '江苏', 'SINGLE', '中等', '平台自建', '某单位安排5人参加会议，从中选出2人发言，共有多少种不同选法？', '组合数C(5,2)=10。', 'B', '数量关系,排列组合', NULL, NULL, 1),
(112, '行测', '数量关系', '广东', 'SINGLE', '中等', '平台自建', '甲车从A地到B地需4小时，乙车从B地到A地需6小时。若两车同时出发相向而行，甲车在相遇时走了全程的几分之几？', '速度比=1/4:1/6=3:2，甲走了3/5。', 'C', '数量关系,行程', NULL, NULL, 1),
(113, '行测', '数量关系', '浙江', 'SINGLE', '较难', '平台自建', '某商品连续两次提价，第一次上涨10%，第二次上涨20%，则现价比原价上涨了：', '总倍数1.1×1.2=1.32，上涨32%。', 'D', '数量关系,百分数', NULL, NULL, 1),
(114, '行测', '数量关系', '全国', 'SINGLE', '基础', '平台自建', '一列数按1，3，5，7，9……排列，第20个数是多少？', '奇数列第n项为2n-1，第20项为39。', 'A', '数量关系,数列', NULL, NULL, 1),
(115, '行测', '数量关系', '江苏', 'SINGLE', '中等', '平台自建', '有红、蓝、黄三种小旗各若干面，现从中任选两面且颜色不同，共有多少种颜色组合？', '三种颜色任选两种，共C(3,2)=3种。', 'B', '数量关系,组合', NULL, NULL, 1),
(116, '行测', '数量关系', '广东', 'SINGLE', '基础', '平台自建', '某人步行上山每小时4千米，下山每小时6千米，往返全程共需5小时。已知上山、下山为同一路程，则单程多少千米？', '设单程x，则x/4+x/6=5，解得x=12。', 'C', '数量关系,行程', NULL, NULL, 1),
(117, '行测', '数量关系', '浙江', 'SINGLE', '较难', '平台自建', '将若干本书平均分给6名学生，余2本；若平均分给8名学生，也余2本。已知书本总数不足100本，则书本总数最多为多少本？', '总数减2后是6和8的公倍数，小于98的最大公倍数为72，故总数最大为74。', 'D', '数量关系,余数问题', NULL, NULL, 1),
(118, '行测', '数量关系', '全国', 'SINGLE', '中等', '平台自建', '一项任务，甲完成所用时间是乙的2倍。若甲、乙效率之和为每小时完成1/6，则乙单独完成任务需要多少小时？', '甲效率为乙一半，设乙为2x，甲为x，则3x=1/6，乙=1/9，需9小时。', 'A', '数量关系,工程问题', NULL, NULL, 1),
(119, '行测', '数量关系', '江苏', 'SINGLE', '中等', '平台自建', '某公司今年员工总数比去年增加了15%，其中技术人员增加了20%。若去年技术人员占员工总数的40%，且今年技术人员占比提高，则今年技术人员占员工总数的比重最接近：', '相对比重变化约为1.2÷1.15×40%=41.7%。', 'B', '数量关系,比重计算', NULL, NULL, 1),
(120, '行测', '数量关系', '广东', 'SINGLE', '基础', '平台自建', '某停车场前1小时收费6元，以后每小时收费4元，不足1小时按1小时计。若一辆车停放3小时20分钟，应收费多少元？', '共按4小时计，收费6+4×3=18元。', 'C', '数量关系,分段计费', NULL, NULL, 1),
(121, '行测', '数量关系', '浙江', 'SINGLE', '中等', '平台自建', '甲、乙、丙三人共有190本书。甲比乙多20本，乙比丙多10本。则甲有多少本书？', '设丙为x，则乙为x+10，甲为x+30，总和为3x+40=190，解得x=50，因此甲为80。', 'A', '数量关系,方程', NULL, NULL, 1);

INSERT INTO question_option (question_id, option_key, option_content, sort_no) VALUES
(22, 'A', '各部门分别建设独立系统，避免数据交换风险', 1),
(22, 'B', '围绕高频事项打通业务链条和数据接口', 2),
(22, 'C', '只保留线下窗口，确保人工把关', 3),
(22, 'D', '将所有审批权集中到一个部门统一办理', 4),
(23, 'A', '直接要求撤销处罚决定', 1),
(23, 'B', '自行变更处罚种类', 2),
(23, 'C', '申请听证', 3),
(23, 'D', '拒绝提交任何陈述申辩材料', 4),
(24, 'A', '及时联系失主或送交公安机关', 1),
(24, 'B', '自行保管一年后据为己有', 2),
(24, 'C', '将现金取出后再归还钱包', 3),
(24, 'D', '通过网络公开钱包内全部证件信息', 4),
(25, 'A', '冰箱冷藏室可以长期存放所有熟食', 1),
(25, 'B', '水果发霉后切掉坏的部分即可放心食用', 2),
(25, 'C', '生熟食品可放在同一砧板连续处理', 3),
(25, 'D', '冷冻肉类应避免反复解冻', 4),
(26, 'A', '立刻乘坐电梯下楼', 1),
(26, 'B', '就近躲在结实家具旁并护住头部', 2),
(26, 'C', '跑到阳台边等待救援', 3),
(26, 'D', '站在窗前观察外部情况', 4),
(27, 'A', '要统筹节能降碳与高质量发展', 1),
(27, 'B', '碳中和就是完全停止使用化石能源', 2),
(27, 'C', '实现碳达峰后不再需要减排', 3),
(27, 'D', '碳交易与绿色低碳转型无关', 4),
(28, 'A', '尚未形成的内部讨论记录', 1),
(28, 'B', '依法确定为国家秘密的信息', 2),
(28, 'C', '关系群众切身利益的政策措施', 3),
(28, 'D', '仅供机关内部培训使用的资料', 4),
(29, 'A', '所有网购商品一律适用七日无理由退货', 1),
(29, 'B', '拆封后的音像制品仍必须支持无理由退货', 2),
(29, 'C', '定作商品必须无条件退货', 3),
(29, 'D', '鲜活易腐商品一般不适用七日无理由退货', 4),
(30, 'A', '立即往锅内泼大量清水', 1),
(30, 'B', '迅速盖上锅盖并关闭火源', 2),
(30, 'C', '端起油锅冲向水池', 3),
(30, 'D', '用电风扇对着火苗吹', 4),
(31, 'A', '十五日', 1),
(31, 'B', '一个月', 2),
(31, 'C', '二个月', 3),
(31, 'D', '六个月', 4),
(32, 'A', '自申请日起二十年', 1),
(32, 'B', '自授权日起十年', 2),
(32, 'C', '自公开日起十五年', 3),
(32, 'D', '自缴费日起二十年', 4),
(33, 'A', '请示可以一文多事，提高行文效率', 1),
(33, 'B', '请示一般应当一文一事', 2),
(33, 'C', '批复只能口头作出，不宜形成文件', 3),
(33, 'D', '报告事项必须同时附带审批请求', 4),
(34, 'A', '申请复议后原决定必然停止执行', 1),
(34, 'B', '行政复议只能审查事实，不能审查适用法律', 2),
(34, 'C', '对所有行政行为都必须先复议后诉讼', 3),
(34, 'D', '行政复议期间原决定一般不停止执行', 4),
(35, 'A', '按压频率一般为每分钟100到120次', 1),
(35, 'B', '按压越快越好，不必控制节奏', 2),
(35, 'C', '按压时只需按胸骨上端即可', 3),
(35, 'D', '按压与呼吸比例完全固定，不分成人儿童', 4),
(36, 'A', '天气类应用读取全部通讯录', 1),
(36, 'B', '手电筒应用要求长期定位权限', 2),
(36, 'C', '打车应用仅获取行程相关定位信息', 3),
(36, 'D', '记账应用默认上传全部相册内容', 4),
(37, 'A', '闪电总是在更高处发生', 1),
(37, 'B', '光传播速度快于声音', 2),
(37, 'C', '雷声需要借助云层反射', 3),
(37, 'D', '人眼比耳朵更敏感', 4),
(38, 'A', '只要发展农业规模经营即可实现乡村振兴', 1),
(38, 'B', '乡村振兴重点在于单纯增加财政补贴', 2),
(38, 'C', '乡村振兴与生态保护目标存在天然冲突', 3),
(38, 'D', '乡村振兴强调产业、人才、文化、生态、组织协同推进', 4),
(39, 'A', '压减一般性支出，保障重点支出', 1),
(39, 'B', '一律暂停公共服务投入', 2),
(39, 'C', '将所有结转资金平均分配', 3),
(39, 'D', '不再开展任何新增项目建设', 4),
(40, 'A', '制造氧气功能', 1),
(40, 'B', '地热储能功能', 2),
(40, 'C', '净化和调蓄功能', 3),
(40, 'D', '提高海拔功能', 4),
(41, 'A', '优先向少数大型城市集中资源', 1),
(41, 'B', '推动优质文化资源向基层延伸', 2),
(41, 'C', '减少数字化公共文化供给', 3),
(41, 'D', '以商业盈利替代基本公共文化服务', 4),
(42, 'A', '统筹', 1),
(42, 'B', '割裂', 2),
(42, 'C', '搁置', 3),
(42, 'D', '冲淡', 4),
(43, 'A', '回避', 1),
(43, 'B', '观望', 2),
(43, 'C', '拆解', 3),
(43, 'D', '防治', 4),
(44, 'A', '雷厉风行', 1),
(44, 'B', '换位思考', 2),
(44, 'C', '闭门决策', 3),
(44, 'D', '随机应变', 4),
(45, 'A', '水到渠成', 1),
(45, 'B', '循序渐进', 2),
(45, 'C', '变形走样', 3),
(45, 'D', '相得益彰', 4),
(46, 'A', '审慎', 1),
(46, 'B', '热情', 2),
(46, 'C', '轻松', 3),
(46, 'D', '迟缓', 4),
(47, 'A', '壁垒', 1),
(47, 'B', '偏差', 2),
(47, 'C', '断层', 3),
(47, 'D', '闭环', 4),
(48, 'A', '模仿', 1),
(48, 'B', '沉淀', 2),
(48, 'C', '喧闹', 3),
(48, 'D', '炫示', 4),
(49, 'A', '规模', 1),
(49, 'B', '表象', 2),
(49, 'C', '分寸', 3),
(49, 'D', '篇幅', 4),
(50, 'A', '长远', 1),
(50, 'B', '零碎', 2),
(50, 'C', '局促', 3),
(50, 'D', '含混', 4),
(51, 'A', '停滞', 1),
(51, 'B', '折返', 2),
(51, 'C', '回避', 3),
(51, 'D', '焕新', 4),
(52, 'A', '线上办理必然比线下办理更高效', 1),
(52, 'B', '政务数字化重在流程再造而非形式迁移', 2),
(52, 'C', '数据共享是所有政务服务改革的唯一手段', 3),
(52, 'D', '群众对网上办事的需求已经明显下降', 4),
(53, 'A', '短视频不适合传播传统文化', 1),
(53, 'B', '年轻人只愿意通过短视频接触传统文化', 2),
(53, 'C', '新媒介传播传统文化要兼顾效果与内容质量', 3),
(53, 'D', '传统文化一旦娱乐化就失去传播价值', 4),
(54, 'A', '共享模式能否成功取决于具体应用场景', 1),
(54, 'B', '共享经济已经失去发展空间', 2),
(54, 'C', '维护成本是共享经济成败的决定因素', 3),
(54, 'D', '共享理念本身存在明显缺陷', 4),
(55, 'A', '提高专注力只能依靠长期训练', 1),
(55, 'B', '意志力强的人不受环境影响', 2),
(55, 'C', '工作效率与任务拆分没有关系', 3),
(55, 'D', '改善专注状态需要同时调整环境和方法', 4),
(56, 'A', '博物馆为什么比商场更受欢迎', 1),
(56, 'B', '文化消费升级更看重深度体验', 2),
(56, 'C', '年轻人为何热衷拍照打卡博物馆', 3),
(56, 'D', '博物馆运营方式必须全面娱乐化', 4),
(57, 'A', '复杂问题往往没有标准答案', 1),
(57, 'B', '各部门目标完全不可能一致', 2),
(57, 'C', '跨部门问题要靠机制协同来解决', 3),
(57, 'D', '明确责任边界会降低工作效率', 4),
(58, 'A', '围绕核心需求做减法和取舍', 1),
(58, 'B', '尽可能增加功能覆盖全部场景', 2),
(58, 'C', '只要收集到用户意见就能设计好产品', 3),
(58, 'D', '产品复杂度越高越能体现专业性', 4),
(59, 'A', '长文章不适合快速阅读', 1),
(59, 'B', '阅读速度一定会影响理解深度', 2),
(59, 'C', '所有文章都应逐字精读', 3),
(59, 'D', '阅读重在理解而非一味求快', 4),
(60, 'A', '基层创新必须依赖先进技术', 1),
(60, 'B', '对既有流程的优化也是重要创新', 2),
(60, 'C', '只有群众直接参与才能形成创新', 3),
(60, 'D', '小改动通常不足以改善服务体验', 4),
(61, 'A', '夜间经济不宜在中小城市发展', 1),
(61, 'B', '增加活动场次是发展夜间经济的核心', 2),
(61, 'C', '夜间经济发展要同步完善配套服务', 3),
(61, 'D', '交通接驳是夜间经济成败的唯一因素', 4),
(62, 'A', '不少群众支持增加灵活办事时段', 1),
(62, 'B', '该地同时优化了事项流程并增设导办人员', 2),
(62, 'C', '其他地区也在尝试错时服务', 3),
(62, 'D', '满意度数据来自第三方调查', 4),
(63, 'A', '活动预算较去年有所增加', 1),
(63, 'B', '社区宣传渠道更加丰富', 2),
(63, 'C', '部分居民对活动组织表示认可', 3),
(63, 'D', '新增活动大多内容重复，居民参与热情下降', 4),
(64, 'A', '复杂问题多次转接，平均解决时长反而上升', 1),
(64, 'B', '智能客服可以24小时在线服务', 2),
(64, 'C', '企业减少了夜班人工排班', 3),
(64, 'D', '客户咨询总量较去年有所增长', 4),
(65, 'A', '学校新增了图书角和阅读课程', 1),
(65, 'B', '参加阅读活动的学生普遍更喜欢文学', 2),
(65, 'C', '这些学生的家庭教育资源也明显更丰富', 3),
(65, 'D', '阅读活动通常安排在课后进行', 4),
(66, 'A', '预约系统上线初期需要额外培训', 1),
(66, 'B', '景区停车拥堵和周边接驳问题仍较突出', 2),
(66, 'C', '大部分游客认可预约分流措施', 3),
(66, 'D', '分时预约减少了高峰时段排队时间', 4),
(67, 'A', '部分技能培训离开现场实操后效果明显下降', 1),
(67, 'B', '线上培训可以覆盖更多地区学员', 2),
(67, 'C', '线下培训需要场地和交通安排', 3),
(67, 'D', '不同培训方式的预算构成不同', 4),
(68, 'A', '团队成员普遍愿意参加会议', 1),
(68, 'B', '项目周期较长，需要定期沟通', 2),
(68, 'C', '会议记录可以帮助后续复盘', 3),
(68, 'D', '会议过多但议题不清，导致关键信息仍未同步', 4),
(69, 'A', '电子围栏建设投入较大', 1),
(69, 'B', '骑行人数在周末明显更多', 2),
(69, 'C', '部分区域围栏模糊，违规停放仍然频繁', 3),
(69, 'D', '原有站点设备老化严重', 4),
(70, 'A', '学生对体育课内容喜好差异较大', 1),
(70, 'B', '规律运动的学生课堂专注度和出勤率更高', 2),
(70, 'C', '学校操场近年完成了改造升级', 3),
(70, 'D', '体育老师希望增加课程展示机会', 4),
(71, 'A', '大部分低频工具在不同家庭之间可错峰使用', 1),
(71, 'B', '共享工具柜建设需要物业配合', 2),
(71, 'C', '部分居民更习惯网上租赁工具', 3),
(71, 'D', '工具损耗率与使用频次有关', 4),
(72, 'A', '员工对新系统的熟悉程度存在差异', 1),
(72, 'B', '财务制度调整需要一定适应期', 2),
(72, 'C', '线上审批会保留完整操作记录', 3),
(72, 'D', '原流程中等待时间主要耗费在纸质单据层层传递上', 4),
(73, 'A', '讲解志愿者需要统一培训口径', 1),
(73, 'B', '讲解时长受场馆开放时间限制', 2),
(73, 'C', '亲子观众和专业观众对讲解深度需求差异明显', 3),
(73, 'D', '门票价格近期没有调整', 4),
(74, 'A', '公司将一个岗位拆分为上午班和夜班，由不同部门各自负责', 1),
(74, 'B', '两名会计按排班共同负责同一个会计岗位的全部工作', 2),
(74, 'C', '企业在旺季临时聘用兼职人员负责仓库搬运', 3),
(74, 'D', '员工同时兼任两个不同岗位职责', 4),
(75, 'A', '空乘人员在航班延误时依然保持耐心安抚乘客', 1),
(75, 'B', '游客在景区因排队过久表达不满', 2),
(75, 'C', '学生在课堂上认真做笔记', 3),
(75, 'D', '工程师加班修改程序漏洞', 4),
(76, 'A', '平台根据用户主动设置推荐不同内容', 1),
(76, 'B', '搜索引擎按关键词相关度排序结果', 2),
(76, 'C', '地图软件根据实时路况规划路线', 3),
(76, 'D', '招聘系统因历史样本偏差明显降低女性简历通过率', 4),
(77, 'A', '商品打折后销量增加的幅度', 1),
(77, 'B', '企业购置新设备的市场报价', 2),
(77, 'C', '生产计划中增加一小时机器使用使利润增加300元', 3),
(77, 'D', '投资者对未来收益的主观预期', 4),
(78, 'A', '执行', 1),
(78, 'B', '设计', 2),
(78, 'C', '宣传', 3),
(78, 'D', '修改', 4),
(79, 'A', '备课', 1),
(79, 'B', '考试', 2),
(79, 'C', '阅卷', 3),
(79, 'D', '授课', 4),
(80, 'A', '讨论', 1),
(80, 'B', '落地', 2),
(80, 'C', '推翻', 3),
(80, 'D', '搁置', 4),
(81, 'A', '项目一定提前完成', 1),
(81, 'B', '项目预算一定减少', 2),
(81, 'C', '项目增加了预算', 3),
(81, 'D', '无法判断预算情况', 4),
(82, 'A', '60', 1),
(82, 'B', '62', 2),
(82, 'C', '64', 3),
(82, 'D', '66', 4),
(83, 'A', '16%', 1),
(83, 'B', '20%', 2),
(83, 'C', '24%', 3),
(83, 'D', '28%', 4),
(84, 'A', '1.05倍', 1),
(84, 'B', '1.10倍', 2),
(84, 'C', '1.13倍', 3),
(84, 'D', '1.25倍', 4),
(85, 'A', '18%', 1),
(85, 'B', '20%', 2),
(85, 'C', '22%', 3),
(85, 'D', '25%', 4),
(86, 'A', '40', 1),
(86, 'B', '42', 2),
(86, 'C', '44', 3),
(86, 'D', '46', 4),
(87, 'A', '20%', 1),
(87, 'B', '25%', 2),
(87, 'C', '30%', 3),
(87, 'D', '35%', 4),
(88, 'A', '90%', 1),
(88, 'B', '92%', 2),
(88, 'C', '95%', 3),
(88, 'D', '98%', 4),
(89, 'A', '6', 1),
(89, 'B', '8', 2),
(89, 'C', '9', 3),
(89, 'D', '10', 4),
(90, 'A', '40%', 1),
(90, 'B', '42%', 2),
(90, 'C', '45%', 3),
(90, 'D', '48%', 4),
(91, 'A', '7.8万', 1),
(91, 'B', '8.0万', 2),
(91, 'C', '8.1万', 3),
(91, 'D', '8.4万', 4),
(92, 'A', '20%', 1),
(92, 'B', '25%', 2),
(92, 'C', '30%', 3),
(92, 'D', '35%', 4),
(93, 'A', '12', 1),
(93, 'B', '13', 2),
(93, 'C', '14', 3),
(93, 'D', '15', 4),
(94, 'A', '门诊量增量一定大于住院量增量', 1),
(94, 'B', '住院量占门诊量比重超过10%', 2),
(94, 'C', '门诊量同比增速高于住院量', 3),
(94, 'D', '住院量同比增速高于门诊量', 4),
(95, 'A', '20%', 1),
(95, 'B', '22%', 2),
(95, 'C', '25%', 3),
(95, 'D', '28%', 4),
(96, 'A', '15%', 1),
(96, 'B', '20%', 2),
(96, 'C', '25%', 3),
(96, 'D', '30%', 4),
(97, 'A', '25%', 1),
(97, 'B', '30%', 2),
(97, 'C', '35%', 3),
(97, 'D', '40%', 4),
(98, 'A', '20%', 1),
(98, 'B', '25%', 2),
(98, 'C', '30%', 3),
(98, 'D', '33%', 4),
(99, 'A', '30', 1),
(99, 'B', '32', 2),
(99, 'C', '36', 3),
(99, 'D', '40', 4),
(100, 'A', '3.0', 1),
(100, 'B', '3.2', 2),
(100, 'C', '3.4', 3),
(100, 'D', '3.6', 4),
(101, 'A', '20%', 1),
(101, 'B', '25%', 2),
(101, 'C', '30%', 3),
(101, 'D', '33%', 4),
(102, 'A', '200', 1),
(102, 'B', '180', 2),
(102, 'C', '220', 3),
(102, 'D', '240', 4),
(103, 'A', '21', 1),
(103, 'B', '22', 2),
(103, 'C', '24', 3),
(103, 'D', '26', 4),
(104, 'A', '30', 1),
(104, 'B', '36', 2),
(104, 'C', '42', 3),
(104, 'D', '48', 4),
(105, 'A', '6', 1),
(105, 'B', '6.5', 2),
(105, 'C', '7', 3),
(105, 'D', '7.2', 4),
(106, 'A', '减少4%', 1),
(106, 'B', '不变', 2),
(106, 'C', '增加4%', 3),
(106, 'D', '增加8%', 4),
(107, 'A', '36', 1),
(107, 'B', '42', 2),
(107, 'C', '48', 3),
(107, 'D', '54', 4),
(108, 'A', '12', 1),
(108, 'B', '15', 2),
(108, 'C', '18', 3),
(108, 'D', '21', 4),
(109, 'A', '5%', 1),
(109, 'B', '8%', 2),
(109, 'C', '10%', 3),
(109, 'D', '12.5%', 4),
(110, 'A', '18', 1),
(110, 'B', '15', 2),
(110, 'C', '12', 3),
(110, 'D', '9', 4),
(111, 'A', '8', 1),
(111, 'B', '10', 2),
(111, 'C', '12', 3),
(111, 'D', '20', 4),
(112, 'A', '1/2', 1),
(112, 'B', '2/3', 2),
(112, 'C', '3/5', 3),
(112, 'D', '4/7', 4),
(113, 'A', '20%', 1),
(113, 'B', '28%', 2),
(113, 'C', '30%', 3),
(113, 'D', '32%', 4),
(114, 'A', '39', 1),
(114, 'B', '41', 2),
(114, 'C', '37', 3),
(114, 'D', '35', 4),
(115, 'A', '2', 1),
(115, 'B', '3', 2),
(115, 'C', '4', 3),
(115, 'D', '6', 4),
(116, 'A', '10', 1),
(116, 'B', '11', 2),
(116, 'C', '12', 3),
(116, 'D', '15', 4),
(117, 'A', '66', 1),
(117, 'B', '68', 2),
(117, 'C', '72', 3),
(117, 'D', '74', 4),
(118, 'A', '9', 1),
(118, 'B', '12', 2),
(118, 'C', '15', 3),
(118, 'D', '18', 4),
(119, 'A', '40%', 1),
(119, 'B', '42%', 2),
(119, 'C', '45%', 3),
(119, 'D', '48%', 4),
(120, 'A', '14', 1),
(120, 'B', '16', 2),
(120, 'C', '18', 3),
(120, 'D', '20', 4),
(121, 'A', '80', 1),
(121, 'B', '70', 2),
(121, 'C', '65', 3),
(121, 'D', '60', 4);
-- stage 23 extra question bank end

INSERT INTO exam (id, title, intro, province, exam_type, season_name, subject_name, duration_minutes, total_score, question_count, status, ranking_enabled, start_time, end_time) VALUES
(1, '2026 国考行测月度模考第一季', '覆盖常识、言语、判断、资料分析与数量关系。', '全国', '国考', '第一季', '行测', 120, 100, 2, 'OPEN', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY)),
(2, '江苏申论阶段测评', '聚焦概括、分析、对策与写作。', '江苏', '省考', '春季', '申论', 150, 100, 2, 'OPEN', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY)),
(3, '广东事业编综合能力模考', '适合事业编综合科目阶段冲刺。', '广东', '事业编', '冲刺季', '综合能力', 90, 100, 2, 'OPEN', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 25 DAY));

INSERT INTO exam_question (exam_id, question_id, score, sort_no) VALUES
(1, 1, 50, 1),
(1, 2, 50, 2),
(2, 3, 50, 1),
(2, 4, 50, 2),
(3, 2, 50, 1),
(3, 5, 50, 2);

INSERT INTO exam_analysis_course (exam_id, course_id, recommend_reason, sort_no) VALUES
(1, 1, '覆盖行测高频题型，适合模考后回到系统课补齐薄弱模块。', 1),
(1, 4, '适合模考后用短周期冲刺课程强化资料分析与判断推理。', 2),
(2, 2, '聚焦申论概括、分析和写作，适合模考后做专题补强。', 1),
(3, 3, '适合综合能力模考后做结构化补强与阶段复盘。', 1);

INSERT INTO course_order (id, order_no, user_id, total_amount, pay_amount, item_count, order_status, payment_status, payment_channel, remark, paid_at, created_at) VALUES
(1, 'ORD202604100001', 2, 1999.00, 1999.00, 1, 'PAID', 'SUCCESS', 'ALIPAY', '历史支付成功订单', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'ORD202604100002', 5, 499.00, 0.00, 1, 'PENDING', 'UNPAID', NULL, '待支付订单', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO course_order_item (order_id, course_id, course_title, course_cover_url, unit_price, quantity) VALUES
(1, 1, '2026 国考行测系统班', 'https://dummyimage.com/600x360/1d4ed8/ffffff&text=行测系统班', 1999.00, 1),
(2, 3, '广东事业编面试冲刺营', 'https://dummyimage.com/600x360/0f766e/ffffff&text=面试冲刺', 499.00, 1);

INSERT INTO payment_log (order_id, user_id, payment_channel, payment_amount, payment_status, transaction_no, paid_at, created_at) VALUES
(1, 2, 'ALIPAY', 1999.00, 'SUCCESS', 'PAY202604100001', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO course_order_refund (id, refund_no, order_id, user_id, refund_amount, refund_reason, refund_status, requested_at, reviewed_by, reviewed_at, finished_at, review_remark, created_at, updated_at) VALUES
(1, 'RFD202604120001', 1, 2, 1999.00, '课程内容与当前备考进度不匹配，申请退款', 'REQUESTED', DATE_SUB(NOW(), INTERVAL 8 HOUR), NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR));

INSERT INTO course_order_refund_log (refund_id, order_id, action_type, from_status, to_status, operator_id, operator_role, operator_name, remark, created_at) VALUES
(1, 1, 'REQUEST', NULL, 'REQUESTED', 2, 'USER', '上岸冲刺生', '课程内容与当前备考进度不匹配，申请退款', DATE_SUB(NOW(), INTERVAL 8 HOUR));

INSERT INTO user_course (user_id, course_id, source_order_id, expire_at, created_at) VALUES
(2, 1, 1, DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 365 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 4, NULL, DATE_ADD(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY));

INSERT INTO user_course_study_progress (user_id, course_id, current_chapter_id, completed_chapter_count, progress_percent, total_study_seconds, last_study_at, created_at, updated_at) VALUES
(2, 1, 2, 1, 33.33, 4200, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(2, 4, 7, 0, 0.00, 600, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO user_course_chapter_progress (user_id, course_id, chapter_id, study_seconds, progress_percent, completed, first_started_at, last_study_at, completed_at, created_at, updated_at) VALUES
(2, 1, 1, 2400, 100.00, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 1, 2, 1800, 65.00, 0, DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(2, 4, 7, 600, 25.00, 0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO user_course_learning_record (user_id, course_id, chapter_id, watch_seconds, progress_percent, completed, action_type, created_at) VALUES
(2, 1, 1, 900, 35.00, 0, 'STUDY', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2, 1, 1, 1500, 100.00, 1, 'COMPLETE', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 1, 2, 1200, 45.00, 0, 'STUDY', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 4, 7, 600, 25.00, 0, 'STUDY', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 1, 2, 600, 65.00, 0, 'STUDY', DATE_SUB(NOW(), INTERVAL 6 HOUR));

INSERT INTO user_study_plan (id, user_id, plan_name, target_exam_type, subject_name, daily_study_minutes, daily_question_count, weekly_exam_count, duration_days, start_date, end_date, focus_knowledge_points, status, archived_at, total_task_count, completed_task_count, completion_rate) VALUES
(1, 2, '2026 国考申论成长计划', '国考', '申论', 45, 8, 1, 10, DATE_SUB(CURDATE(), INTERVAL 16 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY), '归纳概括,综合分析,提出对策', 'ARCHIVED', DATE_SUB(NOW(), INTERVAL 6 DAY), 22, 18, 81.82),
(2, 2, '省考行测成长计划', '省考', '行测', 60, 15, 1, 14, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 11 DAY), '言语理解,资料分析,判断推理', 'ACTIVE', NULL, 0, 0, 0.00);

INSERT INTO user_daily_task (id, user_id, plan_id, task_date, task_type, title, description_text, target_value, completed_value, status, action_path) VALUES
(1, 2, 2, CURDATE(), 'CHECK_IN', '今日学习打卡', '完成一次题库学习打卡，建立每日学习连续性。', 1, 1, 'COMPLETED', '/question-bank'),
(2, 2, 2, CURDATE(), 'LEARN', '今日课程学习', '完成课程学习 60 分钟，保持学习节奏。', 60, 30, 'IN_PROGRESS', '/my-course'),
(3, 2, 2, CURDATE(), 'PRACTICE', '今日专项刷题', '完成 15 道专项练习，巩固行测基础。', 15, 2, 'IN_PROGRESS', '/question-bank'),
(4, 2, 2, CURDATE(), 'EXAM', '本周阶段模考', '本周至少完成 1 次模考检验当前水平。', 1, 0, 'PENDING', '/exam'),
(5, 2, 2, CURDATE(), 'TEST_PAPER', '个性化测卷强化', '围绕薄弱知识点生成 1 套强化测卷。', 1, 0, 'PENDING', '/user/test-paper');

INSERT INTO user_daily_checkin (id, user_id, checkin_date, source_type, source_record_id, streak_days, created_at) VALUES
(1, 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'PRACTICE', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'AUTO', 1, 2, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 2, CURDATE(), 'MANUAL', NULL, 3, DATE_SUB(NOW(), INTERVAL 2 HOUR));

INSERT INTO user_knowledge_mastery (user_id, subject_name, knowledge_point, attempt_count, correct_count, wrong_count, pending_wrong_count, accuracy_rate, mastery_score, mastery_confidence, priority_score, mastery_level, knowledge_status, last_practiced_at) VALUES
(2, '行测', '言语理解', 2, 1, 1, 0, 50.00, 46.00, 35.00, 58.00, 'FOUNDATION', 'TO_DIAGNOSE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, '行测', '资料分析', 2, 0, 2, 1, 0.00, 12.00, 32.00, 88.00, 'WEAK', 'TO_DIAGNOSE', DATE_SUB(NOW(), INTERVAL 19 HOUR)),
(2, '行测', '判断推理', 1, 0, 1, 0, 0.00, 8.00, 28.00, 82.00, 'WEAK', 'TO_DIAGNOSE', DATE_SUB(NOW(), INTERVAL 10 HOUR));

INSERT INTO user_test_paper (id, user_id, title, subject_name, category_name, difficulty_level, question_count, generate_mode, paper_type, knowledge_points, target_knowledge_points, recommendation_reason, generation_snapshot, difficulty_distribution, source_strategy) VALUES
(1, 2, '行测弱项强化测卷（3题）', '行测', NULL, '中等', 3, 'WEAKNESS', 'WEAKNESS', '言语理解,资料分析,判断推理', '言语理解,资料分析,判断推理', '根据近期错题与薄弱知识点生成的强化测卷', '{"paperType":"WEAKNESS","sourceStrategy":"RULE_ENHANCED_V1"}', 'EASY:1,MEDIUM:2,HARD:0', 'RULE_ENHANCED_V1');

INSERT INTO user_test_paper_question (test_paper_id, question_id, sort_no, question_score) VALUES
(1, 1, 1, 1),
(1, 2, 2, 1),
(1, 4, 3, 1);

INSERT INTO user_growth_achievement (id, user_id, plan_id, task_id, achievement_type, title, description_text, achievement_value, created_at) VALUES
(1, 2, 1, NULL, 'PLAN_ARCHIVED', '成长计划归档', '上一轮申论成长计划已归档，完成率达到 81.82%。', 82, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2, 2, 2, 1, 'TASK_COMPLETED', '完成今日学习打卡', '已完成今日题库学习打卡。', 1, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(3, 2, 2, 2, 'TASK_COMPLETED', '完成学习时长任务', '已完成今日课程学习目标 60 分钟。', 60, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(4, 2, 2, 3, 'TASK_COMPLETED', '完成专项刷题任务', '已完成今日专项刷题目标 15 题。', 15, DATE_SUB(NOW(), INTERVAL 2 HOUR));

INSERT INTO user_growth_diagnosis_report (id, user_id, plan_id, diagnosis_date, diagnosis_stage, vitality_score, plan_completion_rate, knowledge_mastery_rate, test_paper_average_score, weak_knowledge_points, recommendation_text, created_at, updated_at) VALUES
(1, 2, 1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 'FOUNDATION', 63.50, 81.82, 58.00, 66.67, '归纳概括,综合分析', '上一阶段计划执行较稳，建议把申论素材积累继续转化成整卷训练。', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2, 2, 2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'TRACKING', 46.20, 25.00, 22.00, 33.33, '资料分析,判断推理', '当前行测计划执行偏慢，优先完成今日刷题任务，并尽快做一套针对性测卷。', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO admin_growth_focus_follow (id, user_id, plan_id, latest_diagnosis_id, follow_status, priority_level, remark, next_follow_up_at, created_by, updated_by, created_at, updated_at) VALUES
(1, 2, 2, 2, 'IN_PROGRESS', 'HIGH', '重点盯资料分析和判断推理完成率，建议 3 天后回看诊断变化。', DATE_ADD(NOW(), INTERVAL 3 DAY), 1, 1, DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR));

INSERT INTO teacher_growth_focus_follow (id, classroom_id, user_id, plan_id, latest_diagnosis_id, follow_status, priority_level, remark, next_follow_up_at, created_by, updated_by, created_at, updated_at) VALUES
(1, 1, 2, 2, 2, 'IN_PROGRESS', 'HIGH', '先盯住资料分析的每日刷题达成，再补一套判断推理强化测卷。', DATE_ADD(NOW(), INTERVAL 2 DAY), 3, 3, DATE_SUB(NOW(), INTERVAL 10 HOUR), DATE_SUB(NOW(), INTERVAL 10 HOUR));

INSERT INTO admin_growth_focus_follow_history (admin_follow_id, user_id, plan_id, latest_diagnosis_id, follow_status, priority_level, remark, next_follow_up_at, operator_id, action_type, created_at) VALUES
(1, 2, 2, 2, 'PENDING', 'MEDIUM', '初始纳入重点跟进人群。', DATE_ADD(NOW(), INTERVAL 5 DAY), 1, 'CREATE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 2, 2, 2, 'IN_PROGRESS', 'HIGH', '重点盯资料分析和判断推理完成率，建议 3 天后回看诊断变化。', DATE_ADD(NOW(), INTERVAL 3 DAY), 1, 'SAVE', DATE_SUB(NOW(), INTERVAL 12 HOUR));

INSERT INTO teacher_growth_focus_follow_history (teacher_follow_id, classroom_id, user_id, plan_id, latest_diagnosis_id, follow_status, priority_level, remark, next_follow_up_at, operator_id, action_type, created_at) VALUES
(1, 1, 2, 2, 2, 'PENDING', 'MEDIUM', '班级首轮识别为需要重点关注。', DATE_ADD(NOW(), INTERVAL 4 DAY), 3, 'CREATE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 1, 2, 2, 2, 'IN_PROGRESS', 'HIGH', '先盯住资料分析的每日刷题达成，再补一套判断推理强化测卷。', DATE_ADD(NOW(), INTERVAL 2 DAY), 3, 'SAVE', DATE_SUB(NOW(), INTERVAL 10 HOUR));

INSERT INTO admin_growth_dashboard_preference (user_id, segment, min_completion_rate, follow_status, created_at, updated_at) VALUES
(1, 'AT_RISK', 0.00, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR));

INSERT INTO teacher_growth_dashboard_preference (user_id, classroom_id, segment, follow_status, created_at, updated_at) VALUES
(3, 1, 'AT_RISK', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR));

INSERT INTO shopping_cart_item (user_id, course_id, selected, created_at) VALUES
(2, 2, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(5, 1, 1, DATE_SUB(NOW(), INTERVAL 5 HOUR));

INSERT INTO practice_record (id, user_id, practice_type, practice_mode, title, subject_name, province, question_count, answered_count, correct_count, score, accuracy_rate, duration_seconds, status, submitted_at, source_snapshot, created_at, updated_at) VALUES
(1, 2, 'QUESTION_BANK', 'RANDOM', '行测随机练习', '行测', '全国', 2, 2, 1, 1, 50.00, 420, 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 1 DAY), 'question-bank', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 5, 'WRONG_BOOK', 'WRONG_BOOK', '错题再练', '行测', '江苏', 1, 1, 1, 1, 100.00, 120, 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 6 HOUR), 'wrong-book', DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(3, 2, 'GROWTH_ASSESSMENT', 'ASSESSMENT', '独立入学评测（20题）', '行测', '全国', 20, 20, 14, 14, 70.00, 1800, 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 3 HOUR), 'growth-assessment', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR));

INSERT INTO practice_answer (practice_record_id, question_id, user_answer, correct_answer, is_correct, marked, spent_seconds, question_score, created_at) VALUES
(1, 1, 'B', 'B', 1, 0, 180, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 2, 'A', 'C', 0, 1, 240, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 2, 'C', 'C', 1, 0, 120, 1, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(3, 1, 'B', 'B', 1, 0, 80, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 2, 'C', 'C', 1, 0, 80, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 3, 'B', 'A', 0, 0, 80, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 4, 'D', 'D', 1, 0, 90, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 5, 'A', 'A', 1, 0, 90, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 7, 'B', 'B', 1, 0, 75, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 8, 'C', 'C', 1, 0, 75, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 9, 'A', 'D', 0, 0, 85, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 10, 'A', 'A', 1, 0, 80, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 11, 'A', 'B', 0, 0, 85, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 12, 'C', 'C', 1, 0, 75, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 13, 'A', 'A', 1, 0, 75, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 14, 'A', 'D', 0, 0, 90, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 15, 'C', 'B', 0, 0, 90, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 16, 'C', 'C', 1, 0, 80, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 17, 'A', 'A', 1, 0, 75, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 18, 'D', 'D', 1, 0, 75, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 19, 'A', 'B', 0, 0, 85, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 20, 'A', 'C', 0, 0, 85, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 21, 'A', 'A', 1, 0, 70, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR));

INSERT INTO user_growth_assessment_session (id, user_id, plan_id, practice_record_id, assessment_type, question_count, predicted_pass_rate, predicted_confidence, prediction_stage, weak_knowledge_points, recommendation_text, assessment_status, created_at, updated_at) VALUES
(1, 2, 2, 3, 'ENTRY', 20, 72.00, 84.00, 'PROMISING', '资料分析,判断推理', '当前通过率预测较好，建议结合评测结果继续强化薄弱知识点。', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR));

INSERT INTO user_wrong_question (user_id, question_id, latest_source_type, latest_record_id, latest_answer, removed, wrong_count, last_wrong_at, created_at, updated_at) VALUES
(2, 2, 'PRACTICE', 1, 'A', 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO user_question_favorite (user_id, question_id, created_at) VALUES
(2, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 4, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(5, 2, DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO exam_record (id, exam_id, user_id, title, total_score, question_count, correct_count, score, duration_seconds, status, ranking_enabled, started_at, submitted_at, created_at, updated_at) VALUES
(1, 1, 2, '2026 国考行测月度模考第一季', 100, 2, 1, 50, 3600, 'SUBMITTED', 1, DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_SUB(NOW(), INTERVAL 19 HOUR), DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_SUB(NOW(), INTERVAL 19 HOUR)),
(2, 1, 5, '2026 国考行测月度模考第一季', 100, 2, 2, 100, 3300, 'SUBMITTED', 1, DATE_SUB(NOW(), INTERVAL 10 HOUR), DATE_SUB(NOW(), INTERVAL 9 HOUR), DATE_SUB(NOW(), INTERVAL 10 HOUR), DATE_SUB(NOW(), INTERVAL 9 HOUR));

INSERT INTO exam_answer (exam_record_id, exam_id, question_id, user_answer, correct_answer, is_correct, score, created_at) VALUES
(1, 1, 1, 'B', 'B', 1, 50, DATE_SUB(NOW(), INTERVAL 19 HOUR)),
(1, 1, 2, 'A', 'C', 0, 0, DATE_SUB(NOW(), INTERVAL 19 HOUR)),
(2, 1, 1, 'B', 'B', 1, 50, DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(2, 1, 2, 'C', 'C', 1, 50, DATE_SUB(NOW(), INTERVAL 9 HOUR));

INSERT INTO recommend_config (config_key, config_name, config_value, description_text) VALUES
('collaborative_weight', '协同过滤权重', 0.4000, '行为丰富用户优先'),
('content_weight', '内容推荐权重', 0.4000, '冷启动与画像推荐'),
('hot_weight', '热门推荐权重', 0.2000, '兜底热门资源'),
('pinned_boost', '置顶推荐加权', 0.5000, '后台强制置顶资源优先展示'),
('region_match_boost', '地区匹配加权', 0.2800, '报考地区一致的课程、模考和资讯加权'),
('national_region_boost', '全国兜底加权', 0.1200, '全国资源在地区推荐中的基础保留权重'),
('outside_region_penalty', '外省资源降权', 0.1800, '强地区属性内容与报考地区不一致时降权');

INSERT INTO recommend_pinned_resource (resource_type, resource_id, display_title, display_reason, display_tags, action_text, region_scope, province, fallback_enabled, sort_no, enabled, start_time, end_time) VALUES
('COURSE', 1, '国考冲刺系统课', '运营强制置顶，优先承接首页与推荐页转化', '国考,系统课', '查看课程', 'NATIONAL', '全国', 1, 1, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
('EXAM', 1, '本周重点模考', '固定置顶模考入口，保障推荐池稳定曝光', '模考,冲刺', '查看模考', 'ALL', NULL, 1, 2, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL);

INSERT INTO system_config (config_group, config_key, config_name, config_value, value_type, description_text, sort_no) VALUES
('PARAM', 'platform_name', '平台名称', '公考学习平台', 'STRING', '前后台统一展示的平台名称', 1),
('PARAM', 'support_email', '客服邮箱', 'support@civil-learning.local', 'STRING', '用户咨询与通知联系邮箱', 2),
('PARAM', 'upload_root', '上传根目录', 'D:/Civil_learning_platform/upload', 'STRING', '文件上传统一落盘目录', 3),
('PARAM', 'admin_register_code', '管理员注册码', 'admin2026', 'STRING', '管理员差异化注册时使用的校验码', 4),
('EXPORT', 'default_max_rows', '导出单次最大行数', '500', 'NUMBER', 'Excel 导出单次允许的最大记录数', 1),
('EXPORT', 'include_timestamp', '导出文件名带时间戳', 'true', 'BOOLEAN', '导出文件名是否自动附带时间戳', 2),
('EXPORT', 'allow_growth_export', '允许成长报告导出', 'true', 'BOOLEAN', '控制成长诊断导出入口开关', 3),
('BROADCAST', 'broadcast_enabled', '广播总开关', 'true', 'BOOLEAN', '控制平台广播配置是否启用', 1),
('BROADCAST', 'broadcast_title', '默认广播标题', '系统学习提醒', 'STRING', '后台广播默认标题', 2),
('BROADCAST', 'broadcast_content', '默认广播内容', '请及时完成今日学习任务并关注最新公告。', 'STRING', '后台广播默认内容', 3);

INSERT INTO system_broadcast_message (id, title, content, scope, sender_id, sender_name, receiver_count, sent_at, created_at, updated_at) VALUES
(1, '系统学习提醒', '请及时完成今日学习任务并关注最新公告。', 'ALL', 1, '系统管理员', 3, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(2, '退款处理通知', '后台治理阶段已补齐退款闭环，收到退款申请后会进入真实审批流。', 'ALL', 1, '系统管理员', 2, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR));

INSERT INTO recommend_feedback (user_id, resource_type, resource_id, feedback_type, source_page, reason_text, created_at) VALUES
(2, 'NEWS', 3, 'NOT_INTERESTED', 'RECOMMEND', '近期更关注课程和模考内容', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'COURSE', 3, 'NOT_INTERESTED', 'RECOMMEND', '当前课程难度不匹配', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 'EXAM', 1, 'NOT_INTERESTED', 'RECOMMEND', '近期先完成班级任务', DATE_SUB(NOW(), INTERVAL 8 HOUR));

INSERT INTO recommend_refresh_log (user_id, refresh_source, created_at) VALUES
(2, 'RECOMMEND', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'RECOMMEND', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(NULL, 'RECOMMEND', DATE_SUB(NOW(), INTERVAL 2 HOUR));

INSERT INTO question (id, subject_name, category_name, province, question_type, difficulty_level, source_name, stem, analysis, answer_value, knowledge_points, video_explain_title, video_explain_url, enabled) VALUES
(122, '行测', '常识判断', '全国', 'MULTI', '中等', '平台自建', '推进基层治理数字化转型时，以下哪些做法符合提升治理效能与数据安全并重的要求？', '基层治理数字化既要提升协同效率，也要落实数据分级分类、最小必要和授权管理。A、C、D都符合要求，B片面追求集中留痕，不符合最小必要原则。', 'A,C,D', '常识判断,数字治理,数据安全', NULL, NULL, 1),
(123, '行测', '言语理解与表达', '江苏', 'MULTI', '中等', '平台自建', '某地推动政务服务升级时提出：“既要让数据多跑路，也要让群众少跑腿；既要提高办事速度，也要保证服务温度。”对这句话理解准确的有：', '文段强调效率与体验并重、技术与服务并重。A、B、D符合，C将二者对立起来，曲解文意。', 'A,B,D', '言语理解,文意理解,主旨概括', NULL, NULL, 1),
(124, '行测', '判断推理', '广东', 'MULTI', '较难', '平台自建', '某单位上线智能排班系统后，负责人认为“只要使用智能算法，排班公平性就一定提高”。以下哪些选项如果为真，能够削弱该结论？', '削弱思路主要包括：算法目标单一、输入数据偏差、人工规则仍可干预结果。A、C、D均能削弱，B反而支持系统有效。', 'A,C,D', '判断推理,削弱,因果关系', NULL, NULL, 1),
(125, '行测', '资料分析', '浙江', 'MULTI', '中等', '平台自建', '某市2024年新建社区食堂120家，同比增长20%；全年服务老年人480万人次，同比增长25%。根据上述信息，可以推出的有：', '2023年社区食堂数为100家，2023年服务人次为384万人次；服务人次增速高于食堂数量增速，平均单店服务量提升。A、B、D正确，C错误。', 'A,B,D', '资料分析,增长率,现期与基期', NULL, NULL, 1),
(126, '行测', '数量关系', '全国', 'MULTI', '中等', '平台自建', '某培训班有行测学员40人，申论学员30人，其中两科都报的有18人。根据这些信息，下列说法正确的有：', '至少报一科的人数为40+30-18=52；只报行测22人，只报申论12人；两科都报人数少于只报行测人数。A、B、D正确，C错误。', 'A,B,D', '数量关系,容斥原理', NULL, NULL, 1),
(127, '行测', '判断推理', '全国', 'MULTI', '中等', '平台自建', '关于公文流转效率提升措施，下列哪些选项之间的逻辑关系与“减少重复审批有助于缩短办理时长”最为接近？', '题干强调“减少冗余环节带来效率提升”。A、C、D都符合这一因果结构，B是条件限制带来的速度下降，不一致。', 'A,C,D', '判断推理,类比推理,逻辑关系', NULL, NULL, 1),
(128, '行测', '言语理解与表达', '浙江', 'MULTI', '较难', '平台自建', '阅读下面这段文字：\n“治理创新并不等于一味求新。真正有效的创新，往往是在看清问题本质之后，对制度、流程和工具进行有针对性的重组与优化。”\n根据这段文字，可以合理推出的有：', '文段强调创新要基于问题导向和制度优化，不是为了新而新。A、C、D均可推出，B与原文相反。', 'A,C,D', '言语理解,推断理解', NULL, NULL, 1),
(129, '行测', '常识判断', '广东', 'MULTI', '基础', '平台自建', '关于个人信息保护和网络安全，下列做法正确的有：', '设置高强度密码、谨慎授权、及时更新系统均有助于信息安全；在公共网络中传输敏感信息风险较高，不宜直接操作。A、B、D正确。', 'A,B,D', '常识判断,网络安全,个人信息保护', NULL, NULL, 1);

INSERT INTO question_option (question_id, option_key, option_content, sort_no) VALUES
(122, 'A', '打通网格员、社区、街道之间的事项协同流程', 1),
(122, 'B', '为便于核验，默认采集与事项无关的全部居民信息', 2),
(122, 'C', '对敏感数据实行分级分类和最小必要访问控制', 3),
(122, 'D', '对高频事项设置留痕审计和授权审批机制', 4),
(123, 'A', '政务服务升级不能只强调速度，还要关注群众体验', 1),
(123, 'B', '数字化改革的目标之一是减少群众办事负担', 2),
(123, 'C', '只要服务态度好，办事效率低也没有问题', 3),
(123, 'D', '提升治理水平需要兼顾技术效率与服务温度', 4),
(124, 'A', '算法以“人员利用率最高”为唯一目标，忽略连续加班问题', 1),
(124, 'B', '系统上线后，员工调班等待时间明显缩短', 2),
(124, 'C', '历史排班数据本身就对某些岗位存在偏向', 3),
(124, 'D', '主管仍可在系统外手工调整结果而不留下说明', 4),
(125, 'A', '2023年该市社区食堂数量为100家', 1),
(125, 'B', '2023年服务老年人约为384万人次', 2),
(125, 'C', '2024年平均每家食堂服务人次低于2023年', 3),
(125, 'D', '服务人次增速高于食堂数量增速', 4),
(126, 'A', '至少报一科的学员有52人', 1),
(126, 'B', '只报行测的学员有22人', 2),
(126, 'C', '只报申论的学员有18人', 3),
(126, 'D', '两科都报的学员少于只报行测的学员', 4),
(127, 'A', '减少重复录入有助于缩短窗口办理时间', 1),
(127, 'B', '增加审批层级往往会延长文件流转时间', 2),
(127, 'C', '合并相近报表有助于降低基层填报负担', 3),
(127, 'D', '取消不必要的证明材料有助于提升办事效率', 4),
(128, 'A', '治理创新需要建立在问题导向基础上', 1),
(128, 'B', '只要形式新颖，就属于有效治理创新', 2),
(128, 'C', '制度和流程优化也是创新的重要组成部分', 3),
(128, 'D', '创新应当服务于问题解决，而非单纯求新', 4),
(129, 'A', '不同平台尽量使用不相同的高强度密码', 1),
(129, 'B', '安装应用时只授权与功能相关的必要权限', 2),
(129, 'C', '连接公共Wi-Fi后直接办理含敏感信息的转账业务', 3),
(129, 'D', '及时更新系统和应用补丁，修补安全漏洞', 4);

SET FOREIGN_KEY_CHECKS = 1;

-- Stage 26 question bank special practice seed

DELETE FROM question_option WHERE question_id BETWEEN 200 AND 234;
DELETE FROM question WHERE id BETWEEN 200 AND 234;

INSERT INTO question (id, subject_name, category_name, province, question_type, difficulty_level, source_name, stem, analysis, answer_value, knowledge_points, video_explain_title, video_explain_url, enabled) VALUES
(200, '行测', '政治理论', '全国', 'SINGLE', '基础', '平台自建', '新时代推进中国式现代化，最根本的政治保证是：', '党的领导是中国式现代化的根本保证，必须坚持和加强党的全面领导。', 'A', '政治理论,中国式现代化', NULL, NULL, 1),
(201, '行测', '政治理论', '全国', 'SINGLE', '中等', '平台自建', '全过程人民民主强调民主实践贯穿民主选举、民主协商、民主决策、民主管理和：', '全过程人民民主覆盖民主运行各环节，其中包括民主监督。', 'C', '政治理论,全过程人民民主', NULL, NULL, 1),
(202, '行测', '政治理论', '江苏', 'SINGLE', '中等', '平台自建', '高质量发展是全面建设社会主义现代化国家的：', '高质量发展是全面建设社会主义现代化国家的首要任务。', 'B', '政治理论,高质量发展', NULL, NULL, 1),
(203, '行测', '政治理论', '广东', 'SINGLE', '基础', '平台自建', '新发展理念中，解决发展动力问题的是：', '创新发展注重解决发展动力问题。', 'A', '政治理论,新发展理念', NULL, NULL, 1),
(204, '行测', '政治理论', '浙江', 'SINGLE', '中等', '平台自建', '推进基层治理现代化，关键在于提升治理的系统性、整体性和：', '基层治理要强化协同联动，提升系统性、整体性和协同性。', 'D', '政治理论,基层治理', NULL, NULL, 1),
(205, '行测', '政治理论', '全国', 'SINGLE', '中等', '平台自建', '关于共同富裕，下列理解最准确的是：', '共同富裕不是平均主义，而是在高质量发展中促进机会公平和收入分配更合理。', 'B', '政治理论,共同富裕', NULL, NULL, 1),
(206, '行测', '政治理论', '江苏', 'SINGLE', '基础', '平台自建', '全面依法治国的总目标是建设中国特色社会主义法治体系、建设社会主义：', '全面依法治国总目标包括建设中国特色社会主义法治体系、建设社会主义法治国家。', 'C', '政治理论,依法治国', NULL, NULL, 1),
(207, '行测', '政治理论', '广东', 'SINGLE', '中等', '平台自建', '坚持人民至上，体现的根本立场是：', '人民立场是中国共产党的根本政治立场。', 'A', '政治理论,人民至上', NULL, NULL, 1),
(208, '行测', '政治理论', '浙江', 'SINGLE', '较难', '平台自建', '推进国家治理体系和治理能力现代化，既要完善制度体系，也要提升制度的：', '制度优势要转化为治理效能，关键是提高制度执行力。', 'D', '政治理论,治理现代化', NULL, NULL, 1),
(209, '行测', '政治理论', '全国', 'SINGLE', '中等', '平台自建', '建设现代化产业体系，应把发展经济的着力点放在：', '建设现代化产业体系应坚持把发展经济的着力点放在实体经济上。', 'B', '政治理论,实体经济', NULL, NULL, 1),
(210, '申论', '归纳概括', '全国', 'SHORT_ANSWER', '基础', '平台自建', '材料反映某地政务服务窗口存在重复填表、跨部门材料不互认、办理进度不透明等问题。请概括主要问题。', '参考答案应围绕流程繁琐、数据共享不足、协同机制不完善、服务透明度不够四方面归纳。', '流程繁琐；数据共享不足；部门协同不畅；办理进度不透明。', '申论,归纳概括,政务服务', NULL, NULL, 1),
(211, '申论', '归纳概括', '江苏', 'SHORT_ANSWER', '中等', '平台自建', '材料介绍老旧小区改造中居民意见分散、停车矛盾突出、后续管护缺位。请概括治理难点。', '治理难点主要包括诉求多元、空间资源不足、资金与管护机制不足、协商平台不健全。', '诉求多元难统一；公共空间紧张；长效管护不足；协商机制不健全。', '申论,归纳概括,基层治理', NULL, NULL, 1),
(212, '申论', '归纳概括', '广东', 'SHORT_ANSWER', '中等', '平台自建', '材料中某县发展乡村旅游，出现同质化经营、配套服务薄弱、品牌传播不足。请概括制约因素。', '应从产品、服务、品牌和人才运营等角度提炼。', '产品同质化；配套服务弱；品牌传播不足；运营人才缺乏。', '申论,归纳概括,乡村振兴', NULL, NULL, 1),
(213, '申论', '归纳概括', '浙江', 'SHORT_ANSWER', '基础', '平台自建', '材料反映社区养老服务供给不足、专业人员短缺、信息匹配不畅。请概括主要短板。', '围绕供给、人员、平台和精细化服务归纳。', '服务供给不足；专业人员短缺；信息匹配不畅；精细化不足。', '申论,归纳概括,养老服务', NULL, NULL, 1),
(214, '申论', '归纳概括', '全国', 'SHORT_ANSWER', '中等', '平台自建', '材料反映青年就业中岗位匹配度低、实习实践不足、职业规划模糊。请概括问题表现。', '应概括就业信息、能力准备和规划意识三类问题。', '岗位匹配度低；实践经验不足；职业规划不清；就业信息不对称。', '申论,归纳概括,就业服务', NULL, NULL, 1),
(215, '申论', '综合分析', '全国', 'SHORT_ANSWER', '中等', '平台自建', '请结合材料，分析“数字化不是把线下流程简单搬到线上”的含义。', '答案应解释表层上线与深层流程重构的区别，强调数据共享、业务协同和用户体验。', '数字化重在流程重构、数据共享和业务协同，目标是提升群众办事体验，而非形式上线。', '申论,综合分析,数字政府', NULL, NULL, 1),
(216, '申论', '综合分析', '江苏', 'SHORT_ANSWER', '较难', '平台自建', '材料提到“城市更新既要留住烟火气，也要提升功能品质”。请谈谈理解。', '要从保护生活场景、改善基础设施、平衡历史文化与现代功能等方面分析。', '城市更新应保护原有生活场景和文化记忆，同时补齐设施短板，实现保护与改善统一。', '申论,综合分析,城市更新', NULL, NULL, 1),
(217, '申论', '综合分析', '广东', 'SHORT_ANSWER', '中等', '平台自建', '请分析基层减负不能只看文件数量减少的原因。', '基层负担包括形式主义考核、多头报表、重复留痕等，不能只用文件数量衡量。', '基层负担还来自多头检查、重复报表和留痕考核，减负应关注实际工作负荷和治理效能。', '申论,综合分析,基层减负', NULL, NULL, 1),
(218, '申论', '综合分析', '浙江', 'SHORT_ANSWER', '中等', '平台自建', '材料中说“文化传播要有流量，更要有分量”。请分析其含义。', '需要说明传播热度与内容质量、文化内涵之间的关系。', '流量能扩大触达，分量体现内容质量和文化内涵，二者统一才能实现有效传播。', '申论,综合分析,文化传播', NULL, NULL, 1),
(219, '申论', '综合分析', '全国', 'SHORT_ANSWER', '基础', '平台自建', '请分析公共服务“一老一小”需求增长对基层治理提出的新要求。', '从服务供给、资源统筹、专业能力和精细治理作答。', '要求基层提升资源统筹、专业服务、信息匹配和精细化治理能力。', '申论,综合分析,公共服务', NULL, NULL, 1),
(220, '申论', '提出对策', '全国', 'SHORT_ANSWER', '基础', '平台自建', '针对材料中政务服务材料重复提交问题，提出改进对策。', '对策应包括数据共享、事项梳理、一次告知、流程监督等。', '推进数据共享；梳理办事事项；落实一次告知；强化流程监督。', '申论,提出对策,政务服务', NULL, NULL, 1),
(221, '申论', '提出对策', '江苏', 'SHORT_ANSWER', '中等', '平台自建', '针对社区停车矛盾突出问题，提出治理建议。', '从资源挖潜、协商共治、错峰共享、规范管理等方面提出。', '挖掘闲置空间；推动错峰共享；建立居民协商机制；规范停车秩序。', '申论,提出对策,社区治理', NULL, NULL, 1),
(222, '申论', '提出对策', '广东', 'SHORT_ANSWER', '中等', '平台自建', '针对乡村旅游同质化问题，提出发展建议。', '应突出差异定位、文化资源转化、服务提升和品牌运营。', '明确差异定位；挖掘本地文化；完善配套服务；加强品牌运营。', '申论,提出对策,乡村旅游', NULL, NULL, 1),
(223, '申论', '提出对策', '浙江', 'SHORT_ANSWER', '基础', '平台自建', '针对社区养老服务供需匹配不足，提出改进措施。', '对策包括需求摸排、平台建设、专业队伍和志愿服务联动。', '开展需求摸排；建设服务平台；培育专业队伍；联动志愿力量。', '申论,提出对策,养老服务', NULL, NULL, 1),
(224, '申论', '提出对策', '全国', 'SHORT_ANSWER', '中等', '平台自建', '针对青年就业服务不足，提出提升就业帮扶效果的措施。', '从岗位信息、实践训练、职业指导、精准帮扶作答。', '完善岗位信息平台；增加实习实践；加强职业指导；开展精准帮扶。', '申论,提出对策,就业服务', NULL, NULL, 1),
(225, '申论', '应用文写作', '全国', 'SHORT_ANSWER', '中等', '平台自建', '请以社区居委会名义，拟写一则垃圾分类志愿者招募启事要点。', '应用文应明确对象、任务、报名方式、时间地点和倡议语。', '标题明确；说明招募对象、服务内容、报名方式、时间地点；语言简明有号召力。', '申论,应用文写作,启事', NULL, NULL, 1),
(226, '申论', '应用文写作', '江苏', 'SHORT_ANSWER', '中等', '平台自建', '请为老旧小区改造居民议事会写一份会议通知要点。', '通知应包含会议主题、时间地点、参会人员、议题和注意事项。', '写明主题、时间地点、参会对象、主要议题、准备事项和落款。', '申论,应用文写作,通知', NULL, NULL, 1),
(227, '申论', '应用文写作', '广东', 'SHORT_ANSWER', '较难', '平台自建', '请拟写一份乡村旅游推介短文的结构要点。', '推介文要突出特色资源、体验项目、交通服务和品牌形象。', '突出乡土特色；介绍体验项目；说明服务配套；表达欢迎与品牌定位。', '申论,应用文写作,推介文', NULL, NULL, 1),
(228, '申论', '应用文写作', '浙江', 'SHORT_ANSWER', '基础', '平台自建', '请为社区养老服务站写一则服务说明要点。', '服务说明应清晰说明服务对象、内容、时间、预约方式。', '明确服务对象、服务项目、服务时间、预约渠道和注意事项。', '申论,应用文写作,说明', NULL, NULL, 1),
(229, '申论', '应用文写作', '全国', 'SHORT_ANSWER', '中等', '平台自建', '请拟写一份文明旅游倡议书的主要内容。', '倡议书要有背景、倡议事项和号召。', '说明倡议背景；提出文明出行、爱护环境、遵守秩序等事项；发出号召。', '申论,应用文写作,倡议书', NULL, NULL, 1),
(230, '申论', '文章写作', '全国', 'SHORT_ANSWER', '中等', '平台自建', '围绕“以实干提升基层治理温度”写一篇议论文提纲。', '文章提纲应包含中心论点、分论点和结尾提升。', '中心论点明确；分论点围绕问题导向、协同治理、服务民生；结尾升华实干价值。', '申论,文章写作,基层治理', NULL, NULL, 1),
(231, '申论', '文章写作', '江苏', 'SHORT_ANSWER', '较难', '平台自建', '围绕“让数字政府更有温度”写一篇文章提纲。', '需兼顾技术效率与人本服务。', '论点聚焦数字赋能服务；分论点包括流程再造、数据共享、适老化服务、监督反馈。', '申论,文章写作,数字政府', NULL, NULL, 1),
(232, '申论', '文章写作', '广东', 'SHORT_ANSWER', '中等', '平台自建', '围绕“乡村振兴关键在特色与长效”写一篇议论文提纲。', '应从产业特色、人才支撑、治理长效展开。', '总论点突出特色发展和长效机制；分论点谈产业、人才、治理、品牌。', '申论,文章写作,乡村振兴', NULL, NULL, 1),
(233, '申论', '文章写作', '浙江', 'SHORT_ANSWER', '中等', '平台自建', '围绕“公共服务要从有没有走向好不好”写一篇文章提纲。', '应强调质量、均衡、精准和可持续。', '中心论点强调服务提质；分论点包括均衡供给、精准匹配、专业支撑、长效保障。', '申论,文章写作,公共服务', NULL, NULL, 1),
(234, '申论', '文章写作', '全国', 'SHORT_ANSWER', '基础', '平台自建', '围绕“青年发展需要平台也需要奋斗”写一篇议论文提纲。', '需要统一外部支持和主体努力。', '总论点强调平台赋能与青年奋斗统一；分论点谈政策支持、实践平台、能力提升、责任担当。', '申论,文章写作,青年发展', NULL, NULL, 1);

INSERT INTO question_option (question_id, option_key, option_content, sort_no) VALUES
(200, 'A', '坚持和加强党的全面领导', 1),
(200, 'B', '单纯扩大投资规模', 2),
(200, 'C', '完全依赖市场自发调节', 3),
(200, 'D', '以速度替代质量', 4),
(201, 'A', '民主评估', 1),
(201, 'B', '民主轮换', 2),
(201, 'C', '民主监督', 3),
(201, 'D', '民主授权', 4),
(202, 'A', '一般任务', 1),
(202, 'B', '首要任务', 2),
(202, 'C', '阶段任务', 3),
(202, 'D', '辅助任务', 4),
(203, 'A', '创新', 1),
(203, 'B', '协调', 2),
(203, 'C', '绿色', 3),
(203, 'D', '共享', 4),
(204, 'A', '临时性', 1),
(204, 'B', '分散性', 2),
(204, 'C', '随意性', 3),
(204, 'D', '协同性', 4),
(205, 'A', '同步同等富裕', 1),
(205, 'B', '在高质量发展中促进机会公平和分配合理', 2),
(205, 'C', '只强调财富数量增长', 3),
(205, 'D', '取消收入差异', 4),
(206, 'A', '法制社会', 1),
(206, 'B', '法治政府', 2),
(206, 'C', '法治国家', 3),
(206, 'D', '法治机关', 4),
(207, 'A', '人民立场', 1),
(207, 'B', '效率立场', 2),
(207, 'C', '部门立场', 3),
(207, 'D', '技术立场', 4),
(208, 'A', '宣传力度', 1),
(208, 'B', '文件数量', 2),
(208, 'C', '会议频次', 3),
(208, 'D', '执行力', 4),
(209, 'A', '虚拟经济', 1),
(209, 'B', '实体经济', 2),
(209, 'C', '短期流量', 3),
(209, 'D', '概念产业', 4);

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

