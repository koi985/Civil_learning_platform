package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_record")
public class PracticeRecord extends BaseEntity {

    private Long userId;
    private String practiceType;
    private String practiceMode;
    private String title;
    private String subjectName;
    private String province;
    private Integer questionCount;
    private Integer answeredCount;
    private Integer correctCount;
    private Integer score;
    private BigDecimal accuracyRate;
    private Integer durationSeconds;
    private String status;
    private LocalDateTime submittedAt;
    private String sourceSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
