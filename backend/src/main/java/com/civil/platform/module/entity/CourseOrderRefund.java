package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_order_refund")
public class CourseOrderRefund extends BaseEntity {

    private String refundNo;
    private Long orderId;
    private Long userId;
    private BigDecimal refundAmount;
    private String refundReason;
    private String refundStatus;
    private LocalDateTime requestedAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime finishedAt;
    private String reviewRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
