package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_irt_stat")
public class QuestionIrtStat extends BaseEntity {

    private Long questionId;
    private String subjectName;
    private String knowledgePoints;
    private BigDecimal difficultyB;
    private Integer sampleCount;
    private BigDecimal correctRate;
    private LocalDateTime calibratedAt;
    private String calibrationVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
