package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class OrderCreateRequest {

    private Long courseId;

    private List<Long> cartItemIds;

    @NotNull(message = "支付方式不能为空")
    private String paymentChannel;

    @NotEmpty(message = "订单备注不能为空")
    private String remark;
}
