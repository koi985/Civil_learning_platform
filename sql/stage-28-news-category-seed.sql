CREATE TABLE IF NOT EXISTS user_news_category_subscription (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_news_category (user_id, category_id)
);

INSERT INTO news_category (id, name, sort_no) VALUES
(1, '国考', 1),
(2, '省考', 2),
(3, '选调', 3),
(4, '事业单位', 4),
(5, '军队文职', 5),
(6, '面试', 6)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_no = VALUES(sort_no);

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
(18, 6, '无领导小组讨论入门训练要点', '围绕角色定位、发言节奏和总结陈词进行训练。', 'https://dummyimage.com/600x360/e11d48/ffffff&text=无领导面试', '平台原创', '面试组', '<p>无领导讨论要避免抢话和沉默，重点体现推动讨论的能力。</p>', '面试,无领导小组,表达', 610, 25, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 15 DAY))
ON DUPLICATE KEY UPDATE
category_id = VALUES(category_id),
title = VALUES(title),
summary = VALUES(summary),
cover_url = VALUES(cover_url),
source_name = VALUES(source_name),
author_name = VALUES(author_name),
content = VALUES(content),
tags = VALUES(tags),
read_count = VALUES(read_count),
share_count = VALUES(share_count),
status = VALUES(status),
published_at = VALUES(published_at);
