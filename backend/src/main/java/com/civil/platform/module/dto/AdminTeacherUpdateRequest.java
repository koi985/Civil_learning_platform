package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class AdminTeacherUpdateRequest {

    @NotNull(message = "教师档案不能为空")
    private Long teacherProfileId;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过64个字符")
    private String nickname;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 64, message = "真实姓名长度不能超过64个字符")
    private String realName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @Size(max = 32, message = "手机号长度不能超过32个字符")
    private String phone;

    @Size(max = 1000, message = "简介长度不能超过1000个字符")
    private String intro;

    @Size(max = 32, message = "审核状态长度不能超过32个字符")
    private String auditStatus;

    @Size(max = 255, message = "审核备注长度不能超过255个字符")
    private String auditRemark;

    @NotNull(message = "账号状态不能为空")
    private Boolean enabled;

    @NotNull(message = "锁定状态不能为空")
    private Boolean locked;
}
