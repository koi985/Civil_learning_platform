package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamSaveRequest {

    private Long id;

    @NotBlank(message = "模考名称不能为空")
    private String title;

    private String intro;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "考试类型不能为空")
    private String examType;

    @NotBlank(message = "模考季数不能为空")
    private String seasonName;

    @NotBlank(message = "科目不能为空")
    private String subjectName;

    @NotNull(message = "时长不能为空")
    private Integer durationMinutes;

    @NotNull(message = "总分不能为空")
    private Integer totalScore;

    @NotBlank(message = "状态不能为空")
    private String status;

    private Boolean rankingEnabled;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private List<Long> questionIds;
}
