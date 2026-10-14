package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ClassroomSaveRequest {

    private Long id;

    @NotBlank(message = "班级名称不能为空")
    private String name;

    @NotBlank(message = "科目不能为空")
    private String subjectName;

    private Long coverFileId;

    private String coverUrl;

    @NotBlank(message = "班级简介不能为空")
    private String intro;

    @NotNull(message = "容量不能为空")
    @Min(value = 1, message = "容量至少为 1")
    private Integer capacity;

    @NotNull(message = "请指定是否公开")
    private Boolean isPublic;

    private String inviteCode;

    private String status;
}
