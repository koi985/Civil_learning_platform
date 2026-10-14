-- Stage 33: add 50 published courses across exam categories.

DROP TEMPORARY TABLE IF EXISTS tmp_stage33_courses;

CREATE TEMPORARY TABLE tmp_stage33_courses (
  title VARCHAR(128) NOT NULL,
  cover_url VARCHAR(255) NOT NULL,
  teacher_name VARCHAR(64) NOT NULL,
  teacher_avatar VARCHAR(255),
  province VARCHAR(32) NOT NULL,
  subject_name VARCHAR(64) NOT NULL,
  exam_type VARCHAR(64) NOT NULL,
  course_type VARCHAR(32) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2) NOT NULL,
  tags VARCHAR(255),
  learn_count INT NOT NULL,
  description TEXT,
  highlights TEXT,
  target_users VARCHAR(255),
  validity_days INT NOT NULL,
  status VARCHAR(32) NOT NULL
);

INSERT INTO tmp_stage33_courses
(title, cover_url, teacher_name, teacher_avatar, province, subject_name, exam_type, course_type, price, original_price, tags, learn_count, description, highlights, target_users, validity_days, status)
VALUES
('2026国考行测系统精讲班', 'https://dummyimage.com/600x360/1d4ed8/ffffff&text=国考行测', '王老师', NULL, '全国', '行测', '国考', 'RECORDED', 1299.00, 1899.00, '国考,行测,系统课', 982, '覆盖国考行测核心模块，适合系统打基础和阶段提分。', '六大模块系统讲解|配套高频题训练|阶段测评反馈', '国考基础薄弱及系统备考学员', 365, 'PUBLISHED'),
('2026国考申论高分突破班', 'https://dummyimage.com/600x360/f97316/ffffff&text=国考申论', '陈老师', NULL, '全国', '申论', '国考', 'RECORDED', 1199.00, 1699.00, '国考,申论,高分', 836, '围绕归纳概括、综合分析、应用文和大作文构建申论作答体系。', '题型方法拆解|范文精批示范|热点素材积累', '国考申论提分学员', 365, 'PUBLISHED'),
('国考常识判断专项刷题营', 'https://dummyimage.com/600x360/0f766e/ffffff&text=常识判断', '刘老师', NULL, '全国', '常识判断', '国考', 'RECORDED', 399.00, 699.00, '国考,常识,刷题', 721, '以法律、政治、科技、人文等常识高频考点为主线训练。', '高频考点清单|错题复盘路径|考前速记资料', '常识正确率不稳定学员', 180, 'PUBLISHED'),
('国考言语理解高频题型班', 'https://dummyimage.com/600x360/2563eb/ffffff&text=言语理解', '孙老师', NULL, '全国', '言语理解与表达', '国考', 'LIVE', 699.00, 999.00, '国考,言语,直播', 654, '训练片段阅读、语句表达、逻辑填空等题型的解题策略。', '直播带练|题型归纳|限时训练', '言语模块需要稳定提速学员', 240, 'PUBLISHED'),
('国考判断推理图形逻辑专项课', 'https://dummyimage.com/600x360/7c3aed/ffffff&text=判断推理', '赵老师', NULL, '全国', '判断推理', '国考', 'RECORDED', 599.00, 899.00, '国考,判断推理,图形', 590, '系统讲解图形推理、定义判断、类比推理和逻辑判断。', '规律体系化|题型模板化|错题归因', '判断推理薄弱学员', 240, 'PUBLISHED'),
('国考资料分析速算技巧班', 'https://dummyimage.com/600x360/db2777/ffffff&text=资料分析', '周老师', NULL, '全国', '资料分析', '国考', 'RECORDED', 499.00, 799.00, '国考,资料分析,速算', 676, '强化资料分析读数、列式、估算和速算方法。', '公式速记|估算训练|套题节奏', '资料分析耗时较长学员', 180, 'PUBLISHED'),
('国考数量关系入门到突破', 'https://dummyimage.com/600x360/ea580c/ffffff&text=数量关系', '吴老师', NULL, '全国', '数量关系', '国考', 'RECORDED', 699.00, 999.00, '国考,数量关系,基础', 438, '从基础模型入手，提升数量关系可做题比例。', '模型归纳|取舍策略|真题训练', '数量关系零基础学员', 240, 'PUBLISHED'),
('国考行政执法类专项提升班', 'https://dummyimage.com/600x360/0891b2/ffffff&text=行政执法', '李老师', NULL, '全国', '行政执法', '国考', 'LIVE', 899.00, 1299.00, '国考,行政执法,专项', 512, '聚焦行政执法类岗位考试特点和依法行政考点。', '执法考点串讲|案例题训练|热点解读', '报考行政执法类岗位学员', 300, 'PUBLISHED'),
('国考考前30天冲刺密训班', 'https://dummyimage.com/600x360/dc2626/ffffff&text=国考冲刺', '高老师', NULL, '全国', '综合冲刺', '国考', 'LIVE', 999.00, 1499.00, '国考,冲刺,密训', 801, '适合考前集中复盘，强化套题节奏和高频考点。', '高频考点回炉|限时模考|考前策略', '国考考前冲刺学员', 120, 'PUBLISHED'),

