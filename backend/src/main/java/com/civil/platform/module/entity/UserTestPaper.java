package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_test_paper")
public class UserTestPaper extends BaseEntity {

    private Long userId;
    private String title;
    private String subjectName;
    private String categoryName;
    private String difficultyLevel;
    private Integer questionCount;
    private String generateMode;
    private String paperType;
    private String knowledgePoints;
    private String targetKnowledgePoints;
    private String recommendationReason;
    private String generationSnapshot;
    private String difficultyDistribution;
    private String sourceStrategy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
