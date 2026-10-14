package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ExamSubmitRequest {

    @NotNull(message = "答题时长不能为空")
    private Integer durationSeconds;

    @Valid
    @NotEmpty(message = "提交答案不能为空")
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {

        @NotNull(message = "题目不能为空")
        private Long questionId;

        private String userAnswer;
    }
}
