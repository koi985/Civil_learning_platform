package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class RecommendPinnedResourceSaveRequest {

    private Long id;

    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    @NotNull(message = "资源ID不能为空")
    private Long resourceId;

    private String displayTitle;
    private String displayReason;
    private String displayTags;
    private String actionText;
    private String regionScope;
    private String province;
    private Boolean fallbackEnabled;
    private Integer sortNo;
    private Boolean enabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
