package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_growth_focus_follow_history")
public class AdminGrowthFocusFollowHistory extends BaseEntity {

    private Long adminFollowId;
    private Long userId;
    private Long planId;
    private Long latestDiagnosisId;
    private String followStatus;
    private String priorityLevel;
    private String remark;
    private LocalDateTime nextFollowUpAt;
    private Long operatorId;
    private String actionType;
    private LocalDateTime createdAt;
}
