-- Homepage and public listing query indexes.

CREATE INDEX idx_banner_enabled_sort ON banner (enabled, sort_no);
CREATE INDEX idx_notice_status_published ON notice (status, published_at);
CREATE INDEX idx_course_status_learn ON course (status, learn_count);
CREATE INDEX idx_news_status_published ON news (status, published_at);
CREATE INDEX idx_news_status_province_published ON news (status, province, published_at);
CREATE INDEX idx_news_status_read ON news (status, read_count);
CREATE INDEX idx_classroom_status_public_member ON classroom (status, is_public, member_count);
CREATE INDEX idx_classroom_teacher_public_status ON classroom (teacher_id, is_public, status);
CREATE INDEX idx_exam_status_created ON exam (status, created_at);
CREATE INDEX idx_question_enabled_created ON question (enabled, created_at);
CREATE INDEX idx_teacher_profile_audit_audited ON teacher_profile (audit_status, audited_at);
CREATE INDEX idx_user_course_user_created ON user_course (user_id, created_at);
CREATE INDEX idx_practice_record_user_created ON practice_record (user_id, created_at);
CREATE INDEX idx_exam_record_user_created ON exam_record (user_id, created_at);
