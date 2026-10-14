package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shopping_cart_item")
public class ShoppingCartItem extends BaseEntity {

    private Long userId;
    private Long courseId;
    private Boolean selected;
    private LocalDateTime createdAt;
}
