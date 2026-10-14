package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classroom_exam_task")
public class ClassroomExamTask extends BaseEntity {

    private Long classroomId;
    private Long teacherId;
    private String title;
    private String description;
    private Integer questionCount;
    private Integer totalScore;
    private Integer durationMinutes;
    private Boolean antiCheatEnabled;
    private String antiCheatNotice;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
