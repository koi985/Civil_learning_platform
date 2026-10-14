package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_news_category_subscription")
public class UserNewsCategorySubscription extends BaseEntity {

    private Long userId;
    private Long categoryId;
    private LocalDateTime createdAt;
}
