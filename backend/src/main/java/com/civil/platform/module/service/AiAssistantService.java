package com.civil.platform.module.service;

import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.config.AiAssistantProperties;
import com.civil.platform.module.dto.AiAssistantChatRequest;
import com.civil.platform.module.entity.SystemConfig;
import com.civil.platform.module.mapper.SystemConfigMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAssistantService {

    private static final int MAX_RETRIEVE_ATTEMPTS = 120;
    private static final long RETRIEVE_INTERVAL_MILLIS = 1000L;

    private final AiAssistantProperties properties;
    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public AiAssistantService(AiAssistantProperties properties,
                              SystemConfigMapper systemConfigMapper,
                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.systemConfigMapper = systemConfigMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> chat(AiAssistantChatRequest request) {
        validateConfig();
        String conversationId = StringUtils.hasText(request.getConversationId())
                ? request.getConversationId().trim()
                : "";
        Map<String, Object> body = buildRequestBody(request.getMessage().trim());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(resolveApiKey());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    buildChatUrl(conversationId),
                    new HttpEntity<>(body, headers),
                    String.class);
            return buildResult(response.getBody(), conversationId, headers);
        } catch (RestClientException ex) {
            throw new BizException("公考小助手暂时不可用，请稍后再试");
        }
    }

    private Map<String, Object> buildRequestBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bot_id", resolveBotId());
        body.put("user_id", resolveUserId());
        body.put("stream", false);
        body.put("auto_save_history", true);

        List<Map<String, Object>> additionalMessages = new ArrayList<>();
        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content_type", "text");
        userMessage.put("content", message);
        additionalMessages.add(userMessage);
        body.put("additional_messages", additionalMessages);
        return body;
    }

    private String buildChatUrl(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return resolveApiUrl();
        }
        return UriComponentsBuilder.fromHttpUrl(resolveApiUrl())
                .queryParam("conversation_id", conversationId)
                .toUriString();
    }

    private String resolveUserId() {
        try {
            return "user-" + SecurityUtils.getUserId();
        } catch (Exception ignored) {
            return "guest";
        }
    }

    private Map<String, Object> buildResult(String rawBody, String fallbackConversationId, HttpHeaders headers) {
        JsonNode root = readJson(rawBody);
        assertUpstreamSuccess(root);

        JsonNode data = root.path("data");
        String conversationId = firstText(data.path("conversation_id"), root.path("conversation_id"), fallbackConversationId);
        String chatId = firstText(data.path("id"), data.path("chat_id"), root.path("chat_id"));

        String answer = extractAnswer(rawBody);
        if (!StringUtils.hasText(answer) && StringUtils.hasText(conversationId) && StringUtils.hasText(chatId)) {
            waitForCompletion(conversationId, chatId, headers);
            answer = fetchAnswer(conversationId, chatId, headers);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conversationId);
        result.put("answer", StringUtils.hasText(answer) ? answer.trim() : "小助手没有返回内容，请换个问法再试。");
        return result;
    }

    private void waitForCompletion(String conversationId, String chatId, HttpHeaders headers) {
        String status = "";
        for (int i = 0; i < MAX_RETRIEVE_ATTEMPTS; i++) {
            JsonNode data = retrieveChat(conversationId, chatId, headers).path("data");
            status = data.path("status").asText(status);
            if ("completed".equals(status)) {
                return;
            }
            if ("failed".equals(status) || "requires_action".equals(status) || "canceled".equals(status)) {
                throw new BizException("公考小助手暂时不可用，请稍后再试");
            }
            sleepBeforeRetry();
        }
        throw new BizException("公考小助手响应超时，请稍后再试");
    }

    private JsonNode retrieveChat(String conversationId, String chatId, HttpHeaders headers) {
        String url = UriComponentsBuilder.fromHttpUrl(resolveApiUrl() + "/retrieve")
                .queryParam("conversation_id", conversationId)
                .queryParam("chat_id", chatId)
                .toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        JsonNode root = readJson(response.getBody());
        assertUpstreamSuccess(root);
        return root;
    }

    private String fetchAnswer(String conversationId, String chatId, HttpHeaders headers) {
        String url = UriComponentsBuilder.fromHttpUrl(resolveApiUrl() + "/message/list")
                .queryParam("conversation_id", conversationId)
                .queryParam("chat_id", chatId)
                .toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        JsonNode root = readJson(response.getBody());
        assertUpstreamSuccess(root);
        return extractAnswerFromMessages(root.path("data"));
    }

    private String extractAnswerFromMessages(JsonNode messagesNode) {
        StringBuilder answer = new StringBuilder();
        StringBuilder fallback = new StringBuilder();
        JsonNode messages = messagesNode.isArray() ? messagesNode : messagesNode.path("messages");
        if (!messages.isArray()) {
            return "";
        }
        for (JsonNode message : messages) {
            String role = message.path("role").asText("");
            String type = message.path("type").asText("");
            String content = message.path("content").asText("");
            if (!StringUtils.hasText(content)) {
                continue;
            }
            if ("answer".equals(type)) {
                answer.append(content);
            } else if ("assistant".equals(role) && !"follow_up".equals(type) && !"verbose".equals(type)) {
                fallback.append(content);
            }
        }
        return answer.length() > 0 ? answer.toString() : fallback.toString();
    }

    private void assertUpstreamSuccess(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return;
        }
        JsonNode codeNode = root.path("code");
        if (!codeNode.isMissingNode() && codeNode.asInt(0) != 0) {
            String message = firstText(root.path("msg"), root.path("message"), "Coze 调用失败");
            throw new BizException(message);
        }
    }

    private String extractAnswer(String rawBody) {
        if (!StringUtils.hasText(rawBody)) {
            return "";
        }
        if (rawBody.contains("event:") && rawBody.contains("data:")) {
            return extractStreamAnswer(rawBody);
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String choiceAnswer = root.path("choices").path(0).path("message").path("content").asText("");
            if (StringUtils.hasText(choiceAnswer)) {
                return choiceAnswer;
            }
            String directAnswer = root.path("answer").asText("");
            if (StringUtils.hasText(directAnswer)) {
                return directAnswer;
            }
            JsonNode messages = root.path("messages");
            if (messages.isArray()) {
                for (JsonNode message : messages) {
                    String type = message.path("type").asText("");
                    String role = message.path("role").asText("");
                    if ("answer".equals(type) || "assistant".equals(role)) {
                        String content = message.path("content").asText("");
                        if (StringUtils.hasText(content)) {
                            return content;
                        }
                    }
                }
            }
            JsonNode dataMessages = root.path("data").path("messages");
            if (dataMessages.isArray()) {
                for (JsonNode message : dataMessages) {
                    String type = message.path("type").asText("");
                    String role = message.path("role").asText("");
                    if ("answer".equals(type) || "assistant".equals(role)) {
                        String content = message.path("content").asText("");
                        if (StringUtils.hasText(content)) {
                            return content;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return rawBody;
        }
        return "";
    }

    private String extractStreamAnswer(String rawBody) {
        StringBuilder answer = new StringBuilder();
        StringBuilder completedAnswer = new StringBuilder();
        String currentEvent = "";
        String[] lines = rawBody.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith("event:")) {
                currentEvent = line.substring("event:".length()).trim();
                continue;
            }
            if (!line.startsWith("data:")) {
                continue;
            }
            String data = line.substring("data:".length()).trim();
            if (!StringUtils.hasText(data) || "[DONE]".equals(data)) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(data);
                JsonNode message = node.path("message");
                String type = message.path("type").asText(node.path("type").asText(""));
                String content = message.path("content").asText(node.path("content").asText(""));
                if (!StringUtils.hasText(content) || !"answer".equals(type)) {
                    continue;
                }
                if (currentEvent.contains("message.delta")) {
                    answer.append(content);
                } else if (currentEvent.contains("message.completed")) {
                    completedAnswer.setLength(0);
                    completedAnswer.append(content);
                }
            } catch (Exception ignored) {
            }
        }
        return answer.length() > 0 ? answer.toString() : completedAnswer.toString();
    }

    private JsonNode readJson(String rawBody) {
        if (!StringUtils.hasText(rawBody) || rawBody.contains("event:")) {
            return objectMapper.missingNode();
        }
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            throw new BizException("公考小助手响应解析失败，请稍后再试");
        }
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            if (value instanceof JsonNode) {
                String text = ((JsonNode) value).asText("");
                if (StringUtils.hasText(text)) {
                    return text;
                }
            } else if (value instanceof String && StringUtils.hasText((String) value)) {
                return (String) value;
            }
        }
        return "";
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRIEVE_INTERVAL_MILLIS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("公考小助手响应中断，请稍后再试");
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(resolveApiUrl())
                || !StringUtils.hasText(resolveBotId())
                || !StringUtils.hasText(resolveApiKey())) {
            throw new BizException("公考小助手配置不完整");
        }
    }

    private String resolveApiUrl() {
        if (StringUtils.hasText(properties.getApiUrl())) {
            return normalizeChatApiUrl(properties.getApiUrl());
        }
        return normalizeChatApiUrl(resolveConfigValue("ai_assistant_api_url"));
    }

    private String resolveBotId() {
        if (StringUtils.hasText(properties.getBotId())) {
            return properties.getBotId();
        }
        return resolveConfigValue("ai_assistant_bot_id");
    }

    private String resolveApiKey() {
        if (StringUtils.hasText(properties.getApiKey())) {
            return properties.getApiKey();
        }
        return resolveConfigValue("ai_assistant_api_key");
    }

    private String resolveConfigValue(String configKey) {
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigGroup, "AI")
                .eq(SystemConfig::getConfigKey, configKey)
                .last("LIMIT 1"));
        return config == null ? "" : config.getConfigValue();
    }

    private String normalizeChatApiUrl(String apiUrl) {
        if (!StringUtils.hasText(apiUrl)) {
            return "";
        }
        String normalized = apiUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v3/chat")) {
            return normalized;
        }
        return normalized + "/v3/chat";
    }
}
