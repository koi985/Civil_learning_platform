package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classroom_chat_message")
public class ClassroomChatMessage extends BaseEntity {

    private Long classroomId;
    private Long senderId;
    private String senderRole;
    private String senderName;
    private String messageType;
    private String content;
    private Long fileId;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private LocalDateTime createdAt;
}
