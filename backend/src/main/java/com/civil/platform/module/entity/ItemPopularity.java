package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("item_popularity")
public class ItemPopularity extends BaseEntity {

    private String itemType;
    private Long itemId;
    private Integer userCount;
    private BigDecimal idfWeight;
    private LocalDateTime computedAt;
}
