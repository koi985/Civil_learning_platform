package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String province;

    private String examType;

    private String preferredSubject;

    private String roleCode;

    private String realName;

    private String gender;

    @Email(message = "邮箱格式错误")
    private String email;

    private Long certificateFileId;

    private String intro;

    private String adminRegisterCode;
}