('2026省考行测系统班', 'https://dummyimage.com/600x360/1d4ed8/ffffff&text=省考行测', '王老师', NULL, '全国', '行测', '省考', 'RECORDED', 1099.00, 1599.00, '省考,行测,系统课', 914, '覆盖多省联考行测常见题型，兼顾基础与提升。', '省考题型覆盖|专项训练|真题复盘', '参加省考或联考学员', 365, 'PUBLISHED'),
('省考申论综合写作提升班', 'https://dummyimage.com/600x360/f97316/ffffff&text=省考申论', '陈老师', NULL, '全国', '申论', '省考', 'RECORDED', 999.00, 1399.00, '省考,申论,写作', 742, '围绕省考申论材料阅读、要点提炼和文章写作训练。', '材料分析|作答结构|作文模板优化', '省考申论备考学员', 365, 'PUBLISHED'),
('江苏省考A类专项精讲班', 'https://dummyimage.com/600x360/0f766e/ffffff&text=江苏省考', '陈老师', NULL, '江苏', '行测', '省考', 'LIVE', 899.00, 1299.00, '江苏,省考,A类', 466, '结合江苏省考A类考试特点进行模块化训练。', '本省考情分析|行政能力训练|真题讲评', '江苏省考A类考生', 300, 'PUBLISHED'),
('广东省考县级卷提升班', 'https://dummyimage.com/600x360/2563eb/ffffff&text=广东省考', '赵老师', NULL, '广东', '行测', '省考', 'RECORDED', 799.00, 1199.00, '广东,省考,县级', 533, '面向广东省考县级卷，强化行测与申论联动提升。', '广东考情|县级卷重点|套题训练', '广东省考考生', 300, 'PUBLISHED'),
('山东省考申论实战班', 'https://dummyimage.com/600x360/7c3aed/ffffff&text=山东申论', '李老师', NULL, '山东', '申论', '省考', 'LIVE', 699.00, 999.00, '山东,省考,申论', 421, '结合山东省考申论材料特点进行实战训练。', '材料精读|答案组织|热点积累', '山东省考申论学员', 240, 'PUBLISHED'),
('浙江省考行测提速班', 'https://dummyimage.com/600x360/db2777/ffffff&text=浙江行测', '孙老师', NULL, '浙江', '行测', '省考', 'RECORDED', 699.00, 999.00, '浙江,省考,提速', 394, '针对浙江省考行测题量和节奏训练快速解题。', '限时刷题|技巧精讲|错题复盘', '浙江省考行测提速学员', 240, 'PUBLISHED'),
('四川省考综合能力系统课', 'https://dummyimage.com/600x360/ea580c/ffffff&text=四川省考', '周老师', NULL, '四川', '综合能力', '省考', 'RECORDED', 899.00, 1299.00, '四川,省考,综合', 358, '覆盖四川省考综合能力常见考点和作答方法。', '考情拆解|模块训练|阶段测评', '四川省考考生', 300, 'PUBLISHED'),
('省考公安岗专项基础班', 'https://dummyimage.com/600x360/0891b2/ffffff&text=公安岗', '刘老师', NULL, '全国', '公安专业科目', '省考', 'RECORDED', 799.00, 1199.00, '省考,公安岗,专业科目', 382, '针对公安岗位专业科目基础知识和常考题型。', '法条梳理|案例训练|高频考点', '省考公安岗考生', 300, 'PUBLISHED'),
('省考考前套题刷题营', 'https://dummyimage.com/600x360/dc2626/ffffff&text=省考刷题', '吴老师', NULL, '全国', '综合冲刺', '省考', 'LIVE', 499.00, 799.00, '省考,套题,冲刺', 702, '考前通过套题训练稳定做题节奏和得分结构。', '套卷讲评|时间分配|薄弱点回补', '省考冲刺阶段学员', 120, 'PUBLISHED'),

