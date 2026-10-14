package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("news_share_log")
public class NewsShareLog extends BaseEntity {

    private Long newsId;
    private Long userId;
    private String shareChannel;
    private LocalDateTime createdAt;
}
