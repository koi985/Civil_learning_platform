package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class OrderPayRequest {

    @NotBlank(message = "支付方式不能为空")
    private String paymentChannel;
}
