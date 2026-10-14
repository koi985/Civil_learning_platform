package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teacher_growth_dashboard_preference")
public class TeacherGrowthDashboardPreference extends BaseEntity {

    private Long userId;
    private Long classroomId;
    private String segment;
    private String followStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
