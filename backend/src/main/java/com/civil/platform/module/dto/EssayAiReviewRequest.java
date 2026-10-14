package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class EssayAiReviewRequest {

    @NotBlank(message = "请先输入答案")
    @Size(max = 5000, message = "答案内容过长")
    private String answerText;
}
