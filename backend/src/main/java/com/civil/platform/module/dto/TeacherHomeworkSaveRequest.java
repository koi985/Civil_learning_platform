package com.civil.platform.module.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeacherHomeworkSaveRequest {

    private Long id;

    @NotNull(message = "请选择班级")
    private Long classroomId;

    @NotBlank(message = "作业标题不能为空")
    private String title;

    @NotBlank(message = "作业说明不能为空")
    private String description;

    private Long attachmentFileId;

    @NotNull(message = "截止时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadlineAt;

    private String status;

    @NotEmpty(message = "请至少选择一道题目")
    private List<Long> questionIds;
}
