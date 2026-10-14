package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_knowledge_mastery")
public class UserKnowledgeMastery extends BaseEntity {

    private Long userId;
    private String subjectName;
    private String knowledgePoint;
    private Integer attemptCount;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer pendingWrongCount;
    private BigDecimal accuracyRate;
    private BigDecimal masteryScore;
    private BigDecimal masteryConfidence;
    private BigDecimal priorityScore;
    private String masteryLevel;
    private String knowledgeStatus;
    private LocalDateTime lastPracticedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
