SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'question'
          AND COLUMN_NAME = 'group_id'
    ),
    'SELECT 1',
    'ALTER TABLE question ADD COLUMN group_id BIGINT NULL AFTER id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'question'
          AND COLUMN_NAME = 'group_question_no'
    ),
    'SELECT 1',
    'ALTER TABLE question ADD COLUMN group_question_no INT NULL AFTER group_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'question'
          AND COLUMN_NAME = 'is_group_shared_material'
    ),
    'SELECT 1',
    'ALTER TABLE question ADD COLUMN is_group_shared_material TINYINT NOT NULL DEFAULT 0 AFTER group_question_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'question'
          AND COLUMN_NAME = 'group_material_refs'
    ),
    'SELECT 1',
    'ALTER TABLE question ADD COLUMN group_material_refs VARCHAR(64) NULL AFTER is_group_shared_material'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'question'
          AND INDEX_NAME = 'idx_question_group_id'
    ),
    'SELECT 1',
    'ALTER TABLE question ADD INDEX idx_question_group_id (group_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'question'
          AND INDEX_NAME = 'idx_question_subject_group'
    ),
    'SELECT 1',
    'ALTER TABLE question ADD INDEX idx_question_subject_group (subject_name, group_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE practice_answer
    MODIFY COLUMN user_answer TEXT NULL,
    MODIFY COLUMN correct_answer TEXT NOT NULL;

CREATE TABLE IF NOT EXISTS shenlun_group (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    subject_name VARCHAR(64) NOT NULL DEFAULT '申论',
    year INT NULL,
    region VARCHAR(32) NULL,
    paper_type VARCHAR(64) NULL,
    source_name VARCHAR(255) NOT NULL,
    material_mode VARCHAR(32) NOT NULL DEFAULT 'GROUP_SHARED',
    total_question_count INT NOT NULL DEFAULT 0,
    total_score INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_shenlun_group_source_region (source_name, region),
    KEY idx_shenlun_group_subject_year (subject_name, year),
    KEY idx_shenlun_group_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shenlun_group_material (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    sort_no INT NOT NULL,
    material_label VARCHAR(64) NOT NULL,
    title VARCHAR(255) NULL,
    content LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_shenlun_group_material_group_sort (group_id, sort_no),
    KEY idx_shenlun_group_material_group (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO shenlun_group (
    title,
    subject_name,
    year,
    region,
    paper_type,
    source_name,
    material_mode,
    total_question_count,
    total_score,
    status
)
SELECT
    q.source_name AS title,
    '申论' AS subject_name,
    CAST(REGEXP_SUBSTR(q.source_name, '[0-9]{4}') AS UNSIGNED) AS year,
    COALESCE(NULLIF(q.province, ''), '全国') AS region,
    CASE
        WHEN q.source_name LIKE '%副省级%' THEN '副省级'
        WHEN q.source_name LIKE '%地市级%' THEN '地市级'
        WHEN q.source_name LIKE '%行政执法%' THEN '行政执法卷'
        ELSE '申论材料组'
    END AS paper_type,
    q.source_name,
    'GROUP_SHARED' AS material_mode,
    COUNT(*) AS total_question_count,
    COALESCE(SUM(q.score), 0) AS total_score,
    1 AS status
FROM question q
WHERE q.subject_name = '申论'
GROUP BY q.source_name, COALESCE(NULLIF(q.province, ''), '全国')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    year = VALUES(year),
    paper_type = VALUES(paper_type),
    total_question_count = VALUES(total_question_count),
    total_score = VALUES(total_score),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP;

UPDATE question q
JOIN shenlun_group g
    ON g.source_name = q.source_name
   AND g.region = COALESCE(NULLIF(q.province, ''), '全国')
JOIN (
    SELECT
        id,
        ROW_NUMBER() OVER (PARTITION BY source_name, COALESCE(NULLIF(province, ''), '全国') ORDER BY id) AS question_no
    FROM question
    WHERE subject_name = '申论'
) seq
    ON seq.id = q.id
SET
    q.group_id = g.id,
    q.group_question_no = seq.question_no,
    q.is_group_shared_material = 1,
    q.group_material_refs = CAST(seq.question_no AS CHAR)
WHERE q.subject_name = '申论';

DELETE gm
FROM shenlun_group_material gm
JOIN shenlun_group g ON g.id = gm.group_id
WHERE g.subject_name = '申论';

INSERT INTO shenlun_group_material (
    group_id,
    sort_no,
    material_label,
    title,
    content
)
SELECT
    q.group_id,
    q.group_question_no,
    CONCAT('给定资料', q.group_question_no),
    NULL,
    q.material
FROM question q
WHERE q.subject_name = '申论'
  AND q.group_id IS NOT NULL
  AND q.group_question_no IS NOT NULL
  AND q.material IS NOT NULL
ORDER BY q.group_id, q.group_question_no;
