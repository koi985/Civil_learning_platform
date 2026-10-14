package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class RecommendFeedbackRequest {

    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    @NotNull(message = "资源ID不能为空")
    private Long resourceId;

    private String sourcePage;

    private String reasonText;
}
