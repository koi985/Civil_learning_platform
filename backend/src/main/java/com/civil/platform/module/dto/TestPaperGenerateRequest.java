package com.civil.platform.module.dto;

import lombok.Data;

import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class TestPaperGenerateRequest {

    private String subjectName;
    private String categoryName;
    private String difficultyLevel;
    private String knowledgePoint;
    private String generateMode;
    private String paperType;
    private List<String> targetKnowledgePoints;
    private Integer excludeRecentDays;
    private String difficultyStrategy;
    private Boolean diagnosisMode;
    private Boolean onlyWrong;

    @Min(value = 3, message = "测卷题量不能少于 3 题")
    @Max(value = 20, message = "测卷题量不能超过 20 题")
    private Integer questionCount;
}
