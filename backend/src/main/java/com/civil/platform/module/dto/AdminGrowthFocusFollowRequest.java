package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class AdminGrowthFocusFollowRequest {

    @NotNull(message = "用户不能为空")
    private Long userId;

    private Long planId;

    private Long latestDiagnosisId;

    @NotBlank(message = "跟进状态不能为空")
    private String followStatus;

    @NotBlank(message = "优先级不能为空")
    private String priorityLevel;

    private String remark;

    private LocalDateTime nextFollowUpAt;
}
