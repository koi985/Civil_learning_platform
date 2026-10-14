package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classroom_homework_question")
public class ClassroomHomeworkQuestion extends BaseEntity {

    private Long homeworkId;
    private Long questionId;
    private Integer score;
    private Integer sortNo;
}
