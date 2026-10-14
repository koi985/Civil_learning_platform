package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_daily_task")
public class UserDailyTask extends BaseEntity {

    private Long userId;
    private Long planId;
    private LocalDate taskDate;
    private String taskType;
    private String title;
    private String descriptionText;
    private Integer targetValue;
    private Integer completedValue;
    private String status;
    private String actionPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
