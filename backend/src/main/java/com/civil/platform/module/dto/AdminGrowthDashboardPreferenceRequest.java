package com.civil.platform.module.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminGrowthDashboardPreferenceRequest {

    private String segment;

    private BigDecimal minCompletionRate;

    private String followStatus;
}
