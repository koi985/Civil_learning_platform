package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("banner")
public class Banner extends BaseEntity {

    private String title;
    private String imageUrl;
    private String linkUrl;
    private Integer sortNo;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
