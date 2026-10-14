package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CartAddRequest {

    @NotNull(message = "课程不能为空")
    private Long courseId;
}
