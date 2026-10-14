package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class TeacherGrowthFocusFollowRequest {

    @NotNull(message = "班级不能为空")
    private Long classroomId;

    @NotNull(message = "学员不能为空")
    private Long userId;

    @NotNull(message = "学习计划不能为空")
    private Long planId;

    private Long latestDiagnosisId;

    @NotBlank(message = "跟进状态不能为空")
    private String followStatus;

    @NotBlank(message = "优先级不能为空")
    private String priorityLevel;

    private String remark;

    private LocalDateTime nextFollowUpAt;
}
