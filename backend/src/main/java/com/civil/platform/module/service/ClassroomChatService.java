package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.ClassroomChatSendRequest;
import com.civil.platform.module.entity.Classroom;
import com.civil.platform.module.entity.ClassroomChatMessage;
import com.civil.platform.module.entity.ClassroomMember;
import com.civil.platform.module.entity.SysFileRecord;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.mapper.ClassroomChatMessageMapper;
import com.civil.platform.module.mapper.ClassroomMapper;
import com.civil.platform.module.mapper.ClassroomMemberMapper;
import com.civil.platform.module.mapper.SysFileRecordMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.websocket.ClassroomChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClassroomChatService {

    private final ClassroomMapper classroomMapper;
    private final ClassroomMemberMapper classroomMemberMapper;
    private final ClassroomChatMessageMapper classroomChatMessageMapper;
    private final SysUserMapper sysUserMapper;
    private final SysFileRecordMapper sysFileRecordMapper;
    private final ClassroomChatWebSocketHandler classroomChatWebSocketHandler;
    private final ObjectMapper objectMapper;

    public ClassroomChatService(ClassroomMapper classroomMapper,
                                ClassroomMemberMapper classroomMemberMapper,
                                ClassroomChatMessageMapper classroomChatMessageMapper,
                                SysUserMapper sysUserMapper,
                                SysFileRecordMapper sysFileRecordMapper,
                                ClassroomChatWebSocketHandler classroomChatWebSocketHandler,
                                ObjectMapper objectMapper) {
        this.classroomMapper = classroomMapper;
        this.classroomMemberMapper = classroomMemberMapper;
        this.classroomChatMessageMapper = classroomChatMessageMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysFileRecordMapper = sysFileRecordMapper;
        this.classroomChatWebSocketHandler = classroomChatWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    public PageResult<Map<String, Object>> pageMessages(Long classroomId, Long pageNo, Long pageSize) {
        ensureClassroomMember(classroomId, SecurityUtils.getUserId());
        Page<ClassroomChatMessage> page = classroomChatMessageMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<ClassroomChatMessage>()
                .eq(ClassroomChatMessage::getClassroomId, classroomId)
                .orderByDesc(ClassroomChatMessage::getId));
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(this::toView)
                .collect(Collectors.toList());
        Collections.reverse(records);
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sendMessage(Long classroomId, ClassroomChatSendRequest request) {
        if (request == null) {
            request = new ClassroomChatSendRequest();
        }
        Long userId = SecurityUtils.getUserId();
        ClassroomMember member = ensureClassroomMember(classroomId, userId);
        String messageType = StringUtils.hasText(request.getMessageType()) ? request.getMessageType().trim().toUpperCase() : "TEXT";
        if (!"TEXT".equals(messageType) && !"IMAGE".equals(messageType)) {
            throw new BizException("不支持的消息类型");
        }

        ClassroomChatMessage message = new ClassroomChatMessage();
        message.setClassroomId(classroomId);
        message.setSenderId(userId);
        message.setSenderRole(member.getRoleCode());
        message.setSenderName(resolveSenderName(userId));
        message.setMessageType(messageType);
        message.setCreatedAt(LocalDateTime.now());

        if ("IMAGE".equals(messageType)) {
            applyImageMessage(message, request.getFileId());
            message.setContent(StringUtils.hasText(request.getContent()) ? request.getContent().trim() : "");
        } else {
            String content = request.getContent() == null ? "" : request.getContent().trim();
            if (!StringUtils.hasText(content)) {
                throw new BizException("请输入消息内容");
            }
            message.setContent(content);
        }
        classroomChatMessageMapper.insert(message);
        Map<String, Object> view = toView(message);
        broadcastMessage(classroomId, view);
        return view;
    }

    public boolean canAccessClassroom(Long classroomId, Long userId) {
        if (classroomId == null || userId == null) {
            return false;
        }
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null || "DISMISSED".equals(classroom.getStatus())) {
            return false;
        }
        if (userId.equals(classroom.getTeacherId())) {
            return true;
        }
        return classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .eq(ClassroomMember::getUserId, userId)) > 0;
    }

    private ClassroomMember ensureClassroomMember(Long classroomId, Long userId) {
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null || "DISMISSED".equals(classroom.getStatus())) {
            throw new BizException(404, "班级不存在");
        }
        ClassroomMember member = classroomMemberMapper.selectOne(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .eq(ClassroomMember::getUserId, userId)
                .last("LIMIT 1"));
        if (member != null) {
            return member;
        }
        if (userId.equals(classroom.getTeacherId())) {
            ClassroomMember teacherMember = new ClassroomMember();
            teacherMember.setClassroomId(classroomId);
            teacherMember.setUserId(userId);
            teacherMember.setRoleCode("TEACHER");
            return teacherMember;
        }
        throw new BizException(403, "无权访问该班群");
    }

    private void applyImageMessage(ClassroomChatMessage message, Long fileId) {
        if (fileId == null) {
            throw new BizException("请先上传图片");
        }
        SysFileRecord file = sysFileRecordMapper.selectById(fileId);
        if (file == null) {
            throw new BizException("图片文件不存在");
        }
        if (!StringUtils.hasText(file.getFileType()) || !file.getFileType().toLowerCase().startsWith("image/")) {
            throw new BizException("仅支持发送图片文件");
        }
        message.setFileId(file.getId());
        message.setFileUrl("/api/files/view/" + file.getId());
        message.setFileName(file.getOriginalName());
        message.setFileType(file.getFileType());
        message.setFileSize(file.getFileSize());
    }

    private String resolveSenderName(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return SecurityUtils.getUsername();
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        return user.getUsername();
    }

    private void broadcastMessage(Long classroomId, Map<String, Object> message) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "CLASSROOM_CHAT_MESSAGE");
            payload.put("classroomId", classroomId);
            payload.put("message", message);
            classroomChatWebSocketHandler.broadcastToClassroom(classroomId, objectMapper.writeValueAsString(payload));
        } catch (Exception ignored) {
        }
    }

    private Map<String, Object> toView(ClassroomChatMessage message) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", message.getId());
        view.put("classroomId", message.getClassroomId());
        view.put("senderId", message.getSenderId());
        view.put("senderRole", message.getSenderRole());
        view.put("senderName", message.getSenderName());
        view.put("messageType", message.getMessageType());
        view.put("content", message.getContent());
        view.put("fileId", message.getFileId());
        view.put("fileUrl", message.getFileUrl());
        view.put("fileName", message.getFileName());
        view.put("fileType", message.getFileType());
        view.put("fileSize", message.getFileSize());
        view.put("createdAt", message.getCreatedAt());
        view.put("mine", message.getSenderId() != null && message.getSenderId().equals(SecurityUtils.getUserId()));
        return view;
    }
}
