package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class QuestionPaperSaveRequest {

    private Long id;

    @NotBlank(message = "试卷名称不能为空")
    private String title;

    private String province = "全国";

    private String subjectName = "行测";

    @NotNull(message = "时长不能为空")
    private Integer durationMinutes = 120;

    @NotNull(message = "总分不能为空")
    private Integer totalScore = 120;

    @NotBlank(message = "状态不能为空")
    private String status = "PUBLISHED";
}
