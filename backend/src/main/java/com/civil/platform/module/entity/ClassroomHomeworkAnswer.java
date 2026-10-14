package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classroom_homework_answer")
public class ClassroomHomeworkAnswer extends BaseEntity {

    private Long submissionId;
    private Long homeworkId;
    private Long questionId;
    private String userAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private Integer score;
    private String teacherComment;
}
