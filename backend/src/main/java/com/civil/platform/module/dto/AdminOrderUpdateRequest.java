package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AdminOrderUpdateRequest {

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotBlank(message = "订单状态不能为空")
    private String orderStatus;

    @NotBlank(message = "支付状态不能为空")
    private String paymentStatus;
}
