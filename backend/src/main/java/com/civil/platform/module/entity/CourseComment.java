package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_comment")
public class CourseComment extends BaseEntity {

    private Long courseId;
    private Long userId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
