package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_daily_checkin")
public class UserDailyCheckin extends BaseEntity {

    private Long userId;
    private LocalDate checkinDate;
    private String sourceType;
    private Long sourceRecordId;
    private Integer streakDays;
    private LocalDateTime createdAt;
}
