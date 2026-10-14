package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classroom_exam_question")
public class ClassroomExamQuestion extends BaseEntity {

    private Long classroomExamId;
    private Long questionId;
    private Integer score;
    private Integer sortNo;
}
