package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CourseCommentSaveRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最少为 1 分")
    @Max(value = 5, message = "评分最高为 5 分")
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    @Size(max = 300, message = "评价内容不能超过 300 字")
    private String content;
}
