package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_record")
public class ExamRecord extends BaseEntity {

    private Long examId;
    private Long userId;
    private String title;
    private Integer totalScore;
    private Integer questionCount;
    private Integer correctCount;
    private Integer score;
    private Integer durationSeconds;
    private String status;
    private Boolean rankingEnabled;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
