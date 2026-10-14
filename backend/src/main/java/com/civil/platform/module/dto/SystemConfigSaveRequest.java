package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class SystemConfigSaveRequest {

    @NotBlank(message = "配置分组不能为空")
    private String configGroup;

    @NotEmpty(message = "配置项不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        @NotBlank(message = "配置键不能为空")
        private String configKey;

        @Size(max = 1000, message = "配置值长度不能超过1000个字符")
        private String configValue;
    }
}
