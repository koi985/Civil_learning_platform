ALTER TABLE user_knowledge_mastery
  ADD COLUMN mastery_confidence DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER mastery_score,
  ADD COLUMN priority_score DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER mastery_confidence,
  ADD COLUMN knowledge_status VARCHAR(32) NOT NULL DEFAULT 'TO_DIAGNOSE' AFTER mastery_level;

ALTER TABLE user_test_paper
  ADD COLUMN paper_type VARCHAR(32) NOT NULL DEFAULT 'WEAKNESS' AFTER generate_mode,
  ADD COLUMN target_knowledge_points VARCHAR(255) NULL AFTER knowledge_points,
  ADD COLUMN generation_snapshot TEXT NULL AFTER recommendation_reason,
  ADD COLUMN difficulty_distribution VARCHAR(255) NULL AFTER generation_snapshot,
  ADD COLUMN source_strategy VARCHAR(64) NULL AFTER difficulty_distribution;
