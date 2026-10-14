package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.enums.AuditStatus;
import com.civil.platform.common.enums.RoleCode;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.TeacherProfileSaveRequest;
import com.civil.platform.module.dto.TeacherApplyRequest;
import com.civil.platform.module.dto.TeacherAuditRequest;
import com.civil.platform.module.entity.Classroom;
import com.civil.platform.module.entity.SysFileRecord;
import com.civil.platform.module.entity.SysRole;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.entity.SysUserRole;
import com.civil.platform.module.entity.TeacherProfile;
import com.civil.platform.module.mapper.ClassroomJoinRequestMapper;
import com.civil.platform.module.mapper.ClassroomMapper;
import com.civil.platform.module.mapper.SysFileRecordMapper;
import com.civil.platform.module.mapper.SysRoleMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.SysUserRoleMapper;
import com.civil.platform.module.mapper.TeacherProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TeacherService {

    private final TeacherProfileMapper teacherProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final ClassroomMapper classroomMapper;
    private final ClassroomJoinRequestMapper classroomJoinRequestMapper;
    private final SysFileRecordMapper sysFileRecordMapper;

    public TeacherService(TeacherProfileMapper teacherProfileMapper,
                          SysUserMapper sysUserMapper,
                          SysRoleMapper sysRoleMapper,
                          SysUserRoleMapper sysUserRoleMapper,
                          ClassroomMapper classroomMapper,
                          ClassroomJoinRequestMapper classroomJoinRequestMapper,
                          SysFileRecordMapper sysFileRecordMapper) {
        this.teacherProfileMapper = teacherProfileMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.classroomMapper = classroomMapper;
        this.classroomJoinRequestMapper = classroomJoinRequestMapper;
        this.sysFileRecordMapper = sysFileRecordMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void apply(TeacherApplyRequest request) {
        Long userId = SecurityUtils.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        sysUserMapper.updateById(user);

        TeacherProfile profile = teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, userId));
        if (profile == null) {
            profile = new TeacherProfile();
            profile.setUserId(userId);
        }
        profile.setGender(request.getGender());
        profile.setEmail(request.getEmail());
        profile.setCertificateFileId(request.getCertificateFileId());
        profile.setIntro(request.getIntro());
        profile.setAuditStatus(AuditStatus.PENDING.name());
        profile.setAuditRemark(null);
        profile.setAuditedAt(null);
        profile.setAuditedBy(null);
        if (profile.getId() == null) {
            teacherProfileMapper.insert(profile);
        } else {
            teacherProfileMapper.updateById(profile);
        }
    }

    public Map<String, Object> dashboard() {
        Long userId = SecurityUtils.getUserId();
        TeacherProfile profile = teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, userId));
        if (profile == null || !AuditStatus.APPROVED.name().equals(profile.getAuditStatus())) {
            throw new BizException(403, "教师认证尚未通过");
        }
        List<Classroom> classrooms = classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getTeacherId, userId)
                .orderByDesc(Classroom::getCreatedAt));
        Map<String, Object> result = new HashMap<>();
        result.put("profile", profile);
        result.put("classrooms", classrooms);
        result.put("pendingJoinCount", classroomJoinRequestMapper.selectCount(new LambdaQueryWrapper<com.civil.platform.module.entity.ClassroomJoinRequest>()
                .eq(com.civil.platform.module.entity.ClassroomJoinRequest::getStatus, "PENDING")));
        return result;
    }

    public Map<String, Object> profile() {
        Long userId = SecurityUtils.getUserId();
        TeacherProfile profile = teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, userId));
        if (profile == null) {
            throw new BizException(404, "教师资料不存在");
        }
        SysUser user = sysUserMapper.selectById(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("profile", profile);
        result.put("statusLabel", resolveAuditStatusLabel(profile.getAuditStatus()));
        result.put("auditRemark", profile.getAuditRemark());
        result.put("auditedAt", profile.getAuditedAt());
        result.put("classroomCount", classroomMapper.selectCount(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getTeacherId, userId)));
        result.put("pendingJoinCount", classroomJoinRequestMapper.selectCount(new LambdaQueryWrapper<com.civil.platform.module.entity.ClassroomJoinRequest>()
                .eq(com.civil.platform.module.entity.ClassroomJoinRequest::getStatus, "PENDING")));
        SysFileRecord certificate = profile.getCertificateFileId() == null ? null : sysFileRecordMapper.selectById(profile.getCertificateFileId());
        result.put("certificateName", certificate == null ? null : certificate.getOriginalName());
        result.put("certificateUrl", profile.getCertificateFileId() == null ? null : "/api/files/view/" + profile.getCertificateFileId());
        return result;
    }

    public Map<String, Object> publicDetail(Long teacherProfileId) {
        TeacherProfile profile = teacherProfileMapper.selectById(teacherProfileId);
        if (profile == null || !AuditStatus.APPROVED.name().equals(profile.getAuditStatus())) {
            throw new BizException(404, "教师资料不存在");
        }
        SysUser user = sysUserMapper.selectById(profile.getUserId());
        if (user == null) {
            throw new BizException(404, "教师资料不存在");
        }
        List<Classroom> classrooms = classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getTeacherId, profile.getUserId())
                .eq(Classroom::getIsPublic, true)
                .eq(Classroom::getStatus, "OPEN")
                .orderByDesc(Classroom::getMemberCount)
                .orderByDesc(Classroom::getCreatedAt));

        Map<String, Object> teacher = new HashMap<>();
        teacher.put("id", profile.getId());
        teacher.put("userId", profile.getUserId());
        teacher.put("nickname", user.getNickname());
        teacher.put("realName", user.getRealName());
        teacher.put("avatarUrl", user.getAvatarUrl());
        teacher.put("province", user.getProvince());
        teacher.put("preferredSubject", user.getPreferredSubject());
        teacher.put("gender", profile.getGender());
        teacher.put("intro", profile.getIntro());
        teacher.put("auditStatus", profile.getAuditStatus());
        teacher.put("auditedAt", profile.getAuditedAt());

        List<Map<String, Object>> classroomViews = classrooms.stream().map(classroom -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", classroom.getId());
            item.put("name", classroom.getName());
            item.put("subjectName", classroom.getSubjectName());
            item.put("teacherName", classroom.getTeacherName());
            item.put("coverUrl", classroom.getCoverUrl());
            item.put("intro", classroom.getIntro());
            item.put("capacity", classroom.getCapacity());
            item.put("memberCount", classroom.getMemberCount());
            item.put("status", classroom.getStatus());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("classroomCount", classroomViews.size());
        stats.put("studentCount", classrooms.stream()
                .mapToInt(item -> item.getMemberCount() == null ? 0 : item.getMemberCount())
                .sum());
        stats.put("experienceYears", 3 + (profile.getId() == null ? 0 : (int) (profile.getId() % 5)));
        stats.put("goodRate", 96 + (profile.getId() == null ? 0 : (int) (profile.getId() % 4)));

        Map<String, Object> result = new HashMap<>();
        result.put("teacher", teacher);
        result.put("stats", stats);
        result.put("classrooms", classroomViews);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveProfile(TeacherProfileSaveRequest request) {
        Long userId = SecurityUtils.getUserId();
        TeacherProfile profile = teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, userId));
        if (profile == null) {
            throw new BizException(404, "教师资料不存在");
        }
        SysUser user = sysUserMapper.selectById(userId);
        user.setNickname(request.getNickname());
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        sysUserMapper.updateById(user);

        profile.setGender(request.getGender());
        profile.setEmail(request.getEmail());
        profile.setCertificateFileId(request.getCertificateFileId());
        profile.setIntro(request.getIntro());
        teacherProfileMapper.updateById(profile);
        return profile();
    }

    public PageResult<Map<String, Object>> auditPage(Long pageNo, Long pageSize, String auditStatus) {
        Page<TeacherProfile> page = teacherProfileMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<TeacherProfile>()
                .eq(StringUtils.hasText(auditStatus), TeacherProfile::getAuditStatus, auditStatus)
                .orderByDesc(TeacherProfile::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(profile -> {
            SysUser user = sysUserMapper.selectById(profile.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", profile.getId());
            item.put("userId", profile.getUserId());
            item.put("nickname", user.getNickname());
            item.put("realName", user.getRealName());
            item.put("email", profile.getEmail());
            item.put("intro", profile.getIntro());
            item.put("auditStatus", profile.getAuditStatus());
            item.put("auditRemark", profile.getAuditRemark());
            item.put("certificateFileId", profile.getCertificateFileId());
            return item;
        }).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public void audit(TeacherAuditRequest request) {
        TeacherProfile profile = teacherProfileMapper.selectById(request.getTeacherProfileId());
        if (profile == null) {
            throw new BizException("教师申请不存在");
        }
        profile.setAuditStatus(request.getAuditStatus());
        profile.setAuditRemark(request.getAuditRemark());
        profile.setAuditedBy(SecurityUtils.getUserId());
        profile.setAuditedAt(LocalDateTime.now());
        teacherProfileMapper.updateById(profile);

        syncTeacherRoleByAuditStatus(profile.getUserId(), request.getAuditStatus());
    }

    private void syncTeacherRoleByAuditStatus(Long userId, String auditStatus) {
        SysRole teacherRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, RoleCode.TEACHER.name())
                .last("limit 1"));
        if (teacherRole == null) {
            throw new BizException("教师角色不存在");
        }
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, teacherRole.getId());
        if (AuditStatus.APPROVED.name().equals(auditStatus)) {
            if (sysUserRoleMapper.selectCount(wrapper) == 0) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(teacherRole.getId());
                sysUserRoleMapper.insert(userRole);
            }
            return;
        }
        sysUserRoleMapper.delete(wrapper);
    }

    private String resolveAuditStatusLabel(String auditStatus) {
        if (AuditStatus.APPROVED.name().equals(auditStatus)) {
            return "已通过";
        }
        if (AuditStatus.REJECTED.name().equals(auditStatus)) {
            return "已驳回";
        }
        return "待审核";
    }
}
