package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.entity.Classroom;
import com.civil.platform.module.entity.ClassroomExamSubmission;
import com.civil.platform.module.entity.ClassroomExamTask;
import com.civil.platform.module.entity.ClassroomHomework;
import com.civil.platform.module.entity.ClassroomHomeworkSubmission;
import com.civil.platform.module.entity.ClassroomJoinRequest;
import com.civil.platform.module.entity.ClassroomMaterial;
import com.civil.platform.module.entity.ClassroomMember;
import com.civil.platform.module.entity.SysFileRecord;
import com.civil.platform.module.mapper.ClassroomJoinRequestMapper;
import com.civil.platform.module.mapper.ClassroomMaterialMapper;
import com.civil.platform.module.mapper.ClassroomMapper;
import com.civil.platform.module.mapper.ClassroomExamSubmissionMapper;
import com.civil.platform.module.mapper.ClassroomExamTaskMapper;
import com.civil.platform.module.mapper.ClassroomHomeworkMapper;
import com.civil.platform.module.mapper.ClassroomHomeworkSubmissionMapper;
import com.civil.platform.module.mapper.ClassroomMemberMapper;
import com.civil.platform.module.mapper.SysFileRecordMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClassroomService {

    private final ClassroomMapper classroomMapper;
    private final ClassroomMemberMapper classroomMemberMapper;
    private final ClassroomJoinRequestMapper classroomJoinRequestMapper;
    private final ClassroomMaterialMapper classroomMaterialMapper;
    private final SysUserMapper sysUserMapper;
    private final ClassroomHomeworkMapper classroomHomeworkMapper;
    private final ClassroomHomeworkSubmissionMapper classroomHomeworkSubmissionMapper;
    private final ClassroomExamTaskMapper classroomExamTaskMapper;
    private final ClassroomExamSubmissionMapper classroomExamSubmissionMapper;
    private final SysFileRecordMapper sysFileRecordMapper;

    public ClassroomService(ClassroomMapper classroomMapper,
                            ClassroomMemberMapper classroomMemberMapper,
                            ClassroomJoinRequestMapper classroomJoinRequestMapper,
                            ClassroomMaterialMapper classroomMaterialMapper,
                            SysUserMapper sysUserMapper,
                            ClassroomHomeworkMapper classroomHomeworkMapper,
                            ClassroomHomeworkSubmissionMapper classroomHomeworkSubmissionMapper,
                            ClassroomExamTaskMapper classroomExamTaskMapper,
                            ClassroomExamSubmissionMapper classroomExamSubmissionMapper,
                            SysFileRecordMapper sysFileRecordMapper) {
        this.classroomMapper = classroomMapper;
        this.classroomMemberMapper = classroomMemberMapper;
        this.classroomJoinRequestMapper = classroomJoinRequestMapper;
        this.classroomMaterialMapper = classroomMaterialMapper;
        this.sysUserMapper = sysUserMapper;
        this.classroomHomeworkMapper = classroomHomeworkMapper;
        this.classroomHomeworkSubmissionMapper = classroomHomeworkSubmissionMapper;
        this.classroomExamTaskMapper = classroomExamTaskMapper;
        this.classroomExamSubmissionMapper = classroomExamSubmissionMapper;
        this.sysFileRecordMapper = sysFileRecordMapper;
    }

    public PageResult<Classroom> page(Long pageNo, Long pageSize, String keyword, String subjectName) {
        Page<Classroom> page = classroomMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getIsPublic, true)
                .eq(Classroom::getStatus, "OPEN")
                .like(StringUtils.hasText(keyword), Classroom::getName, keyword)
                .eq(StringUtils.hasText(subjectName), Classroom::getSubjectName, subjectName)
                .orderByDesc(Classroom::getMemberCount));
        return new PageResult<>(page.getTotal(), pageNo, pageSize, page.getRecords());
    }

    public Map<String, Object> detail(Long classroomId) {
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BizException(404, "班级不存在");
        }
        if ("DISMISSED".equals(classroom.getStatus())) {
            throw new BizException(404, "班级已解散");
        }
        List<Map<String, Object>> members = classroomMemberMapper.selectList(new LambdaQueryWrapper<ClassroomMember>()
                        .eq(ClassroomMember::getClassroomId, classroomId)
                        .orderByAsc(ClassroomMember::getJoinedAt))
                .stream().map(item -> {
                    Map<String, Object> member = new HashMap<>();
                    member.put("userId", item.getUserId());
                    member.put("roleCode", item.getRoleCode());
                    member.put("joinedAt", item.getJoinedAt());
                    member.put("nickname", sysUserMapper.selectById(item.getUserId()).getNickname());
                    return member;
                }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("classroom", classroom);
        result.put("members", members);
        result.put("currentUser", buildCurrentUserSnapshot(classroomId, classroom));
        result.put("materials", loadClassroomMaterials(classroomId));
        result.put("homeworks", loadHomeworkSummaries(classroomId));
        result.put("classroomExams", loadClassroomExamSummaries(classroomId));
        return result;
    }

    public void applyJoin(Long classroomId, String remark) {
        Long userId = SecurityUtils.getUserId();
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BizException(404, "班级不存在");
        }
        if (!"OPEN".equals(classroom.getStatus())) {
            throw new BizException("当前班级暂不接受入班申请");
        }
        ClassroomJoinRequest existing = classroomJoinRequestMapper.selectOne(new LambdaQueryWrapper<ClassroomJoinRequest>()
                .eq(ClassroomJoinRequest::getClassroomId, classroomId)
                .eq(ClassroomJoinRequest::getUserId, userId)
                .eq(ClassroomJoinRequest::getStatus, "PENDING"));
        if (existing != null) {
            throw new BizException("已提交过申请，请勿重复操作");
        }
        Long memberCount = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .eq(ClassroomMember::getRoleCode, "STUDENT"));
        if (memberCount >= classroom.getCapacity()) {
            throw new BizException("班级招生人数已满");
        }
        ClassroomJoinRequest request = new ClassroomJoinRequest();
        request.setClassroomId(classroomId);
        request.setUserId(userId);
        request.setStatus("PENDING");
        request.setRemark(remark);
        classroomJoinRequestMapper.insert(request);
    }

    private Map<String, Object> buildCurrentUserSnapshot(Long classroomId, Classroom classroom) {
        Map<String, Object> currentUser = new HashMap<>();
        if (SecurityUtils.getRoles().isEmpty()) {
            currentUser.put("loggedIn", false);
            currentUser.put("isMember", false);
            currentUser.put("roleCode", null);
            currentUser.put("joinStatus", null);
            return currentUser;
        }
        Long userId = SecurityUtils.getUserId();
        currentUser.put("loggedIn", true);
        currentUser.put("teacherOwner", userId.equals(classroom.getTeacherId()));
        ClassroomMember member = classroomMemberMapper.selectOne(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .eq(ClassroomMember::getUserId, userId));
        currentUser.put("isMember", member != null);
        currentUser.put("roleCode", member == null ? null : member.getRoleCode());
        ClassroomJoinRequest request = classroomJoinRequestMapper.selectOne(new LambdaQueryWrapper<ClassroomJoinRequest>()
                .eq(ClassroomJoinRequest::getClassroomId, classroomId)
                .eq(ClassroomJoinRequest::getUserId, userId)
                .orderByDesc(ClassroomJoinRequest::getId)
                .last("limit 1"));
        currentUser.put("joinStatus", request == null ? null : request.getStatus());
        return currentUser;
    }

    private List<Map<String, Object>> loadHomeworkSummaries(Long classroomId) {
        List<ClassroomHomework> homeworks = classroomHomeworkMapper.selectList(new LambdaQueryWrapper<ClassroomHomework>()
                .eq(ClassroomHomework::getClassroomId, classroomId)
                .eq(ClassroomHomework::getStatus, "PUBLISHED")
                .orderByDesc(ClassroomHomework::getCreatedAt));
        Long currentUserId = SecurityUtils.getRoles().isEmpty() ? null : SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        return homeworks.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("title", item.getTitle());
            map.put("description", item.getDescription());
            map.put("questionCount", item.getQuestionCount());
            map.put("totalScore", item.getTotalScore());
            map.put("deadlineAt", item.getDeadlineAt());
            boolean deadlineExpired = item.getDeadlineAt() != null && item.getDeadlineAt().isBefore(now);
            map.put("deadlineExpired", deadlineExpired);
            map.put("attachmentFileId", item.getAttachmentFileId());
            if (item.getAttachmentFileId() != null) {
                SysFileRecord file = sysFileRecordMapper.selectById(item.getAttachmentFileId());
                map.put("attachmentName", file == null ? null : file.getOriginalName());
                map.put("attachmentUrl", "/api/files/view/" + item.getAttachmentFileId());
            }
            if (currentUserId != null) {
                ClassroomHomeworkSubmission submission = classroomHomeworkSubmissionMapper.selectOne(new LambdaQueryWrapper<ClassroomHomeworkSubmission>()
                        .eq(ClassroomHomeworkSubmission::getHomeworkId, item.getId())
                        .eq(ClassroomHomeworkSubmission::getUserId, currentUserId)
                        .orderByDesc(ClassroomHomeworkSubmission::getId)
                        .last("limit 1"));
                map.put("submissionId", submission == null ? null : submission.getId());
                map.put("submissionStatus", submission == null ? null : submission.getStatus());
                map.put("reviewStatus", submission == null ? null : submission.getReviewStatus());
                map.put("score", submission == null ? null : submission.getScore());
            }
            map.put("allowStart", !deadlineExpired || "SUBMITTED".equals(map.get("submissionStatus")));
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> loadClassroomMaterials(Long classroomId) {
        return classroomMaterialMapper.selectList(new LambdaQueryWrapper<ClassroomMaterial>()
                        .eq(ClassroomMaterial::getClassroomId, classroomId)
                        .eq(ClassroomMaterial::getStatus, "ENABLED")
                        .orderByAsc(ClassroomMaterial::getSortNo)
                        .orderByDesc(ClassroomMaterial::getId))
                .stream()
                .map(item -> {
                    SysFileRecord file = sysFileRecordMapper.selectById(item.getFileId());
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

    private List<Map<String, Object>> loadClassroomExamSummaries(Long classroomId) {
        List<ClassroomExamTask> exams = classroomExamTaskMapper.selectList(new LambdaQueryWrapper<ClassroomExamTask>()
                .eq(ClassroomExamTask::getClassroomId, classroomId)
                .eq(ClassroomExamTask::getStatus, "PUBLISHED")
                .orderByDesc(ClassroomExamTask::getCreatedAt));
        Long currentUserId = SecurityUtils.getRoles().isEmpty() ? null : SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        return exams.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("title", item.getTitle());
            map.put("description", item.getDescription());
            map.put("questionCount", item.getQuestionCount());
            map.put("totalScore", item.getTotalScore());
            map.put("durationMinutes", item.getDurationMinutes());
            map.put("antiCheatEnabled", item.getAntiCheatEnabled());
            map.put("antiCheatNotice", item.getAntiCheatNotice());
            map.put("startTime", item.getStartTime());
            map.put("endTime", item.getEndTime());
            boolean notStarted = item.getStartTime() != null && item.getStartTime().isAfter(now);
            boolean ended = item.getEndTime() != null && item.getEndTime().isBefore(now);
            map.put("notStarted", notStarted);
            map.put("ended", ended);
            if (currentUserId != null) {
                ClassroomExamSubmission submission = classroomExamSubmissionMapper.selectOne(new LambdaQueryWrapper<ClassroomExamSubmission>()
                        .eq(ClassroomExamSubmission::getClassroomExamId, item.getId())
                        .eq(ClassroomExamSubmission::getUserId, currentUserId)
                        .orderByDesc(ClassroomExamSubmission::getId)
                        .last("limit 1"));
                map.put("submissionId", submission == null ? null : submission.getId());
                map.put("submissionStatus", submission == null ? null : submission.getStatus());
                map.put("reviewStatus", submission == null ? null : submission.getReviewStatus());
                map.put("score", submission == null ? null : submission.getScore());
            }
            map.put("allowStart", (!notStarted && !ended) || "SUBMITTED".equals(map.get("submissionStatus")));
            return map;
        }).collect(Collectors.toList());
    }
}

