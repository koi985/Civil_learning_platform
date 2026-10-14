package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_user_similarity")
public class UserUserSimilarity extends BaseEntity {

    private Long userId;
    private Long similarUserId;
    private BigDecimal similarity;
    private Integer rankNo;
    private LocalDateTime computedAt;
}
