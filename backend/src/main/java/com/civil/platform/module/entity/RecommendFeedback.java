package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommend_feedback")
public class RecommendFeedback extends BaseEntity {

    private Long userId;
    private String resourceType;
    private Long resourceId;
    private String feedbackType;
    private String sourcePage;
    private String reasonText;
    private LocalDateTime createdAt;
}
