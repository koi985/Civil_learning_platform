-- Stage 33: rebuild exam center with seven unified xingce mock exams.
-- Rule: seven exams, each 120 points, six sections, 20 questions per section,
-- 1 point per question, questions randomly sampled from the question bank.

DROP PROCEDURE IF EXISTS rebuild_stage33_xingce_exams;

DELIMITER $$

CREATE PROCEDURE rebuild_stage33_xingce_exams()
BEGIN
  DECLARE v_exam_id INT DEFAULT 1;
  DECLARE v_exam_title VARCHAR(128);
  DECLARE v_season_name VARCHAR(32);
  DECLARE v_start_time DATETIME;
  DECLARE v_end_time DATETIME;

  DELETE ea
  FROM exam_answer ea
  JOIN exam_record er ON er.id = ea.exam_record_id
  WHERE er.exam_id BETWEEN 1 AND 7;

  DELETE FROM exam_record WHERE exam_id BETWEEN 1 AND 7;
  DELETE FROM exam_question WHERE exam_id BETWEEN 1 AND 7;
  DELETE FROM exam_analysis_course WHERE exam_id BETWEEN 1 AND 7;

  WHILE v_exam_id <= 7 DO
    SET v_exam_title = CONCAT('2026 国考行测六专项随机模考第', v_exam_id, '套');
    SET v_season_name = CONCAT('第', v_exam_id, '套');
    SET v_start_time = DATE_SUB(NOW(), INTERVAL 1 DAY);
    SET v_end_time = DATE_ADD(NOW(), INTERVAL 120 DAY);

    INSERT INTO exam (
      id, title, intro, province, exam_type, season_name, subject_name,
      duration_minutes, total_score, question_count, status, ranking_enabled,
      start_time, end_time, created_at, updated_at
    ) VALUES (
      v_exam_id,
      v_exam_title,
      '行测六个专项各 20 题，每题 1 分，共 120 题，题目从题库随机抽取组成。',
      '全国',
      '国考',
      v_season_name,
      '行测',
      120,
      120,
      120,
      'OPEN',
      1,
      v_start_time,
      v_end_time,
      NOW(),
      NOW()
    )
    ON DUPLICATE KEY UPDATE
      title = VALUES(title),
      intro = VALUES(intro),
      province = VALUES(province),
      exam_type = VALUES(exam_type),
      season_name = VALUES(season_name),
      subject_name = VALUES(subject_name),
      duration_minutes = VALUES(duration_minutes),
      total_score = VALUES(total_score),
      question_count = VALUES(question_count),
      status = VALUES(status),
      ranking_enabled = VALUES(ranking_enabled),
      start_time = VALUES(start_time),
      end_time = VALUES(end_time),
      updated_at = NOW();

    INSERT INTO exam_question (exam_id, question_id, score, sort_no)
    SELECT
      v_exam_id,
      picked.id,
      1,
      CASE picked.category_name
        WHEN '常识判断' THEN picked.pick_no
        WHEN '言语理解与表达' THEN 20 + picked.pick_no
        WHEN '判断推理' THEN 40 + picked.pick_no
        WHEN '资料分析' THEN 60 + picked.pick_no
        WHEN '数量关系' THEN 80 + picked.pick_no
        ELSE 100 + picked.pick_no
      END AS sort_no
    FROM (
      SELECT
        q.id,
        q.category_name,
        ROW_NUMBER() OVER (PARTITION BY q.category_name ORDER BY RAND()) AS pick_no
      FROM question q
      WHERE q.subject_name = '行测'
        AND q.enabled = 1
        AND q.category_name IN (
          '常识判断',
          '言语理解与表达',
          '判断推理',
          '资料分析',
          '数量关系',
          '政治理论'
        )
    ) picked
    WHERE picked.pick_no <= 20
    ORDER BY sort_no;

    INSERT INTO exam_analysis_course (exam_id, course_id, recommend_reason, sort_no) VALUES
      (v_exam_id, 1, '适合模考后回到系统课补齐行测六专项的基础方法。', 1),
      (v_exam_id, 4, '适合模考后用短周期专题课强化资料分析与判断推理。', 2);

    SET v_exam_id = v_exam_id + 1;
  END WHILE;
END$$

DELIMITER ;

CALL rebuild_stage33_xingce_exams();

DROP PROCEDURE IF EXISTS rebuild_stage33_xingce_exams;
