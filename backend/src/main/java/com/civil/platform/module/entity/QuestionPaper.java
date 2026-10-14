package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_paper")
public class QuestionPaper extends BaseEntity {

    private String paperNo;
    private String title;
    private String paperType;
    private String examType;
    private String subject;
    private String region;
    private Integer year;
    private String source;
    private String description;
    private Integer duration;
    private Integer totalScore;
    private Integer questionCount;
    private Integer difficulty;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
