package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class TeacherSubmissionReviewRequest {

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @NotNull(message = "提交记录不能为空")
    private Long submissionId;

    @NotNull(message = "评分不能为空")
    private Integer score;

    @NotBlank(message = "批改状态不能为空")
    private String reviewStatus;

    private String reviewRemark;

    private List<AnnotationItem> annotations;

    @Data
    public static class AnnotationItem {
        @NotNull(message = "题目不能为空")
        private Long questionId;

        private Integer score;

        private String teacherComment;
    }
}
