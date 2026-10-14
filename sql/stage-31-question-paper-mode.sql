-- Stage 31: question bank paper practice mode.
-- Adds paper tables, tops up 行测-政治理论 to 20 questions, and seeds 5 papers.

CREATE TABLE IF NOT EXISTS question_paper (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_no VARCHAR(50) NOT NULL UNIQUE,
  title VARCHAR(150) NOT NULL,
  paper_type VARCHAR(20) NOT NULL DEFAULT 'QUESTION_BANK',
  exam_type VARCHAR(50),
  subject VARCHAR(50),
  region VARCHAR(50),
  year INT,
  source VARCHAR(100),
  description VARCHAR(500),
  duration INT NOT NULL DEFAULT 120,
  total_score INT NOT NULL DEFAULT 120,
  question_count INT NOT NULL DEFAULT 120,
  difficulty INT NOT NULL DEFAULT 2,
  status INT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question_paper_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  section_name VARCHAR(64) NOT NULL,
  score INT NOT NULL DEFAULT 1,
  sort_no INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_question_paper_question (paper_id, question_id),
  KEY idx_question_paper_question_paper_sort (paper_id, sort_no)
);

INSERT INTO question (subject_name, category_name, province, question_type, difficulty_level, source_name, stem, analysis, answer_value, knowledge_points, enabled)
SELECT '行测', '政治理论', '全国', 'SINGLE', '中等', '平台自建',
       CONCAT('政治理论专项补充题', seq.n, '：关于推进中国式现代化，下列理解最准确的是：'),
       '中国式现代化强调党的领导、人民立场、高质量发展、共同富裕、人与自然和谐共生等基本要求。',
       CASE seq.n % 4 WHEN 0 THEN 'A' WHEN 1 THEN 'B' WHEN 2 THEN 'C' ELSE 'D' END,
       '政治理论,中国式现代化', 1
FROM (
  SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) seq
WHERE (SELECT COUNT(*) FROM question WHERE subject_name = '行测' AND category_name = '政治理论' AND enabled = 1) < 20
  AND NOT EXISTS (
    SELECT 1 FROM question q
    WHERE q.subject_name = '行测'
      AND q.category_name = '政治理论'
      AND q.source_name = '平台自建'
      AND q.stem = CONCAT('政治理论专项补充题', seq.n, '：关于推进中国式现代化，下列理解最准确的是：')
  );

INSERT INTO question_option (question_id, option_key, option_content, sort_no)
SELECT q.id, opt.option_key,
       CASE opt.option_key
         WHEN 'A' THEN IF(q.answer_value = 'A', '坚持党的领导和以人民为中心的发展思想', '只强调经济速度，其他目标可以暂缓考虑')
         WHEN 'B' THEN IF(q.answer_value = 'B', '统筹发展和安全，推动高质量发展', '现代化只体现为城市规模扩大')
         WHEN 'C' THEN IF(q.answer_value = 'C', '促进全体人民共同富裕并不断改善民生', '共同富裕意味着平均分配所有收入')
         ELSE IF(q.answer_value = 'D', '推进人与自然和谐共生，走绿色发展道路', '生态保护与现代化建设没有关联')
       END,
       opt.sort_no
FROM question q
JOIN (
  SELECT 'A' option_key, 1 sort_no UNION ALL SELECT 'B', 2 UNION ALL SELECT 'C', 3 UNION ALL SELECT 'D', 4
) opt
WHERE q.subject_name = '行测'
  AND q.category_name = '政治理论'
  AND q.stem LIKE '政治理论专项补充题%：关于推进中国式现代化，下列理解最准确的是：'
  AND NOT EXISTS (
    SELECT 1 FROM question_option qo WHERE qo.question_id = q.id AND qo.option_key = opt.option_key
  );

DROP PROCEDURE IF EXISTS seed_stage31_question_papers;

DELIMITER $$

CREATE PROCEDURE seed_stage31_question_papers()
BEGIN
  DECLARE p INT DEFAULT 1;
  DECLARE paper_id BIGINT;
  DECLARE section_offset INT;

  DELETE FROM question_paper_question WHERE paper_id IN (SELECT id FROM question_paper WHERE title LIKE '行测六专项标准试卷%');
  DELETE FROM question_paper WHERE title LIKE '行测六专项标准试卷%';

  WHILE p <= 5 DO
    INSERT INTO question_paper (paper_no, title, paper_type, exam_type, subject, region, year, source, description, duration, total_score, question_count, difficulty, status)
    VALUES (
      CONCAT('XINGCE-STANDARD-', p),
      CONCAT('行测六专项标准试卷', p),
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
    SET section_offset = (p - 1) * 20;

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

    SET p = p + 1;
  END WHILE;
END$$

DELIMITER ;

CALL seed_stage31_question_papers();

DROP PROCEDURE IF EXISTS seed_stage31_question_papers;
