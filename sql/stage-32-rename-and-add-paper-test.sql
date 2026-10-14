-- Stage 32: rename standard 行测试卷 and add the sixth paper.

UPDATE question_paper
SET title = CONCAT('套卷测试', SUBSTRING(paper_no, LENGTH('XINGCE-STANDARD-') + 1))
WHERE paper_no LIKE 'XINGCE-STANDARD-%';

DROP PROCEDURE IF EXISTS seed_stage32_sixth_question_paper;

DELIMITER $$

CREATE PROCEDURE seed_stage32_sixth_question_paper()
BEGIN
  DECLARE paper_id BIGINT;
  DECLARE section_offset INT DEFAULT 100;

  IF NOT EXISTS (SELECT 1 FROM question_paper WHERE paper_no = 'XINGCE-STANDARD-6') THEN
    INSERT INTO question_paper (paper_no, title, paper_type, exam_type, subject, region, year, source, description, duration, total_score, question_count, difficulty, status)
    VALUES (
      'XINGCE-STANDARD-6',
      '套卷测试6',
      'QUESTION_BANK',
      '公务员',
      '行测',
      '全国',
      2026,
      '平台组卷',
      '行测六个专项各20题，每题1分，总分120分。',
      120,
      120,
      120,
      2,
      1
    );
    SET paper_id = LAST_INSERT_ID();

    INSERT INTO question_paper_question (paper_id, question_id, section_name, score, sort_no)
    SELECT paper_id, ranked.id, ranked.category_name, 1,
           CASE ranked.category_name
             WHEN '常识判断' THEN 0
             WHEN '言语理解与表达' THEN 20
             WHEN '资料分析' THEN 40
             WHEN '判断推理' THEN 60
             WHEN '政治理论' THEN 80
             ELSE 100
           END + ranked.pick_no
    FROM (
      SELECT q.id, q.category_name,
             ROW_NUMBER() OVER (PARTITION BY q.category_name ORDER BY q.id DESC) AS rn,
             ROW_NUMBER() OVER (PARTITION BY q.category_name ORDER BY q.id DESC) AS pick_no,
             COUNT(*) OVER (PARTITION BY q.category_name) AS category_count
      FROM question q
      WHERE q.subject_name = '行测'
        AND q.enabled = 1
        AND q.category_name IN ('常识判断', '言语理解与表达', '资料分析', '判断推理', '政治理论', '数量关系')
    ) ranked
    WHERE (
      ranked.category_count >= section_offset + 20
      AND ranked.rn > section_offset
      AND ranked.rn <= section_offset + 20
    ) OR (
      ranked.category_count < section_offset + 20
      AND ranked.rn <= 20
    );
  END IF;
END$$

DELIMITER ;

CALL seed_stage32_sixth_question_paper();

DROP PROCEDURE IF EXISTS seed_stage32_sixth_question_paper;

