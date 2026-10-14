-- Stage 27: course subject filter seed data.
-- Ensures every course filter tab has real published course records.

UPDATE course
SET exam_type = '事业单位',
    tags = '直播,事业单位,面试,冲刺'
WHERE id = 3;

INSERT INTO course (id, title, cover_url, teacher_name, teacher_avatar, province, subject_name, exam_type, course_type, price, original_price, tags, learn_count, description, highlights, target_users, validity_days, status) VALUES
(5, '2026 选调生综合能力提升班', 'https://dummyimage.com/600x360/7c3aed/ffffff&text=选调提升班', '李老师', 'https://dummyimage.com/120x120/7c3aed/ffffff&text=T3', '全国', '综合能力', '选调', 'LIVE', 1299.00, 1699.00, '直播,选调,综合能力', 742, '围绕选调生笔试高频模块与基层治理写作能力做系统训练。', '政策梳理 + 写作训练 + 阶段测评', '适合备考选调生的应届与基层项目考生', 240, 'PUBLISHED'),
(6, '结构化面试全真演练课', 'https://dummyimage.com/600x360/db2777/ffffff&text=面试演练课', '赵老师', 'https://dummyimage.com/120x120/db2777/ffffff&text=T4', '全国', '面试', '面试', 'LIVE', 699.00, 999.00, '直播,面试,全真演练', 1180, '覆盖结构化面试审题、破题、表达与追问应对。', '小班演练 + 点评复盘 + 热点题库', '适合笔试通过后进入面试备考阶段的学员', 120, 'PUBLISHED'),
(7, '军队文职公共科目系统班', 'https://dummyimage.com/600x360/0f766e/ffffff&text=军队文职系统班', '周老师', 'https://dummyimage.com/120x120/0f766e/ffffff&text=T5', '全国', '公共科目', '军队文职', 'RECORDED', 799.00, 1199.00, '录播,军队文职,公共科目', 624, '系统讲解军队文职公共科目基础知识、岗位能力与考前冲刺。', '知识框架 + 真题精讲 + 冲刺清单', '适合第一次备考军队文职公共科目的学员', 180, 'PUBLISHED')
ON DUPLICATE KEY UPDATE
title = VALUES(title),
cover_url = VALUES(cover_url),
teacher_name = VALUES(teacher_name),
teacher_avatar = VALUES(teacher_avatar),
province = VALUES(province),
subject_name = VALUES(subject_name),
exam_type = VALUES(exam_type),
course_type = VALUES(course_type),
price = VALUES(price),
original_price = VALUES(original_price),
tags = VALUES(tags),
learn_count = VALUES(learn_count),
description = VALUES(description),
highlights = VALUES(highlights),
target_users = VALUES(target_users),
validity_days = VALUES(validity_days),
status = VALUES(status);

DELETE FROM course_chapter WHERE course_id IN (5, 6, 7);
INSERT INTO course_chapter (course_id, title, chapter_type, duration_minutes, sort_no, live_start_time, video_url, trial_available) VALUES
(5, '选调考试政策与备考路径', 'LIVE', 90, 1, DATE_ADD(NOW(), INTERVAL 3 DAY), 'https://example.com/video/course5-1', 1),
(5, '基层治理写作专题', 'RECORDED', 85, 2, NULL, 'https://example.com/video/course5-2', 0),
(6, '结构化面试答题框架', 'LIVE', 100, 1, DATE_ADD(NOW(), INTERVAL 4 DAY), 'https://example.com/video/course6-1', 1),
(6, '追问与现场表达训练', 'RECORDED', 75, 2, NULL, 'https://example.com/video/course6-2', 0),
(7, '军队文职公共科目导学', 'RECORDED', 70, 1, NULL, 'https://example.com/video/course7-1', 1),
(7, '岗位能力真题精讲', 'RECORDED', 95, 2, NULL, 'https://example.com/video/course7-2', 0);

DELETE FROM course_material WHERE course_id IN (5, 6, 7);
INSERT INTO course_material (course_id, title, material_type, file_id, sort_no) VALUES
(5, '选调综合能力讲义', 'PDF', 3, 1),
(6, '面试热点题本', 'PDF', 4, 1),
(7, '军队文职公共科目清单', 'PDF', 3, 1);

INSERT INTO course_comment (course_id, user_id, rating, content)
SELECT 5, 2, 5, '选调政策讲得清楚，写作训练很实用。'
WHERE NOT EXISTS (SELECT 1 FROM course_comment WHERE course_id = 5 AND user_id = 2);

INSERT INTO course_comment (course_id, user_id, rating, content)
SELECT 6, 5, 5, '面试演练节奏很接近真实考场。'
WHERE NOT EXISTS (SELECT 1 FROM course_comment WHERE course_id = 6 AND user_id = 5);

INSERT INTO course_comment (course_id, user_id, rating, content)
SELECT 7, 2, 4, '公共科目框架清晰，适合系统复习。'
WHERE NOT EXISTS (SELECT 1 FROM course_comment WHERE course_id = 7 AND user_id = 2);
