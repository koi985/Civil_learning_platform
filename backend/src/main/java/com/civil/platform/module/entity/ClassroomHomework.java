package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classroom_homework")
public class ClassroomHomework extends BaseEntity {

    private Long classroomId;
    private Long teacherId;
    private String title;
    private String description;
    private Long attachmentFileId;
    private Integer questionCount;
    private Integer totalScore;
    private String status;
    private LocalDateTime deadlineAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
