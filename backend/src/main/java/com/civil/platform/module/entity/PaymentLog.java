package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_log")
public class PaymentLog extends BaseEntity {

    private Long orderId;
    private Long userId;
    private String paymentChannel;
    private BigDecimal paymentAmount;
    private String paymentStatus;
    private String transactionNo;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
