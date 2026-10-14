package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class BannerSaveRequest {

    private Long id;

    @NotBlank(message = "Banner标题不能为空")
    private String title;

    @NotBlank(message = "Banner图片不能为空")
    private String imageUrl;

    private String linkUrl;

    @NotNull(message = "排序不能为空")
    private Integer sortNo;

    private Boolean enabled;
}
