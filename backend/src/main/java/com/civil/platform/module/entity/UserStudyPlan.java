package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_study_plan")
public class UserStudyPlan extends BaseEntity {

    private Long userId;
    private String planName;
    private String targetExamType;
    private String subjectName;
    private Integer dailyStudyMinutes;
    private Integer dailyQuestionCount;
    private Integer weeklyExamCount;
    private Integer durationDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private String focusKnowledgePoints;
    private String status;
    private LocalDateTime archivedAt;
    private Integer totalTaskCount;
    private Integer completedTaskCount;
    private BigDecimal completionRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
