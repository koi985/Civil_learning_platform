package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommend_refresh_log")
public class RecommendRefreshLog extends BaseEntity {

    private Long userId;
    private String refreshSource;
    private LocalDateTime createdAt;
}
