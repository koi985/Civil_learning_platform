ALTER TABLE news
  ADD COLUMN info_category VARCHAR(64) NULL AFTER category_id,
  ADD COLUMN province VARCHAR(64) NULL AFTER info_category;

UPDATE news n
LEFT JOIN news_category c ON c.id = n.category_id
SET n.info_category = COALESCE(n.info_category, '考试公告'),
    n.province = COALESCE(
      n.province,
      CASE
        WHEN n.tags LIKE '%,北京,%' OR n.tags LIKE '北京,%' THEN '北京'
        WHEN n.tags LIKE '%,天津,%' OR n.tags LIKE '天津,%' THEN '天津'
        WHEN n.tags LIKE '%,河北,%' OR n.tags LIKE '河北,%' THEN '河北'
        WHEN n.tags LIKE '%,山西,%' OR n.tags LIKE '山西,%' THEN '山西'
        WHEN n.tags LIKE '%,内蒙古,%' OR n.tags LIKE '内蒙古,%' THEN '内蒙古'
        WHEN n.tags LIKE '%,辽宁,%' OR n.tags LIKE '辽宁,%' THEN '辽宁'
        WHEN n.tags LIKE '%,吉林,%' OR n.tags LIKE '吉林,%' THEN '吉林'
        WHEN n.tags LIKE '%,黑龙江,%' OR n.tags LIKE '黑龙江,%' THEN '黑龙江'
        WHEN n.tags LIKE '%,上海,%' OR n.tags LIKE '上海,%' THEN '上海'
        WHEN n.tags LIKE '%,江苏,%' OR n.tags LIKE '江苏,%' THEN '江苏'
        WHEN n.tags LIKE '%,浙江,%' OR n.tags LIKE '浙江,%' THEN '浙江'
        WHEN n.tags LIKE '%,安徽,%' OR n.tags LIKE '安徽,%' THEN '安徽'
        WHEN n.tags LIKE '%,福建,%' OR n.tags LIKE '福建,%' THEN '福建'
        WHEN n.tags LIKE '%,江西,%' OR n.tags LIKE '江西,%' THEN '江西'
        WHEN n.tags LIKE '%,山东,%' OR n.tags LIKE '山东,%' THEN '山东'
        WHEN n.tags LIKE '%,河南,%' OR n.tags LIKE '河南,%' THEN '河南'
        WHEN n.tags LIKE '%,湖北,%' OR n.tags LIKE '湖北,%' THEN '湖北'
        WHEN n.tags LIKE '%,湖南,%' OR n.tags LIKE '湖南,%' THEN '湖南'
        WHEN n.tags LIKE '%,广东,%' OR n.tags LIKE '广东,%' THEN '广东'
        WHEN n.tags LIKE '%,广西,%' OR n.tags LIKE '广西,%' THEN '广西'
        WHEN n.tags LIKE '%,海南,%' OR n.tags LIKE '海南,%' THEN '海南'
        WHEN n.tags LIKE '%,重庆,%' OR n.tags LIKE '重庆,%' THEN '重庆'
        WHEN n.tags LIKE '%,四川,%' OR n.tags LIKE '四川,%' THEN '四川'
        WHEN n.tags LIKE '%,贵州,%' OR n.tags LIKE '贵州,%' THEN '贵州'
        WHEN n.tags LIKE '%,云南,%' OR n.tags LIKE '云南,%' THEN '云南'
        WHEN n.tags LIKE '%,西藏,%' OR n.tags LIKE '西藏,%' THEN '西藏'
        WHEN n.tags LIKE '%,陕西,%' OR n.tags LIKE '陕西,%' THEN '陕西'
        WHEN n.tags LIKE '%,甘肃,%' OR n.tags LIKE '甘肃,%' THEN '甘肃'
        WHEN n.tags LIKE '%,青海,%' OR n.tags LIKE '青海,%' THEN '青海'
        WHEN n.tags LIKE '%,宁夏,%' OR n.tags LIKE '宁夏,%' THEN '宁夏'
        WHEN n.tags LIKE '%,新疆,%' OR n.tags LIKE '新疆,%' THEN '新疆'
        ELSE '全国'
      END
    );

UPDATE news
SET source_name = NULL,
    author_name = NULL,
    tags = TRIM(BOTH ',' FROM REPLACE(tags, ',中公网校', '')),
    content = REGEXP_REPLACE(
      REGEXP_REPLACE(
        REGEXP_REPLACE(content, '<p>来源：[^<]*</p>', ''),
        '<p>原文链接：[\\s\\S]*?</p>',
        ''
      ),
      '<p>文章来源：[\\s\\S]*?</p>',
      ''
    )
WHERE source_name = '中公网校';
