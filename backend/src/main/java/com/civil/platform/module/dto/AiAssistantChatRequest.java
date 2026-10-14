package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AiAssistantChatRequest {

    @NotBlank(message = "请输入咨询内容")
    @Size(max = 1000, message = "咨询内容不能超过1000个字符")
    private String message;

    @Size(max = 128, message = "会话标识长度不能超过128个字符")
    private String conversationId;
}
