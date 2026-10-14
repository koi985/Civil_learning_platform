package com.civil.platform.module.dto;

import lombok.Data;

@Data
public class TeacherGrowthDashboardPreferenceRequest {

    private Long classroomId;

    private String segment;

    private String followStatus;
}
