package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("news")
public class News extends BaseEntity {

    private Long categoryId;
    private String infoCategory;
    private String province;
    private String title;
    private String summary;
    private Long coverFileId;
    private String coverUrl;
    private String sourceName;
    private String authorName;
    private String content;
    private String tags;
    private Integer readCount;
    private Integer shareCount;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
