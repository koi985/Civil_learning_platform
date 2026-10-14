package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classroom_homework_submission")
public class ClassroomHomeworkSubmission extends BaseEntity {

    private Long homeworkId;
    private Long classroomId;
    private Long userId;
    private Integer score;
    private Integer objectiveScore;
    private Integer questionCount;
    private Integer answeredCount;
    private Integer correctCount;
    private Integer durationSeconds;
    private String status;
    private String reviewStatus;
    private String reviewRemark;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
