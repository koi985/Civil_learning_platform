package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class TeacherAuditRequest {

    @NotNull(message = "教师申请记录不能为空")
    private Long teacherProfileId;

    @NotBlank(message = "审核状态不能为空")
    private String auditStatus;

    private String auditRemark;
}
