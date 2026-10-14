package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.AdminBroadcastSendRequest;
import com.civil.platform.module.entity.SystemBroadcastMessage;
import com.civil.platform.module.entity.SystemConfig;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.mapper.SystemBroadcastMessageMapper;
import com.civil.platform.module.mapper.SystemConfigMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.websocket.GlobalBroadcastWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BroadcastService {

    private final SystemBroadcastMessageMapper systemBroadcastMessageMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final SysUserMapper sysUserMapper;
    private final GlobalBroadcastWebSocketHandler globalBroadcastWebSocketHandler;
    private final ObjectMapper objectMapper;

    public BroadcastService(SystemBroadcastMessageMapper systemBroadcastMessageMapper,
                            SystemConfigMapper systemConfigMapper,
                            SysUserMapper sysUserMapper,
                            GlobalBroadcastWebSocketHandler globalBroadcastWebSocketHandler,
                            ObjectMapper objectMapper) {
        this.systemBroadcastMessageMapper = systemBroadcastMessageMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.sysUserMapper = sysUserMapper;
        this.globalBroadcastWebSocketHandler = globalBroadcastWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> recentBroadcasts(int limit) {
        return systemBroadcastMessageMapper.selectList(new LambdaQueryWrapper<SystemBroadcastMessage>()
                        .orderByDesc(SystemBroadcastMessage::getSentAt)
                        .orderByDesc(SystemBroadcastMessage::getId))
                .stream()
                .limit(limit)
                .map(this::buildBroadcastView)
                .collect(Collectors.toList());
    }

    public Map<String, Object> pageBroadcasts(Long pageNo, Long pageSize, String keyword) {
        List<Map<String, Object>> all = systemBroadcastMessageMapper.selectList(new LambdaQueryWrapper<SystemBroadcastMessage>()
                        .orderByDesc(SystemBroadcastMessage::getSentAt)
                        .orderByDesc(SystemBroadcastMessage::getId))
                .stream()
                .map(this::buildBroadcastView)
                .filter(item -> !StringUtils.hasText(keyword)
                        || containsIgnoreCase(item.get("title"), keyword)
                        || containsIgnoreCase(item.get("content"), keyword)
                        || containsIgnoreCase(item.get("senderName"), keyword))
                .collect(Collectors.toList());
        int fromIndex = Math.max(0, Math.toIntExact((pageNo - 1) * pageSize));
        int toIndex = Math.min(all.size(), fromIndex + Math.toIntExact(pageSize));
        List<Map<String, Object>> records = fromIndex >= all.size() ? new ArrayList<>() : new ArrayList<>(all.subList(fromIndex, toIndex));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", (long) all.size());
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        result.put("records", records);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sendBroadcast(AdminBroadcastSendRequest request) {
        if (!resolveBroadcastEnabled()) {
            throw new BizException("广播功能已关闭");
        }
        Long senderId = SecurityUtils.getUserId();
        SysUser sender = sysUserMapper.selectById(senderId);
        SystemBroadcastMessage message = new SystemBroadcastMessage();
        message.setTitle(resolveBroadcastTitle(request.getTitle()));
        message.setContent(resolveBroadcastContent(request.getContent()));
        message.setScope(StringUtils.hasText(request.getScope()) ? request.getScope() : "ALL");
        message.setSenderId(senderId);
        message.setSenderName(sender == null ? "系统管理员" : sender.getNickname());
        message.setReceiverCount(globalBroadcastWebSocketHandler.getSessionCount());
        message.setSentAt(LocalDateTime.now());
        systemBroadcastMessageMapper.insert(message);
        Map<String, Object> payload = buildBroadcastView(message);
        payload.put("type", "GLOBAL_BROADCAST");
        try {
            globalBroadcastWebSocketHandler.broadcast(objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            throw new BizException("广播发送失败");
        }
        return payload;
    }

    public Map<String, Object> summary() {
        List<SystemBroadcastMessage> messages = systemBroadcastMessageMapper.selectList(new LambdaQueryWrapper<SystemBroadcastMessage>()
                .orderByDesc(SystemBroadcastMessage::getSentAt)
                .orderByDesc(SystemBroadcastMessage::getId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("broadcastCount", messages.size());
        result.put("onlineCount", globalBroadcastWebSocketHandler.getSessionCount());
        result.put("latestBroadcastAt", messages.isEmpty() ? null : messages.get(0).getSentAt());
        result.put("latestBroadcastTitle", messages.isEmpty() ? null : messages.get(0).getTitle());
        return result;
    }

    private boolean resolveBroadcastEnabled() {
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigGroup, "BROADCAST")
                .eq(SystemConfig::getConfigKey, "broadcast_enabled")
                .last("LIMIT 1"));
        return config == null || Boolean.parseBoolean(config.getConfigValue());
    }

    private String resolveBroadcastTitle(String title) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }
        return resolveConfigValue("broadcast_title", "系统学习提醒");
    }

    private String resolveBroadcastContent(String content) {
        if (StringUtils.hasText(content)) {
            return content.trim();
        }
        return resolveConfigValue("broadcast_content", "请及时完成今日学习任务并关注最新公告。");
    }

    private String resolveConfigValue(String configKey, String defaultValue) {
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigGroup, "BROADCAST")
                .eq(SystemConfig::getConfigKey, configKey)
                .last("LIMIT 1"));
        return config == null || !StringUtils.hasText(config.getConfigValue()) ? defaultValue : config.getConfigValue();
    }

    private Map<String, Object> buildBroadcastView(SystemBroadcastMessage message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", message.getId());
        item.put("title", message.getTitle());
        item.put("content", message.getContent());
        item.put("scope", message.getScope());
        item.put("senderId", message.getSenderId());
        item.put("senderName", message.getSenderName());
        item.put("receiverCount", message.getReceiverCount());
        item.put("sentAt", message.getSentAt());
        return item;
    }

    private boolean containsIgnoreCase(Object value, String keyword) {
        return value != null && String.valueOf(value).toLowerCase().contains(keyword.toLowerCase());
    }
}
