package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ClassroomJoinReviewRequest {

    @NotNull(message = "申请记录不能为空")
    private Long requestId;

    @NotBlank(message = "审核状态不能为空")
    private String status;
}
