package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_item_interaction")
public class UserItemInteraction extends BaseEntity {

    private Long userId;
    private String itemType;
    private Long itemId;
    private BigDecimal weight;
    private LocalDateTime lastActedAt;
}
