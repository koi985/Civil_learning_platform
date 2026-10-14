package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_answer")
public class PracticeAnswer extends BaseEntity {

    private Long practiceRecordId;
    private Long questionId;
    private String userAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private Boolean marked;
    private Integer spentSeconds;
    private Integer questionScore;
    private LocalDateTime createdAt;
}
