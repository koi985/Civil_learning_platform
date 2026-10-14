package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ExamRegionSaveRequest {

    @NotBlank(message = "报考地区不能为空")
    @Size(max = 32, message = "报考地区长度不能超过32个字符")
    private String province;
}
