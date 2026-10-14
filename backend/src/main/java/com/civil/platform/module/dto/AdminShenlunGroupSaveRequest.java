package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class AdminShenlunGroupSaveRequest {

    private Long id;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotNull(message = "年份不能为空")
    private Integer year;

    @NotBlank(message = "地区不能为空")
    private String region;

    @NotBlank(message = "卷型不能为空")
    private String paperType;

    @NotBlank(message = "来源不能为空")
    private String sourceName;

    @NotBlank(message = "材料模式不能为空")
    private String materialMode;

    @NotBlank(message = "状态不能为空")
    private String status;

    @Valid
    @NotEmpty(message = "至少添加一份材料")
    private List<MaterialItem> materials;

    @Valid
    @NotEmpty(message = "至少添加一道题目")
    private List<QuestionItem> questions;

    @Data
    public static class MaterialItem {
        private Long id;
        private Integer sortNo;
        @NotBlank(message = "材料标签不能为空")
        private String materialLabel;
        private String title;
        @NotBlank(message = "材料内容不能为空")
        private String content;
    }

    @Data
    public static class QuestionItem {
        private Long id;
        private Integer sortNo;
        @NotBlank(message = "分类不能为空")
        private String categoryName;
        @NotBlank(message = "题型不能为空")
        private String questionType;
        @NotBlank(message = "难度不能为空")
        private String difficultyLevel;
        @NotNull(message = "分值不能为空")
        private Integer score;
        @NotBlank(message = "题干不能为空")
        private String stem;
        @NotBlank(message = "解析不能为空")
        private String analysis;
        @NotBlank(message = "答案不能为空")
        private String answerValue;
        private String knowledgePoints;
        @NotEmpty(message = "每题至少关联一份材料")
        private List<String> materialRefs;
    }
}
