package com.civil.platform.module.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private String realName;
    private String avatarUrl;
    private String email;
    private String phone;
    private String province;
    private String examType;
    private String preferredSubject;
    private LocalDateTime vipExpireAt;
    private Boolean enabled;
    private Boolean locked;
    @TableLogic
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
