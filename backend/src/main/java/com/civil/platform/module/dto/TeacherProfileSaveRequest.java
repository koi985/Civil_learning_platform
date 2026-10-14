package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class TeacherProfileSaveRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过64个字符")
    private String nickname;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 64, message = "真实姓名长度不能超过64个字符")
    private String realName;

    @NotBlank(message = "性别不能为空")
    @Size(max = 16, message = "性别长度不能超过16个字符")
    private String gender;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @Size(max = 32, message = "手机号长度不能超过32个字符")
    private String phone;

    @Size(max = 255, message = "头像地址长度不能超过255个字符")
    private String avatarUrl;

    @NotNull(message = "证书文件不能为空")
    private Long certificateFileId;

    @NotBlank(message = "教师简介不能为空")
    @Size(max = 1000, message = "教师简介长度不能超过1000个字符")
    private String intro;
}