('中央选调综合能力系统班', 'https://dummyimage.com/600x360/7c3aed/ffffff&text=中央选调', '李老师', NULL, '全国', '综合能力', '选调', 'RECORDED', 1299.00, 1899.00, '选调,中央选调,综合', 512, '覆盖中央选调笔试重点和综合能力训练。', '政策理解|案例分析|综合表达', '中央选调备考学员', 365, 'PUBLISHED'),
('定向选调行测申论双科班', 'https://dummyimage.com/600x360/1d4ed8/ffffff&text=定向选调', '王老师', NULL, '全国', '行测+申论', '选调', 'LIVE', 1599.00, 2199.00, '选调,双科,直播', 476, '面向定向选调，兼顾行测方法和申论表达。', '双科体系|名校题型|阶段答疑', '定向选调考生', 365, 'PUBLISHED'),
('选调生申论政策理论班', 'https://dummyimage.com/600x360/f97316/ffffff&text=选调申论', '陈老师', NULL, '全国', '申论', '选调', 'RECORDED', 799.00, 1199.00, '选调,申论,政策', 421, '强化政策理解、基层治理、青年干部等选调高频主题。', '政策专题|范文积累|作答框架', '选调申论提升学员', 300, 'PUBLISHED'),
('选调生面谈与面试通关班', 'https://dummyimage.com/600x360/db2777/ffffff&text=选调面试', '赵老师', NULL, '全国', '面试', '选调', 'LIVE', 999.00, 1499.00, '选调,面试,面谈', 333, '训练选调面谈、结构化面试和综合表达能力。', '面谈模拟|表达反馈|岗位匹配', '进入选调面试阶段学员', 180, 'PUBLISHED'),
('山东选调专项提升班', 'https://dummyimage.com/600x360/0f766e/ffffff&text=山东选调', '周老师', NULL, '山东', '综合能力', '选调', 'RECORDED', 899.00, 1299.00, '山东,选调,专项', 294, '结合山东选调考情进行专项训练。', '本省考情|政策热点|真题复盘', '山东选调考生', 300, 'PUBLISHED'),
('江苏名校优选专项班', 'https://dummyimage.com/600x360/2563eb/ffffff&text=名校优选', '孙老师', NULL, '江苏', '综合能力', '选调', 'RECORDED', 899.00, 1299.00, '江苏,名校优选,选调', 267, '针对江苏名校优选笔面特点提供系统辅导。', '笔试模块|面试表达|材料分析', '江苏名校优选考生', 300, 'PUBLISHED'),
('选调基层治理案例分析课', 'https://dummyimage.com/600x360/ea580c/ffffff&text=基层治理', '刘老师', NULL, '全国', '案例分析', '选调', 'RECORDED', 499.00, 799.00, '选调,基层治理,案例', 358, '围绕基层治理典型场景训练分析和表达。', '案例拆解|治理语言|对策生成', '选调综合分析薄弱学员', 180, 'PUBLISHED'),
('选调考前冲刺预测班', 'https://dummyimage.com/600x360/dc2626/ffffff&text=选调冲刺', '高老师', NULL, '全国', '综合冲刺', '选调', 'LIVE', 699.00, 999.00, '选调,冲刺,预测', 402, '考前集中梳理选调高频专题和答题策略。', '热点预测|套题训练|考前答疑', '选调考前冲刺学员', 120, 'PUBLISHED'),

