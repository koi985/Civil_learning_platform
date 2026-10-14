package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.common.enums.AuditStatus;
import com.civil.platform.common.enums.RoleCode;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.LoginRequest;
import com.civil.platform.module.dto.RegisterRequest;
import com.civil.platform.module.entity.SysRole;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.entity.SysUserRole;
import com.civil.platform.module.entity.SystemConfig;
import com.civil.platform.module.entity.TeacherProfile;
import com.civil.platform.module.mapper.SysRoleMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.SysUserRoleMapper;
import com.civil.platform.module.mapper.SystemConfigMapper;
import com.civil.platform.module.mapper.TeacherProfileMapper;
import com.civil.platform.security.CustomUserDetailsService;
import com.civil.platform.security.JwtTokenProvider;
import com.civil.platform.security.LoginUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(SysUserMapper sysUserMapper,
                       SysRoleMapper sysRoleMapper,
                       SysUserRoleMapper sysUserRoleMapper,
                       TeacherProfileMapper teacherProfileMapper,
                       SystemConfigMapper systemConfigMapper,
                       PasswordEncoder passwordEncoder,
                       CustomUserDetailsService customUserDetailsService,
                       JwtTokenProvider jwtTokenProvider) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.teacherProfileMapper = teacherProfileMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.passwordEncoder = passwordEncoder;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Map<String, Object> login(LoginRequest request) {
        LoginUser loginUser = customUserDetailsService.loadLoginUserByUsername(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), loginUser.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        TeacherProfile teacherProfile = teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, user.getId()));

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtTokenProvider.createToken(loginUser));
        result.put("user", buildUserInfo(user, teacherProfile, loginUser));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        if (sysUserMapper.selectByUsername(request.getUsername()) != null) {
            throw new BizException("用户名已存在");
        }
        RoleCode registerRole = resolveRegisterRole(request.getRoleCode());
        validateRegisterRequest(request, registerRole);

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRealName(StringUtils.hasText(request.getRealName()) ? request.getRealName() : request.getNickname());
        user.setEmail(request.getEmail());
        user.setProvince(request.getProvince());
        user.setExamType(request.getExamType());
        user.setPreferredSubject(request.getPreferredSubject());
        user.setEnabled(Boolean.TRUE);
        user.setLocked(Boolean.FALSE);
        user.setDeleted(Boolean.FALSE);
        user.setVipExpireAt(LocalDateTime.now().plusMonths(12));
        sysUserMapper.insert(user);

        if (registerRole == RoleCode.ADMIN) {
            bindUserRole(user.getId(), RoleCode.ADMIN);
            return;
        }
        bindUserRole(user.getId(), RoleCode.USER);
        if (registerRole == RoleCode.TEACHER) {
            createPendingTeacherProfile(user.getId(), request);
        }
    }

    private RoleCode resolveRegisterRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return RoleCode.USER;
        }
        try {
            return RoleCode.valueOf(roleCode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("注册角色不合法");
        }
    }

    private void validateRegisterRequest(RegisterRequest request, RoleCode registerRole) {
        if (registerRole == RoleCode.TEACHER) {
            requireText(request.getRealName(), "教师真实姓名不能为空");
            requireText(request.getGender(), "教师性别不能为空");
            requireText(request.getEmail(), "教师邮箱不能为空");
            if (request.getCertificateFileId() == null) {
                throw new BizException("请上传教师资格证");
            }
            requireText(request.getIntro(), "教师简介不能为空");
            return;
        }
        if (registerRole == RoleCode.ADMIN) {
            requireText(request.getAdminRegisterCode(), "管理员注册码不能为空");
            SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getConfigGroup, "PARAM")
                    .eq(SystemConfig::getConfigKey, "admin_register_code")
                    .last("LIMIT 1"));
            if (config == null || !request.getAdminRegisterCode().trim().equals(config.getConfigValue())) {
                throw new BizException("管理员注册码错误");
            }
        }
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(message);
        }
    }

    private void bindUserRole(Long userId, RoleCode roleCode) {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, roleCode.name()).last("LIMIT 1"));
        if (role == null) {
            throw new BizException("角色不存在: " + roleCode.name());
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        sysUserRoleMapper.insert(userRole);
    }

    private void createPendingTeacherProfile(Long userId, RegisterRequest request) {
        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(userId);
        profile.setGender(request.getGender());
        profile.setEmail(request.getEmail());
        profile.setCertificateFileId(request.getCertificateFileId());
        profile.setIntro(request.getIntro());
        profile.setAuditStatus(AuditStatus.PENDING.name());
        teacherProfileMapper.insert(profile);
    }

    public Map<String, Object> currentUser() {
        Long userId = SecurityUtils.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        TeacherProfile teacherProfile = teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, userId));
        LoginUser loginUser = customUserDetailsService.loadLoginUserByUsername(user.getUsername());
        return buildUserInfo(user, teacherProfile, loginUser);
    }

    private Map<String, Object> buildUserInfo(SysUser user, TeacherProfile teacherProfile, LoginUser loginUser) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("realName", user.getRealName());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("province", user.getProvince());
        result.put("examType", user.getExamType());
        result.put("preferredSubject", user.getPreferredSubject());
        result.put("vipExpireAt", user.getVipExpireAt());
        result.put("enabled", user.getEnabled());
        result.put("locked", user.getLocked());
        result.put("roles", loginUser.getRoles());
        result.put("teacherAuditStatus", teacherProfile == null ? null : teacherProfile.getAuditStatus());
        return result;
    }
}
