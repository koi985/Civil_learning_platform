package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_chapter")
public class CourseChapter extends BaseEntity {

    private Long courseId;
    private String title;
    private String chapterType;
    private Integer durationMinutes;
    private Integer sortNo;
    private LocalDateTime liveStartTime;
    private Long videoFileId;
    private String videoUrl;
    private Boolean trialAvailable;
}
