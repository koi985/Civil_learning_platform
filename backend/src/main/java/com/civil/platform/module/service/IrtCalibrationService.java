package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.module.entity.ExamAnswer;
import com.civil.platform.module.entity.ExamRecord;
import com.civil.platform.module.entity.PracticeAnswer;
import com.civil.platform.module.entity.PracticeRecord;
import com.civil.platform.module.entity.Question;
import com.civil.platform.module.entity.QuestionIrtStat;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.entity.UserAbilityProfile;
import com.civil.platform.module.entity.UserKnowledgeAbility;
import com.civil.platform.module.entity.UserKnowledgeMastery;
import com.civil.platform.module.mapper.ExamAnswerMapper;
import com.civil.platform.module.mapper.ExamRecordMapper;
import com.civil.platform.module.mapper.PracticeAnswerMapper;
import com.civil.platform.module.mapper.PracticeRecordMapper;
import com.civil.platform.module.mapper.QuestionIrtStatMapper;
import com.civil.platform.module.mapper.QuestionMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.UserAbilityProfileMapper;
import com.civil.platform.module.mapper.UserKnowledgeAbilityMapper;
import com.civil.platform.module.mapper.UserKnowledgeMasteryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IrtCalibrationService {

    public static final String CURRENT_VERSION = "rasch-v1";
    private static final int QUESTION_MIN_SAMPLE = 30;
    private static final int KNOWLEDGE_MIN_SAMPLE = 5;
    private static final double LEARNING_RATE = 0.08d;
    private static final int ITERATIONS = 4;

    private final QuestionMapper questionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeAnswerMapper practiceAnswerMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final UserKnowledgeMasteryMapper userKnowledgeMasteryMapper;
    private final SysUserMapper sysUserMapper;
    private final QuestionIrtStatMapper questionIrtStatMapper;
    private final UserAbilityProfileMapper userAbilityProfileMapper;
    private final UserKnowledgeAbilityMapper userKnowledgeAbilityMapper;

    public IrtCalibrationService(QuestionMapper questionMapper,
                                 PracticeRecordMapper practiceRecordMapper,
                                 PracticeAnswerMapper practiceAnswerMapper,
                                 ExamRecordMapper examRecordMapper,
                                 ExamAnswerMapper examAnswerMapper,
                                 UserKnowledgeMasteryMapper userKnowledgeMasteryMapper,
                                 SysUserMapper sysUserMapper,
                                 QuestionIrtStatMapper questionIrtStatMapper,
                                 UserAbilityProfileMapper userAbilityProfileMapper,
                                 UserKnowledgeAbilityMapper userKnowledgeAbilityMapper) {
        this.questionMapper = questionMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.practiceAnswerMapper = practiceAnswerMapper;
        this.examRecordMapper = examRecordMapper;
        this.examAnswerMapper = examAnswerMapper;
        this.userKnowledgeMasteryMapper = userKnowledgeMasteryMapper;
        this.sysUserMapper = sysUserMapper;
        this.questionIrtStatMapper = questionIrtStatMapper;
        this.userAbilityProfileMapper = userAbilityProfileMapper;
        this.userKnowledgeAbilityMapper = userKnowledgeAbilityMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshAll() {
        CalibrationContext context = buildContext(null);
        upsertQuestionStats(context);
        for (Long userId : context.answerEventsByUser.keySet()) {
            upsertUserAbilities(userId, context);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshUser(Long userId) {
        if (userId == null) {
            return;
        }
        CalibrationContext context = buildContext(Collections.singleton(userId));
        if (questionIrtStatMapper.selectCount(null) == 0) {
            upsertQuestionStats(context);
        }
        upsertUserAbilities(userId, context);
    }

    public Map<Long, BigDecimal> loadDifficultyMap() {
        Map<Long, BigDecimal> stats = questionIrtStatMapper.selectList(null).stream()
                .collect(Collectors.toMap(QuestionIrtStat::getQuestionId,
                        item -> defaultScale(item.getDifficultyB()),
                        (left, right) -> right));
        if (stats.isEmpty()) {
            List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                    .eq(Question::getEnabled, true));
            for (Question question : questions) {
                stats.put(question.getId(), fallbackDifficulty(question.getDifficultyLevel()));
            }
        }
        return stats;
    }

    private CalibrationContext buildContext(Set<Long> userFilter) {
        CalibrationContext context = new CalibrationContext();
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true));
        context.questionMap = questions.stream().collect(Collectors.toMap(Question::getId, item -> item, (left, right) -> left));
        context.masteryByUser = userKnowledgeMasteryMapper.selectList(userFilter == null || userFilter.isEmpty()
                        ? new LambdaQueryWrapper<>()
                        : new LambdaQueryWrapper<UserKnowledgeMastery>().in(UserKnowledgeMastery::getUserId, userFilter))
                .stream()
                .collect(Collectors.groupingBy(UserKnowledgeMastery::getUserId, LinkedHashMap::new, Collectors.toList()));

        List<PracticeRecord> practiceRecords = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getStatus, "SUBMITTED")
                .in(userFilter != null && !userFilter.isEmpty(), PracticeRecord::getUserId, userFilter));
        Map<Long, PracticeRecord> practiceRecordMap = practiceRecords.stream()
                .collect(Collectors.toMap(PracticeRecord::getId, item -> item, (left, right) -> left));
        if (!practiceRecordMap.isEmpty()) {
            List<PracticeAnswer> practiceAnswers = practiceAnswerMapper.selectList(new LambdaQueryWrapper<PracticeAnswer>()
                    .in(PracticeAnswer::getPracticeRecordId, practiceRecordMap.keySet()));
            for (PracticeAnswer answer : practiceAnswers) {
                PracticeRecord record = practiceRecordMap.get(answer.getPracticeRecordId());
                if (record == null) {
                    continue;
                }
                Question question = context.questionMap.get(answer.getQuestionId());
                if (!isObjectiveQuestion(question)) {
                    continue;
                }
                context.add(record.getUserId(), question, Boolean.TRUE.equals(answer.getIsCorrect()),
                        answer.getCreatedAt(), record.getSubmittedAt(), record.getId(), "PRACTICE");
            }
        }

        List<ExamRecord> examRecords = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStatus, "SUBMITTED")
                .in(userFilter != null && !userFilter.isEmpty(), ExamRecord::getUserId, userFilter));
        Map<Long, ExamRecord> examRecordMap = examRecords.stream()
                .collect(Collectors.toMap(ExamRecord::getId, item -> item, (left, right) -> left));
        if (!examRecordMap.isEmpty()) {
            List<ExamAnswer> examAnswers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                    .in(ExamAnswer::getExamRecordId, examRecordMap.keySet()));
            for (ExamAnswer answer : examAnswers) {
                ExamRecord record = examRecordMap.get(answer.getExamRecordId());
                if (record == null) {
                    continue;
                }
                Question question = context.questionMap.get(answer.getQuestionId());
                if (!isObjectiveQuestion(question)) {
                    continue;
                }
                context.add(record.getUserId(), question, Boolean.TRUE.equals(answer.getIsCorrect()),
                        answer.getCreatedAt(), record.getSubmittedAt(), record.getId(), "EXAM");
            }
        }
        return context;
    }

    private void upsertQuestionStats(CalibrationContext context) {
        Map<Long, List<AnswerEvent>> byQuestion = context.answerEventsByQuestion;
        Map<Long, QuestionIrtStat> existing = questionIrtStatMapper.selectList(null).stream()
                .collect(Collectors.toMap(QuestionIrtStat::getQuestionId, item -> item, (left, right) -> left));
        LocalDateTime now = LocalDateTime.now();
        for (Question question : context.questionMap.values()) {
            List<AnswerEvent> answers = byQuestion.getOrDefault(question.getId(), Collections.emptyList());
            int sampleCount = answers.size();
            int correctCount = (int) answers.stream().filter(item -> item.correct).count();
            BigDecimal difficulty = fallbackDifficulty(question.getDifficultyLevel());
            BigDecimal correctRate = sampleCount == 0
                    ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(correctCount).divide(BigDecimal.valueOf(sampleCount), 4, RoundingMode.HALF_UP);
            if (sampleCount >= QUESTION_MIN_SAMPLE) {
                double p = (correctCount + 0.5d) / (sampleCount + 1.0d);
                double logit = Math.log((1.0d - p) / p);
                difficulty = clamp(logit);
            }
            QuestionIrtStat stat = existing.getOrDefault(question.getId(), new QuestionIrtStat());
            stat.setQuestionId(question.getId());
            stat.setSubjectName(question.getSubjectName());
            stat.setKnowledgePoints(question.getKnowledgePoints());
            stat.setDifficultyB(difficulty);
            stat.setSampleCount(sampleCount);
            stat.setCorrectRate(correctRate);
            stat.setCalibratedAt(now);
            stat.setCalibrationVersion(CURRENT_VERSION);
            if (stat.getId() == null) {
                questionIrtStatMapper.insert(stat);
            } else {
                questionIrtStatMapper.updateById(stat);
            }
        }
    }

    private void upsertUserAbilities(Long userId, CalibrationContext context) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return;
        }
        List<AnswerEvent> answers = new ArrayList<>(context.answerEventsByUser.getOrDefault(userId, Collections.emptyList()));
        answers.sort(Comparator.comparing(item -> item.occurredAt == null ? LocalDateTime.MIN : item.occurredAt));
        Map<Long, BigDecimal> difficultyMap = loadDifficultyMap();
        List<UserKnowledgeMastery> masteries = context.masteryByUser.getOrDefault(userId, Collections.emptyList());
        Map<String, UserKnowledgeMastery> masteryMap = masteries.stream()
                .filter(item -> StringUtils.hasText(item.getKnowledgePoint()))
                .collect(Collectors.toMap(item -> abilityKey(item.getSubjectName(), item.getKnowledgePoint()),
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new));

        double overallTheta = iterateTheta(answers, difficultyMap, masterySeed(masteries));
        double preferredTheta = iterateTheta(filterBySubject(answers, preferredSubject(user, answers)),
                difficultyMap, subjectSeed(masteries, preferredSubject(user, answers), overallTheta));
        int distinctQuestions = (int) answers.stream().map(item -> item.questionId).filter(Objects::nonNull).distinct().count();
        int activeDays = (int) answers.stream()
                .map(item -> item.occurredAt == null ? null : item.occurredAt.toLocalDate())
                .filter(Objects::nonNull)
                .distinct()
                .count();
        BigDecimal confidence = abilityConfidence(answers.size(), distinctQuestions, activeDays);

        UserAbilityProfile profile = userAbilityProfileMapper.selectOne(new LambdaQueryWrapper<UserAbilityProfile>()
                .eq(UserAbilityProfile::getUserId, userId)
                .last("limit 1"));
        if (profile == null) {
            profile = new UserAbilityProfile();
            profile.setUserId(userId);
        }
        profile.setOverallTheta(decimal(overallTheta));
        profile.setPreferredSubjectTheta(decimal(preferredTheta));
        profile.setAbilityConfidence(confidence);
        profile.setCalibratedAt(LocalDateTime.now());
        profile.setCalibrationVersion(CURRENT_VERSION);
        if (profile.getId() == null) {
            userAbilityProfileMapper.insert(profile);
        } else {
            userAbilityProfileMapper.updateById(profile);
        }

        userKnowledgeAbilityMapper.delete(new LambdaQueryWrapper<UserKnowledgeAbility>()
                .eq(UserKnowledgeAbility::getUserId, userId));
        for (Map.Entry<String, UserKnowledgeMastery> entry : masteryMap.entrySet()) {
            UserKnowledgeMastery mastery = entry.getValue();
            List<AnswerEvent> knowledgeAnswers = filterByKnowledge(answers, mastery.getSubjectName(), mastery.getKnowledgePoint());
            double seed = masteryScoreSeed(mastery.getMasteryScore());
            double theta = knowledgeAnswers.isEmpty() ? seed : iterateTheta(knowledgeAnswers, difficultyMap, seed);
            BigDecimal thetaConfidence = knowledgeConfidence(knowledgeAnswers);
            UserKnowledgeAbility ability = new UserKnowledgeAbility();
            ability.setUserId(userId);
            ability.setSubjectName(mastery.getSubjectName());
            ability.setKnowledgePoint(mastery.getKnowledgePoint());
            ability.setTheta(decimal(theta));
            ability.setThetaConfidence(thetaConfidence);
            ability.setMasteryScoreSnapshot(defaultScale(mastery.getMasteryScore()));
            ability.setCalibratedAt(LocalDateTime.now());
            ability.setCalibrationVersion(CURRENT_VERSION);
            userKnowledgeAbilityMapper.insert(ability);
        }
    }

    private String preferredSubject(SysUser user, List<AnswerEvent> answers) {
        if (StringUtils.hasText(user.getPreferredSubject())) {
            return user.getPreferredSubject();
        }
        return answers.stream()
                .filter(item -> StringUtils.hasText(item.subjectName))
                .collect(Collectors.groupingBy(item -> item.subjectName, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("综合");
    }

    private List<AnswerEvent> filterBySubject(List<AnswerEvent> answers, String subjectName) {
        if (!StringUtils.hasText(subjectName)) {
            return answers;
        }
        return answers.stream()
                .filter(item -> subjectName.equals(item.subjectName))
                .collect(Collectors.toList());
    }

    private List<AnswerEvent> filterByKnowledge(List<AnswerEvent> answers, String subjectName, String knowledgePoint) {
        return answers.stream()
                .filter(item -> (!StringUtils.hasText(subjectName) || subjectName.equals(item.subjectName))
                        && item.knowledgePoints.contains(knowledgePoint))
                .collect(Collectors.toList());
    }

    private double iterateTheta(List<AnswerEvent> answers, Map<Long, BigDecimal> difficultyMap, double seed) {
        if (answers.isEmpty()) {
            return seed;
        }
        double theta = seed;
        for (int i = 0; i < ITERATIONS; i++) {
            for (AnswerEvent answer : answers) {
                double b = difficultyMap.getOrDefault(answer.questionId, BigDecimal.ZERO).doubleValue();
                double probability = sigmoid(theta - b);
                theta += LEARNING_RATE * ((answer.correct ? 1.0d : 0.0d) - probability);
            }
            theta = Math.max(-3.0d, Math.min(3.0d, theta));
        }
        return theta;
    }

    private BigDecimal abilityConfidence(int attempts, int distinctQuestions, int activeDays) {
        double score = Math.min(attempts, 60) / 60.0d * 55.0d
                + Math.min(distinctQuestions, 30) / 30.0d * 25.0d
                + Math.min(activeDays, 14) / 14.0d * 20.0d;
        return decimal(score);
    }

    private BigDecimal knowledgeConfidence(List<AnswerEvent> answers) {
        int attempts = answers.size();
        int sessions = (int) answers.stream().map(item -> item.sessionKey).filter(StringUtils::hasText).distinct().count();
        long recentCount = answers.stream()
                .filter(item -> item.occurredAt != null && !item.occurredAt.toLocalDate().isBefore(LocalDate.now().minusDays(30)))
                .count();
        double score = Math.min(attempts, 20) / 20.0d * 55.0d
                + Math.min(sessions, 8) / 8.0d * 25.0d
                + Math.min(recentCount, 10) / 10.0d * 20.0d;
        if (attempts < KNOWLEDGE_MIN_SAMPLE) {
            score = Math.min(score, 45.0d);
        }
        return decimal(score);
    }

    private double masterySeed(List<UserKnowledgeMastery> masteries) {
        if (masteries.isEmpty()) {
            return 0.0d;
        }
        return masteries.stream()
                .map(UserKnowledgeMastery::getMasteryScore)
                .filter(Objects::nonNull)
                .mapToDouble(this::masteryScoreSeed)
                .average()
                .orElse(0.0d);
    }

    private double subjectSeed(List<UserKnowledgeMastery> masteries, String subjectName, double fallback) {
        return masteries.stream()
                .filter(item -> subjectName.equals(item.getSubjectName()))
                .map(UserKnowledgeMastery::getMasteryScore)
                .filter(Objects::nonNull)
                .mapToDouble(this::masteryScoreSeed)
                .average()
                .orElse(fallback);
    }

    private double masteryScoreSeed(BigDecimal masteryScore) {
        if (masteryScore == null) {
            return 0.0d;
        }
        return (masteryScore.doubleValue() - 50.0d) / 20.0d;
    }

    private BigDecimal fallbackDifficulty(String difficultyLevel) {
        String text = StringUtils.hasText(difficultyLevel) ? difficultyLevel.toLowerCase(Locale.ROOT) : "";
        if (text.contains("难") || text.contains("hard")) {
            return decimal(1.0d);
        }
        if (text.contains("易") || text.contains("simple") || text.contains("easy")) {
            return decimal(-1.0d);
        }
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    private boolean isObjectiveQuestion(Question question) {
        if (question == null || !Boolean.TRUE.equals(question.getEnabled())) {
            return false;
        }
        String subject = safe(question.getSubjectName());
        String type = safe(question.getQuestionType());
        return !subject.contains("申论")
                && !type.contains("主观")
                && !type.contains("写作")
                && !type.contains("简答")
                && !type.contains("论述");
    }

    private String abilityKey(String subjectName, String knowledgePoint) {
        return safe(subjectName) + "::" + safe(knowledgePoint);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultScale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal clamp(double value) {
        return decimal(Math.max(-3.0d, Math.min(3.0d, value)));
    }

    private double sigmoid(double value) {
        return 1.0d / (1.0d + Math.exp(-value));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static class CalibrationContext {
        private Map<Long, Question> questionMap = Collections.emptyMap();
        private final Map<Long, List<AnswerEvent>> answerEventsByUser = new LinkedHashMap<>();
        private final Map<Long, List<AnswerEvent>> answerEventsByQuestion = new LinkedHashMap<>();
        private Map<Long, List<UserKnowledgeMastery>> masteryByUser = Collections.emptyMap();

        private void add(Long userId,
                         Question question,
                         boolean correct,
                         LocalDateTime createdAt,
                         LocalDateTime submittedAt,
                         Long recordId,
                         String sourceType) {
            if (userId == null || question == null) {
                return;
            }
            AnswerEvent event = new AnswerEvent();
            event.userId = userId;
            event.questionId = question.getId();
            event.subjectName = question.getSubjectName();
            event.knowledgePoints = splitKnowledge(question.getKnowledgePoints());
            event.correct = correct;
            event.occurredAt = createdAt != null ? createdAt : submittedAt;
            event.sessionKey = sourceType + ":" + recordId;
            answerEventsByUser.computeIfAbsent(userId, key -> new ArrayList<>()).add(event);
            answerEventsByQuestion.computeIfAbsent(question.getId(), key -> new ArrayList<>()).add(event);
        }

        private List<String> splitKnowledge(String raw) {
            if (!StringUtils.hasText(raw)) {
                return Collections.emptyList();
            }
            Set<String> values = new LinkedHashSet<>();
            for (String part : raw.split("[,，/|；;、\\n\\r]+")) {
                String value = part == null ? "" : part.trim();
                if (StringUtils.hasText(value)) {
                    values.add(value);
                }
            }
            return new ArrayList<>(values);
        }
    }

    private static class AnswerEvent {
        private Long userId;
        private Long questionId;
        private String subjectName;
        private List<String> knowledgePoints = Collections.emptyList();
        private boolean correct;
        private LocalDateTime occurredAt;
        private String sessionKey;
    }
}
