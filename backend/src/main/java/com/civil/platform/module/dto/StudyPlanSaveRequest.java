package com.civil.platform.module.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class StudyPlanSaveRequest {

    @NotBlank(message = "目标考试类型不能为空")
    private String targetExamType;

    @NotBlank(message = "主攻科目不能为空")
    private String subjectName;

    @NotNull(message = "每日学习时长不能为空")
    @Min(value = 10, message = "每日学习时长不能少于 10 分钟")
    @Max(value = 600, message = "每日学习时长不能超过 600 分钟")
    private Integer dailyStudyMinutes;

    @NotNull(message = "每日刷题量不能为空")
    @Min(value = 5, message = "每日刷题量不能少于 5 题")
    @Max(value = 200, message = "每日刷题量不能超过 200 题")
    private Integer dailyQuestionCount;

    @NotNull(message = "每周模考次数不能为空")
    @Min(value = 0, message = "每周模考次数不能小于 0")
    @Max(value = 7, message = "每周模考次数不能超过 7")
    private Integer weeklyExamCount;

    @NotNull(message = "计划周期不能为空")
    @Min(value = 7, message = "计划周期不能少于 7 天")
    @Max(value = 180, message = "计划周期不能超过 180 天")
    private Integer durationDays;

    @NotNull(message = "计划开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    private String focusKnowledgePoints;
}
