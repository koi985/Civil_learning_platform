package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class AdminBroadcastSendRequest {

    @Size(max = 128, message = "广播标题长度不能超过128个字符")
    private String title;

    @Size(max = 1000, message = "广播内容长度不能超过1000个字符")
    private String content;

    @Size(max = 32, message = "广播范围长度不能超过32个字符")
    private String scope;
}
