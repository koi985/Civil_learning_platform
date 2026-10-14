package com.civil.platform.module.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class TeacherInviteStudentsRequest {

    @NotEmpty(message = "请选择要邀请的学生")
    private List<Long> userIds;
}
