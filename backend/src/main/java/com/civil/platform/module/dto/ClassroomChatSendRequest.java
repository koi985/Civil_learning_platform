package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class ClassroomChatSendRequest {

    private String messageType;

    @Size(max = 1000, message = "消息内容不能超过1000字")
    private String content;

    private Long fileId;
}
