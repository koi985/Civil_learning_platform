package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_paper_question")
public class QuestionPaperQuestion extends BaseEntity {

    private Long paperId;
    private Long questionId;
    private String sectionName;
    private Integer score;
    private Integer sortNo;
}
