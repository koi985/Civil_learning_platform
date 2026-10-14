package com.civil.platform.module.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
public class NoticeSaveRequest {

    private Long id;

    @NotBlank(message = "公告标题不能为空")
    private String title;

    private String summary;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;
}
