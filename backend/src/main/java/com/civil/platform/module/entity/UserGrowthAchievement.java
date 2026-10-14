package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_growth_achievement")
public class UserGrowthAchievement extends BaseEntity {

    private Long userId;
    private Long planId;
    private Long taskId;
    private String achievementType;
    private String title;
    private String descriptionText;
    private Integer achievementValue;
    private LocalDateTime createdAt;
}
