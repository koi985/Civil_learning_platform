package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommend_practice_bundle")
public class RecommendPracticeBundle extends BaseEntity {

    private Long userId;
    private String title;
    private String recommendMode;
    private String subjectName;
    private String province;
    private String knowledgePoints;
    private Integer questionCount;
    private Integer estimatedMinutes;
    private String questionIds;
    private String thetaRange;
    private String difficultyRange;
    private BigDecimal abilityFitScore;
    private String explainReasons;
    private String sourceStrategy;
    private String status;
    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
