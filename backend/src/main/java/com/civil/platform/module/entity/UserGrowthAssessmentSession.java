package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_growth_assessment_session")
public class UserGrowthAssessmentSession extends BaseEntity {

    private Long userId;
    private Long planId;
    private Long practiceRecordId;
    private String assessmentType;
    private Integer questionCount;
    private BigDecimal predictedPassRate;
    private BigDecimal predictedConfidence;
    private String predictionStage;
    private String weakKnowledgePoints;
    private String recommendationText;
    private String assessmentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
