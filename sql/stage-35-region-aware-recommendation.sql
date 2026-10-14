ALTER TABLE course
  ADD COLUMN region_scope VARCHAR(32) NOT NULL DEFAULT 'NATIONAL' AFTER teacher_avatar;

UPDATE course
SET region_scope = CASE WHEN province = '全国' THEN 'NATIONAL' ELSE 'PROVINCE' END
WHERE region_scope IS NULL OR region_scope = '';

ALTER TABLE recommend_pinned_resource
  ADD COLUMN region_scope VARCHAR(32) NOT NULL DEFAULT 'ALL' AFTER action_text,
  ADD COLUMN province VARCHAR(32) NULL AFTER region_scope,
  ADD COLUMN fallback_enabled TINYINT NOT NULL DEFAULT 1 AFTER province;

UPDATE recommend_pinned_resource
SET region_scope = COALESCE(NULLIF(region_scope, ''), 'ALL'),
    fallback_enabled = COALESCE(fallback_enabled, 1);

INSERT INTO recommend_config (config_key, config_name, config_value, description_text) VALUES
('region_match_boost', '地区匹配加权', 0.2800, '报考地区一致的课程、模考和资讯加权'),
('national_region_boost', '全国兜底加权', 0.1200, '全国资源在地区推荐中的基础保留权重'),
('outside_region_penalty', '外省资源降权', 0.1800, '强地区属性内容与报考地区不一致时降权')
ON DUPLICATE KEY UPDATE
  config_name = VALUES(config_name),
  config_value = VALUES(config_value),
  description_text = VALUES(description_text);
