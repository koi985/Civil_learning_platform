package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class UserSettingSaveRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过64个字符")
    private String nickname;

    @Size(max = 64, message = "真实姓名长度不能超过64个字符")
    private String realName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @Size(max = 32, message = "手机号长度不能超过32个字符")
    private String phone;

    @Size(max = 255, message = "头像地址长度不能超过255个字符")
    private String avatarUrl;

    @Size(max = 32, message = "省份长度不能超过32个字符")
    private String province;

    @Size(max = 64, message = "考试类型长度不能超过64个字符")
    private String examType;

    @Size(max = 64, message = "偏好科目长度不能超过64个字符")
    private String preferredSubject;
}
