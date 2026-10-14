package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_test_paper_question")
public class UserTestPaperQuestion extends BaseEntity {

    private Long testPaperId;
    private Long questionId;
    private Integer sortNo;
    private Integer questionScore;
}
