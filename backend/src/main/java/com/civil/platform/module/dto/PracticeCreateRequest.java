package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class PracticeCreateRequest {

    private Long groupId;
    private String keyword;
    private String province;
    private String subjectName;
    private String categoryName;
    private String questionType;
    private String difficultyLevel;
    private String sourceName;
    private String mode;

    @Min(value = 1, message = "题量不能少于 1")
    @Max(value = 50, message = "题量不能超过 50")
    private Integer questionCount;
}
