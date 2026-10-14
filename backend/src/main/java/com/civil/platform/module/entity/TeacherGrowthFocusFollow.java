package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teacher_growth_focus_follow")
public class TeacherGrowthFocusFollow extends BaseEntity {

    private Long classroomId;
    private Long userId;
    private Long planId;
    private Long latestDiagnosisId;
    private String followStatus;
    private String priorityLevel;
    private String remark;
    private LocalDateTime nextFollowUpAt;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
