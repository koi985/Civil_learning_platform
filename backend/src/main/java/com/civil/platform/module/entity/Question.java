package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question")
public class Question extends BaseEntity {

    private Long groupId;
    private Integer groupQuestionNo;
    private Boolean isGroupSharedMaterial;
    private String groupMaterialRefs;
    private String subjectName;
    private String categoryName;
    private String province;
    private String questionType;
    private String difficultyLevel;
    private Integer score;
    private String sourceName;
    private String material;
    private String stem;
    private String analysis;
    private String answerValue;
    private String knowledgePoints;
    private String videoExplainTitle;
    private String videoExplainUrl;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
