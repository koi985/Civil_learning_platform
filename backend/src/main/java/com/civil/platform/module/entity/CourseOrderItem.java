package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_order_item")
public class CourseOrderItem extends BaseEntity {

    private Long orderId;
    private Long courseId;
    private String courseTitle;
    private String courseCoverUrl;
    private BigDecimal unitPrice;
    private Integer quantity;
    private LocalDateTime createdAt;
}
