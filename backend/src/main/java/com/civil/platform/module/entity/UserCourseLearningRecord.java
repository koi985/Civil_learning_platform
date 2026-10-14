package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_course_learning_record")
public class UserCourseLearningRecord extends BaseEntity {

    private Long userId;
    private Long courseId;
    private Long chapterId;
    private Integer watchSeconds;
    private BigDecimal progressPercent;
    private Boolean completed;
    private String actionType;
    private LocalDateTime createdAt;
}
