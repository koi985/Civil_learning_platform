package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_growth_dashboard_preference")
public class AdminGrowthDashboardPreference extends BaseEntity {

    private Long userId;
    private String segment;
    private BigDecimal minCompletionRate;
    private String followStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
