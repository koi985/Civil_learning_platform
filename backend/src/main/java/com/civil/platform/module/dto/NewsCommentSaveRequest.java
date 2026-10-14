package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class NewsCommentSaveRequest {

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
