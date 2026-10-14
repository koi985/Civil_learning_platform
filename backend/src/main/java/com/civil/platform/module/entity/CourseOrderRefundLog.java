package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_order_refund_log")
public class CourseOrderRefundLog extends BaseEntity {

    private Long refundId;
    private Long orderId;
    private String actionType;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String operatorRole;
    private String operatorName;
    private String remark;
    private LocalDateTime createdAt;
}