('军队文职公共科目精讲班', 'https://dummyimage.com/600x360/0f766e/ffffff&text=文职公共科目', '周老师', NULL, '全国', '公共科目', '军队文职', 'RECORDED', 899.00, 1299.00, '军队文职,公共科目,精讲', 642, '系统覆盖军队文职公共科目基础知识和岗位能力。', '公共知识|岗位能力|章节练习', '军队文职公共科目考生', 365, 'PUBLISHED'),
('军队文职岗位能力专项班', 'https://dummyimage.com/600x360/1d4ed8/ffffff&text=岗位能力', '王老师', NULL, '全国', '岗位能力', '军队文职', 'RECORDED', 799.00, 1199.00, '军队文职,岗位能力', 510, '强化言语、判断、资料等岗位能力题型。', '题型方法|限时训练|错题讲评', '岗位能力薄弱学员', 300, 'PUBLISHED'),
('军队文职公共知识速记班', 'https://dummyimage.com/600x360/f97316/ffffff&text=公共知识', '刘老师', NULL, '全国', '公共知识', '军队文职', 'RECORDED', 599.00, 899.00, '军队文职,公共知识,速记', 488, '梳理政治、经济、法律、人文等公共知识考点。', '考点清单|记忆口诀|刷题巩固', '公共知识记忆压力大学员', 240, 'PUBLISHED'),
('军队文职管理学专业课', 'https://dummyimage.com/600x360/2563eb/ffffff&text=管理学', '李老师', NULL, '全国', '管理学', '军队文职', 'RECORDED', 999.00, 1499.00, '军队文职,管理学,专业课', 316, '面向管理学专业科目，建立知识框架和答题方法。', '专业框架|重点章节|真题训练', '报考管理类岗位学员', 365, 'PUBLISHED'),
('军队文职会计学专业课', 'https://dummyimage.com/600x360/7c3aed/ffffff&text=会计学', '陈老师', NULL, '全国', '会计学', '军队文职', 'RECORDED', 999.00, 1499.00, '军队文职,会计学,专业课', 292, '覆盖会计学专业科目重点知识和计算题型。', '知识框架|例题精讲|专业题训练', '报考会计类岗位学员', 365, 'PUBLISHED'),
('军队文职护理学专业课', 'https://dummyimage.com/600x360/db2777/ffffff&text=护理学', '赵老师', NULL, '全国', '护理学', '军队文职', 'RECORDED', 999.00, 1499.00, '军队文职,护理学,专业课', 268, '针对护理学岗位专业知识进行系统梳理。', '基础知识|临床重点|题型训练', '报考护理类岗位学员', 365, 'PUBLISHED'),
('军队文职面试礼仪表达班', 'https://dummyimage.com/600x360/ea580c/ffffff&text=文职面试', '高老师', NULL, '全国', '面试', '军队文职', 'LIVE', 699.00, 999.00, '军队文职,面试,表达', 356, '训练军队文职面试礼仪、表达和岗位匹配。', '礼仪规范|结构化问答|模拟点评', '军队文职面试学员', 180, 'PUBLISHED'),
('军队文职考前冲刺刷题班', 'https://dummyimage.com/600x360/dc2626/ffffff&text=文职冲刺', '吴老师', NULL, '全国', '综合冲刺', '军队文职', 'LIVE', 599.00, 899.00, '军队文职,冲刺,刷题', 429, '考前集中刷题，补齐公共科目和岗位能力薄弱点。', '套题训练|高频回顾|考前答疑', '军队文职冲刺学员', 120, 'PUBLISHED'),

