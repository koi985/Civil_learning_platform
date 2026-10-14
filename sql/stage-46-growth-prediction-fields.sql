ALTER TABLE user_growth_diagnosis_report
  ADD COLUMN predicted_pass_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER test_paper_average_score,
  ADD COLUMN prediction_confidence DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER predicted_pass_rate,
  ADD COLUMN formula_version VARCHAR(64) NOT NULL DEFAULT 'growth-pass-v2' AFTER prediction_confidence;
