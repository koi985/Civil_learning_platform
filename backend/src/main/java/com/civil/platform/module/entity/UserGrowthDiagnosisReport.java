package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_growth_diagnosis_report")
public class UserGrowthDiagnosisReport extends BaseEntity {

    private Long userId;
    private Long planId;
    private LocalDate diagnosisDate;
    private String diagnosisStage;
    private BigDecimal vitalityScore;
    private BigDecimal planCompletionRate;
    private BigDecimal knowledgeMasteryRate;
    private BigDecimal testPaperAverageScore;
    private BigDecimal predictedPassRate;
    private BigDecimal predictionConfidence;
    private String formulaVersion;
    private String weakKnowledgePoints;
    private String recommendationText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