('结构化面试全程系统班', 'https://dummyimage.com/600x360/db2777/ffffff&text=结构化面试', '赵老师', NULL, '全国', '面试', '面试', 'LIVE', 1299.00, 1899.00, '面试,结构化,系统', 830, '从审题、结构、表达、追问到模拟全流程训练。', '题型框架|表达训练|模拟点评', '进入面试阶段学员', 180, 'PUBLISHED'),
('国省考面试热点精讲班', 'https://dummyimage.com/600x360/f97316/ffffff&text=面试热点', '陈老师', NULL, '全国', '面试热点', '面试', 'RECORDED', 499.00, 799.00, '面试,热点,国省考', 698, '梳理国省考面试热点主题和答题素材。', '热点专题|素材积累|答题示范', '面试热点积累不足学员', 120, 'PUBLISHED'),
('无领导小组讨论实战班', 'https://dummyimage.com/600x360/7c3aed/ffffff&text=无领导', '李老师', NULL, '全国', '无领导小组', '面试', 'LIVE', 899.00, 1299.00, '面试,无领导,实战', 316, '训练无领导小组讨论流程、角色定位和表达策略。', '流程拆解|实战模拟|个性点评', '无领导小组面试学员', 120, 'PUBLISHED'),
('面试表达与仪态训练营', 'https://dummyimage.com/600x360/1d4ed8/ffffff&text=表达仪态', '高老师', NULL, '全国', '表达训练', '面试', 'LIVE', 699.00, 999.00, '面试,表达,仪态', 502, '提升语言组织、现场状态、仪态礼仪和抗压表达。', '表达纠偏|仪态训练|镜头模拟', '表达不流畅或紧张学员', 120, 'PUBLISHED'),
('基层岗位面试专项班', 'https://dummyimage.com/600x360/0f766e/ffffff&text=基层面试', '刘老师', NULL, '全国', '面试', '面试', 'RECORDED', 599.00, 899.00, '面试,基层岗位,专项', 380, '围绕基层治理、群众工作、应急处突等场景训练。', '基层场景|群众工作|对策表达', '基层岗位面试学员', 120, 'PUBLISHED'),
('行政执法面试专项班', 'https://dummyimage.com/600x360/0891b2/ffffff&text=执法面试', '周老师', NULL, '全国', '面试', '面试', 'RECORDED', 699.00, 999.00, '面试,行政执法,专项', 344, '训练行政执法岗位面试中的法治思维和现场处置。', '执法案例|法治表达|情景模拟', '行政执法岗位面试学员', 120, 'PUBLISHED'),
('面试逆袭一对一小班课', 'https://dummyimage.com/600x360/ea580c/ffffff&text=面试小班', '孙老师', NULL, '全国', '面试', '面试', 'LIVE', 1999.00, 2999.00, '面试,小班,逆袭', 256, '小班模拟点评，针对个人问题定制提升方案。', '小班带练|个性反馈|高频追问', '面试分差较大或需冲高学员', 90, 'PUBLISHED'),
('面试考前7天冲刺营', 'https://dummyimage.com/600x360/dc2626/ffffff&text=面试冲刺', '吴老师', NULL, '全国', '面试', '面试', 'LIVE', 399.00, 699.00, '面试,考前,冲刺', 620, '考前集中模拟，梳理高频题型和临场策略。', '考前模拟|热点回顾|临场策略', '面试考前冲刺学员', 60, 'PUBLISHED'),

