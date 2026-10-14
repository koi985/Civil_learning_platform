package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_course_chapter_progress")
public class UserCourseChapterProgress extends BaseEntity {

    private Long userId;
    private Long courseId;
    private Long chapterId;
    private Integer studySeconds;
    private BigDecimal progressPercent;
    private Boolean completed;
    private LocalDateTime firstStartedAt;
    private LocalDateTime lastStudyAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
