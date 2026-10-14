package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.config.AiAssistantProperties;
import com.civil.platform.config.EssayGraderProperties;
import com.civil.platform.module.dto.EssayAiReviewRequest;
import com.civil.platform.module.entity.PracticeAnswer;
import com.civil.platform.module.entity.PracticeRecord;
import com.civil.platform.module.entity.Question;
import com.civil.platform.module.entity.SystemConfig;
import com.civil.platform.module.mapper.PracticeAnswerMapper;
import com.civil.platform.module.mapper.PracticeRecordMapper;
import com.civil.platform.module.mapper.QuestionMapper;
import com.civil.platform.module.mapper.SystemConfigMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EssayAiReviewService {

    private static final Logger log = LoggerFactory.getLogger(EssayAiReviewService.class);
    private static final int MAX_RETRIEVE_ATTEMPTS = 90;
    private static final long RETRIEVE_INTERVAL_MILLIS = 1000L;

    private final EssayGraderProperties properties;
    private final AiAssistantProperties aiAssistantProperties;
    private final ObjectMapper objectMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeAnswerMapper practiceAnswerMapper;
    private final QuestionMapper questionMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public EssayAiReviewService(EssayGraderProperties properties,
                                AiAssistantProperties aiAssistantProperties,
                                ObjectMapper objectMapper,
                                PracticeRecordMapper practiceRecordMapper,
                                PracticeAnswerMapper practiceAnswerMapper,
                                QuestionMapper questionMapper,
                                SystemConfigMapper systemConfigMapper) {
        this.properties = properties;
        this.aiAssistantProperties = aiAssistantProperties;
        this.objectMapper = objectMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.practiceAnswerMapper = practiceAnswerMapper;
        this.questionMapper = questionMapper;
        this.systemConfigMapper = systemConfigMapper;
    }

    public Map<String, Object> review(Long practiceId, Long questionId, EssayAiReviewRequest request) {
        String answerText = request.getAnswerText() == null ? "" : request.getAnswerText().trim();
        if (!StringUtils.hasText(answerText)) {
            throw new BizException("请先输入答案");
        }

        PracticeRecord record = getOwnedPractice(practiceId);
        PracticeAnswer practiceAnswer = getPracticeAnswer(practiceId, questionId);
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BizException(404, "题目不存在");
        }
        if (!"申论".equals(record.getSubjectName()) && !"申论".equals(question.getSubjectName())) {
            throw new BizException("AI批改仅支持申论题目");
        }

        int maxScore = resolveMaxScore(question, practiceAnswer);
        if (!hasValidConfig()) {
            throw new BizException("申论AI批改老师配置不完整，请在后台系统设置中配置智能体参数");
        }
        try {
            String rawAnswer = callCoze(buildPrompt(record, question, answerText, maxScore));
            Map<String, Object> remote = parseReview(rawAnswer, maxScore);
            remote.put("reviewSource", "AI");
            return remote;
        } catch (BizException ex) {
            log.warn("AI essay review failed. practiceId={}, questionId={}, reason={}",
                    practiceId, questionId, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("AI essay review unexpected error. practiceId={}, questionId={}",
                    practiceId, questionId, ex);
            throw new BizException("AI批改老师调用异常，请稍后重试");
        }
    }

    private PracticeRecord getOwnedPractice(Long practiceId) {
        PracticeRecord record = practiceRecordMapper.selectById(practiceId);
        if (record == null) {
            throw new BizException(404, "练习不存在");
        }
        if (!record.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BizException(403, "无权查看该练习");
        }
        return record;
    }

    private PracticeAnswer getPracticeAnswer(Long practiceId, Long questionId) {
        PracticeAnswer answer = practiceAnswerMapper.selectOne(new LambdaQueryWrapper<PracticeAnswer>()
                .eq(PracticeAnswer::getPracticeRecordId, practiceId)
                .eq(PracticeAnswer::getQuestionId, questionId));
        if (answer == null) {
            throw new BizException("题目不属于本次练习");
        }
        return answer;
    }

    private int resolveMaxScore(Question question, PracticeAnswer practiceAnswer) {
        if (practiceAnswer.getQuestionScore() != null && practiceAnswer.getQuestionScore() > 0) {
            return practiceAnswer.getQuestionScore();
        }
        if (question.getScore() != null && question.getScore() > 0) {
            return question.getScore();
        }
        return 25;
    }

    private String buildPrompt(PracticeRecord record, Question question, String answerText, int maxScore) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是申论AI批改老师。请严格根据题目、要求和用户答案进行批改。\n");
        prompt.append("必须只返回一个JSON对象，不要使用Markdown代码块，不要输出额外说明。\n");
        prompt.append("JSON字段固定为：总分、内容要点分、结构层次分、逻辑论证分、语言表达分、格式规范分、等级、总体评价、优点、主要问题、提升建议、优化参考答案。\n");
        prompt.append("总分必须是0到").append(maxScore).append("之间的数字，可以保留1位小数；");
        prompt.append("等级只能是优秀、良好、一般、较差之一；");
        prompt.append("优点、主要问题、提升建议必须是字符串数组。\n\n");
        prompt.append("优化参考答案必须由你基于给定材料、题干要求和申论作答规范独立生成，");
        prompt.append("不得复述题库导入答案，不得声称来自原参考答案。\n\n");
        prompt.append("练习标题：").append(defaultText(record.getTitle(), "申论练习")).append("\n");
        prompt.append("专项：").append(defaultText(question.getCategoryName(), "综合")).append("\n");
        prompt.append("题型：").append(defaultText(question.getQuestionType(), "简答题")).append("\n");
        prompt.append("题目分值：").append(maxScore).append("分\n");
        prompt.append("给定材料：").append(defaultText(question.getMaterial(), "未提供")).append("\n");
        prompt.append("题干：").append(defaultText(question.getStem(), "未提供")).append("\n");
        prompt.append("用户答案：").append(answerText).append("\n");
        return prompt.toString();
    }

    private String callCoze(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(resolveApiKey());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    resolveApiUrl(),
                    new HttpEntity<Map<String, Object>>(buildRequestBody(prompt), headers),
                    String.class);
            return extractCozeAnswer(response.getBody(), headers);
        } catch (BizException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new BizException("AI批改老师暂时不可用，请稍后重试");
        }
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bot_id", resolveBotId());
        body.put("user_id", "user-" + SecurityUtils.getUserId());
        body.put("stream", false);
        body.put("auto_save_history", true);

        List<Map<String, Object>> additionalMessages = new ArrayList<>();
        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("type", "question");
        userMessage.put("content_type", "text");
        userMessage.put("content", prompt);
        additionalMessages.add(userMessage);
        body.put("additional_messages", additionalMessages);
        return body;
    }

    private String extractCozeAnswer(String rawBody, HttpHeaders headers) {
        JsonNode root = readJson(rawBody);
        assertUpstreamSuccess(root);

        String answer = extractDirectAnswer(rawBody);
        JsonNode data = root.path("data");
        String conversationId = firstText(data.path("conversation_id"), root.path("conversation_id"));
        String chatId = firstText(data.path("id"), data.path("chat_id"), root.path("chat_id"));
        if (!StringUtils.hasText(answer) && StringUtils.hasText(conversationId) && StringUtils.hasText(chatId)) {
            waitForCompletion(conversationId, chatId, headers);
            answer = fetchAnswer(conversationId, chatId, headers);
        }
        if (!StringUtils.hasText(answer)) {
            throw new BizException("AI批改老师没有返回内容，请稍后重试");
        }
        return answer.trim();
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
                throw new BizException("AI批改老师暂时不可用，请稍后重试");
            }
            sleepBeforeRetry();
        }
        throw new BizException("AI批改老师响应较慢，请稍后重试");
    }

    private JsonNode retrieveChat(String conversationId, String chatId, HttpHeaders headers) {
        String url = UriComponentsBuilder.fromHttpUrl(resolveApiUrl() + "/retrieve")
                .queryParam("conversation_id", conversationId)
                .queryParam("chat_id", chatId)
                .toUriString();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(headers), String.class);
        JsonNode root = readJson(response.getBody());
        assertUpstreamSuccess(root);
        return root;
    }

    private String fetchAnswer(String conversationId, String chatId, HttpHeaders headers) {
        String url = UriComponentsBuilder.fromHttpUrl(resolveApiUrl() + "/message/list")
                .queryParam("conversation_id", conversationId)
                .queryParam("chat_id", chatId)
                .toUriString();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(headers), String.class);
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

    private String extractDirectAnswer(String rawBody) {
        if (!StringUtils.hasText(rawBody)) {
            return "";
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
            JsonNode messages = root.path("data").path("messages");
            if (messages.isArray()) {
                return extractAnswerFromMessages(messages);
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private Map<String, Object> parseReview(String rawAnswer, int maxScore) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("maxScore", maxScore);
        result.put("rawText", rawAnswer);
        result.put("formatWarning", false);

        JsonNode node = readReviewJson(rawAnswer);
        if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
            result.put("score", null);
            result.put("level", "");
            result.put("comment", "AI解析格式异常，但原文已返回。");
            result.put("strengths", Collections.emptyList());
            result.put("problems", Collections.emptyList());
            result.put("improvements", Collections.emptyList());
            result.put("referenceAnswer", "");
            result.put("formatWarning", true);
            return result;
        }

        BigDecimal score = parseScore(firstNode(node, "score", "总分", "得分", "评分"), maxScore);
        result.put("score", score);
        result.put("contentScore", parseLooseScore(firstNode(node, "contentScore", "内容要点分")));
        result.put("structureScore", parseLooseScore(firstNode(node, "structureScore", "结构层次分")));
        result.put("logicScore", parseLooseScore(firstNode(node, "logicScore", "逻辑论证分")));
        result.put("languageScore", parseLooseScore(firstNode(node, "languageScore", "语言表达分")));
        result.put("formatScore", parseLooseScore(firstNode(node, "formatScore", "格式规范分")));
        result.put("level", firstNode(node, "level", "等级", "档次").asText(""));
        result.put("comment", firstNode(node, "comment", "总体评价", "总评", "评语").asText(""));
        result.put("strengths", toStringList(firstNode(node, "strengths", "优点", "亮点")));
        result.put("problems", toStringList(firstNode(node, "problems", "主要问题", "问题", "不足")));
        result.put("improvements", toStringList(firstNode(node, "improvements", "改进建议", "提升建议", "建议")));
        result.put("referenceAnswer", firstNode(node, "referenceAnswer", "优化参考答案", "参考答案", "参考作答").asText(""));
        return result;
    }

    private JsonNode firstNode(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return objectMapper.missingNode();
    }

    private JsonNode readReviewJson(String rawAnswer) {
        String text = rawAnswer == null ? "" : rawAnswer.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception ignored) {
            return objectMapper.missingNode();
        }
    }

    private BigDecimal parseScore(JsonNode scoreNode, int maxScore) {
        if (scoreNode == null || scoreNode.isMissingNode() || scoreNode.isNull()) {
            return null;
        }
        try {
            BigDecimal score = new BigDecimal(scoreNode.asText());
            if (score.compareTo(BigDecimal.ZERO) < 0) {
                score = BigDecimal.ZERO;
            }
            BigDecimal upper = BigDecimal.valueOf(maxScore);
            if (score.compareTo(upper) > 0) {
                score = upper;
            }
            return score.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal parseLooseScore(JsonNode scoreNode) {
        if (scoreNode == null || scoreNode.isMissingNode() || scoreNode.isNull()) {
            return null;
        }
        try {
            BigDecimal score = new BigDecimal(scoreNode.asText());
            if (score.compareTo(BigDecimal.ZERO) < 0) {
                score = BigDecimal.ZERO;
            }
            return score.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> toStringList(JsonNode node) {
        if (!node.isArray()) {
            return Collections.emptyList();
        }
        List<String> items = new ArrayList<>();
        for (JsonNode item : node) {
            String text = item.asText("");
            if (StringUtils.hasText(text)) {
                items.add(text);
            }
        }
        return items;
    }

    private JsonNode readJson(String rawBody) {
        if (!StringUtils.hasText(rawBody)) {
            return objectMapper.missingNode();
        }
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            throw new BizException("AI批改老师响应解析失败，请稍后重试");
        }
    }

    private void assertUpstreamSuccess(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return;
        }
        JsonNode codeNode = root.path("code");
        if (!codeNode.isMissingNode() && codeNode.asInt(0) != 0) {
            String message = firstText(root.path("msg"), root.path("message"), "AI批改老师调用失败");
            throw new BizException(message);
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

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRIEVE_INTERVAL_MILLIS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("AI批改老师响应中断，请稍后重试");
        }
    }

    private boolean hasValidConfig() {
        return StringUtils.hasText(resolveApiUrl())
                && StringUtils.hasText(resolveBotId())
                && StringUtils.hasText(resolveApiKey());
    }

    private String resolveApiUrl() {
        String configuredApiUrl = firstNonBlank(properties.getApiUrl(), aiAssistantProperties.getApiUrl());
        if (StringUtils.hasText(configuredApiUrl)) {
            return normalizeChatApiUrl(configuredApiUrl);
        }
        return normalizeChatApiUrl(resolveConfigValue("essay_grader_api_url"));
    }

    private String resolveBotId() {
        String configuredBotId = firstNonBlank(properties.getBotId(), aiAssistantProperties.getBotId());
        if (StringUtils.hasText(configuredBotId)) {
            return configuredBotId;
        }
        return resolveConfigValue("essay_grader_bot_id");
    }

    private String resolveApiKey() {
        String configuredApiKey = firstNonBlank(properties.getApiKey(), aiAssistantProperties.getApiKey());
        if (StringUtils.hasText(configuredApiKey)) {
            return configuredApiKey;
        }
        return resolveConfigValue("essay_grader_api_key");
    }

    private String resolveConfigValue(String configKey) {
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigGroup, "AI")
                .eq(SystemConfig::getConfigKey, configKey)
                .last("LIMIT 1"));
        return config == null ? "" : config.getConfigValue();
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
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
