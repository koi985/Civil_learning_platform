package com.civil.platform.module.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.civil.platform.module.entity.UserKnowledgeMastery;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserKnowledgeMasteryMapper extends BaseMapper<UserKnowledgeMastery> {

    @Insert("INSERT INTO user_knowledge_mastery (user_id, subject_name, knowledge_point, attempt_count, correct_count, wrong_count, pending_wrong_count, accuracy_rate, mastery_score, mastery_confidence, priority_score, mastery_level, knowledge_status, last_practiced_at) " +
            "VALUES (#{userId}, #{subjectName}, #{knowledgePoint}, #{attemptCount}, #{correctCount}, #{wrongCount}, #{pendingWrongCount}, #{accuracyRate}, #{masteryScore}, #{masteryConfidence}, #{priorityScore}, #{masteryLevel}, #{knowledgeStatus}, #{lastPracticedAt}) " +
            "ON DUPLICATE KEY UPDATE attempt_count = VALUES(attempt_count), correct_count = VALUES(correct_count), wrong_count = VALUES(wrong_count), pending_wrong_count = VALUES(pending_wrong_count), accuracy_rate = VALUES(accuracy_rate), mastery_score = VALUES(mastery_score), mastery_confidence = VALUES(mastery_confidence), priority_score = VALUES(priority_score), mastery_level = VALUES(mastery_level), knowledge_status = VALUES(knowledge_status), last_practiced_at = VALUES(last_practiced_at), updated_at = CURRENT_TIMESTAMP")
    int upsert(UserKnowledgeMastery mastery);
}
