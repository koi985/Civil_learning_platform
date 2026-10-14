-- Stage 38: add per-question score and assign 18-25 points to stage 37 essay questions.

SET @score_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'question'
    AND COLUMN_NAME = 'score'
);

SET @ddl := IF(
  @score_column_exists = 0,
  'ALTER TABLE question ADD COLUMN score INT NOT NULL DEFAULT 1 AFTER difficulty_level',
  'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE question
SET score = 18 + (id % 8)
WHERE source_name = '申论真题风格改编-原创扩展';
