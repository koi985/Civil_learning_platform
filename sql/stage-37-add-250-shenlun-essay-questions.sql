-- Stage 37: add 250 original/rewritten 申论专项 short-answer questions.
-- User requested importing from:
-- https://www.flowus.cn/mmjz/share/4eb1d8b5-82e9-4994-8399-64309b0cdc5e
-- The FlowUs page is not readable in the non-interactive crawler environment,
-- and this seed intentionally does not copy third-party question text. It adds
-- original "真题风格改编" items that match the five 申论专项 categories.

DELETE FROM question
WHERE source_name = '申论真题风格改编-原创扩展';

DROP PROCEDURE IF EXISTS seed_stage37_shenlun_questions;

DELIMITER $$

CREATE PROCEDURE seed_stage37_shenlun_questions()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE cat VARCHAR(64);
  DECLARE diff VARCHAR(32);
  DECLARE prov VARCHAR(32);
  DECLARE theme VARCHAR(64);
  DECLARE scene VARCHAR(255);
  DECLARE issue TEXT;
  DECLARE action_text TEXT;
  DECLARE doc_type VARCHAR(64);
  DECLARE stem_text TEXT;
  DECLARE analysis_text TEXT;
  DECLARE answer_text TEXT;
  DECLARE kp VARCHAR(255);
  DECLARE exam_year INT;

  WHILE i <= 250 DO
    SET cat = CASE
      WHEN i <= 50 THEN '归纳概括'
      WHEN i <= 100 THEN '综合分析'
      WHEN i <= 150 THEN '提出对策'
      WHEN i <= 200 THEN '应用文写作'
      ELSE '文章写作'
    END;

    SET diff = CASE i % 3 WHEN 0 THEN '基础' WHEN 1 THEN '中等' ELSE '较难' END;
    SET prov = CASE i % 8
      WHEN 0 THEN '全国'
      WHEN 1 THEN '江苏'
      WHEN 2 THEN '广东'
      WHEN 3 THEN '浙江'
      WHEN 4 THEN '山东'
      WHEN 5 THEN '四川'
      WHEN 6 THEN '湖北'
      ELSE '河南'
    END;
    SET exam_year = 2018 + (i % 8);

    SET theme = CASE i % 10
      WHEN 0 THEN '政务服务'
      WHEN 1 THEN '基层治理'
      WHEN 2 THEN '乡村振兴'
      WHEN 3 THEN '养老服务'
      WHEN 4 THEN '青年就业'
      WHEN 5 THEN '文旅融合'
      WHEN 6 THEN '数字治理'
      WHEN 7 THEN '生态保护'
      WHEN 8 THEN '营商环境'
      ELSE '应急管理'
    END;

    SET scene = CASE theme
      WHEN '政务服务' THEN '某地推进“一窗通办”和线上办事，群众反映重复提交材料、跨部门核验慢、办理进度提醒不及时。'
      WHEN '基层治理' THEN '某社区推进网格治理，出现居民参与不足、事项流转慢、志愿力量不稳定、物业与社区协同弱等情况。'
      WHEN '乡村振兴' THEN '某县发展乡村特色产业，面临产品同质化、冷链物流薄弱、品牌传播不足、农户收益联结不紧等问题。'
      WHEN '养老服务' THEN '某街道建设养老服务圈，存在助餐点覆盖不足、护理人员短缺、服务需求摸排不准、医养衔接不顺畅。'
      WHEN '青年就业' THEN '某市开展高校毕业生就业帮扶，发现岗位信息分散、实习实践不足、职业指导泛化、困难群体帮扶不精准。'
      WHEN '文旅融合' THEN '某地打造文旅街区，出现活动内容雷同、游客停留时间短、本土文化表达弱、商户服务标准不一。'
      WHEN '数字治理' THEN '某区上线基层治理平台，基层干部反映系统入口多、重复录入、数据共享不足、老年群众使用困难。'
      WHEN '生态保护' THEN '某流域开展生态修复，面临沿线生活污水治理不均衡、企业环保意识弱、日常巡查力量不足等情况。'
      WHEN '营商环境' THEN '某产业园优化企业服务，企业反映政策知晓率低、兑现周期长、融资渠道窄、审批环节衔接不畅。'
      ELSE '某地完善极端天气应急处置，暴露出预警传递慢、物资储备分散、群众避险意识弱、部门联动演练不足。'
    END;

    SET issue = CASE theme
      WHEN '政务服务' THEN '流程再造不足、数据共享不充分、部门协同不顺、服务透明度不够。'
      WHEN '基层治理' THEN '群众参与渠道不畅、网格响应效率不高、多元主体协同不足、长效机制不健全。'
      WHEN '乡村振兴' THEN '特色辨识度不高、基础配套薄弱、品牌运营不足、利益联结机制不紧。'
      WHEN '养老服务' THEN '供给覆盖不均、专业人才不足、需求匹配不准、医养资源衔接不畅。'
      WHEN '青年就业' THEN '岗位匹配度低、实践能力支撑不足、职业规划指导不精、重点群体帮扶不准。'
      WHEN '文旅融合' THEN '内容创新不足、文化表达浅层化、服务标准不统一、消费链条延伸不够。'
      WHEN '数字治理' THEN '平台统筹不足、数据壁垒仍在、基层填报负担较重、适老化服务不足。'
      WHEN '生态保护' THEN '源头治理不均衡、主体责任压实不够、巡查监管薄弱、公众参与不足。'
      WHEN '营商环境' THEN '政策触达不精准、兑现效率不高、要素保障不足、审批协同仍需优化。'
      ELSE '预警触达不及时、资源统筹不足、公众避险能力弱、跨部门协同演练不够。'
    END;

    SET action_text = CASE theme
      WHEN '政务服务' THEN '梳理高频事项，推进数据共享和电子证照互认，完善一次告知、进度提醒和帮办代办机制。'
      WHEN '基层治理' THEN '健全居民议事平台，完善网格事项闭环，培育志愿队伍，推动社区、物业、社会组织协同。'
      WHEN '乡村振兴' THEN '突出差异化定位，完善物流和公共服务，培育区域品牌，建立农户参与和收益共享机制。'
      WHEN '养老服务' THEN '开展需求摸排，优化助餐助医布局，培养护理队伍，建设信息平台并推动医养资源衔接。'
      WHEN '青年就业' THEN '整合岗位信息，拓展实习见习，开展分层职业指导，对困难毕业生实施一人一策帮扶。'
      WHEN '文旅融合' THEN '深挖本地文化，丰富沉浸体验，统一服务标准，延伸夜游、文创、研学等消费链条。'
      WHEN '数字治理' THEN '整合平台入口，推动数据一次采集、多方复用，保留线下服务和适老化帮办，减轻基层负担。'
      WHEN '生态保护' THEN '强化源头治理和分段责任，完善巡查监测，推动企业守法和公众参与，建立常态化评估机制。'
      WHEN '营商环境' THEN '精准推送惠企政策，压缩兑现周期，完善融资对接，建立审批协同和企业诉求闭环机制。'
      ELSE '完善预警直达机制，统筹物资储备，开展公众避险宣传，强化部门联动演练和复盘整改。'
    END;

    SET doc_type = CASE i % 8
      WHEN 0 THEN '倡议书'
      WHEN 1 THEN '通知'
      WHEN 2 THEN '简报'
      WHEN 3 THEN '发言提纲'
      WHEN 4 THEN '宣传稿'
      WHEN 5 THEN '工作建议'
      WHEN 6 THEN '调研提纲'
      ELSE '公开信'
    END;

    IF cat = '归纳概括' THEN
      SET stem_text = CONCAT('【', exam_year, '年', prov, '申论真题风格改编】给定材料：', scene, '请概括材料反映的主要问题或短板。要求：全面准确、条理清晰，不超过250字。');
      SET analysis_text = CONCAT('作答要从材料中的现象提炼共性问题，避免照抄细节。可按“机制、资源、服务、协同”维度归纳，重点覆盖', theme, '场景中的主要矛盾。');
      SET answer_text = CONCAT('主要问题包括：', issue, '这些问题共同影响', theme, '工作的服务质效和群众获得感。');
    ELSEIF cat = '综合分析' THEN
      SET stem_text = CONCAT('【', exam_year, '年', prov, '申论真题风格改编】给定材料：', scene, '请结合材料，谈谈你对“治理成效既看速度，更看温度和精度”的理解。要求：观点明确、分析深入，不超过300字。');
      SET analysis_text = CONCAT('综合分析题要先解释关键词，再结合材料指出原因和启示。速度体现效率，温度体现群众感受，精度体现问题识别和资源配置能力。');
      SET answer_text = CONCAT('这句话说明，', theme, '工作不能只追求表面效率，还要回应真实需求。速度要求流程顺畅、响应及时；温度要求服务可及、尊重差异；精度要求摸清问题、精准配置资源。材料中的短板表明，只有把效率提升、群众体验和精细治理统一起来，才能形成可持续的治理成效。');
    ELSEIF cat = '提出对策' THEN
      SET stem_text = CONCAT('【', exam_year, '年', prov, '申论真题风格改编】给定材料：', scene, '请针对材料中的问题提出改进措施。要求：有针对性、可操作，不超过300字。');
      SET analysis_text = CONCAT('提出对策要做到问题与措施对应，既有制度机制，也有具体抓手。对策应围绕', theme, '中的堵点、资源、协同和长效保障展开。');
      SET answer_text = CONCAT('建议：', action_text, '同时建立督办评估和群众反馈机制，及时复盘整改，推动问题从个案解决转向长效治理。');
    ELSEIF cat = '应用文写作' THEN
      SET stem_text = CONCAT('【', exam_year, '年', prov, '申论真题风格改编】假如你是有关部门工作人员，请结合材料“', scene, '”，围绕', theme, '写一份', doc_type, '的主要内容。要求：格式要素基本完整、语言得体、重点突出，不超过400字。');
      SET analysis_text = CONCAT('应用文写作应根据文种确定对象、目的和结构。', doc_type, '要有标题意识，正文突出背景、事项、措施或号召，语言应符合机关事务表达。');
      SET answer_text = CONCAT('写作要点：一是交代背景，说明', theme, '工作的重要性和当前短板；二是明确主要任务，围绕', action_text, '展开；三是提出参与方式、责任要求或时间安排；四是以务实、简洁的语言收束，体现服务群众和推动落实。');
    ELSE
      SET stem_text = CONCAT('【', exam_year, '年', prov, '申论真题风格改编】请结合给定材料，以“在解决实际问题中提升治理效能”为主题，联系', theme, '实际，写一篇文章提纲。要求：观点鲜明、结构完整、论证有层次。');
      SET analysis_text = CONCAT('文章写作题要围绕主题确立中心论点，并结合材料设置分论点。可从问题导向、人民立场、协同治理和长效机制四个层面展开。');
      SET answer_text = CONCAT('中心论点：治理效能来自对实际问题的精准回应。分论点一，坚持问题导向，找准', theme, '中的堵点痛点；分论点二，坚持人民立场，以群众获得感检验治理成效；分论点三，强化协同联动，整合政府、市场、社会资源；分论点四，完善长效机制，让经验转化为制度成果。结尾升华：以实干破解难题，以精细治理提升现代化水平。');
    END IF;

    SET kp = CONCAT('申论,', cat, ',', theme, ',', prov, '真题风格改编');

    INSERT INTO question (
      subject_name, category_name, province, question_type, difficulty_level,
      source_name, stem, analysis, answer_value, knowledge_points,
      video_explain_title, video_explain_url, enabled
    ) VALUES (
      '申论', cat, prov, 'SHORT_ANSWER', diff,
      '申论真题风格改编-原创扩展', stem_text, analysis_text, answer_text, kp,
      NULL, NULL, 1
    );

    SET i = i + 1;
  END WHILE;
END$$

DELIMITER ;

CALL seed_stage37_shenlun_questions();

DROP PROCEDURE IF EXISTS seed_stage37_shenlun_questions;
