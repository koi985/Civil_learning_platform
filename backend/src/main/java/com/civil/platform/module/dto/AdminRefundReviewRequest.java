package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class AdminRefundReviewRequest {

    @Size(max = 500, message = "处理备注长度不能超过500个字符")
    private String reviewRemark;
}
