package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_broadcast_message")
public class SystemBroadcastMessage extends BaseEntity {

    private String title;
    private String content;
    private String scope;
    private Long senderId;
    private String senderName;
    private Integer receiverCount;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
