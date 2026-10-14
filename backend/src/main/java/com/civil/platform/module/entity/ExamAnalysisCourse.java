package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_analysis_course")
public class ExamAnalysisCourse extends BaseEntity {

    private Long examId;
    private Long courseId;
    private String recommendReason;
    private Integer sortNo;
    private LocalDateTime createdAt;
}
