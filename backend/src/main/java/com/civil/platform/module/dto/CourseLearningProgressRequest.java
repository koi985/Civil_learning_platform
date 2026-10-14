package com.civil.platform.module.dto;

import lombok.Data;

@Data
public class CourseLearningProgressRequest {

    private Long chapterId;
    private Integer watchSeconds;
    private Boolean completed;
}
