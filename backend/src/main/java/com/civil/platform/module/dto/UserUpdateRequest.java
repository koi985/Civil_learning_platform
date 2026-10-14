package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserUpdateRequest {

    @NotNull(message = "用户不能为空")
    private Long userId;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotBlank(message = "角色不能为空")
    private String roleCode;

    private List<String> roleCodes;

    private String province;

    private String examType;

    private String preferredSubject;

    private Boolean enabled;

    private Boolean locked;

    private LocalDateTime vipExpireAt;
}
