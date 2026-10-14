INSERT INTO sys_user (
  username, password, nickname, real_name, avatar_url, email, phone,
  province, exam_type, preferred_subject, vip_expire_at, enabled, locked, deleted
) VALUES
('teacher3', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '行测冲刺讲师', '赵老师', 'https://dummyimage.com/120x120/2563eb/ffffff&text=T3', 'teacher3@civil.local', '13800000103', '北京', '国考', '行测', '2028-12-31 23:59:59', 1, 0, 0),
('teacher4', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '判断推理名师', '孙老师', 'https://dummyimage.com/120x120/0891b2/ffffff&text=T4', 'teacher4@civil.local', '13800000104', '上海', '省考', '判断推理', '2028-12-31 23:59:59', 1, 0, 0),
('teacher5', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '资料分析导师', '周老师', 'https://dummyimage.com/120x120/16a34a/ffffff&text=T5', 'teacher5@civil.local', '13800000105', '广东', '事业编', '资料分析', '2028-12-31 23:59:59', 1, 0, 0),
('teacher6', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '数量关系教练', '吴老师', 'https://dummyimage.com/120x120/f97316/ffffff&text=T6', 'teacher6@civil.local', '13800000106', '山东', '国考', '数量关系', '2028-12-31 23:59:59', 1, 0, 0),
('teacher7', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '公基系统讲师', '郑老师', 'https://dummyimage.com/120x120/7c3aed/ffffff&text=T7', 'teacher7@civil.local', '13800000107', '四川', '事业编', '公共基础', '2028-12-31 23:59:59', 1, 0, 0),
('teacher8', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '面试实战导师', '何老师', 'https://dummyimage.com/120x120/db2777/ffffff&text=T8', 'teacher8@civil.local', '13800000108', '江苏', '面试', '结构化面试', '2028-12-31 23:59:59', 1, 0, 0),
('teacher9', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '申论批改导师', '马老师', 'https://dummyimage.com/120x120/0f766e/ffffff&text=T9', 'teacher9@civil.local', '13800000109', '浙江', '省考', '申论', '2028-12-31 23:59:59', 1, 0, 0),
('teacher10', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '常识判断讲师', '高老师', 'https://dummyimage.com/120x120/ca8a04/ffffff&text=T10', 'teacher10@civil.local', '13800000110', '河南', '国考', '常识判断', '2028-12-31 23:59:59', 1, 0, 0),
('teacher11', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '职测提分讲师', '刘老师', 'https://dummyimage.com/120x120/1d4ed8/ffffff&text=T11', 'teacher11@civil.local', '13800000111', '湖北', '事业编', '职测', '2028-12-31 23:59:59', 1, 0, 0),
('teacher12', '$2b$12$R8P7gVVaGG6HAV/pbSCoF.in7Wl5UcEAwiGfzy3nQ3zyoJE1I.0K.', '综合应用导师', '黄老师', 'https://dummyimage.com/120x120/9333ea/ffffff&text=T12', 'teacher12@civil.local', '13800000112', '福建', '事业编', '综合应用能力', '2028-12-31 23:59:59', 1, 0, 0)
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  real_name = VALUES(real_name),
  avatar_url = VALUES(avatar_url),
  email = VALUES(email),
  phone = VALUES(phone),
  province = VALUES(province),
  exam_type = VALUES(exam_type),
  preferred_subject = VALUES(preferred_subject),
  enabled = VALUES(enabled),
  locked = VALUES(locked),
  deleted = VALUES(deleted);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'TEACHER'
WHERE u.username IN (
  'teacher3', 'teacher4', 'teacher5', 'teacher6', 'teacher7',
  'teacher8', 'teacher9', 'teacher10', 'teacher11', 'teacher12'
);

INSERT INTO teacher_profile (
  user_id, gender, email, intro, audit_status, audit_remark, audited_by, audited_at
)
SELECT u.id, x.gender, u.email, x.intro, 'APPROVED', '批量新增教师账号，资质审核通过', 1, NOW()
FROM sys_user u
JOIN (
  SELECT 'teacher3' username, '女' gender, '多年行测模块化授课经验，擅长考点拆解与冲刺提分。' intro UNION ALL
  SELECT 'teacher4', '男', '专注判断推理图形、定义、类比和逻辑判断，强调方法迁移。' UNION ALL
  SELECT 'teacher5', '女', '主讲资料分析速算技巧，帮助学员建立稳定做题节奏。' UNION ALL
  SELECT 'teacher6', '男', '深耕数量关系与数学运算，擅长低频难点快速突破。' UNION ALL
  SELECT 'teacher7', '女', '系统讲解公共基础知识，覆盖法律、政治、经济和时政模块。' UNION ALL
  SELECT 'teacher8', '男', '面试实战辅导经验丰富，擅长结构化答题框架与表达训练。' UNION ALL
  SELECT 'teacher9', '女', '长期从事申论批改与写作训练，强调材料阅读和观点表达。' UNION ALL
  SELECT 'teacher10', '男', '常识判断与时政热点讲解清晰，帮助学员扩大知识覆盖面。' UNION ALL
  SELECT 'teacher11', '女', '事业单位职测专项讲师，擅长分层训练和错题复盘。' UNION ALL
  SELECT 'teacher12', '男', '综合应用能力辅导教师，注重审题、结构和应用写作训练。'
) x ON x.username = u.username
ON DUPLICATE KEY UPDATE
  gender = VALUES(gender),
  email = VALUES(email),
  intro = VALUES(intro),
  audit_status = VALUES(audit_status),
  audit_remark = VALUES(audit_remark),
  audited_by = VALUES(audited_by),
  audited_at = VALUES(audited_at);