('事业单位职测系统班', 'https://dummyimage.com/600x360/1d4ed8/ffffff&text=事业职测', '王老师', NULL, '全国', '职测', '事业单位', 'RECORDED', 899.00, 1299.00, '事业单位,职测,系统', 804, '覆盖事业单位职测常见模块和解题方法。', '模块精讲|刷题训练|阶段测评', '事业单位职测备考学员', 365, 'PUBLISHED'),
('事业单位综合应用能力A类班', 'https://dummyimage.com/600x360/f97316/ffffff&text=综应A类', '陈老师', NULL, '全国', '综合应用能力A类', '事业单位', 'RECORDED', 999.00, 1399.00, '事业单位,综应,A类', 530, '面向管理岗A类，训练材料分析和事务处理能力。', '材料阅读|事务处理|公文写作', '事业单位A类考生', 365, 'PUBLISHED'),
('事业单位综合应用能力B类班', 'https://dummyimage.com/600x360/7c3aed/ffffff&text=综应B类', '李老师', NULL, '全国', '综合应用能力B类', '事业单位', 'RECORDED', 999.00, 1399.00, '事业单位,综应,B类', 412, '面向社科专技B类，强化概念分析和论证评价。', '概念分析|论证评价|写作训练', '事业单位B类考生', 365, 'PUBLISHED'),
('事业单位综合应用能力C类班', 'https://dummyimage.com/600x360/0f766e/ffffff&text=综应C类', '周老师', NULL, '全国', '综合应用能力C类', '事业单位', 'RECORDED', 999.00, 1399.00, '事业单位,综应,C类', 386, '面向自然科学专技C类，训练科技文献阅读和应用写作。', '科技文献|实验设计|应用写作', '事业单位C类考生', 365, 'PUBLISHED'),
('事业单位公共基础知识班', 'https://dummyimage.com/600x360/2563eb/ffffff&text=公基', '刘老师', NULL, '全国', '公共基础知识', '事业单位', 'RECORDED', 599.00, 899.00, '事业单位,公基,基础', 694, '梳理法律、政治、经济、公文、科技人文等公基考点。', '考点清单|记忆方法|章节题库', '公基备考学员', 300, 'PUBLISHED'),
('事业单位联考E类医学基础班', 'https://dummyimage.com/600x360/db2777/ffffff&text=联考E类', '赵老师', NULL, '全国', '医学基础知识', '事业单位', 'RECORDED', 1199.00, 1699.00, '事业单位,E类,医学', 278, '面向医疗卫生E类考试，覆盖医学基础核心内容。', '医学基础|重点章节|专业题训练', '事业单位E类考生', 365, 'PUBLISHED'),
('事业单位面试通关班', 'https://dummyimage.com/600x360/ea580c/ffffff&text=事业面试', '高老师', NULL, '全国', '面试', '事业单位', 'LIVE', 699.00, 999.00, '事业单位,面试,通关', 496, '训练事业单位面试常见题型和岗位表达。', '结构化训练|岗位匹配|模拟点评', '事业单位面试学员', 120, 'PUBLISHED'),
('事业单位考前冲刺刷题班', 'https://dummyimage.com/600x360/dc2626/ffffff&text=事业冲刺', '吴老师', NULL, '全国', '综合冲刺', '事业单位', 'LIVE', 499.00, 799.00, '事业单位,冲刺,刷题', 588, '考前集中刷题，串联职测、公基和综应高频考点。', '高频回顾|套题训练|考前答疑', '事业单位考前冲刺学员', 120, 'PUBLISHED');

INSERT INTO course
(title, cover_url, teacher_name, teacher_avatar, province, subject_name, exam_type, course_type, price, original_price, tags, learn_count, description, highlights, target_users, validity_days, status)
SELECT t.title, t.cover_url, t.teacher_name, t.teacher_avatar, t.province, t.subject_name, t.exam_type, t.course_type, t.price, t.original_price, t.tags, t.learn_count, t.description, t.highlights, t.target_users, t.validity_days, t.status
FROM tmp_stage33_courses t
WHERE NOT EXISTS (
  SELECT 1 FROM course c WHERE c.title = t.title
);

INSERT INTO course_chapter
(course_id, title, chapter_type, duration_minutes, sort_no, live_start_time, video_file_id, video_url, trial_available)
SELECT c.id, chapter.title, c.course_type, chapter.duration_minutes, chapter.sort_no, NULL, NULL, NULL, chapter.trial_available
FROM course c
JOIN tmp_stage33_courses t ON t.title = c.title
JOIN (
  SELECT 1 AS sort_no, '课程导学与考情分析' AS title, 30 AS duration_minutes, 1 AS trial_available
  UNION ALL SELECT 2, '核心知识系统精讲', 60, 0
  UNION ALL SELECT 3, '高频题型实战训练', 55, 0
  UNION ALL SELECT 4, '阶段测评与复盘提升', 45, 0
) chapter
WHERE NOT EXISTS (
  SELECT 1 FROM course_chapter cc WHERE cc.course_id = c.id
);

DROP TEMPORARY TABLE IF EXISTS tmp_stage33_courses;

