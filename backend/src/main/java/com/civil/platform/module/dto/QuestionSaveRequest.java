package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class QuestionSaveRequest {

    private Long id;

    @NotBlank(message = "科目不能为空")
    private String subjectName;

    @NotBlank(message = "分类不能为空")
    private String categoryName;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "题型不能为空")
    private String questionType;

    @NotBlank(message = "难度不能为空")
    private String difficultyLevel;

    @NotBlank(message = "来源不能为空")
    private String sourceName;

    private String material;

    @NotBlank(message = "题干不能为空")
    private String stem;

    @NotBlank(message = "解析不能为空")
    private String analysis;

    @NotBlank(message = "答案不能为空")
    private String answerValue;

    private String knowledgePoints;

    private Boolean enabled;

    private List<OptionItem> options;

    @Data
    public static class OptionItem {
        private String optionKey;
        private String optionContent;
    }
}
