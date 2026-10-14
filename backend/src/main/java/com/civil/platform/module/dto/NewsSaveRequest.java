package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class NewsSaveRequest {

    private Long id;

    @NotNull(message = "资讯分类不能为空")
    private Long categoryId;

    private String infoCategory;
    private String province;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String summary;
    private Long coverFileId;
    private String coverUrl;
    private String sourceName;
    private String authorName;

    @NotBlank(message = "正文不能为空")
    private String content;

    private String tags;

    private String status;

    private LocalDateTime publishedAt;
}
