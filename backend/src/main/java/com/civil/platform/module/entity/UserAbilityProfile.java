package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_ability_profile")
public class UserAbilityProfile extends BaseEntity {

    private Long userId;
    private BigDecimal overallTheta;
    private BigDecimal preferredSubjectTheta;
    private BigDecimal abilityConfidence;
    private LocalDateTime calibratedAt;
    private String calibrationVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
