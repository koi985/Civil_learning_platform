package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shenlun_group")
public class ShenlunGroup extends BaseEntity {

    private String title;
    private String subjectName;
    private Integer year;
    private String region;
    private String paperType;
    private String sourceName;
    private String materialMode;
    private Integer totalQuestionCount;
    private Integer totalScore;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
