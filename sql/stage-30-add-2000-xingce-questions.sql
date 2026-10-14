-- Stage 30: add 2000 original 行测 objective questions.
-- Reference site reviewed: https://gwy.gkzhenti.cn/
-- Note: this seed follows the public site's 行测 category structure, but uses
-- original deterministic templates instead of copying third-party question text.

DELETE qo
FROM question_option qo
JOIN question q ON q.id = qo.question_id
WHERE q.source_name = '公开真题库参考-原创扩展';

DELETE FROM question
WHERE source_name = '公开真题库参考-原创扩展';

DROP PROCEDURE IF EXISTS seed_stage30_xingce_questions;

DELIMITER $$

CREATE PROCEDURE seed_stage30_xingce_questions()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE qid BIGINT;
  DECLARE cat VARCHAR(64);
  DECLARE qtype VARCHAR(32);
  DECLARE diff VARCHAR(32);
  DECLARE prov VARCHAR(32);
  DECLARE stem_text TEXT;
  DECLARE analysis_text TEXT;
  DECLARE ans VARCHAR(255);
  DECLARE kp VARCHAR(255);
  DECLARE a TEXT;
  DECLARE b TEXT;
  DECLARE c TEXT;
  DECLARE d TEXT;
  DECLARE base_num INT;
  DECLARE rate_num INT;
  DECLARE correct_num INT;
  DECLARE wrong1 INT;
  DECLARE wrong2 INT;
  DECLARE wrong3 INT;
  DECLARE answer_slot INT;

  WHILE i <= 2000 DO
    SET cat = CASE i % 5
      WHEN 0 THEN '常识判断'
      WHEN 1 THEN '言语理解与表达'
      WHEN 2 THEN '数量关系'
      WHEN 3 THEN '判断推理'
      ELSE '资料分析'
    END;

    SET qtype = IF(i % 4 = 0, 'MULTI', 'SINGLE');
    SET diff = CASE i % 3 WHEN 0 THEN '基础' WHEN 1 THEN '中等' ELSE '较难' END;
    SET prov = CASE i % 6
      WHEN 0 THEN '全国'
      WHEN 1 THEN '江苏'
      WHEN 2 THEN '广东'
      WHEN 3 THEN '浙江'
      WHEN 4 THEN '山东'
      ELSE '四川'
    END;
    SET answer_slot = i % 4;
    SET base_num = 40 + (i % 80);
    SET rate_num = CASE i % 4 WHEN 0 THEN 10 WHEN 1 THEN 20 WHEN 2 THEN 25 ELSE 50 END;

    IF qtype = 'MULTI' THEN
      SET ans = CASE answer_slot WHEN 0 THEN 'A,B,D' WHEN 1 THEN 'A,C,D' WHEN 2 THEN 'A,B,C' ELSE 'B,C,D' END;

      IF cat = '常识判断' THEN
        SET stem_text = CONCAT('【原创题', i, '】推进基层治理现代化时，下列哪些做法符合依法行政、便民服务和风险防控并重的要求？');
        SET analysis_text = '依法行政强调权限清晰、程序规范、便民高效和风险留痕；凡是扩大无关采集、忽视授权边界或取消必要监督的做法均不恰当。';
        SET kp = '常识判断,依法行政,基层治理';
        IF ans = 'A,B,D' THEN
          SET a='明确事项办理权限和责任边界'; SET b='对高频事项优化流程并公开办理时限'; SET c='默认收集与业务无关的全部个人信息'; SET d='对关键环节保留必要审计记录';
        ELSEIF ans = 'A,C,D' THEN
          SET a='按照法定权限办理行政事项'; SET b='为追求速度省略依法应有的告知程序'; SET c='设置群众反馈和纠错渠道'; SET d='对敏感数据实行分级授权';
        ELSEIF ans = 'A,B,C' THEN
          SET a='压减重复证明材料'; SET b='推动跨部门数据核验'; SET c='保留必要的安全审查'; SET d='将所有事项都改为口头办理且不留痕';
        ELSE
          SET a='以内部方便为唯一标准增加群众填报事项'; SET b='公开服务指南和办理依据'; SET c='建立异常事项复核机制'; SET d='对窗口人员开展规范服务培训';
        END IF;
      ELSEIF cat = '言语理解与表达' THEN
        SET stem_text = CONCAT('【原创题', i, '】阅读观点：“公共服务数字化不是把纸质流程简单搬到线上，而是围绕群众需求重构流程、提升协同效率。”下列理解正确的有：');
        SET analysis_text = '材料强调数字化改革要服务群众需求，重点在流程重构和协同效率，而非形式上线或单纯增加系统。';
        SET kp = '言语理解,文意理解,主旨概括';
        IF ans = 'A,B,D' THEN
          SET a='数字化改革应以群众办事体验为导向'; SET b='线上化不等于简单复制原有线下流程'; SET c='系统数量越多，服务效果必然越好'; SET d='跨部门协同是提升服务效率的重要方向';
        ELSEIF ans = 'A,C,D' THEN
          SET a='流程重构比形式上线更关键'; SET b='只要材料改成电子版就完成了数字化改革'; SET c='群众需求是改革设计的重要依据'; SET d='数据协同有助于减少重复提交';
        ELSEIF ans = 'A,B,C' THEN
          SET a='公共服务数字化应避免形式主义'; SET b='改革需要关注办理链条整体效率'; SET c='线上服务也应保持必要的人性化支持'; SET d='群众必须适应系统，流程无需调整';
        ELSE
          SET a='数字化意味着完全取消线下服务渠道'; SET b='公共服务改革要关注真实需求'; SET c='流程优化能提升协同效率'; SET d='简单搬线上可能无法改善体验';
        END IF;
      ELSEIF cat = '数量关系' THEN
        SET stem_text = CONCAT('【原创题', i, '】某单位开展两项培训，报名行测的有', base_num, '人，报名申论的有', base_num - 8, '人，两项都报名的有', 12 + (i % 8), '人。根据这些信息，下列说法正确的有：');
        SET correct_num = base_num + (base_num - 8) - (12 + (i % 8));
        SET analysis_text = CONCAT('至少报名一项人数=', base_num, '+', base_num - 8, '-', 12 + (i % 8), '=', correct_num, '；只报行测和只报申论分别用总数减交集计算。');
        SET kp = '数量关系,容斥原理';
        IF ans = 'A,B,D' THEN
          SET a=CONCAT('至少报名一项的有', correct_num, '人'); SET b=CONCAT('只报名行测的有', base_num - (12 + (i % 8)), '人'); SET c=CONCAT('两项都报名的有', correct_num, '人'); SET d='两项都报名人数少于报名行测人数';
        ELSEIF ans = 'A,C,D' THEN
          SET a=CONCAT('只报名申论的有', base_num - 8 - (12 + (i % 8)), '人'); SET b='至少报名一项的人数等于两项报名人数之和'; SET c=CONCAT('至少报名一项的有', correct_num, '人'); SET d='只报名行测人数多于两项都报名人数';
        ELSEIF ans = 'A,B,C' THEN
          SET a='两项都报名人数不能重复计入总人数'; SET b=CONCAT('只报名行测的有', base_num - (12 + (i % 8)), '人'); SET c=CONCAT('至少报名一项的有', correct_num, '人'); SET d=CONCAT('只报名申论的有', base_num, '人');
        ELSE
          SET a='至少报名一项人数应直接相加不减交集'; SET b=CONCAT('两项都报名的有', 12 + (i % 8), '人'); SET c=CONCAT('只报名申论的有', base_num - 8 - (12 + (i % 8)), '人'); SET d=CONCAT('至少报名一项的有', correct_num, '人');
        END IF;
      ELSEIF cat = '判断推理' THEN
        SET stem_text = CONCAT('【原创题', i, '】某地认为“上线智能预约系统后，排队时间下降，因此只要使用信息系统，服务质量就一定提高”。下列哪些选项能够削弱这一结论？');
        SET analysis_text = '题干把单一案例扩大为必然结论，削弱可从其他变量、适用条件、负面影响和结论过度概括入手。';
        SET kp = '判断推理,削弱论证';
        IF ans = 'A,B,D' THEN
          SET a='排队时间下降还与窗口数量增加有关'; SET b='部分老年群体因不会预约反而办理更困难'; SET c='系统上线后预约提醒更加及时'; SET d='服务质量还包括准确性、可及性和满意度等指标';
        ELSEIF ans = 'A,C,D' THEN
          SET a='该系统只适用于预约类事项，不适用于现场核验事项'; SET b='群众平均等待时间确实有所缩短'; SET c='系统故障时缺少人工兜底会影响办理'; SET d='个别事项效率提升不能推出所有服务质量提高';
        ELSEIF ans = 'A,B,C' THEN
          SET a='服务质量评价还取决于一次办成率'; SET b='上线同期同步开展了人员培训'; SET c='部分群众反馈线上入口过多不易找到'; SET d='预约系统提供了短信提醒功能';
        ELSE
          SET a='系统上线后办事指南更加清晰'; SET b='信息系统若设计不当可能增加填报负担'; SET c='排队时间变化可能来自业务量下降'; SET d='服务质量不能只用排队时间衡量';
        END IF;
      ELSE
        SET stem_text = CONCAT('【原创题', i, '】某市2024年公共服务事项线上办理量为', base_num, '万件，同比增长', rate_num, '%。根据上述信息，下列说法正确的有：');
        SET correct_num = CASE rate_num WHEN 10 THEN ROUND(base_num / 1.1) WHEN 20 THEN ROUND(base_num / 1.2) WHEN 25 THEN ROUND(base_num / 1.25) ELSE ROUND(base_num / 1.5) END;
        SET analysis_text = CONCAT('基期量=现期量÷(1+增长率)，约为', correct_num, '万件；增长率为正说明现期高于基期。');
        SET kp = '资料分析,增长率,基期量';
        IF ans = 'A,B,D' THEN
          SET a=CONCAT('2023年线上办理量约为', correct_num, '万件'); SET b='2024年线上办理量高于2023年'; SET c='同比增长率为负值'; SET d='已知现期量和增长率可以估算基期量';
        ELSEIF ans = 'A,C,D' THEN
          SET a='该指标属于现期量与增长率结合的问题'; SET b=CONCAT('2023年线上办理量约为', base_num + rate_num, '万件'); SET c=CONCAT('2023年线上办理量约为', correct_num, '万件'); SET d='增长率越高，现期量相同条件下基期量越小';
        ELSEIF ans = 'A,B,C' THEN
          SET a='基期量需要用除法估算'; SET b=CONCAT('2023年线上办理量约为', correct_num, '万件'); SET c='2024年较2023年有所增加'; SET d='2024年办理量低于2023年';
        ELSE
          SET a='增长率为正不能说明现期高于基期'; SET b='2024年办理量是现期量'; SET c=CONCAT('2023年线上办理量约为', correct_num, '万件'); SET d='本题可用现期量除以1加增长率计算';
        END IF;
      END IF;
    ELSE
      SET ans = CASE answer_slot WHEN 0 THEN 'A' WHEN 1 THEN 'B' WHEN 2 THEN 'C' ELSE 'D' END;

      IF cat = '常识判断' THEN
        SET stem_text = CONCAT('【原创题', i, '】关于政务服务和公共管理，下列说法最恰当的是：');
        SET analysis_text = '政务服务改革强调依法依规、公开透明、数据共享、便民高效，同时要保护个人信息和公共安全。';
        SET kp = '常识判断,政务服务';
        SET a=IF(ans='A','推动跨部门数据核验，减少群众重复提交材料','为提高速度，可以省略依法必须履行的告知程序');
        SET b=IF(ans='B','涉及个人信息处理时，应坚持最小必要原则','所有政务数据都应无条件向社会公开');
        SET c=IF(ans='C','窗口服务应公开办理依据、流程和时限','基层治理只需要技术系统，不需要人员服务');
        SET d=IF(ans='D','对高频事项优化流程有助于提升办事体验','行政机关可以随意增加没有依据的证明材料');
      ELSEIF cat = '言语理解与表达' THEN
        SET stem_text = CONCAT('【原创题', i, '】基层治理不能只追求“看得见”的新设备、新平台，更要关注群众实际感受。若流程没有优化、责任没有厘清，再多系统也可能只是增加新的负担。这段文字意在说明：');
        SET analysis_text = '文段对“重设备轻流程”的倾向进行提醒，核心是治理创新要服务实际问题和群众体验。';
        SET kp = '言语理解,主旨概括';
        SET a=IF(ans='A','治理创新应以解决实际问题为导向','基层治理应优先购买更多设备');
        SET b=IF(ans='B','平台建设必须替代全部线下服务','优化流程和明确责任比形式上线更重要');
        SET c=IF(ans='C','群众体验是检验治理成效的重要标准','系统数量越多，治理能力越强');
        SET d=IF(ans='D','技术应用应与流程再造相结合','基层工作负担增加是数字化的必然结果');
      ELSEIF cat = '数量关系' THEN
        SET correct_num = base_num * 3 + 12;
        SET wrong1 = correct_num - 6;
        SET wrong2 = correct_num + 8;
        SET wrong3 = correct_num + 18;
        SET stem_text = CONCAT('【原创题', i, '】某资料整理小组第一天完成', base_num, '份材料，第二天完成量是第一天的2倍，第三天比第一天多12份。三天共完成多少份材料？');
        SET analysis_text = CONCAT('三天共完成', base_num, '+', base_num * 2, '+', base_num + 12, '=', correct_num, '份。');
        SET kp = '数量关系,基础计算';
        SET a=CONCAT(IF(ans='A', correct_num, wrong1), '份');
        SET b=CONCAT(IF(ans='B', correct_num, wrong2), '份');
        SET c=CONCAT(IF(ans='C', correct_num, wrong3), '份');
        SET d=CONCAT(IF(ans='D', correct_num, wrong1 + 3), '份');
      ELSEIF cat = '判断推理' THEN
        SET stem_text = CONCAT('【原创题', i, '】某部门认为，试点窗口实行预约办理后投诉量下降，因此预约办理是投诉量下降的唯一原因。以下哪项最能削弱该结论？');
        SET analysis_text = '若同期还存在其他能导致投诉下降的因素，就能削弱“唯一原因”的判断。';
        SET kp = '判断推理,削弱论证';
        SET a=IF(ans='A','试点期间该窗口同时增加了导办人员并优化了告知材料','预约办理确实能减少现场等待时间');
        SET b=IF(ans='B','该窗口群众满意度有所提升','同期开展了业务培训和流程简化，也可能减少投诉');
        SET c=IF(ans='C','投诉量下降可能与办理事项数量减少有关','预约系统提供了短信提醒功能');
        SET d=IF(ans='D','投诉下降还可能来自评价入口调整和现场协调加强','不少群众认为预约提醒比较方便');
      ELSE
        SET correct_num = CASE rate_num WHEN 10 THEN ROUND(base_num / 1.1) WHEN 20 THEN ROUND(base_num / 1.2) WHEN 25 THEN ROUND(base_num / 1.25) ELSE ROUND(base_num / 1.5) END;
        SET wrong1 = correct_num + 5;
        SET wrong2 = correct_num + 10;
        SET wrong3 = correct_num - 4;
        SET stem_text = CONCAT('【原创题', i, '】某地2024年新增便民服务点', base_num, '个，同比增长', rate_num, '%。则2023年新增便民服务点约为多少个？');
        SET analysis_text = CONCAT('2023年数量=', base_num, '÷(1+', rate_num, '%)，约为', correct_num, '个。');
        SET kp = '资料分析,基期量';
        SET a=CONCAT(IF(ans='A', correct_num, wrong1), '个');
        SET b=CONCAT(IF(ans='B', correct_num, wrong2), '个');
        SET c=CONCAT(IF(ans='C', correct_num, wrong3), '个');
        SET d=CONCAT(IF(ans='D', correct_num, wrong1 + 7), '个');
      END IF;
    END IF;

    INSERT INTO question (
      subject_name, category_name, province, question_type, difficulty_level,
      source_name, stem, analysis, answer_value, knowledge_points,
      video_explain_title, video_explain_url, enabled
    ) VALUES (
      '行测', cat, prov, qtype, diff,
      '公开真题库参考-原创扩展', stem_text, analysis_text, ans, kp,
      NULL, NULL, 1
    );

    SET qid = LAST_INSERT_ID();

    INSERT INTO question_option (question_id, option_key, option_content, sort_no) VALUES
      (qid, 'A', a, 1),
      (qid, 'B', b, 2),
      (qid, 'C', c, 3),
      (qid, 'D', d, 4);

    SET i = i + 1;
  END WHILE;
END$$

DELIMITER ;

CALL seed_stage30_xingce_questions();

DROP PROCEDURE IF EXISTS seed_stage30_xingce_questions;
