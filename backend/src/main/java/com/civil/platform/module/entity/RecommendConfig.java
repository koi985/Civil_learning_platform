package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommend_config")
public class RecommendConfig extends BaseEntity {

    private String configKey;
    private String configName;
    private BigDecimal configValue;
    private String descriptionText;
    private LocalDateTime updatedAt;
}
