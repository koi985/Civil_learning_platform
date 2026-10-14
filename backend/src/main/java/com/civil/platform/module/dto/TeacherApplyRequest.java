package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class TeacherApplyRequest {

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotBlank(message = "性别不能为空")
    private String gender;

    @Email(message = "邮箱格式错误")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotNull(message = "请先上传教师资格证")
    private Long certificateFileId;

    @NotBlank(message = "请填写教师简介")
    private String intro;
}
