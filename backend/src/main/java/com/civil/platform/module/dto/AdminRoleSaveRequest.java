package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class AdminRoleSaveRequest {

    private Long id;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 32, message = "角色编码长度不能超过32个字符")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过64个字符")
    private String name;

    @Size(max = 255, message = "角色描述长度不能超过255个字符")
    private String descriptionText;

    @NotNull(message = "排序不能为空")
    private Integer sortNo;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
