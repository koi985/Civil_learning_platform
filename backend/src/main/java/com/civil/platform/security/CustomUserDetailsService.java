package com.civil.platform.security;

import com.civil.platform.common.exception.BizException;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class CustomUserDetailsService {

    private final SysUserMapper sysUserMapper;

    public CustomUserDetailsService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    public LoginUser loadLoginUserByUsername(String username) {
        SysUser sysUser = sysUserMapper.selectByUsername(username);
        if (sysUser == null || Boolean.TRUE.equals(sysUser.getDeleted())) {
            throw new BizException(401, "用户名或密码错误");
        }
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(sysUser.getId());
        if (CollectionUtils.isEmpty(roles)) {
            throw new BizException(401, "账号未绑定角色");
        }
        return new LoginUser(sysUser.getId(), sysUser.getUsername(), sysUser.getPassword(), sysUser.getEnabled(), sysUser.getLocked(), roles);
    }
}
