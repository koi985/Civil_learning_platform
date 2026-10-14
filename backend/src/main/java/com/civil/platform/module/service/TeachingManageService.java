package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.enums.AuditStatus;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.*;
import com.civil.platform.module.entity.*;
import com.civil.platform.module.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeachingManageService {

    private static final Random INVITE_CODE_RANDOM = new Random();

    private final TeacherProfileMapper teacherProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final ClassroomMapper classroomMapper;
    private final ClassroomMemberMapper classroomMemberMapper;
    private final ClassroomJoinRequestMapper classroomJoinRequestMapper;
    private final ClassroomMaterialMapper classroomMaterialMapper;
    private final ClassroomHomeworkMapper classroomHomeworkMapper;
    private final ClassroomHomeworkQuestionMapper classroomHomeworkQuestionMapper;
    private final ClassroomHomeworkSubmissionMapper classroomHomeworkSubmissionMapper;
    private final ClassroomHomeworkAnswerMapper classroomHomeworkAnswerMapper;
    private final ClassroomExamTaskMapper classroomExamTaskMapper;
    private final ClassroomExamQuestionMapper classroomExamQuestionMapper;
    private final ClassroomExamSubmissionMapper classroomExamSubmissionMapper;
    private final ClassroomExamAnswerMapper classroomExamAnswerMapper;
    private final QuestionMapper questionMapper;
    private final SysFileRecordMapper sysFileRecordMapper;
    private final UserStudyPlanMapper userStudyPlanMapper;
    private final UserGrowthDiagnosisReportMapper userGrowthDiagnosisReportMapper;
    private final TeacherGrowthFocusFollowMapper teacherGrowthFocusFollowMapper;
    private final AdminGrowthFocusFollowMapper adminGrowthFocusFollowMapper;
    private final TeacherGrowthFocusFollowHistoryMapper teacherGrowthFocusFollowHistoryMapper;
    private final TeacherGrowthDashboardPreferenceMapper teacherGrowthDashboardPreferenceMapper;

    public TeachingManageService(TeacherProfileMapper teacherProfileMapper,
                                 SysUserMapper sysUserMapper,
                                 SysRoleMapper sysRoleMapper,
                                 SysUserRoleMapper sysUserRoleMapper,
                                 ClassroomMapper classroomMapper,
                                 ClassroomMemberMapper classroomMemberMapper,
                                 ClassroomJoinRequestMapper classroomJoinRequestMapper,
                                 ClassroomMaterialMapper classroomMaterialMapper,
                                 ClassroomHomeworkMapper classroomHomeworkMapper,
                                 ClassroomHomeworkQuestionMapper classroomHomeworkQuestionMapper,
                                 ClassroomHomeworkSubmissionMapper classroomHomeworkSubmissionMapper,
                                 ClassroomHomeworkAnswerMapper classroomHomeworkAnswerMapper,
                                 ClassroomExamTaskMapper classroomExamTaskMapper,
                                 ClassroomExamQuestionMapper classroomExamQuestionMapper,
                                 ClassroomExamSubmissionMapper classroomExamSubmissionMapper,
                                 ClassroomExamAnswerMapper classroomExamAnswerMapper,
                                 QuestionMapper questionMapper,
                                 SysFileRecordMapper sysFileRecordMapper,
                                 UserStudyPlanMapper userStudyPlanMapper,
                                 UserGrowthDiagnosisReportMapper userGrowthDiagnosisReportMapper,
                                 TeacherGrowthFocusFollowMapper teacherGrowthFocusFollowMapper,
                                 AdminGrowthFocusFollowMapper adminGrowthFocusFollowMapper,
                                 TeacherGrowthFocusFollowHistoryMapper teacherGrowthFocusFollowHistoryMapper,
                                 TeacherGrowthDashboardPreferenceMapper teacherGrowthDashboardPreferenceMapper) {
        this.teacherProfileMapper = teacherProfileMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.classroomMapper = classroomMapper;
        this.classroomMemberMapper = classroomMemberMapper;
        this.classroomJoinRequestMapper = classroomJoinRequestMapper;
        this.classroomMaterialMapper = classroomMaterialMapper;
        this.classroomHomeworkMapper = classroomHomeworkMapper;
        this.classroomHomeworkQuestionMapper = classroomHomeworkQuestionMapper;
        this.classroomHomeworkSubmissionMapper = classroomHomeworkSubmissionMapper;
        this.classroomHomeworkAnswerMapper = classroomHomeworkAnswerMapper;
        this.classroomExamTaskMapper = classroomExamTaskMapper;
        this.classroomExamQuestionMapper = classroomExamQuestionMapper;
        this.classroomExamSubmissionMapper = classroomExamSubmissionMapper;
        this.classroomExamAnswerMapper = classroomExamAnswerMapper;
        this.questionMapper = questionMapper;
        this.sysFileRecordMapper = sysFileRecordMapper;
        this.userStudyPlanMapper = userStudyPlanMapper;
        this.userGrowthDiagnosisReportMapper = userGrowthDiagnosisReportMapper;
        this.teacherGrowthFocusFollowMapper = teacherGrowthFocusFollowMapper;
        this.adminGrowthFocusFollowMapper = adminGrowthFocusFollowMapper;
        this.teacherGrowthFocusFollowHistoryMapper = teacherGrowthFocusFollowHistoryMapper;
        this.teacherGrowthDashboardPreferenceMapper = teacherGrowthDashboardPreferenceMapper;
    }

    public Map<String, Object> dashboard() {
        Long teacherId = SecurityUtils.getUserId();
        TeacherProfile profile = getApprovedProfile(teacherId);
        List<Classroom> classrooms = classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getTeacherId, teacherId)
                .ne(Classroom::getStatus, "DISMISSED")
                .orderByDesc(Classroom::getCreatedAt));
        List<Long> classroomIds = classrooms.stream().map(Classroom::getId).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("profile", profile);
        result.put("classrooms", classrooms);
        result.put("pendingJoinCount", classroomIds.isEmpty() ? 0 : classroomJoinRequestMapper.selectCount(new LambdaQueryWrapper<ClassroomJoinRequest>()
                .in(ClassroomJoinRequest::getClassroomId, classroomIds)
                .eq(ClassroomJoinRequest::getStatus, "PENDING")));
        result.put("homeworkCount", classroomIds.isEmpty() ? 0 : classroomHomeworkMapper.selectCount(new LambdaQueryWrapper<ClassroomHomework>()
                .in(ClassroomHomework::getClassroomId, classroomIds)));
        result.put("examCount", classroomIds.isEmpty() ? 0 : classroomExamTaskMapper.selectCount(new LambdaQueryWrapper<ClassroomExamTask>()
                .in(ClassroomExamTask::getClassroomId, classroomIds)));
        result.put("pendingReviewCount", countPendingReviews(classroomIds));
        return result;
    }

    public PageResult<Map<String, Object>> studentRecommendations(Long classroomId, Long pageNo, Long pageSize) {
        Long teacherId = SecurityUtils.getUserId();
        getApprovedProfile(teacherId);
        List<Classroom> classrooms = loadRecommendationClassrooms(teacherId, classroomId);
        if (CollectionUtils.isEmpty(classrooms)) {
            return new PageResult<>(0L, pageNo, pageSize, Collections.emptyList());
        }
        List<Long> classroomIds = classrooms.stream().map(Classroom::getId).collect(Collectors.toList());
        Map<Long, Set<Long>> memberUserIdsByClassroom = loadMemberUserIdsByClassroom(classroomIds);
        Map<Long, Set<Long>> openRequestUserIdsByClassroom = loadOpenRequestUserIdsByClassroom(classroomIds);
        Map<Long, Long> studentCountByClassroom = loadStudentCountByClassroom(classroomIds);
        Set<Long> studentRoleUserIds = loadStudentRoleUserIds();
        if (CollectionUtils.isEmpty(studentRoleUserIds)) {
            return new PageResult<>(0L, pageNo, pageSize, Collections.emptyList());
        }
        SysUser teacher = sysUserMapper.selectById(teacherId);
        List<SysUser> students = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, studentRoleUserIds)
                .eq(SysUser::getEnabled, true)
                .eq(SysUser::getLocked, false)
                .eq(SysUser::getDeleted, false));
        List<Map<String, Object>> recommendations = students.stream()
                .map(student -> buildBestStudentRecommendation(student, classrooms, memberUserIdsByClassroom,
                        openRequestUserIdsByClassroom, studentCountByClassroom, teacher))
                .filter(Objects::nonNull)
                .sorted((left, right) -> {
                    int scoreCompare = Integer.compare((Integer) right.get("matchScore"), (Integer) left.get("matchScore"));
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    return Long.compare((Long) left.get("userId"), (Long) right.get("userId"));
                })
                .collect(Collectors.toList());
        long safePageNo = pageNo == null || pageNo < 1 ? 1L : pageNo;
        long safePageSize = pageSize == null || pageSize < 1 ? 10L : pageSize;
        int fromIndex = Math.max(0, (int) ((safePageNo - 1) * safePageSize));
        int toIndex = Math.min(recommendations.size(), fromIndex + (int) safePageSize);
        List<Map<String, Object>> records = fromIndex >= recommendations.size()
                ? Collections.emptyList()
                : recommendations.subList(fromIndex, toIndex);
        return new PageResult<>((long) recommendations.size(), safePageNo, safePageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> inviteStudents(Long classroomId, TeacherInviteStudentsRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        getApprovedProfile(teacherId);
        Classroom classroom = getOwnedClassroom(classroomId, teacherId);
        if ("DISMISSED".equals(classroom.getStatus())) {
            throw new BizException("班级已解散，不能邀请学生");
        }
        List<Long> requestedUserIds = request.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(requestedUserIds)) {
            throw new BizException("请选择要邀请的学生");
        }
        Set<Long> studentRoleUserIds = loadStudentRoleUserIds();
        if (!studentRoleUserIds.containsAll(requestedUserIds)) {
            throw new BizException("存在不可邀请的学生账号");
        }
        List<SysUser> validStudents = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, requestedUserIds)
                .eq(SysUser::getEnabled, true)
                .eq(SysUser::getLocked, false)
                .eq(SysUser::getDeleted, false));
        Set<Long> validStudentIds = validStudents.stream().map(SysUser::getId).collect(Collectors.toSet());
        if (!validStudentIds.containsAll(requestedUserIds)) {
            throw new BizException("存在不可邀请的学生账号");
        }
        Set<Long> existingMemberIds = classroomMemberMapper.selectList(new LambdaQueryWrapper<ClassroomMember>()
                        .eq(ClassroomMember::getClassroomId, classroomId)
                        .in(ClassroomMember::getUserId, requestedUserIds))
                .stream()
                .map(ClassroomMember::getUserId)
                .collect(Collectors.toSet());
        Set<Long> existingOpenRequestIds = classroomJoinRequestMapper.selectList(new LambdaQueryWrapper<ClassroomJoinRequest>()
                        .eq(ClassroomJoinRequest::getClassroomId, classroomId)
                        .in(ClassroomJoinRequest::getUserId, requestedUserIds)
                        .in(ClassroomJoinRequest::getStatus, Arrays.asList("PENDING", "INVITED")))
                .stream()
                .map(ClassroomJoinRequest::getUserId)
                .collect(Collectors.toSet());
        List<Long> inviteUserIds = requestedUserIds.stream()
                .filter(userId -> !existingMemberIds.contains(userId))
                .filter(userId -> !existingOpenRequestIds.contains(userId))
                .collect(Collectors.toList());
        long currentStudentCount = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .eq(ClassroomMember::getRoleCode, "STUDENT"));
        int capacity = classroom.getCapacity() == null ? 0 : classroom.getCapacity();
        if (!CollectionUtils.isEmpty(inviteUserIds) && currentStudentCount + inviteUserIds.size() > capacity) {
            throw new BizException("班级容量已满或剩余名额不足");
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long userId : inviteUserIds) {
            ClassroomJoinRequest invitation = classroomJoinRequestMapper.selectOne(new LambdaQueryWrapper<ClassroomJoinRequest>()
                    .eq(ClassroomJoinRequest::getClassroomId, classroomId)
                    .eq(ClassroomJoinRequest::getUserId, userId)
                    .orderByDesc(ClassroomJoinRequest::getId)
                    .last("limit 1"));
            if (invitation == null) {
                invitation = new ClassroomJoinRequest();
                invitation.setClassroomId(classroomId);
                invitation.setUserId(userId);
                invitation.setCreatedAt(now);
            }
            invitation.setStatus("INVITED");
            invitation.setRemark("教师邀请加入班级");
            invitation.setReviewedBy(teacherId);
            invitation.setReviewedAt(now);
            if (invitation.getId() == null) {
                classroomJoinRequestMapper.insert(invitation);
            } else {
                classroomJoinRequestMapper.updateById(invitation);
            }
        }
        long memberCount = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId));
        classroom.setMemberCount((int) memberCount);
        classroomMapper.updateById(classroom);
        Map<String, Object> result = new HashMap<>();
        result.put("invitedCount", inviteUserIds.size());
        result.put("skippedCount", requestedUserIds.size() - inviteUserIds.size());
        result.put("memberCount", memberCount);
        return result;
    }

    public PageResult<Map<String, Object>> teacherClassrooms(Long pageNo, Long pageSize, String keyword) {
        Long teacherId = SecurityUtils.getUserId();
        getApprovedProfile(teacherId);
        Page<Classroom> page = classroomMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getTeacherId, teacherId)
                .ne(Classroom::getStatus, "DISMISSED")
                .like(StringUtils.hasText(keyword), Classroom::getName, keyword)
                .orderByDesc(Classroom::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(classroom -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", classroom.getId());
            item.put("name", classroom.getName());
            item.put("subjectName", classroom.getSubjectName());
            item.put("coverFileId", classroom.getCoverFileId());
            item.put("coverUrl", resolveClassroomCoverUrl(classroom));
            item.put("legacyCover", classroom.getCoverFileId() == null && StringUtils.hasText(classroom.getCoverUrl()));
            item.put("intro", classroom.getIntro());
            item.put("memberCount", classroom.getMemberCount());
            item.put("capacity", classroom.getCapacity());
            item.put("status", classroom.getStatus());
            item.put("isPublic", classroom.getIsPublic());
            item.put("inviteCode", classroom.getInviteCode());
            item.put("seatUsage", Math.max(0, classroom.getMemberCount() == null ? 0 : classroom.getMemberCount() - 1));
            item.put("remainingSeats", Math.max(0, (classroom.getCapacity() == null ? 0 : classroom.getCapacity()) - Math.max(0, classroom.getMemberCount() == null ? 0 : classroom.getMemberCount() - 1)));
            item.put("materialCount", classroomMaterialMapper.selectCount(new LambdaQueryWrapper<ClassroomMaterial>()
                    .eq(ClassroomMaterial::getClassroomId, classroom.getId())
                    .eq(ClassroomMaterial::getStatus, "ENABLED")));
            item.put("pendingJoinCount", classroomJoinRequestMapper.selectCount(new LambdaQueryWrapper<ClassroomJoinRequest>()
                    .eq(ClassroomJoinRequest::getClassroomId, classroom.getId())
                    .eq(ClassroomJoinRequest::getStatus, "PENDING")));
            item.put("homeworkCount", classroomHomeworkMapper.selectCount(new LambdaQueryWrapper<ClassroomHomework>()
                    .eq(ClassroomHomework::getClassroomId, classroom.getId())));
            item.put("examCount", classroomExamTaskMapper.selectCount(new LambdaQueryWrapper<ClassroomExamTask>()
                    .eq(ClassroomExamTask::getClassroomId, classroom.getId())));
            return item;
        }).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveClassroom(ClassroomSaveRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        getApprovedProfile(teacherId);
        SysUser teacher = sysUserMapper.selectById(teacherId);
        if (request.getCoverFileId() == null && request.getId() != null && StringUtils.hasText(request.getCoverUrl())) {
            Classroom legacyClassroom = getOwnedClassroom(request.getId(), teacherId);
            legacyClassroom.setName(request.getName());
            legacyClassroom.setSubjectName(request.getSubjectName());
            legacyClassroom.setCoverUrl(request.getCoverUrl());
            legacyClassroom.setCoverFileId(null);
            legacyClassroom.setIntro(request.getIntro());
            legacyClassroom.setCapacity(request.getCapacity());
            legacyClassroom.setIsPublic(request.getIsPublic());
            legacyClassroom.setInviteCode(resolveInviteCode(request, legacyClassroom));
            legacyClassroom.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : legacyClassroom.getStatus());
            classroomMapper.updateById(legacyClassroom);
            return;
        }
        if (request.getCoverFileId() == null && request.getId() != null) {
            Classroom legacyClassroom = getOwnedClassroom(request.getId(), teacherId);
            if (legacyClassroom.getCoverFileId() == null && StringUtils.hasText(legacyClassroom.getCoverUrl())) {
                legacyClassroom.setName(request.getName());
                legacyClassroom.setSubjectName(request.getSubjectName());
                legacyClassroom.setIntro(request.getIntro());
                legacyClassroom.setCapacity(request.getCapacity());
                legacyClassroom.setIsPublic(request.getIsPublic());
                legacyClassroom.setInviteCode(resolveInviteCode(request, legacyClassroom));
                legacyClassroom.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : legacyClassroom.getStatus());
                classroomMapper.updateById(legacyClassroom);
                return;
            }
        }
        if (request.getCoverFileId() == null) {
            throw new BizException("请先上传班级封面");
        }
        SysFileRecord coverFile = sysFileRecordMapper.selectById(request.getCoverFileId());
        if (coverFile == null) {
            throw new BizException("班级封面文件不存在");
        }
        Classroom classroom;
        boolean create = request.getId() == null;
        if (create) {
            classroom = new Classroom();
            classroom.setTeacherId(teacherId);
            classroom.setTeacherName(teacher == null ? SecurityUtils.getUsername() : (StringUtils.hasText(teacher.getRealName()) ? teacher.getRealName() : teacher.getNickname()));
            classroom.setMemberCount(1);
        } else {
            classroom = getOwnedClassroom(request.getId(), teacherId);
        }
        applyClassroomCover(classroom, request, create);
        ensureClassroomCapacity(classroom.getId(), request.getCapacity(), teacherId, create);
        classroom.setName(request.getName());
        classroom.setSubjectName(request.getSubjectName());
        classroom.setIntro(request.getIntro());
        classroom.setCapacity(request.getCapacity());
        classroom.setIsPublic(request.getIsPublic());
        classroom.setInviteCode(resolveInviteCode(request, classroom));
        classroom.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "OPEN");
        if (create) {
            classroomMapper.insert(classroom);
            ClassroomMember member = new ClassroomMember();
            member.setClassroomId(classroom.getId());
            member.setUserId(teacherId);
            member.setRoleCode("TEACHER");
            classroomMemberMapper.insert(member);
        } else {
            classroomMapper.updateById(classroom);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void dismissClassroom(Long classroomId) {
        Long teacherId = SecurityUtils.getUserId();
        Classroom classroom = getOwnedClassroom(classroomId, teacherId);
        if ("DISMISSED".equals(classroom.getStatus())) {
            return;
        }
        classroom.setStatus("DISMISSED");
        classroom.setIsPublic(false);
        classroom.setInviteCode(null);
        classroom.setMemberCount(1);
        classroomMapper.updateById(classroom);
        classroomMemberMapper.delete(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .ne(ClassroomMember::getUserId, teacherId));
        List<ClassroomJoinRequest> requests = classroomJoinRequestMapper.selectList(new LambdaQueryWrapper<ClassroomJoinRequest>()
                .eq(ClassroomJoinRequest::getClassroomId, classroomId)
                .eq(ClassroomJoinRequest::getStatus, "PENDING"));
        requests.forEach(item -> {
            item.setStatus("REJECTED");
            item.setReviewedBy(teacherId);
            item.setReviewedAt(LocalDateTime.now());
            classroomJoinRequestMapper.updateById(item);
        });
    }

    public Map<String, Object> teacherClassroomDetail(Long classroomId) {
        Long teacherId = SecurityUtils.getUserId();
        Classroom classroom = getOwnedClassroom(classroomId, teacherId);
        Map<String, Object> result = new HashMap<>();
        result.put("classroom", buildTeacherClassroomCard(classroom));
        result.put("members", loadTeacherMembers(classroomId));
        result.put("joinRequests", classroomJoinRequestMapper.selectList(new LambdaQueryWrapper<ClassroomJoinRequest>()
                        .eq(ClassroomJoinRequest::getClassroomId, classroomId)
                        .orderByDesc(ClassroomJoinRequest::getCreatedAt))
                .stream()
                .map(this::toJoinRequestItem)
                .collect(Collectors.toList()));
        result.put("materials", loadTeacherMaterials(classroomId));
        result.put("homeworks", pageHomeworks(1L, 100L, classroomId).getRecords());
        result.put("classroomExams", pageClassroomExams(1L, 100L, classroomId).getRecords());
        return result;
    }

    public PageResult<Map<String, Object>> joinRequests(Long classroomId, Long pageNo, Long pageSize, String status) {
        Long teacherId = SecurityUtils.getUserId();
        getOwnedClassroom(classroomId, teacherId);
        Page<ClassroomJoinRequest> page = classroomJoinRequestMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<ClassroomJoinRequest>()
                .eq(ClassroomJoinRequest::getClassroomId, classroomId)
                .eq(StringUtils.hasText(status), ClassroomJoinRequest::getStatus, status)
                .orderByDesc(ClassroomJoinRequest::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(item -> {
            SysUser user = sysUserMapper.selectById(item.getUserId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("userId", item.getUserId());
            map.put("nickname", user == null ? "未知学员" : user.getNickname());
            map.put("province", user == null ? null : user.getProvince());
            map.put("examType", user == null ? null : user.getExamType());
            map.put("preferredSubject", user == null ? null : user.getPreferredSubject());
            map.put("remark", item.getRemark());
            map.put("status", item.getStatus());
            map.put("createdAt", item.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewJoinRequest(ClassroomJoinReviewRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        ClassroomJoinRequest joinRequest = classroomJoinRequestMapper.selectById(request.getRequestId());
        if (joinRequest == null) {
            throw new BizException("入班申请不存在");
        }
        Classroom classroom = getOwnedClassroom(joinRequest.getClassroomId(), teacherId);
        joinRequest.setStatus(request.getStatus());
        joinRequest.setReviewedBy(teacherId);
        joinRequest.setReviewedAt(LocalDateTime.now());
        classroomJoinRequestMapper.updateById(joinRequest);
        if ("APPROVED".equals(request.getStatus())) {
            long studentCount = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                    .eq(ClassroomMember::getClassroomId, classroom.getId())
                    .eq(ClassroomMember::getRoleCode, "STUDENT"));
            if (studentCount >= classroom.getCapacity()) {
                throw new BizException("班级招生人数已满，无法继续通过");
            }
            Long exists = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                    .eq(ClassroomMember::getClassroomId, classroom.getId())
                    .eq(ClassroomMember::getUserId, joinRequest.getUserId()));
            if (exists == 0) {
                ClassroomMember member = new ClassroomMember();
                member.setClassroomId(classroom.getId());
                member.setUserId(joinRequest.getUserId());
                member.setRoleCode("STUDENT");
                classroomMemberMapper.insert(member);
                classroom.setMemberCount((classroom.getMemberCount() == null ? 0 : classroom.getMemberCount()) + 1);
                classroomMapper.updateById(classroom);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveClassroomMaterial(TeacherClassroomMaterialSaveRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        getOwnedClassroom(request.getClassroomId(), teacherId);
        if (!Arrays.asList("TRIAL_VIDEO", "TRIAL_DOCUMENT").contains(request.getMaterialCategory())) {
            throw new BizException("不支持的材料类型");
        }
        SysFileRecord fileRecord = sysFileRecordMapper.selectById(request.getFileId());
        if (fileRecord == null) {
            throw new BizException("材料文件不存在");
        }
        ClassroomMaterial material = request.getId() == null ? new ClassroomMaterial() : getOwnedMaterial(request.getId(), teacherId);
        material.setClassroomId(request.getClassroomId());
        material.setMaterialCategory(request.getMaterialCategory());
        material.setTitle(request.getTitle());
        material.setFileId(request.getFileId());
        material.setDescription(request.getDescription());
        material.setSortNo(request.getSortNo());
        material.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ENABLED");
        if (material.getId() == null) {
            classroomMaterialMapper.insert(material);
        } else {
            classroomMaterialMapper.updateById(material);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteClassroomMaterial(Long materialId) {
        Long teacherId = SecurityUtils.getUserId();
        ClassroomMaterial material = getOwnedMaterial(materialId, teacherId);
        classroomMaterialMapper.deleteById(material.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long classroomId, Long userId) {
        Long teacherId = SecurityUtils.getUserId();
        Classroom classroom = getOwnedClassroom(classroomId, teacherId);
        if (teacherId.equals(userId)) {
            throw new BizException("不能移除班级教师本人");
        }
        ClassroomMember member = classroomMemberMapper.selectOne(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .eq(ClassroomMember::getUserId, userId)
                .last("LIMIT 1"));
        if (member == null || !"STUDENT".equals(member.getRoleCode())) {
            throw new BizException("班级学员不存在");
        }
        classroomMemberMapper.deleteById(member.getId());
        classroomJoinRequestMapper.delete(new LambdaQueryWrapper<ClassroomJoinRequest>()
                .eq(ClassroomJoinRequest::getClassroomId, classroomId)
                .eq(ClassroomJoinRequest::getUserId, userId)
                .eq(ClassroomJoinRequest::getStatus, "APPROVED"));
        classroom.setMemberCount(Math.max(1, classroom.getMemberCount() == null ? 1 : classroom.getMemberCount() - 1));
        classroomMapper.updateById(classroom);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveHomework(TeacherHomeworkSaveRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        Classroom classroom = getOwnedClassroom(request.getClassroomId(), teacherId);
        validateQuestions(request.getQuestionIds());
        ClassroomHomework homework = request.getId() == null ? new ClassroomHomework() : getOwnedHomework(request.getId(), teacherId);
        homework.setClassroomId(classroom.getId());
        homework.setTeacherId(teacherId);
        homework.setTitle(request.getTitle());
        homework.setDescription(request.getDescription());
        homework.setAttachmentFileId(request.getAttachmentFileId());
        homework.setQuestionCount(request.getQuestionIds().size());
        homework.setTotalScore(request.getQuestionIds().size());
        homework.setDeadlineAt(request.getDeadlineAt());
        homework.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "PUBLISHED");
        if (homework.getId() == null) {
            classroomHomeworkMapper.insert(homework);
        } else {
            classroomHomeworkMapper.updateById(homework);
            classroomHomeworkQuestionMapper.delete(new LambdaQueryWrapper<ClassroomHomeworkQuestion>()
                    .eq(ClassroomHomeworkQuestion::getHomeworkId, homework.getId()));
        }
        int sortNo = 1;
        for (Long questionId : request.getQuestionIds()) {
            ClassroomHomeworkQuestion relation = new ClassroomHomeworkQuestion();
            relation.setHomeworkId(homework.getId());
            relation.setQuestionId(questionId);
            relation.setScore(1);
            relation.setSortNo(sortNo++);
            classroomHomeworkQuestionMapper.insert(relation);
        }
    }

    public PageResult<Map<String, Object>> pageHomeworks(Long pageNo, Long pageSize, Long classroomId) {
        Long teacherId = SecurityUtils.getUserId();
        List<Long> classroomIds = loadTeacherClassroomIds(teacherId, classroomId);
        if (CollectionUtils.isEmpty(classroomIds)) {
            return new PageResult<>(0L, pageNo, pageSize, Collections.emptyList());
        }
        Page<ClassroomHomework> page = classroomHomeworkMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<ClassroomHomework>()
                .in(ClassroomHomework::getClassroomId, classroomIds)
                .orderByDesc(ClassroomHomework::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(this::toHomeworkSummary).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveClassroomExam(TeacherClassroomExamSaveRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        Classroom classroom = getOwnedClassroom(request.getClassroomId(), teacherId);
        validateQuestions(request.getQuestionIds());
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BizException("结束时间不能早于开始时间");
        }
        ClassroomExamTask task = request.getId() == null ? new ClassroomExamTask() : getOwnedClassroomExam(request.getId(), teacherId);
        task.setClassroomId(classroom.getId());
        task.setTeacherId(teacherId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDurationMinutes(request.getDurationMinutes());
        task.setQuestionCount(request.getQuestionIds().size());
        task.setTotalScore(request.getQuestionIds().size());
        task.setStartTime(request.getStartTime());
        task.setEndTime(request.getEndTime());
        task.setAntiCheatEnabled(Boolean.TRUE.equals(request.getAntiCheatEnabled()));
        task.setAntiCheatNotice(StringUtils.hasText(request.getAntiCheatNotice()) ? request.getAntiCheatNotice().trim() : null);
        task.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "PUBLISHED");
        if (task.getId() == null) {
            classroomExamTaskMapper.insert(task);
        } else {
            classroomExamTaskMapper.updateById(task);
            classroomExamQuestionMapper.delete(new LambdaQueryWrapper<ClassroomExamQuestion>()
                    .eq(ClassroomExamQuestion::getClassroomExamId, task.getId()));
        }
        int sortNo = 1;
        for (Long questionId : request.getQuestionIds()) {
            ClassroomExamQuestion relation = new ClassroomExamQuestion();
            relation.setClassroomExamId(task.getId());
            relation.setQuestionId(questionId);
            relation.setScore(1);
            relation.setSortNo(sortNo++);
            classroomExamQuestionMapper.insert(relation);
        }
    }

    public PageResult<Map<String, Object>> pageClassroomExams(Long pageNo, Long pageSize, Long classroomId) {
        Long teacherId = SecurityUtils.getUserId();
        List<Long> classroomIds = loadTeacherClassroomIds(teacherId, classroomId);
        if (CollectionUtils.isEmpty(classroomIds)) {
            return new PageResult<>(0L, pageNo, pageSize, Collections.emptyList());
        }
        Page<ClassroomExamTask> page = classroomExamTaskMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<ClassroomExamTask>()
                .in(ClassroomExamTask::getClassroomId, classroomIds)
                .orderByDesc(ClassroomExamTask::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(this::toClassroomExamSummary).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    public PageResult<Map<String, Object>> pageReviews(Long pageNo, Long pageSize, String taskType, Long classroomId, String reviewStatus) {
        Long teacherId = SecurityUtils.getUserId();
        List<Map<String, Object>> records = new ArrayList<>();
        if (!"EXAM".equals(taskType)) {
            records.addAll(loadHomeworkReviewRecords(teacherId, classroomId, reviewStatus));
        }
        if (!"HOMEWORK".equals(taskType)) {
            records.addAll(loadExamReviewRecords(teacherId, classroomId, reviewStatus));
        }
        records.sort((a, b) -> String.valueOf(b.get("submittedAt")).compareTo(String.valueOf(a.get("submittedAt"))));
        return paginate(records, pageNo, pageSize);
    }

    public Map<String, Object> reportOverview(Long classroomId, String timeRange, String subjectName, String taskType) {
        Long teacherId = SecurityUtils.getUserId();
        List<Long> classroomIds = filterClassroomIdsBySubject(loadTeacherClassroomIds(teacherId, classroomId), subjectName);
        LocalDateTime startAt = resolveTimeRangeStart(timeRange);
        Map<String, Object> result = new HashMap<>();
        result.put("classrooms", classroomIds.isEmpty() ? Collections.emptyList() : classroomMapper.selectBatchIds(classroomIds));
        result.put("summary", buildTeacherReportSummary(classroomIds, startAt, taskType));
        result.put("classroomStats", buildTeacherClassroomStats(classroomIds, startAt, taskType));
        result.put("homeworkStats", includeHomework(taskType) ? buildHomeworkStats(classroomIds, startAt) : Collections.emptyList());
        result.put("examStats", includeExam(taskType) ? buildExamStats(classroomIds, startAt) : Collections.emptyList());
        result.put("scoreDistribution", buildScoreDistribution(classroomIds, startAt, taskType));
        result.put("filters", buildTeacherReportFilterState(classroomId, timeRange, subjectName, taskType));
        return result;
    }

    public byte[] exportReportOverview(Long classroomId, String timeRange, String subjectName, String taskType) {
        Map<String, Object> report = reportOverview(classroomId, timeRange, subjectName, taskType);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> classroomStats = (List<Map<String, Object>>) report.get("classroomStats");
        List<String> headers = Arrays.asList("班级", "科目", "公开状态", "招生上限", "当前学员", "待审核", "材料数", "作业数", "考试数", "作业均分", "考试均分", "平均正确率");
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> item : classroomStats) {
            rows.add(Arrays.asList(
                    item.get("classroomName"),
                    item.get("subjectName"),
                    item.get("visibilityLabel"),
                    item.get("capacity"),
                    item.get("studentCount"),
                    item.get("pendingJoinCount"),
                    item.get("materialCount"),
                    item.get("homeworkCount"),
                    item.get("examCount"),
                    item.get("homeworkAverageScore"),
                    item.get("examAverageScore"),
                    item.get("averageCorrectRate")
            ));
        }
        return com.civil.platform.common.util.ExcelUtils.buildWorkbook("教师成绩报表", headers, rows);
    }

    public Map<String, Object> growthDashboard(Long classroomId, String segment, String followStatus, String timeRange, String subjectName, String taskType) {
        Long teacherId = SecurityUtils.getUserId();
        TeacherGrowthDashboardPreference preference = loadTeacherGrowthDashboardPreference(teacherId);
        Long resolvedClassroomId = classroomId != null ? classroomId : (preference == null ? null : preference.getClassroomId());
        String resolvedSegment = StringUtils.hasText(segment) ? segment : (preference == null ? null : preference.getSegment());
        String resolvedFollowStatus = StringUtils.hasText(followStatus) ? followStatus : (preference == null ? null : preference.getFollowStatus());
        List<Long> classroomIds = filterClassroomIdsBySubject(loadTeacherClassroomIds(teacherId, resolvedClassroomId), subjectName);
        LocalDate startDate = resolveTimeRangeStartDate(timeRange);
        Map<String, Object> result = new HashMap<>();
        result.put("classroomOptions", classroomIds.isEmpty() ? Collections.emptyList() : classroomMapper.selectBatchIds(classroomIds));
        result.put("summary", buildTeacherGrowthSummary(classroomIds, startDate));
        result.put("segmentStats", buildTeacherSegmentStats(classroomIds, startDate));
        result.put("trend", buildTeacherDiagnosisTrend(classroomIds, timeRange));
        result.put("focusStudents", buildTeacherFocusStudents(classroomIds, resolvedSegment, resolvedFollowStatus, startDate));
        result.put("classroomBoards", buildTeacherClassroomBoards(classroomIds, startDate));
        result.put("followHistory", buildTeacherFollowHistory(classroomIds));
        result.put("reminderSummary", buildTeacherReminderSummary(classroomIds));
        result.put("followReminders", buildTeacherFollowReminders(classroomIds));
        result.put("filters", buildTeacherGrowthFilterState(resolvedClassroomId, resolvedSegment, resolvedFollowStatus, timeRange, subjectName, taskType));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveGrowthFocusFollow(TeacherGrowthFocusFollowRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        saveGrowthFocusFollowInternal(request.getClassroomId(), request.getUserId(), request.getPlanId(), request.getLatestDiagnosisId(),
                request.getFollowStatus(), request.getPriorityLevel(), request.getRemark(), request.getNextFollowUpAt(), teacherId, "SAVE");
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchSaveGrowthFocusFollow(TeacherGrowthBatchFollowRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        for (TeacherGrowthBatchFollowRequest.Item item : request.getItems()) {
            saveGrowthFocusFollowInternal(item.getClassroomId(), item.getUserId(), item.getPlanId(), item.getLatestDiagnosisId(),
                    request.getFollowStatus(), request.getPriorityLevel(), request.getRemark(), request.getNextFollowUpAt(), teacherId, "BATCH");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveGrowthDashboardPreference(TeacherGrowthDashboardPreferenceRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        TeacherGrowthDashboardPreference preference = loadTeacherGrowthDashboardPreference(teacherId);
        if (preference == null) {
            preference = new TeacherGrowthDashboardPreference();
            preference.setUserId(teacherId);
            teacherGrowthDashboardPreferenceMapper.insert(preference);
        }
        preference.setClassroomId(request.getClassroomId());
        preference.setSegment(request.getSegment());
        preference.setFollowStatus(request.getFollowStatus());
        teacherGrowthDashboardPreferenceMapper.updateById(preference);
    }

    public byte[] exportGrowthDashboard(Long classroomId, String segment, String followStatus, String timeRange, String subjectName, String taskType) {
        Map<String, Object> dashboard = growthDashboard(classroomId, segment, followStatus, timeRange, subjectName, taskType);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> focusStudents = (List<Map<String, Object>>) dashboard.get("focusStudents");
        List<String> headers = Arrays.asList("班级", "学员", "当前计划", "成长分层", "完成率", "成长活力", "知识掌握度", "测卷平均准确率", "教师跟进状态", "教师优先级", "运营跟进状态", "薄弱知识点", "教师备注", "下次跟进时间", "提醒状态");
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> item : focusStudents) {
            List<Object> row = new ArrayList<>();
            row.add(item.get("classroomName"));
            row.add(item.get("studentName"));
            row.add(item.get("planName"));
            row.add(item.get("segmentLabel"));
            row.add(item.get("completionRate"));
            row.add(item.get("vitalityScore"));
            row.add(item.get("knowledgeMasteryRate"));
            row.add(item.get("testPaperAverageScore"));
            row.add(item.get("teacherFollowStatusLabel"));
            row.add(item.get("teacherPriorityLevelLabel"));
            row.add(item.get("adminFollowStatusLabel"));
            @SuppressWarnings("unchecked")
            List<String> weakPoints = (List<String>) item.get("weakKnowledgePoints");
            row.add(CollectionUtils.isEmpty(weakPoints) ? "" : String.join(" / ", weakPoints));
            row.add(item.get("teacherFollowRemark"));
            row.add(item.get("nextFollowUpAt"));
            row.add(item.get("reminderStatusLabel"));
            rows.add(row);
        }
        return com.civil.platform.common.util.ExcelUtils.buildWorkbook("教师成长协同", headers, rows);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewSubmission(TeacherSubmissionReviewRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        if ("HOMEWORK".equals(request.getTaskType())) {
            ClassroomHomeworkSubmission submission = classroomHomeworkSubmissionMapper.selectById(request.getSubmissionId());
            if (submission == null) {
                throw new BizException("作业提交不存在");
            }
            ClassroomHomework homework = getOwnedHomework(submission.getHomeworkId(), teacherId);
            int finalScore = applyHomeworkAnnotations(submission, request.getAnnotations(), homework.getId());
            submission.setScore(finalScore >= 0 ? finalScore : request.getScore());
            submission.setReviewStatus(request.getReviewStatus());
            submission.setReviewRemark(request.getReviewRemark());
            submission.setReviewedBy(teacherId);
            submission.setReviewedAt(LocalDateTime.now());
            classroomHomeworkSubmissionMapper.updateById(submission);
            return;
        }
        if ("EXAM".equals(request.getTaskType())) {
            ClassroomExamSubmission submission = classroomExamSubmissionMapper.selectById(request.getSubmissionId());
            if (submission == null) {
                throw new BizException("考试提交不存在");
            }
            ClassroomExamTask examTask = getOwnedClassroomExam(submission.getClassroomExamId(), teacherId);
            int finalScore = applyExamAnnotations(submission, request.getAnnotations(), examTask.getId());
            submission.setScore(finalScore >= 0 ? finalScore : request.getScore());
            submission.setReviewStatus(request.getReviewStatus());
            submission.setReviewRemark(request.getReviewRemark());
            submission.setReviewedBy(teacherId);
            submission.setReviewedAt(LocalDateTime.now());
            classroomExamSubmissionMapper.updateById(submission);
            return;
        }
        throw new BizException("不支持的批改类型");
    }

    private TeacherProfile getApprovedProfile(Long teacherId) {
        TeacherProfile profile = teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, teacherId));
        if (profile == null || !AuditStatus.APPROVED.name().equals(profile.getAuditStatus())) {
            throw new BizException(403, "教师认证尚未通过");
        }
        return profile;
    }

    private List<Classroom> loadRecommendationClassrooms(Long teacherId, Long classroomId) {
        if (classroomId != null) {
            Classroom classroom = getOwnedClassroom(classroomId, teacherId);
            if ("DISMISSED".equals(classroom.getStatus())) {
                return Collections.emptyList();
            }
            return Collections.singletonList(classroom);
        }
        return classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getTeacherId, teacherId)
                .ne(Classroom::getStatus, "DISMISSED")
                .orderByDesc(Classroom::getCreatedAt));
    }

    private Map<Long, Set<Long>> loadMemberUserIdsByClassroom(List<Long> classroomIds) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyMap();
        }
        return classroomMemberMapper.selectList(new LambdaQueryWrapper<ClassroomMember>()
                        .in(ClassroomMember::getClassroomId, classroomIds))
                .stream()
                .collect(Collectors.groupingBy(ClassroomMember::getClassroomId,
                        Collectors.mapping(ClassroomMember::getUserId, Collectors.toSet())));
    }

    private Map<Long, Set<Long>> loadOpenRequestUserIdsByClassroom(List<Long> classroomIds) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyMap();
        }
        return classroomJoinRequestMapper.selectList(new LambdaQueryWrapper<ClassroomJoinRequest>()
                        .in(ClassroomJoinRequest::getClassroomId, classroomIds)
                        .in(ClassroomJoinRequest::getStatus, Arrays.asList("PENDING", "INVITED")))
                .stream()
                .collect(Collectors.groupingBy(ClassroomJoinRequest::getClassroomId,
                        Collectors.mapping(ClassroomJoinRequest::getUserId, Collectors.toSet())));
    }

    private Map<Long, Long> loadStudentCountByClassroom(List<Long> classroomIds) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyMap();
        }
        return classroomMemberMapper.selectList(new LambdaQueryWrapper<ClassroomMember>()
                        .in(ClassroomMember::getClassroomId, classroomIds)
                        .eq(ClassroomMember::getRoleCode, "STUDENT"))
                .stream()
                .collect(Collectors.groupingBy(ClassroomMember::getClassroomId, Collectors.counting()));
    }

    private Set<Long> loadStudentRoleUserIds() {
        SysRole userRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, "USER")
                .last("limit 1"));
        if (userRole == null) {
            return Collections.emptySet();
        }
        List<SysRole> forbiddenRoles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getCode, Arrays.asList("TEACHER", "ADMIN")));
        Set<Long> forbiddenRoleIds = forbiddenRoles.stream().map(SysRole::getId).collect(Collectors.toSet());
        List<SysUserRole> roles = sysUserRoleMapper.selectList(null);
        Set<Long> forbiddenUserIds = roles.stream()
                .filter(role -> forbiddenRoleIds.contains(role.getRoleId()))
                .map(SysUserRole::getUserId)
                .collect(Collectors.toSet());
        return roles.stream()
                .filter(role -> userRole.getId().equals(role.getRoleId()))
                .map(SysUserRole::getUserId)
                .filter(userId -> !forbiddenUserIds.contains(userId))
                .collect(Collectors.toSet());
    }

    private Map<String, Object> buildBestStudentRecommendation(SysUser student,
                                                               List<Classroom> classrooms,
                                                               Map<Long, Set<Long>> memberUserIdsByClassroom,
                                                               Map<Long, Set<Long>> openRequestUserIdsByClassroom,
                                                               Map<Long, Long> studentCountByClassroom,
                                                               SysUser teacher) {
        Map<String, Object> best = null;
        int bestScore = 0;
        for (Classroom classroom : classrooms) {
            Set<Long> memberIds = memberUserIdsByClassroom.getOrDefault(classroom.getId(), Collections.emptySet());
            Set<Long> openRequestUserIds = openRequestUserIdsByClassroom.getOrDefault(classroom.getId(), Collections.emptySet());
            if (memberIds.contains(student.getId()) || openRequestUserIds.contains(student.getId())) {
                continue;
            }
            List<String> reasons = new ArrayList<>();
            int score = calculateStudentClassroomMatchScore(student, classroom, teacher, reasons);
            if (score <= 0) {
                continue;
            }
            if (best == null || score > bestScore) {
                long studentCount = studentCountByClassroom.getOrDefault(classroom.getId(), 0L);
                int capacity = classroom.getCapacity() == null ? 0 : classroom.getCapacity();
                Map<String, Object> item = new HashMap<>();
                item.put("userId", student.getId());
                item.put("nickname", student.getNickname());
                item.put("realName", student.getRealName());
                item.put("province", student.getProvince());
                item.put("examType", student.getExamType());
                item.put("preferredSubject", student.getPreferredSubject());
                item.put("classroomId", classroom.getId());
                item.put("classroomName", classroom.getName());
                item.put("classroomSubjectName", classroom.getSubjectName());
                item.put("matchScore", score);
                item.put("matchReasons", reasons);
                item.put("matchReason", String.join("、", reasons));
                item.put("studentCount", studentCount);
                item.put("capacity", capacity);
                item.put("remainingSeats", Math.max(0, capacity - (int) studentCount));
                item.put("classroomFull", studentCount >= capacity);
                best = item;
                bestScore = score;
            }
        }
        return best;
    }

    private int calculateStudentClassroomMatchScore(SysUser student, Classroom classroom, SysUser teacher, List<String> reasons) {
        int score = 0;
        if (textEquals(classroom.getSubjectName(), student.getPreferredSubject())) {
            score += 60;
            reasons.add("科目匹配");
        }
        if (teacher != null && textEquals(teacher.getExamType(), student.getExamType())) {
            score += 25;
            reasons.add("考试类型匹配");
        }
        if (teacher != null && textEquals(teacher.getProvince(), student.getProvince())) {
            score += 15;
            reasons.add("地区匹配");
        }
        return score;
    }

    private boolean textEquals(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.trim().equals(right.trim());
    }

    private Classroom getOwnedClassroom(Long classroomId, Long teacherId) {
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null || !teacherId.equals(classroom.getTeacherId())) {
            throw new BizException(403, "无权操作该班级");
        }
        return classroom;
    }

    private ClassroomMaterial getOwnedMaterial(Long materialId, Long teacherId) {
        ClassroomMaterial material = classroomMaterialMapper.selectById(materialId);
        if (material == null) {
            throw new BizException("班级材料不存在");
        }
        getOwnedClassroom(material.getClassroomId(), teacherId);
        return material;
    }

    private ClassroomHomework getOwnedHomework(Long homeworkId, Long teacherId) {
        ClassroomHomework homework = classroomHomeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new BizException("班级作业不存在");
        }
        getOwnedClassroom(homework.getClassroomId(), teacherId);
        return homework;
    }

    private ClassroomExamTask getOwnedClassroomExam(Long examId, Long teacherId) {
        ClassroomExamTask exam = classroomExamTaskMapper.selectById(examId);
        if (exam == null) {
            throw new BizException("班级考试不存在");
        }
        getOwnedClassroom(exam.getClassroomId(), teacherId);
        return exam;
    }

    private List<Long> loadTeacherClassroomIds(Long teacherId, Long classroomId) {
        getApprovedProfile(teacherId);
        if (classroomId != null) {
            getOwnedClassroom(classroomId, teacherId);
            return Collections.singletonList(classroomId);
        }
        return classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                        .eq(Classroom::getTeacherId, teacherId)
                        .select(Classroom::getId))
                .stream()
                .map(Classroom::getId)
                .collect(Collectors.toList());
    }

    private void validateQuestions(List<Long> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            throw new BizException("请至少选择一道题目");
        }
        long count = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .in(Question::getId, questionIds)
                .eq(Question::getEnabled, true));
        if (count != questionIds.size()) {
            throw new BizException("存在不可用题目，请重新选择");
        }
    }

    private void ensureClassroomCapacity(Long classroomId, Integer capacity, Long teacherId, boolean create) {
        if (create) {
            return;
        }
        long currentStudentCount = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .eq(ClassroomMember::getRoleCode, "STUDENT"));
        getOwnedClassroom(classroomId, teacherId);
        if (capacity != null && capacity < currentStudentCount) {
            throw new BizException("招生人数上限不能小于当前已入班学员数");
        }
    }

    private int countPendingReviews(List<Long> classroomIds) {
        return countPendingReviews(classroomIds, null, "ALL");
    }

    private int countPendingReviews(List<Long> classroomIds, LocalDateTime startAt, String taskType) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return 0;
        }
        Long homeworkPending = includeHomework(taskType) ? classroomHomeworkSubmissionMapper.selectCount(new LambdaQueryWrapper<ClassroomHomeworkSubmission>()
                .in(ClassroomHomeworkSubmission::getClassroomId, classroomIds)
                .ge(startAt != null, ClassroomHomeworkSubmission::getSubmittedAt, startAt)
                .in(ClassroomHomeworkSubmission::getReviewStatus, Arrays.asList("AUTO_GRADED", "PENDING"))) : 0L;
        Long examPending = includeExam(taskType) ? classroomExamSubmissionMapper.selectCount(new LambdaQueryWrapper<ClassroomExamSubmission>()
                .in(ClassroomExamSubmission::getClassroomId, classroomIds)
                .ge(startAt != null, ClassroomExamSubmission::getSubmittedAt, startAt)
                .in(ClassroomExamSubmission::getReviewStatus, Arrays.asList("AUTO_GRADED", "PENDING"))) : 0L;
        return homeworkPending.intValue() + examPending.intValue();
    }

    private Map<String, Object> toHomeworkSummary(ClassroomHomework homework) {
        Classroom classroom = classroomMapper.selectById(homework.getClassroomId());
        SysFileRecord attachment = homework.getAttachmentFileId() == null ? null : sysFileRecordMapper.selectById(homework.getAttachmentFileId());
        List<ClassroomHomeworkSubmission> submissions = classroomHomeworkSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkSubmission>()
                .eq(ClassroomHomeworkSubmission::getHomeworkId, homework.getId()));
        Map<String, Object> map = new HashMap<>();
        map.put("id", homework.getId());
        map.put("classroomId", homework.getClassroomId());
        map.put("classroomName", classroom == null ? "-" : classroom.getName());
        map.put("title", homework.getTitle());
        map.put("description", homework.getDescription());
        map.put("questionCount", homework.getQuestionCount());
        map.put("questionIds", classroomHomeworkQuestionMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkQuestion>()
                        .eq(ClassroomHomeworkQuestion::getHomeworkId, homework.getId())
                        .orderByAsc(ClassroomHomeworkQuestion::getSortNo))
                .stream()
                .map(ClassroomHomeworkQuestion::getQuestionId)
                .collect(Collectors.toList()));
        map.put("attachmentFileId", homework.getAttachmentFileId());
        map.put("attachmentName", attachment == null ? null : attachment.getOriginalName());
        map.put("attachmentUrl", attachment == null ? null : "/api/files/view/" + attachment.getId());
        map.put("totalScore", homework.getTotalScore());
        map.put("status", homework.getStatus());
        map.put("deadlineAt", homework.getDeadlineAt());
        map.put("submissionCount", submissions.size());
        map.put("reviewedCount", submissions.stream().filter(item -> "REVIEWED".equals(item.getReviewStatus())).count());
        return map;
    }

    private Map<String, Object> buildTeacherClassroomCard(Classroom classroom) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", classroom.getId());
        item.put("name", classroom.getName());
        item.put("subjectName", classroom.getSubjectName());
        item.put("coverFileId", classroom.getCoverFileId());
        item.put("coverUrl", resolveClassroomCoverUrl(classroom));
        item.put("intro", classroom.getIntro());
        item.put("capacity", classroom.getCapacity());
        item.put("status", classroom.getStatus());
        item.put("isPublic", classroom.getIsPublic());
        item.put("inviteCode", classroom.getInviteCode());
        item.put("visibilityLabel", Boolean.TRUE.equals(classroom.getIsPublic()) ? "公开班级" : "私密班级");
        item.put("studentCount", classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroom.getId())
                .eq(ClassroomMember::getRoleCode, "STUDENT")));
        item.put("pendingJoinCount", classroomJoinRequestMapper.selectCount(new LambdaQueryWrapper<ClassroomJoinRequest>()
                .eq(ClassroomJoinRequest::getClassroomId, classroom.getId())
                .eq(ClassroomJoinRequest::getStatus, "PENDING")));
        item.put("materialCount", classroomMaterialMapper.selectCount(new LambdaQueryWrapper<ClassroomMaterial>()
                .eq(ClassroomMaterial::getClassroomId, classroom.getId())
                .eq(ClassroomMaterial::getStatus, "ENABLED")));
        return item;
    }

    private List<Map<String, Object>> loadTeacherMembers(Long classroomId) {
        return classroomMemberMapper.selectList(new LambdaQueryWrapper<ClassroomMember>()
                        .eq(ClassroomMember::getClassroomId, classroomId)
                        .orderByAsc(ClassroomMember::getJoinedAt))
                .stream()
                .map(item -> {
                    SysUser user = sysUserMapper.selectById(item.getUserId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", item.getUserId());
                    map.put("roleCode", item.getRoleCode());
                    map.put("joinedAt", item.getJoinedAt());
                    map.put("nickname", user == null ? "未知用户" : user.getNickname());
                    map.put("realName", user == null ? null : user.getRealName());
                    map.put("phone", user == null ? null : user.getPhone());
                    map.put("email", user == null ? null : user.getEmail());
                    map.put("province", user == null ? null : user.getProvince());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> toJoinRequestItem(ClassroomJoinRequest item) {
        SysUser user = sysUserMapper.selectById(item.getUserId());
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("userId", item.getUserId());
        map.put("nickname", user == null ? "未知学员" : user.getNickname());
        map.put("realName", user == null ? null : user.getRealName());
        map.put("province", user == null ? null : user.getProvince());
        map.put("examType", user == null ? null : user.getExamType());
        map.put("preferredSubject", user == null ? null : user.getPreferredSubject());
        map.put("remark", item.getRemark());
        map.put("status", item.getStatus());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private List<Map<String, Object>> loadTeacherMaterials(Long classroomId) {
        return classroomMaterialMapper.selectList(new LambdaQueryWrapper<ClassroomMaterial>()
                        .eq(ClassroomMaterial::getClassroomId, classroomId)
                        .orderByAsc(ClassroomMaterial::getSortNo)
                        .orderByDesc(ClassroomMaterial::getId))
                .stream()
                .map(item -> {
                    SysFileRecord file = item.getFileId() == null ? null : sysFileRecordMapper.selectById(item.getFileId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", item.getId());
                    map.put("classroomId", item.getClassroomId());
                    map.put("materialCategory", item.getMaterialCategory());
                    map.put("materialCategoryLabel", "TRIAL_VIDEO".equals(item.getMaterialCategory()) ? "试听视频" : "试用资料");
                    map.put("title", item.getTitle());
                    map.put("description", item.getDescription());
                    map.put("sortNo", item.getSortNo());
                    map.put("status", item.getStatus());
                    map.put("fileId", item.getFileId());
                    map.put("fileName", file == null ? null : file.getOriginalName());
                    map.put("fileType", file == null ? null : file.getFileType());
                    map.put("fileUrl", item.getFileId() == null ? null : "/api/files/view/" + item.getFileId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private String resolveClassroomCoverUrl(Classroom classroom) {
        if (classroom.getCoverFileId() != null) {
            return "/api/files/view/" + classroom.getCoverFileId();
        }
        return classroom.getCoverUrl();
    }

    private void applyClassroomCover(Classroom classroom, ClassroomSaveRequest request, boolean create) {
        if (request.getCoverFileId() != null) {
            classroom.setCoverFileId(request.getCoverFileId());
            classroom.setCoverUrl("/api/files/view/" + request.getCoverFileId());
            return;
        }
        if (StringUtils.hasText(request.getCoverUrl())) {
            classroom.setCoverUrl(request.getCoverUrl());
            if (create) {
                classroom.setCoverFileId(null);
            }
        }
    }

    private String resolveInviteCode(ClassroomSaveRequest request, Classroom classroom) {
        if (Boolean.TRUE.equals(request.getIsPublic())) {
            return null;
        }
        Long classroomId = classroom == null ? request.getId() : classroom.getId();
        if (StringUtils.hasText(request.getInviteCode())
                && request.getInviteCode().matches("\\d{6}")
                && !inviteCodeExists(request.getInviteCode(), classroomId)) {
            return request.getInviteCode();
        }
        if (classroom != null
                && StringUtils.hasText(classroom.getInviteCode())
                && classroom.getInviteCode().matches("\\d{6}")
                && !inviteCodeExists(classroom.getInviteCode(), classroomId)) {
            return classroom.getInviteCode();
        }
        return generateUniqueInviteCode(classroomId);
    }

    private String generateUniqueInviteCode(Long classroomId) {
        for (int i = 0; i < 20; i++) {
            String inviteCode = String.valueOf(100000 + INVITE_CODE_RANDOM.nextInt(900000));
            if (!inviteCodeExists(inviteCode, classroomId)) {
                return inviteCode;
            }
        }
        throw new BizException("邀请码生成失败，请重试");
    }

    private boolean inviteCodeExists(String inviteCode, Long classroomId) {
        return classroomMapper.selectCount(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getInviteCode, inviteCode)
                .ne(classroomId != null, Classroom::getId, classroomId)) > 0;
    }

    private Map<String, Object> toClassroomExamSummary(ClassroomExamTask task) {
        Classroom classroom = classroomMapper.selectById(task.getClassroomId());
        List<ClassroomExamSubmission> submissions = classroomExamSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomExamSubmission>()
                .eq(ClassroomExamSubmission::getClassroomExamId, task.getId()));
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("classroomId", task.getClassroomId());
        map.put("classroomName", classroom == null ? "-" : classroom.getName());
        map.put("title", task.getTitle());
        map.put("description", task.getDescription());
        map.put("questionCount", task.getQuestionCount());
        map.put("questionIds", classroomExamQuestionMapper.selectList(new LambdaQueryWrapper<ClassroomExamQuestion>()
                        .eq(ClassroomExamQuestion::getClassroomExamId, task.getId())
                        .orderByAsc(ClassroomExamQuestion::getSortNo))
                .stream()
                .map(ClassroomExamQuestion::getQuestionId)
                .collect(Collectors.toList()));
        map.put("totalScore", task.getTotalScore());
        map.put("durationMinutes", task.getDurationMinutes());
        map.put("antiCheatEnabled", task.getAntiCheatEnabled());
        map.put("antiCheatNotice", task.getAntiCheatNotice());
        map.put("status", task.getStatus());
        map.put("startTime", task.getStartTime());
        map.put("endTime", task.getEndTime());
        map.put("submissionCount", submissions.size());
        map.put("reviewedCount", submissions.stream().filter(item -> "REVIEWED".equals(item.getReviewStatus())).count());
        return map;
    }

    private List<Map<String, Object>> buildTeacherClassroomStats(List<Long> classroomIds, LocalDateTime startAt, String taskType) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyList();
        }
        return classroomMapper.selectBatchIds(classroomIds).stream()
                .map(classroom -> {
                    List<ClassroomHomeworkSubmission> homeworkSubmissions = includeHomework(taskType) ? classroomHomeworkSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkSubmission>()
                            .eq(ClassroomHomeworkSubmission::getClassroomId, classroom.getId())
                            .eq(ClassroomHomeworkSubmission::getStatus, "SUBMITTED")
                            .ge(startAt != null, ClassroomHomeworkSubmission::getSubmittedAt, startAt)) : Collections.emptyList();
                    List<ClassroomExamSubmission> examSubmissions = includeExam(taskType) ? classroomExamSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomExamSubmission>()
                            .eq(ClassroomExamSubmission::getClassroomId, classroom.getId())
                            .eq(ClassroomExamSubmission::getStatus, "SUBMITTED")
                            .ge(startAt != null, ClassroomExamSubmission::getSubmittedAt, startAt)) : Collections.emptyList();
                    List<java.math.BigDecimal> correctRates = new ArrayList<>();
                    homeworkSubmissions.forEach(item -> correctRates.add(toCorrectRate(item.getCorrectCount(), item.getQuestionCount())));
                    examSubmissions.forEach(item -> correctRates.add(toCorrectRate(item.getCorrectCount(), item.getQuestionCount())));
                    Map<String, Object> map = new HashMap<>();
                    map.put("classroomId", classroom.getId());
                    map.put("classroomName", classroom.getName());
                    map.put("subjectName", classroom.getSubjectName());
                    map.put("visibilityLabel", Boolean.TRUE.equals(classroom.getIsPublic()) ? "公开" : "私密");
                    map.put("capacity", classroom.getCapacity());
                    map.put("studentCount", classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                            .eq(ClassroomMember::getClassroomId, classroom.getId())
                            .eq(ClassroomMember::getRoleCode, "STUDENT")));
                    map.put("pendingJoinCount", classroomJoinRequestMapper.selectCount(new LambdaQueryWrapper<ClassroomJoinRequest>()
                            .eq(ClassroomJoinRequest::getClassroomId, classroom.getId())
                            .eq(ClassroomJoinRequest::getStatus, "PENDING")));
                    map.put("materialCount", classroomMaterialMapper.selectCount(new LambdaQueryWrapper<ClassroomMaterial>()
                            .eq(ClassroomMaterial::getClassroomId, classroom.getId())
                            .eq(ClassroomMaterial::getStatus, "ENABLED")));
                    map.put("homeworkCount", includeHomework(taskType) ? classroomHomeworkMapper.selectCount(new LambdaQueryWrapper<ClassroomHomework>()
                            .eq(ClassroomHomework::getClassroomId, classroom.getId())
                            .ge(startAt != null, ClassroomHomework::getCreatedAt, startAt)) : 0);
                    map.put("examCount", includeExam(taskType) ? classroomExamTaskMapper.selectCount(new LambdaQueryWrapper<ClassroomExamTask>()
                            .eq(ClassroomExamTask::getClassroomId, classroom.getId())
                            .ge(startAt != null, ClassroomExamTask::getCreatedAt, startAt)) : 0);
                    map.put("homeworkAverageScore", averageScore(homeworkSubmissions));
                    map.put("examAverageScore", averageScore(examSubmissions));
                    map.put("averageCorrectRate", averageBigDecimal(correctRates));
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildScoreDistribution(List<Long> classroomIds, LocalDateTime startAt, String taskType) {
        List<Map<String, Object>> buckets = new ArrayList<>();
        List<DistributionCounter> counters = Arrays.asList(
                new DistributionCounter("LT60", "<60%"),
                new DistributionCounter("S60_79", "60-79%"),
                new DistributionCounter("S80_89", "80-89%"),
                new DistributionCounter("GE90", "90%+")
        );
        if (!CollectionUtils.isEmpty(classroomIds) && includeHomework(taskType)) {
            classroomHomeworkSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkSubmission>()
                            .in(ClassroomHomeworkSubmission::getClassroomId, classroomIds)
                            .eq(ClassroomHomeworkSubmission::getStatus, "SUBMITTED")
                            .ge(startAt != null, ClassroomHomeworkSubmission::getSubmittedAt, startAt))
                    .forEach(item -> incrementDistribution(counters, toCorrectRate(item.getCorrectCount(), item.getQuestionCount()), true));
        }
        if (!CollectionUtils.isEmpty(classroomIds) && includeExam(taskType)) {
            classroomExamSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomExamSubmission>()
                            .in(ClassroomExamSubmission::getClassroomId, classroomIds)
                            .eq(ClassroomExamSubmission::getStatus, "SUBMITTED")
                            .ge(startAt != null, ClassroomExamSubmission::getSubmittedAt, startAt))
                    .forEach(item -> incrementDistribution(counters, toCorrectRate(item.getCorrectCount(), item.getQuestionCount()), false));
        }
        counters.forEach(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("bucket", item.bucket);
            map.put("label", item.label);
            map.put("homeworkCount", item.homeworkCount);
            map.put("examCount", item.examCount);
            map.put("totalCount", item.homeworkCount + item.examCount);
            buckets.add(map);
        });
        return buckets;
    }

    private List<Map<String, Object>> loadHomeworkReviewRecords(Long teacherId, Long classroomId, String reviewStatus) {
        List<Long> classroomIds = loadTeacherClassroomIds(teacherId, classroomId);
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyList();
        }
        List<ClassroomHomeworkSubmission> submissions = classroomHomeworkSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkSubmission>()
                .in(ClassroomHomeworkSubmission::getClassroomId, classroomIds)
                .eq(StringUtils.hasText(reviewStatus), ClassroomHomeworkSubmission::getReviewStatus, reviewStatus)
                .orderByDesc(ClassroomHomeworkSubmission::getSubmittedAt));
        Map<Long, ClassroomHomework> homeworkMap = classroomHomeworkMapper.selectBatchIds(submissions.stream()
                        .map(ClassroomHomeworkSubmission::getHomeworkId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ClassroomHomework::getId, Function.identity()));
        return submissions.stream().map(item -> toReviewRecord("HOMEWORK", item.getId(), item.getClassroomId(), item.getUserId(),
                homeworkMap.get(item.getHomeworkId()) == null ? "-" : homeworkMap.get(item.getHomeworkId()).getTitle(),
                item.getScore(), item.getReviewStatus(), item.getSubmittedAt())).collect(Collectors.toList());
    }

    private List<Map<String, Object>> loadExamReviewRecords(Long teacherId, Long classroomId, String reviewStatus) {
        List<Long> classroomIds = loadTeacherClassroomIds(teacherId, classroomId);
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyList();
        }
        List<ClassroomExamSubmission> submissions = classroomExamSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomExamSubmission>()
                .in(ClassroomExamSubmission::getClassroomId, classroomIds)
                .eq(StringUtils.hasText(reviewStatus), ClassroomExamSubmission::getReviewStatus, reviewStatus)
                .orderByDesc(ClassroomExamSubmission::getSubmittedAt));
        Map<Long, ClassroomExamTask> examMap = classroomExamTaskMapper.selectBatchIds(submissions.stream()
                        .map(ClassroomExamSubmission::getClassroomExamId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ClassroomExamTask::getId, Function.identity()));
        return submissions.stream().map(item -> toReviewRecord("EXAM", item.getId(), item.getClassroomId(), item.getUserId(),
                examMap.get(item.getClassroomExamId()) == null ? "-" : examMap.get(item.getClassroomExamId()).getTitle(),
                item.getScore(), item.getReviewStatus(), item.getSubmittedAt())).collect(Collectors.toList());
    }

    private Map<String, Object> toReviewRecord(String taskType, Long submissionId, Long classroomId, Long userId, String taskTitle,
                                               Integer score, String reviewStatus, LocalDateTime submittedAt) {
        SysUser user = sysUserMapper.selectById(userId);
        Classroom classroom = classroomMapper.selectById(classroomId);
        Map<String, Object> map = new HashMap<>();
        map.put("taskType", taskType);
        map.put("submissionId", submissionId);
        map.put("classroomId", classroomId);
        map.put("classroomName", classroom == null ? "-" : classroom.getName());
        map.put("studentName", user == null ? "未知学员" : user.getNickname());
        map.put("taskTitle", taskTitle);
        map.put("score", score);
        map.put("reviewStatus", reviewStatus);
        map.put("submittedAt", submittedAt);
        return map;
    }

    private int applyHomeworkAnnotations(ClassroomHomeworkSubmission submission,
                                         List<TeacherSubmissionReviewRequest.AnnotationItem> annotations,
                                         Long homeworkId) {
        Map<Long, ClassroomHomeworkAnswer> answerMap = classroomHomeworkAnswerMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkAnswer>()
                        .eq(ClassroomHomeworkAnswer::getSubmissionId, submission.getId()))
                .stream()
                .collect(Collectors.toMap(ClassroomHomeworkAnswer::getQuestionId, Function.identity()));
        Map<Long, ClassroomHomeworkQuestion> relationMap = classroomHomeworkQuestionMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkQuestion>()
                        .eq(ClassroomHomeworkQuestion::getHomeworkId, homeworkId))
                .stream()
                .collect(Collectors.toMap(ClassroomHomeworkQuestion::getQuestionId, Function.identity()));
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(relationMap.keySet()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        if (!CollectionUtils.isEmpty(annotations)) {
            for (TeacherSubmissionReviewRequest.AnnotationItem item : annotations) {
                ClassroomHomeworkAnswer answer = answerMap.get(item.getQuestionId());
                ClassroomHomeworkQuestion relation = relationMap.get(item.getQuestionId());
                Question question = questionMap.get(item.getQuestionId());
                if (answer == null || relation == null || question == null) {
                    continue;
                }
                answer.setTeacherComment(item.getTeacherComment());
                if (isSubjectiveQuestion(question.getQuestionType()) && item.getScore() != null) {
                    answer.setScore(Math.max(0, Math.min(relation.getScore(), item.getScore())));
                }
                classroomHomeworkAnswerMapper.updateById(answer);
            }
        }
        return answerMap.values().stream().map(ClassroomHomeworkAnswer::getScore).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    private int applyExamAnnotations(ClassroomExamSubmission submission,
                                     List<TeacherSubmissionReviewRequest.AnnotationItem> annotations,
                                     Long classroomExamId) {
        Map<Long, ClassroomExamAnswer> answerMap = classroomExamAnswerMapper.selectList(new LambdaQueryWrapper<ClassroomExamAnswer>()
                        .eq(ClassroomExamAnswer::getSubmissionId, submission.getId()))
                .stream()
                .collect(Collectors.toMap(ClassroomExamAnswer::getQuestionId, Function.identity()));
        Map<Long, ClassroomExamQuestion> relationMap = classroomExamQuestionMapper.selectList(new LambdaQueryWrapper<ClassroomExamQuestion>()
                        .eq(ClassroomExamQuestion::getClassroomExamId, classroomExamId))
                .stream()
                .collect(Collectors.toMap(ClassroomExamQuestion::getQuestionId, Function.identity()));
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(relationMap.keySet()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        if (!CollectionUtils.isEmpty(annotations)) {
            for (TeacherSubmissionReviewRequest.AnnotationItem item : annotations) {
                ClassroomExamAnswer answer = answerMap.get(item.getQuestionId());
                ClassroomExamQuestion relation = relationMap.get(item.getQuestionId());
                Question question = questionMap.get(item.getQuestionId());
                if (answer == null || relation == null || question == null) {
                    continue;
                }
                answer.setTeacherComment(item.getTeacherComment());
                if (isSubjectiveQuestion(question.getQuestionType()) && item.getScore() != null) {
                    answer.setScore(Math.max(0, Math.min(relation.getScore(), item.getScore())));
                }
                classroomExamAnswerMapper.updateById(answer);
            }
        }
        return answerMap.values().stream().map(ClassroomExamAnswer::getScore).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    private boolean isSubjectiveQuestion(String questionType) {
        return !Arrays.asList("SINGLE", "MULTI", "JUDGE").contains(questionType);
    }

    private Map<String, Object> buildTeacherGrowthSummary(List<Long> classroomIds, LocalDate startDate) {
        List<Map<String, Object>> focusStudents = buildTeacherFocusStudents(classroomIds, null, null, startDate);
        Map<String, Object> summary = new HashMap<>();
        summary.put("classroomCount", classroomIds.size());
        summary.put("focusStudentCount", focusStudents.size());
        summary.put("atRiskCount", focusStudents.stream().filter(item -> "AT_RISK".equals(item.get("segment"))).count());
        summary.put("trackingCount", focusStudents.stream().filter(item -> "TRACKING".equals(item.get("segment"))).count());
        summary.put("teacherFollowPendingCount", focusStudents.stream().filter(item -> "PENDING".equals(item.get("teacherFollowStatus"))).count());
        summary.put("averageCompletionRate", focusStudents.isEmpty() ? 0 : focusStudents.stream()
                .map(item -> (java.math.BigDecimal) item.get("completionRate"))
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .average()
                .orElse(0));
        return summary;
    }

    private List<Map<String, Object>> buildTeacherSegmentStats(List<Long> classroomIds, LocalDate startDate) {
        List<Map<String, Object>> focusStudents = buildTeacherFocusStudents(classroomIds, null, null, startDate);
        List<String> segments = Arrays.asList("AT_RISK", "TRACKING", "RISING", "STABLE");
        List<Map<String, Object>> result = new ArrayList<>();
        for (String segment : segments) {
            Map<String, Object> item = new HashMap<>();
            item.put("segment", segment);
            item.put("segmentLabel", resolveSegmentLabel(segment));
            item.put("count", focusStudents.stream().filter(student -> segment.equals(student.get("segment"))).count());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildTeacherDiagnosisTrend(List<Long> classroomIds, String timeRange) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyList();
        }
        List<Long> studentIds = classroomMemberMapper.selectList(new LambdaQueryWrapper<ClassroomMember>()
                        .in(ClassroomMember::getClassroomId, classroomIds)
                        .eq(ClassroomMember::getRoleCode, "STUDENT"))
                .stream()
                .map(ClassroomMember::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(studentIds)) {
            return Collections.emptyList();
        }
        LocalDate startDate = resolveTimeRangeStartDate(timeRange);
        List<UserGrowthDiagnosisReport> reports = userGrowthDiagnosisReportMapper.selectList(new LambdaQueryWrapper<UserGrowthDiagnosisReport>()
                .in(UserGrowthDiagnosisReport::getUserId, studentIds)
                .ge(startDate != null, UserGrowthDiagnosisReport::getDiagnosisDate, startDate)
                .orderByAsc(UserGrowthDiagnosisReport::getDiagnosisDate));
        List<Map<String, Object>> result = new ArrayList<>();
        int days = trendDays(timeRange);
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<UserGrowthDiagnosisReport> dailyReports = reports.stream()
                    .filter(item -> date.equals(item.getDiagnosisDate()))
                    .collect(Collectors.toList());
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("studentCount", dailyReports.size());
            item.put("averageVitality", dailyReports.isEmpty() ? 0 : dailyReports.stream()
                    .map(UserGrowthDiagnosisReport::getVitalityScore)
                    .filter(Objects::nonNull)
                    .mapToDouble(java.math.BigDecimal::doubleValue)
                    .average()
                    .orElse(0));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildTeacherFocusStudents(List<Long> classroomIds, String segment, String followStatus, LocalDate startDate) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyList();
        }
        List<ClassroomMember> members = classroomMemberMapper.selectList(new LambdaQueryWrapper<ClassroomMember>()
                .in(ClassroomMember::getClassroomId, classroomIds)
                .eq(ClassroomMember::getRoleCode, "STUDENT"));
        if (CollectionUtils.isEmpty(members)) {
            return Collections.emptyList();
        }
        Map<Long, Classroom> classroomMap = classroomMapper.selectBatchIds(classroomIds).stream()
                .collect(Collectors.toMap(Classroom::getId, Function.identity(), (left, right) -> left));
        List<Long> userIds = members.stream().map(ClassroomMember::getUserId).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        Map<Long, UserStudyPlan> activePlanMap = userStudyPlanMapper.selectList(new LambdaQueryWrapper<UserStudyPlan>()
                        .in(UserStudyPlan::getUserId, userIds)
                        .eq(UserStudyPlan::getStatus, "ACTIVE")
                        .orderByDesc(UserStudyPlan::getId))
                .stream()
                .collect(Collectors.toMap(UserStudyPlan::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, UserGrowthDiagnosisReport> diagnosisMap = userGrowthDiagnosisReportMapper.selectList(new LambdaQueryWrapper<UserGrowthDiagnosisReport>()
                        .in(UserGrowthDiagnosisReport::getUserId, userIds)
                        .ge(startDate != null, UserGrowthDiagnosisReport::getDiagnosisDate, startDate)
                        .orderByDesc(UserGrowthDiagnosisReport::getDiagnosisDate)
                        .orderByDesc(UserGrowthDiagnosisReport::getId))
                .stream()
                .collect(Collectors.toMap(UserGrowthDiagnosisReport::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<String, TeacherGrowthFocusFollow> teacherFollowMap = teacherGrowthFocusFollowMapper.selectList(new LambdaQueryWrapper<TeacherGrowthFocusFollow>()
                        .in(TeacherGrowthFocusFollow::getClassroomId, classroomIds)
                        .orderByDesc(TeacherGrowthFocusFollow::getUpdatedAt)
                        .orderByDesc(TeacherGrowthFocusFollow::getId))
                .stream()
                .collect(Collectors.toMap(item -> buildTeacherFollowKey(item.getClassroomId(), item.getUserId(), item.getPlanId()), Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<String, AdminGrowthFocusFollow> adminFollowMap = adminGrowthFocusFollowMapper.selectList(new LambdaQueryWrapper<AdminGrowthFocusFollow>()
                        .in(AdminGrowthFocusFollow::getUserId, userIds)
                        .orderByDesc(AdminGrowthFocusFollow::getUpdatedAt)
                        .orderByDesc(AdminGrowthFocusFollow::getId))
                .stream()
                .collect(Collectors.toMap(item -> buildAdminFollowKey(item.getUserId(), item.getPlanId()), Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return members.stream()
                .map(member -> {
                    UserStudyPlan plan = activePlanMap.get(member.getUserId());
                    if (plan == null) {
                        return null;
                    }
                    UserGrowthDiagnosisReport diagnosis = diagnosisMap.get(member.getUserId());
                    TeacherGrowthFocusFollow teacherFollow = teacherFollowMap.get(buildTeacherFollowKey(member.getClassroomId(), member.getUserId(), plan.getId()));
                    AdminGrowthFocusFollow adminFollow = adminFollowMap.get(buildAdminFollowKey(member.getUserId(), plan.getId()));
                    SysUser student = userMap.get(member.getUserId());
                    Classroom classroom = classroomMap.get(member.getClassroomId());
                    String currentSegment = resolveSegment(diagnosis, plan);
                    Map<String, Object> map = new HashMap<>();
                    map.put("classroomId", member.getClassroomId());
                    map.put("classroomName", classroom == null ? "-" : classroom.getName());
                    map.put("userId", member.getUserId());
                    map.put("studentName", student == null ? "未知学员" : student.getNickname());
                    map.put("username", student == null ? "未知学员" : student.getUsername());
                    map.put("planId", plan.getId());
                    map.put("planName", plan.getPlanName());
                    map.put("segment", currentSegment);
                    map.put("segmentLabel", resolveSegmentLabel(currentSegment));
                    map.put("completionRate", diagnosis == null ? java.math.BigDecimal.ZERO : diagnosis.getPlanCompletionRate());
                    map.put("vitalityScore", diagnosis == null ? java.math.BigDecimal.ZERO : diagnosis.getVitalityScore());
                    map.put("knowledgeMasteryRate", diagnosis == null ? java.math.BigDecimal.ZERO : diagnosis.getKnowledgeMasteryRate());
                    map.put("testPaperAverageScore", diagnosis == null ? java.math.BigDecimal.ZERO : diagnosis.getTestPaperAverageScore());
                    map.put("weakKnowledgePoints", diagnosis == null ? new ArrayList<>() : splitCsv(diagnosis.getWeakKnowledgePoints()));
                    map.put("latestDiagnosisId", diagnosis == null ? null : diagnosis.getId());
                    map.put("diagnosisDate", diagnosis == null ? null : diagnosis.getDiagnosisDate());
                    map.put("teacherFollowStatus", teacherFollow == null ? "PENDING" : teacherFollow.getFollowStatus());
                    map.put("teacherFollowStatusLabel", resolveFollowStatusLabel(teacherFollow == null ? "PENDING" : teacherFollow.getFollowStatus()));
                    map.put("teacherPriorityLevel", teacherFollow == null ? "MEDIUM" : teacherFollow.getPriorityLevel());
                    map.put("teacherPriorityLevelLabel", resolvePriorityLabel(teacherFollow == null ? "MEDIUM" : teacherFollow.getPriorityLevel()));
                    map.put("teacherFollowRemark", teacherFollow == null ? null : teacherFollow.getRemark());
                    map.put("nextFollowUpAt", teacherFollow == null ? null : teacherFollow.getNextFollowUpAt());
                    map.put("teacherFollowUpdatedAt", teacherFollow == null ? null : teacherFollow.getUpdatedAt());
                    map.put("adminFollowStatus", adminFollow == null ? "PENDING" : adminFollow.getFollowStatus());
                    map.put("adminFollowStatusLabel", resolveFollowStatusLabel(adminFollow == null ? "PENDING" : adminFollow.getFollowStatus()));
                    String reminderStatus = resolveReminderStatus(teacherFollow == null ? null : teacherFollow.getNextFollowUpAt(), teacherFollow == null ? "PENDING" : teacherFollow.getFollowStatus());
                    map.put("reminderStatus", reminderStatus);
                    map.put("reminderStatusLabel", resolveReminderStatusLabel(reminderStatus));
                    return map;
                })
                .filter(Objects::nonNull)
                .filter(item -> !StringUtils.hasText(segment) || segment.equals(item.get("segment")))
                .filter(item -> !StringUtils.hasText(followStatus) || followStatus.equals(item.get("teacherFollowStatus")))
                .sorted((left, right) -> {
                    int segmentCompare = Integer.compare(segmentOrder(String.valueOf(left.get("segment"))), segmentOrder(String.valueOf(right.get("segment"))));
                    if (segmentCompare != 0) {
                        return segmentCompare;
                    }
                    return ((java.math.BigDecimal) left.get("vitalityScore")).compareTo((java.math.BigDecimal) right.get("vitalityScore"));
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTeacherClassroomBoards(List<Long> classroomIds, LocalDate startDate) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> focusStudents = buildTeacherFocusStudents(classroomIds, null, null, startDate);
        Map<Long, List<Map<String, Object>>> grouped = focusStudents.stream()
                .collect(Collectors.groupingBy(item -> (Long) item.get("classroomId"), LinkedHashMap::new, Collectors.toList()));
        List<Classroom> classrooms = classroomMapper.selectBatchIds(classroomIds);
        return classrooms.stream()
                .map(classroom -> {
                    List<Map<String, Object>> students = grouped.getOrDefault(classroom.getId(), Collections.emptyList());
                    Map<String, Object> item = new HashMap<>();
                    item.put("classroomId", classroom.getId());
                    item.put("classroomName", classroom.getName());
                    item.put("subjectName", classroom.getSubjectName());
                    item.put("memberCount", classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                            .eq(ClassroomMember::getClassroomId, classroom.getId())
                            .eq(ClassroomMember::getRoleCode, "STUDENT")));
                    item.put("focusStudentCount", students.size());
                    item.put("atRiskCount", students.stream().filter(student -> "AT_RISK".equals(student.get("segment"))).count());
                    item.put("pendingFollowCount", students.stream().filter(student -> "PENDING".equals(student.get("teacherFollowStatus"))).count());
                    item.put("doneFollowCount", students.stream().filter(student -> "DONE".equals(student.get("teacherFollowStatus"))).count());
                    item.put("averageCompletionRate", averageDecimal(students, "completionRate"));
                    item.put("averageVitalityScore", averageDecimal(students, "vitalityScore"));
                    return item;
                })
                .sorted((left, right) -> Integer.compare(((Long) right.get("atRiskCount")).intValue(), ((Long) left.get("atRiskCount")).intValue()))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTeacherFollowHistory(List<Long> classroomIds) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyList();
        }
        List<TeacherGrowthFocusFollowHistory> histories = teacherGrowthFocusFollowHistoryMapper.selectList(new LambdaQueryWrapper<TeacherGrowthFocusFollowHistory>()
                .in(TeacherGrowthFocusFollowHistory::getClassroomId, classroomIds)
                .orderByDesc(TeacherGrowthFocusFollowHistory::getCreatedAt)
                .orderByDesc(TeacherGrowthFocusFollowHistory::getId)
                .last("LIMIT 12"));
        if (CollectionUtils.isEmpty(histories)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = histories.stream().map(TeacherGrowthFocusFollowHistory::getUserId).collect(Collectors.toSet());
        Set<Long> operatorIds = histories.stream().map(TeacherGrowthFocusFollowHistory::getOperatorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        Map<Long, SysUser> operatorMap = operatorIds.isEmpty() ? Collections.emptyMap() : sysUserMapper.selectBatchIds(operatorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        Map<Long, Classroom> classroomMap = classroomMapper.selectBatchIds(classroomIds).stream()
                .collect(Collectors.toMap(Classroom::getId, Function.identity(), (left, right) -> left));
        return histories.stream().map(item -> {
            SysUser student = userMap.get(item.getUserId());
            SysUser operator = operatorMap.get(item.getOperatorId());
            Classroom classroom = classroomMap.get(item.getClassroomId());
            Map<String, Object> map = new HashMap<>();
            map.put("classroomName", classroom == null ? "-" : classroom.getName());
            map.put("studentName", student == null ? "未知学员" : student.getNickname());
            map.put("followStatusLabel", resolveFollowStatusLabel(item.getFollowStatus()));
            map.put("priorityLevelLabel", resolvePriorityLabel(item.getPriorityLevel()));
            map.put("remark", item.getRemark());
            map.put("nextFollowUpAt", item.getNextFollowUpAt());
            map.put("actionType", item.getActionType());
            map.put("operatorName", operator == null ? "系统" : operator.getNickname());
            map.put("createdAt", item.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> buildTeacherGrowthFilterState(Long classroomId, String segment, String followStatus, String timeRange, String subjectName, String taskType) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("classroomId", classroomId);
        filters.put("segment", segment);
        filters.put("followStatus", followStatus);
        filters.put("timeRange", normalizeTimeRange(timeRange));
        filters.put("subjectName", subjectName);
        filters.put("taskType", normalizeTaskType(taskType));
        return filters;
    }

    private Map<String, Object> buildTeacherReportFilterState(Long classroomId, String timeRange, String subjectName, String taskType) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("classroomId", classroomId);
        filters.put("timeRange", normalizeTimeRange(timeRange));
        filters.put("subjectName", subjectName);
        filters.put("taskType", normalizeTaskType(taskType));
        return filters;
    }

    private List<Long> filterClassroomIdsBySubject(List<Long> classroomIds, String subjectName) {
        if (CollectionUtils.isEmpty(classroomIds) || !StringUtils.hasText(subjectName)) {
            return classroomIds;
        }
        return classroomMapper.selectBatchIds(classroomIds).stream()
                .filter(classroom -> subjectName.equals(classroom.getSubjectName()))
                .map(Classroom::getId)
                .collect(Collectors.toList());
    }

    private String normalizeTimeRange(String timeRange) {
        if ("LAST_7_DAYS".equals(timeRange) || "SEMESTER".equals(timeRange) || "ALL".equals(timeRange)) {
            return timeRange;
        }
        return "LAST_30_DAYS";
    }

    private String normalizeTaskType(String taskType) {
        if ("HOMEWORK".equals(taskType) || "EXAM".equals(taskType)) {
            return taskType;
        }
        return "ALL";
    }

    private boolean includeHomework(String taskType) {
        return !"EXAM".equals(normalizeTaskType(taskType));
    }

    private boolean includeExam(String taskType) {
        return !"HOMEWORK".equals(normalizeTaskType(taskType));
    }

    private LocalDateTime resolveTimeRangeStart(String timeRange) {
        String normalized = normalizeTimeRange(timeRange);
        if ("LAST_7_DAYS".equals(normalized)) {
            return LocalDate.now().minusDays(6).atStartOfDay();
        }
        if ("LAST_30_DAYS".equals(normalized)) {
            return LocalDate.now().minusDays(29).atStartOfDay();
        }
        if ("SEMESTER".equals(normalized)) {
            return LocalDate.now().getMonthValue() >= 9
                    ? LocalDate.of(LocalDate.now().getYear(), 9, 1).atStartOfDay()
                    : LocalDate.of(LocalDate.now().getYear(), 3, 1).atStartOfDay();
        }
        return null;
    }

    private LocalDate resolveTimeRangeStartDate(String timeRange) {
        LocalDateTime startAt = resolveTimeRangeStart(timeRange);
        return startAt == null ? null : startAt.toLocalDate();
    }

    private int trendDays(String timeRange) {
        String normalized = normalizeTimeRange(timeRange);
        if ("LAST_7_DAYS".equals(normalized)) {
            return 7;
        }
        if ("ALL".equals(normalized) || "SEMESTER".equals(normalized)) {
            return 30;
        }
        return 30;
    }

    private Map<String, Object> buildTeacherReminderSummary(List<Long> classroomIds) {
        List<Map<String, Object>> reminders = buildTeacherFollowReminders(classroomIds);
        Map<String, Object> summary = new HashMap<>();
        summary.put("overdueCount", reminders.stream().filter(item -> "OVERDUE".equals(item.get("reminderStatus"))).count());
        summary.put("todayCount", reminders.stream().filter(item -> "TODAY".equals(item.get("reminderStatus"))).count());
        summary.put("upcomingCount", reminders.stream().filter(item -> "UPCOMING".equals(item.get("reminderStatus"))).count());
        summary.put("trackedCount", reminders.size());
        return summary;
    }

    private List<Map<String, Object>> buildTeacherFollowReminders(List<Long> classroomIds) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyList();
        }
        return buildTeacherFocusStudents(classroomIds, null, null, null).stream()
                .filter(item -> item.get("nextFollowUpAt") != null)
                .sorted((left, right) -> {
                    int statusCompare = Integer.compare(reminderStatusOrder(String.valueOf(left.get("reminderStatus"))), reminderStatusOrder(String.valueOf(right.get("reminderStatus"))));
                    if (statusCompare != 0) {
                        return statusCompare;
                    }
                    return ((LocalDateTime) left.get("nextFollowUpAt")).compareTo((LocalDateTime) right.get("nextFollowUpAt"));
                })
                .limit(12)
                .collect(Collectors.toList());
    }

    private TeacherGrowthDashboardPreference loadTeacherGrowthDashboardPreference(Long userId) {
        if (userId == null) {
            return null;
        }
        return teacherGrowthDashboardPreferenceMapper.selectOne(new LambdaQueryWrapper<TeacherGrowthDashboardPreference>()
                .eq(TeacherGrowthDashboardPreference::getUserId, userId)
                .last("LIMIT 1"));
    }

    private PageResult<Map<String, Object>> paginate(List<Map<String, Object>> records, Long pageNo, Long pageSize) {
        int fromIndex = Math.max(0, (int) ((pageNo - 1) * pageSize));
        int toIndex = Math.min(records.size(), fromIndex + pageSize.intValue());
        if (fromIndex >= records.size()) {
            return new PageResult<>((long) records.size(), pageNo, pageSize, Collections.emptyList());
        }
        return new PageResult<>((long) records.size(), pageNo, pageSize, records.subList(fromIndex, toIndex));
    }

    private Map<String, Object> buildTeacherReportSummary(List<Long> classroomIds, LocalDateTime startAt, String taskType) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("classroomCount", classroomIds.size());
        summary.put("memberCount", classroomIds.isEmpty() ? 0 : classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .in(ClassroomMember::getClassroomId, classroomIds)
                .eq(ClassroomMember::getRoleCode, "STUDENT")));
        summary.put("homeworkCount", classroomIds.isEmpty() || !includeHomework(taskType) ? 0 : classroomHomeworkMapper.selectCount(new LambdaQueryWrapper<ClassroomHomework>()
                .in(ClassroomHomework::getClassroomId, classroomIds)
                .ge(startAt != null, ClassroomHomework::getCreatedAt, startAt)));
        summary.put("examCount", classroomIds.isEmpty() || !includeExam(taskType) ? 0 : classroomExamTaskMapper.selectCount(new LambdaQueryWrapper<ClassroomExamTask>()
                .in(ClassroomExamTask::getClassroomId, classroomIds)
                .ge(startAt != null, ClassroomExamTask::getCreatedAt, startAt)));
        summary.put("pendingReviewCount", countPendingReviews(classroomIds, startAt, taskType));
        return summary;
    }

    private List<Map<String, Object>> buildHomeworkStats(List<Long> classroomIds, LocalDateTime startAt) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyList();
        }
        return classroomHomeworkMapper.selectList(new LambdaQueryWrapper<ClassroomHomework>()
                        .in(ClassroomHomework::getClassroomId, classroomIds)
                        .ge(startAt != null, ClassroomHomework::getCreatedAt, startAt)
                        .orderByDesc(ClassroomHomework::getCreatedAt))
                .stream()
                .map(homework -> {
                    List<ClassroomHomeworkSubmission> submissions = classroomHomeworkSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkSubmission>()
                            .eq(ClassroomHomeworkSubmission::getHomeworkId, homework.getId())
                            .eq(ClassroomHomeworkSubmission::getStatus, "SUBMITTED")
                            .ge(startAt != null, ClassroomHomeworkSubmission::getSubmittedAt, startAt));
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", homework.getId());
                    item.put("title", homework.getTitle());
                    item.put("classroomId", homework.getClassroomId());
                    item.put("submissionCount", submissions.size());
                    item.put("avgScore", submissions.isEmpty() ? 0 : submissions.stream().mapToInt(value -> value.getScore() == null ? 0 : value.getScore()).average().orElse(0));
                    return item;
                }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildExamStats(List<Long> classroomIds, LocalDateTime startAt) {
        if (CollectionUtils.isEmpty(classroomIds)) {
            return Collections.emptyList();
        }
        return classroomExamTaskMapper.selectList(new LambdaQueryWrapper<ClassroomExamTask>()
                        .in(ClassroomExamTask::getClassroomId, classroomIds)
                        .ge(startAt != null, ClassroomExamTask::getCreatedAt, startAt)
                        .orderByDesc(ClassroomExamTask::getCreatedAt))
                .stream()
                .map(task -> {
                    List<ClassroomExamSubmission> submissions = classroomExamSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomExamSubmission>()
                            .eq(ClassroomExamSubmission::getClassroomExamId, task.getId())
                            .eq(ClassroomExamSubmission::getStatus, "SUBMITTED")
                            .ge(startAt != null, ClassroomExamSubmission::getSubmittedAt, startAt));
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", task.getId());
                    item.put("title", task.getTitle());
                    item.put("classroomId", task.getClassroomId());
                    item.put("submissionCount", submissions.size());
                    item.put("avgScore", submissions.isEmpty() ? 0 : submissions.stream().mapToInt(value -> value.getScore() == null ? 0 : value.getScore()).average().orElse(0));
                    item.put("topScore", submissions.isEmpty() ? 0 : submissions.stream().mapToInt(value -> value.getScore() == null ? 0 : value.getScore()).max().orElse(0));
                    return item;
                }).collect(Collectors.toList());
    }

    private String resolveSegment(UserGrowthDiagnosisReport diagnosis, UserStudyPlan plan) {
        if (diagnosis != null && StringUtils.hasText(diagnosis.getDiagnosisStage())) {
            return diagnosis.getDiagnosisStage();
        }
        if (plan != null && plan.getCompletionRate() != null && plan.getCompletionRate().compareTo(java.math.BigDecimal.valueOf(70)) >= 0) {
            return "RISING";
        }
        return "TRACKING";
    }

    private String resolveSegmentLabel(String segment) {
        if ("STABLE".equals(segment)) {
            return "稳定冲刺";
        }
        if ("RISING".equals(segment)) {
            return "持续上升";
        }
        if ("TRACKING".equals(segment)) {
            return "跟踪补强";
        }
        return "重点预警";
    }

    private String resolveFollowStatusLabel(String followStatus) {
        if ("DONE".equals(followStatus)) {
            return "已跟进";
        }
        if ("IN_PROGRESS".equals(followStatus)) {
            return "跟进中";
        }
        return "待跟进";
    }

    private String resolvePriorityLabel(String priorityLevel) {
        if ("HIGH".equals(priorityLevel)) {
            return "高优先级";
        }
        if ("LOW".equals(priorityLevel)) {
            return "低优先级";
        }
        return "中优先级";
    }

    private int segmentOrder(String segment) {
        if ("AT_RISK".equals(segment)) {
            return 0;
        }
        if ("TRACKING".equals(segment)) {
            return 1;
        }
        if ("RISING".equals(segment)) {
            return 2;
        }
        return 3;
    }

    private String buildTeacherFollowKey(Long classroomId, Long userId, Long planId) {
        return classroomId + "_" + userId + "_" + (planId == null ? 0 : planId);
    }

    private String buildAdminFollowKey(Long userId, Long planId) {
        return userId + "_" + (planId == null ? 0 : planId);
    }

    private List<String> splitCsv(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return new ArrayList<>();
        }
        return Arrays.stream(rawValue.split("[,，]"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private void saveGrowthFocusFollowInternal(Long classroomId,
                                               Long userId,
                                               Long planId,
                                               Long latestDiagnosisId,
                                               String followStatus,
                                               String priorityLevel,
                                               String remark,
                                               LocalDateTime nextFollowUpAt,
                                               Long teacherId,
                                               String actionType) {
        getOwnedClassroom(classroomId, teacherId);
        ClassroomMember member = classroomMemberMapper.selectOne(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .eq(ClassroomMember::getUserId, userId)
                .eq(ClassroomMember::getRoleCode, "STUDENT")
                .last("LIMIT 1"));
        if (member == null) {
            throw new BizException("学员不在当前班级中");
        }
        UserStudyPlan plan = userStudyPlanMapper.selectById(planId);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new BizException("学习计划不存在");
        }
        TeacherGrowthFocusFollow follow = teacherGrowthFocusFollowMapper.selectOne(new LambdaQueryWrapper<TeacherGrowthFocusFollow>()
                .eq(TeacherGrowthFocusFollow::getClassroomId, classroomId)
                .eq(TeacherGrowthFocusFollow::getUserId, userId)
                .eq(TeacherGrowthFocusFollow::getPlanId, planId)
                .last("LIMIT 1"));
        if (follow == null) {
            follow = new TeacherGrowthFocusFollow();
            follow.setClassroomId(classroomId);
            follow.setUserId(userId);
            follow.setPlanId(planId);
            follow.setCreatedBy(teacherId);
        }
        follow.setLatestDiagnosisId(latestDiagnosisId);
        follow.setFollowStatus(followStatus);
        follow.setPriorityLevel(priorityLevel);
        follow.setRemark(remark);
        follow.setNextFollowUpAt(nextFollowUpAt);
        follow.setUpdatedBy(teacherId);
        if (follow.getId() == null) {
            teacherGrowthFocusFollowMapper.insert(follow);
        } else {
            teacherGrowthFocusFollowMapper.updateById(follow);
        }
        TeacherGrowthFocusFollowHistory history = new TeacherGrowthFocusFollowHistory();
        history.setTeacherFollowId(follow.getId());
        history.setClassroomId(classroomId);
        history.setUserId(userId);
        history.setPlanId(planId);
        history.setLatestDiagnosisId(latestDiagnosisId);
        history.setFollowStatus(followStatus);
        history.setPriorityLevel(priorityLevel);
        history.setRemark(remark);
        history.setNextFollowUpAt(nextFollowUpAt);
        history.setOperatorId(teacherId);
        history.setActionType(actionType);
        teacherGrowthFocusFollowHistoryMapper.insert(history);
    }

    private String resolveReminderStatus(LocalDateTime nextFollowUpAt, String followStatus) {
        if ("DONE".equals(followStatus)) {
            return "DONE";
        }
        if (nextFollowUpAt == null) {
            return "UNSCHEDULED";
        }
        LocalDateTime now = LocalDateTime.now();
        if (nextFollowUpAt.isBefore(now)) {
            return "OVERDUE";
        }
        if (nextFollowUpAt.toLocalDate().isEqual(now.toLocalDate())) {
            return "TODAY";
        }
        if (!nextFollowUpAt.isAfter(now.plusDays(7))) {
            return "UPCOMING";
        }
        return "SCHEDULED";
    }

    private String resolveReminderStatusLabel(String reminderStatus) {
        if ("DONE".equals(reminderStatus)) {
            return "已完成";
        }
        if ("OVERDUE".equals(reminderStatus)) {
            return "已逾期";
        }
        if ("TODAY".equals(reminderStatus)) {
            return "今日回访";
        }
        if ("UPCOMING".equals(reminderStatus)) {
            return "近 7 天";
        }
        if ("SCHEDULED".equals(reminderStatus)) {
            return "已排期";
        }
        return "待安排";
    }

    private int reminderStatusOrder(String reminderStatus) {
        if ("OVERDUE".equals(reminderStatus)) {
            return 0;
        }
        if ("TODAY".equals(reminderStatus)) {
            return 1;
        }
        if ("UPCOMING".equals(reminderStatus)) {
            return 2;
        }
        if ("SCHEDULED".equals(reminderStatus)) {
            return 3;
        }
        if ("UNSCHEDULED".equals(reminderStatus)) {
            return 4;
        }
        return 5;
    }

    private java.math.BigDecimal averageDecimal(List<Map<String, Object>> records, String field) {
        if (CollectionUtils.isEmpty(records)) {
            return java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        java.math.BigDecimal total = records.stream()
                .map(item -> (java.math.BigDecimal) item.get(field))
                .filter(Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return total.divide(java.math.BigDecimal.valueOf(records.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private java.math.BigDecimal averageScore(List<?> submissions) {
        if (CollectionUtils.isEmpty(submissions)) {
            return java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        double average = submissions.stream()
                .map(item -> item instanceof ClassroomHomeworkSubmission
                        ? ((ClassroomHomeworkSubmission) item).getScore()
                        : ((ClassroomExamSubmission) item).getScore())
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        return java.math.BigDecimal.valueOf(average).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private java.math.BigDecimal averageBigDecimal(List<java.math.BigDecimal> values) {
        if (CollectionUtils.isEmpty(values)) {
            return java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        java.math.BigDecimal total = values.stream().filter(Objects::nonNull).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return total.divide(java.math.BigDecimal.valueOf(values.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private java.math.BigDecimal toCorrectRate(Integer correctCount, Integer questionCount) {
        if (questionCount == null || questionCount == 0) {
            return java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return java.math.BigDecimal.valueOf(correctCount == null ? 0 : correctCount)
                .multiply(java.math.BigDecimal.valueOf(100))
                .divide(java.math.BigDecimal.valueOf(questionCount), 2, java.math.RoundingMode.HALF_UP);
    }

    private void incrementDistribution(List<DistributionCounter> counters, java.math.BigDecimal scoreRate, boolean homework) {
        double value = scoreRate == null ? 0 : scoreRate.doubleValue();
        DistributionCounter counter = value >= 90 ? counters.get(3) : value >= 80 ? counters.get(2) : value >= 60 ? counters.get(1) : counters.get(0);
        if (homework) {
            counter.homeworkCount++;
            return;
        }
        counter.examCount++;
    }

    private static class DistributionCounter {
        private final String bucket;
        private final String label;
        private long homeworkCount;
        private long examCount;

        private DistributionCounter(String bucket, String label) {
            this.bucket = bucket;
            this.label = label;
        }
    }
}
