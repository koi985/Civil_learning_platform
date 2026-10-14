package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_knowledge_ability")
public class UserKnowledgeAbility extends BaseEntity {

    private Long userId;
    private String subjectName;
    private String knowledgePoint;
    private BigDecimal theta;
    private BigDecimal thetaConfidence;
    private BigDecimal masteryScoreSnapshot;
    private LocalDateTime calibratedAt;
    private String calibrationVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
