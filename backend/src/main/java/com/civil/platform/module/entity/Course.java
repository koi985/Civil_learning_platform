package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course")
public class Course extends BaseEntity {

    private String title;
    private Long coverFileId;
    private String coverUrl;
    private String teacherName;
    private String teacherAvatar;
    private String regionScope;
    private String province;
    private String subjectName;
    private String examType;
    private String courseType;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String tags;
    private Integer learnCount;
    private String description;
    private String highlights;
    private String targetUsers;
    private Integer validityDays;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
