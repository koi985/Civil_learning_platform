package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminGrowthBatchFollowRequest {

    @Valid
    @NotEmpty(message = "至少选择一名用户")
    private List<Item> items;

    @NotBlank(message = "跟进状态不能为空")
    private String followStatus;

    @NotBlank(message = "优先级不能为空")
    private String priorityLevel;

    private String remark;

    private LocalDateTime nextFollowUpAt;

    @Data
    public static class Item {
        @NotNull(message = "用户不能为空")
        private Long userId;

        private Long planId;

        private Long latestDiagnosisId;
    }
}
