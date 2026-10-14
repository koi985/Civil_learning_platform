package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam")
public class Exam extends BaseEntity {

    private String title;
    private String intro;
    private String province;
    private String examType;
    private String seasonName;
    private String subjectName;
    private Integer durationMinutes;
    private Integer totalScore;
    private Integer questionCount;
    private String status;
    private Boolean rankingEnabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<Long> questionIds;
}
