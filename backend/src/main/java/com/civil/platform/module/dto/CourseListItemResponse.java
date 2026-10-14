package com.civil.platform.module.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CourseListItemResponse {

    private Long id;
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

    private Boolean recommended;
    private String recommendReason;
    private List<String> explainTags;
    private BigDecimal finalScore;
    private String recommendActionText;
}
