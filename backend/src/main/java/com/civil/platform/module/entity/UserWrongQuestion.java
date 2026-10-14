package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_wrong_question")
public class UserWrongQuestion extends BaseEntity {

    private Long userId;
    private Long questionId;
    private String latestSourceType;
    private Long latestRecordId;
    private String latestAnswer;
    private Boolean removed;
    private Integer wrongCount;
    private LocalDateTime lastWrongAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
