package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommend_pinned_resource")
public class RecommendPinnedResource extends BaseEntity {

    private String resourceType;
    private Long resourceId;
    private String displayTitle;
    private String displayReason;
    private String displayTags;
    private String actionText;
    private String regionScope;
    private String province;
    private Boolean fallbackEnabled;
    private Integer sortNo;
    private Boolean enabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
