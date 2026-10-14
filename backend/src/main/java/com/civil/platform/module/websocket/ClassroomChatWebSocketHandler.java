package com.civil.platform.module.websocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.module.entity.Classroom;
import com.civil.platform.module.entity.ClassroomMember;
import com.civil.platform.module.mapper.ClassroomMapper;
import com.civil.platform.module.mapper.ClassroomMemberMapper;
import com.civil.platform.security.CustomUserDetailsService;
import com.civil.platform.security.JwtTokenProvider;
import com.civil.platform.security.LoginUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ClassroomChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final ClassroomMapper classroomMapper;
    private final ClassroomMemberMapper classroomMemberMapper;
    private final Map<String, ClientContext> clients = new ConcurrentHashMap<>();
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> classroomSessions = new ConcurrentHashMap<>();

    public ClassroomChatWebSocketHandler(ObjectMapper objectMapper,
                                         JwtTokenProvider jwtTokenProvider,
                                         CustomUserDetailsService customUserDetailsService,
                                         ClassroomMapper classroomMapper,
                                         ClassroomMemberMapper classroomMemberMapper) {
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.classroomMapper = classroomMapper;
        this.classroomMemberMapper = classroomMemberMapper;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode payload = readPayload(message.getPayload());
        String type = payload.path("type").asText("");
        if ("PING".equalsIgnoreCase(message.getPayload()) || "PING".equals(type)) {
            send(session, "PONG", null);
            return;
        }
        if ("AUTH".equals(type)) {
            authenticate(session, payload.path("token").asText(""));
            return;
        }
        if ("SUBSCRIBE_CLASSROOM".equals(type)) {
            subscribeClassroom(session, payload.path("classroomId").asLong(0L));
            return;
        }
        send(session, "ERROR", "不支持的消息类型");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    public void broadcastToClassroom(Long classroomId, String payload) {
        CopyOnWriteArraySet<WebSocketSession> sessions = classroomSessions.get(classroomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                removeSession(session);
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException ex) {
                removeSession(session);
            }
        }
    }

    private void authenticate(WebSocketSession session, String rawToken) throws IOException {
        String token = rawToken == null ? "" : rawToken.replaceFirst("(?i)^Bearer\\s+", "").trim();
        if (!StringUtils.hasText(token)) {
            send(session, "ERROR", "请先登录");
            return;
        }
        try {
            String username = jwtTokenProvider.getUsername(token);
            LoginUser loginUser = customUserDetailsService.loadLoginUserByUsername(username);
            ClientContext context = new ClientContext(loginUser.getUserId(), loginUser.getUsername());
            clients.put(session.getId(), context);
            send(session, "AUTH_OK", null);
        } catch (RuntimeException ex) {
            clients.remove(session.getId());
            send(session, "ERROR", "登录状态无效");
        }
    }

    private void subscribeClassroom(WebSocketSession session, Long classroomId) throws IOException {
        ClientContext context = clients.get(session.getId());
        if (context == null) {
            send(session, "ERROR", "请先登录");
            return;
        }
        if (!canAccessClassroom(classroomId, context.userId)) {
            send(session, "ERROR", "无权访问该班群");
            return;
        }
        context.classroomIds.add(classroomId);
        classroomSessions.computeIfAbsent(classroomId, ignored -> new CopyOnWriteArraySet<>()).add(session);
        send(session, "SUBSCRIBED", String.valueOf(classroomId));
    }

    private boolean canAccessClassroom(Long classroomId, Long userId) {
        if (classroomId == null || classroomId <= 0 || userId == null) {
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

    private JsonNode readPayload(String rawPayload) {
        if (!StringUtils.hasText(rawPayload)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(rawPayload);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private void send(WebSocketSession session, String type, String message) throws IOException {
        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("type", type);
        if (StringUtils.hasText(message)) {
            payload.put("message", message);
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void removeSession(WebSocketSession session) {
        ClientContext context = clients.remove(session.getId());
        if (context != null) {
            for (Long classroomId : context.classroomIds) {
                CopyOnWriteArraySet<WebSocketSession> sessions = classroomSessions.get(classroomId);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        classroomSessions.remove(classroomId);
                    }
                }
            }
        } else {
            classroomSessions.values().forEach(sessions -> sessions.remove(session));
        }
    }

    private static class ClientContext {
        private final Long userId;
        private final String username;
        private final Set<Long> classroomIds = new CopyOnWriteArraySet<>();

        private ClientContext(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }
    }
}
