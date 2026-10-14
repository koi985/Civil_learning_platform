package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_course_study_progress")
public class UserCourseStudyProgress extends BaseEntity {

    private Long userId;
    private Long courseId;
    private Long currentChapterId;
    private Integer completedChapterCount;
    private BigDecimal progressPercent;
    private Integer totalStudySeconds;
    private LocalDateTime lastStudyAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
