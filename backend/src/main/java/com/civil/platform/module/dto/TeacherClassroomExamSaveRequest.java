package com.civil.platform.module.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeacherClassroomExamSaveRequest {

    private Long id;

    @NotNull(message = "请选择班级")
    private Long classroomId;

    @NotBlank(message = "考试标题不能为空")
    private String title;

    @NotBlank(message = "考试说明不能为空")
    private String description;

    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长至少为 1 分钟")
    private Integer durationMinutes;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Boolean antiCheatEnabled;

    private String antiCheatNotice;

    private String status;

    @NotEmpty(message = "请至少选择一道题目")
    private List<Long> questionIds;
}
