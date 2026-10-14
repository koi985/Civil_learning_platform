package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.QuestionAnswerUtils;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.PracticeCreateRequest;
import com.civil.platform.module.dto.PracticeSubmitRequest;
import com.civil.platform.module.entity.PracticeAnswer;
import com.civil.platform.module.entity.PracticeRecord;
import com.civil.platform.module.entity.Question;
import com.civil.platform.module.entity.QuestionOption;
import com.civil.platform.module.entity.QuestionPaper;
import com.civil.platform.module.entity.QuestionPaperQuestion;
import com.civil.platform.module.entity.RecommendPracticeBundle;
import com.civil.platform.module.entity.ShenlunGroup;
import com.civil.platform.module.entity.ShenlunGroupMaterial;
import com.civil.platform.module.entity.UserGrowthAssessmentSession;
import com.civil.platform.module.entity.UserDailyCheckin;
import com.civil.platform.module.entity.UserQuestionFavorite;
import com.civil.platform.module.entity.UserWrongQuestion;
import com.civil.platform.module.mapper.PracticeAnswerMapper;
import com.civil.platform.module.mapper.PracticeRecordMapper;
import com.civil.platform.module.mapper.QuestionMapper;
import com.civil.platform.module.mapper.QuestionOptionMapper;
import com.civil.platform.module.mapper.QuestionPaperMapper;
import com.civil.platform.module.mapper.QuestionPaperQuestionMapper;
import com.civil.platform.module.mapper.RecommendPracticeBundleMapper;
import com.civil.platform.module.mapper.ShenlunGroupMapper;
import com.civil.platform.module.mapper.ShenlunGroupMaterialMapper;
import com.civil.platform.module.mapper.UserGrowthAssessmentSessionMapper;
import com.civil.platform.module.mapper.UserDailyCheckinMapper;
import com.civil.platform.module.mapper.UserQuestionFavoriteMapper;
import com.civil.platform.module.mapper.UserWrongQuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final QuestionPaperMapper questionPaperMapper;
    private final QuestionPaperQuestionMapper questionPaperQuestionMapper;
    private final RecommendPracticeBundleMapper recommendPracticeBundleMapper;
    private final ShenlunGroupMapper shenlunGroupMapper;
    private final ShenlunGroupMaterialMapper shenlunGroupMaterialMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeAnswerMapper practiceAnswerMapper;
    private final UserWrongQuestionMapper userWrongQuestionMapper;
    private final UserQuestionFavoriteMapper userQuestionFavoriteMapper;
    private final UserDailyCheckinMapper userDailyCheckinMapper;
    private final UserGrowthAssessmentSessionMapper userGrowthAssessmentSessionMapper;
    private final RecommendEngineService recommendEngineService;

    public QuestionService(QuestionMapper questionMapper,
                           QuestionOptionMapper questionOptionMapper,
                           QuestionPaperMapper questionPaperMapper,
                           QuestionPaperQuestionMapper questionPaperQuestionMapper,
                           RecommendPracticeBundleMapper recommendPracticeBundleMapper,
                           ShenlunGroupMapper shenlunGroupMapper,
                           ShenlunGroupMaterialMapper shenlunGroupMaterialMapper,
                           PracticeRecordMapper practiceRecordMapper,
                           PracticeAnswerMapper practiceAnswerMapper,
                           UserWrongQuestionMapper userWrongQuestionMapper,
                           UserQuestionFavoriteMapper userQuestionFavoriteMapper,
                           UserDailyCheckinMapper userDailyCheckinMapper,
                           UserGrowthAssessmentSessionMapper userGrowthAssessmentSessionMapper,
                           RecommendEngineService recommendEngineService) {
        this.questionMapper = questionMapper;
        this.questionOptionMapper = questionOptionMapper;
        this.questionPaperMapper = questionPaperMapper;
        this.questionPaperQuestionMapper = questionPaperQuestionMapper;
        this.recommendPracticeBundleMapper = recommendPracticeBundleMapper;
        this.shenlunGroupMapper = shenlunGroupMapper;
        this.shenlunGroupMaterialMapper = shenlunGroupMaterialMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.practiceAnswerMapper = practiceAnswerMapper;
        this.userWrongQuestionMapper = userWrongQuestionMapper;
        this.userQuestionFavoriteMapper = userQuestionFavoriteMapper;
        this.userDailyCheckinMapper = userDailyCheckinMapper;
        this.userGrowthAssessmentSessionMapper = userGrowthAssessmentSessionMapper;
        this.recommendEngineService = recommendEngineService;
    }

    public PageResult<Map<String, Object>> pageQuestions(Long pageNo,
                                                         Long pageSize,
                                                         String keyword,
                                                         String province,
                                                         String subjectName,
                                                         String categoryName,
                                                         String questionType,
                                                         String difficultyLevel,
                                                         String sourceName,
                                                         String sortBy) {
        Page<Question> page = questionMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Question::getStem, keyword)
                        .or()
                        .like(Question::getMaterial, keyword))
                .eq(StringUtils.hasText(province), Question::getProvince, province)
                .eq(StringUtils.hasText(subjectName), Question::getSubjectName, subjectName)
                .eq(StringUtils.hasText(categoryName), Question::getCategoryName, categoryName)
                .eq(StringUtils.hasText(questionType), Question::getQuestionType, questionType)
                .eq(StringUtils.hasText(difficultyLevel), Question::getDifficultyLevel, difficultyLevel)
                .eq(StringUtils.hasText(sourceName), Question::getSourceName, sourceName)
                .orderByDesc(!"OLDEST".equalsIgnoreCase(sortBy), Question::getCreatedAt)
                .orderByAsc("OLDEST".equalsIgnoreCase(sortBy), Question::getCreatedAt));
        Map<Long, UserQuestionFavorite> favoriteMap = loadFavoriteMap(page.getRecords().stream()
                .map(Question::getId)
                .collect(Collectors.toList()));
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(question -> toQuestionSummary(question, favoriteMap.containsKey(question.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    public PageResult<Map<String, Object>> pageShenlunGroups(Long pageNo,
                                                             Long pageSize,
                                                             String keyword,
                                                             String province,
                                                             String sourceName,
                                                             Integer year) {
        Page<ShenlunGroup> page = shenlunGroupMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<ShenlunGroup>()
                .eq(ShenlunGroup::getStatus, 1)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(ShenlunGroup::getTitle, keyword)
                        .or()
                        .like(ShenlunGroup::getSourceName, keyword))
                .eq(StringUtils.hasText(province), ShenlunGroup::getRegion, province)
                .eq(StringUtils.hasText(sourceName), ShenlunGroup::getSourceName, sourceName)
                .eq(year != null, ShenlunGroup::getYear, year)
                .orderByDesc(ShenlunGroup::getYear)
                .orderByDesc(ShenlunGroup::getId));
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(this::toShenlunGroupSummary)
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    public Map<String, Object> shenlunGroupDetail(Long groupId) {
        ShenlunGroup group = getPublishedShenlunGroup(groupId);
        List<Question> questions = loadShenlunGroupQuestions(groupId);
        List<ShenlunGroupMaterial> materials = loadShenlunGroupMaterials(groupId);
        Map<String, Object> result = new HashMap<>();
        result.put("group", toShenlunGroupDetail(group, questions, materials));
        return result;
    }

    public List<Map<String, Object>> randomQuestions(Integer count) {
        int normalizedCount = count == null ? 120 : count;
        if (normalizedCount < 1 || normalizedCount > 200) {
            throw new BizException("随机抽题数量需在 1-200 道之间");
        }
        long availableCount = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true));
        if (availableCount < normalizedCount) {
            throw new BizException("题库可用题目不足 " + normalizedCount + " 道");
        }
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                        .eq(Question::getEnabled, true)
                        .last("ORDER BY RAND() LIMIT " + normalizedCount))
                .stream()
                .map(question -> toQuestionSummary(question, false))
                .collect(Collectors.toList());
    }

    public Map<String, Object> searchDashboard() {
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true)
                .orderByDesc(Question::getCreatedAt));
        Map<String, Object> result = new HashMap<>();
        result.put("filterOptions", buildFilterOptions(questions));
        result.put("hotQuestions", questions.stream().limit(6)
                .map(question -> toQuestionSummary(question, false))
                .collect(Collectors.toList()));
        if (SecurityUtils.getRoles().isEmpty()) {
            result.put("favoriteCount", 0L);
            result.put("favoriteSubjects", new ArrayList<>());
            result.put("recentPractices", new ArrayList<>());
            result.put("practiceTrend", buildPracticeTrend(new ArrayList<>()));
            return result;
        }

        Long userId = SecurityUtils.getUserId();
        List<UserQuestionFavorite> favorites = userQuestionFavoriteMapper.selectList(new LambdaQueryWrapper<UserQuestionFavorite>()
                .eq(UserQuestionFavorite::getUserId, userId)
                .orderByDesc(UserQuestionFavorite::getCreatedAt));
        List<PracticeRecord> practices = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getPracticeType, "QUESTION_BANK")
                .eq(PracticeRecord::getStatus, "SUBMITTED")
                .orderByDesc(PracticeRecord::getSubmittedAt));
        Set<Long> favoriteQuestionIds = favorites.stream()
                .map(UserQuestionFavorite::getQuestionId)
                .collect(Collectors.toSet());
        Map<Long, Question> favoriteQuestionMap = CollectionUtils.isEmpty(favoriteQuestionIds)
                ? Collections.emptyMap()
                : questionMapper.selectBatchIds(favoriteQuestionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (left, right) -> left));
        result.put("favoriteCount", favorites.size());
        result.put("favoriteSubjects", favorites.stream()
                .map(item -> favoriteQuestionMap.get(item.getQuestionId()))
                .filter(item -> item != null && StringUtils.hasText(item.getSubjectName()))
                .collect(Collectors.groupingBy(Question::getSubjectName, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("subjectName", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList()));
        result.put("recentPractices", practices.stream().limit(6).map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("title", item.getTitle());
            map.put("subjectName", item.getSubjectName());
            map.put("questionCount", item.getQuestionCount());
            map.put("accuracyRate", item.getAccuracyRate());
            map.put("submittedAt", item.getSubmittedAt());
            map.put("reportPath", "/practice/report/" + item.getId());
            return map;
        }).collect(Collectors.toList()));
        result.put("practiceTrend", buildPracticeTrend(practices));
        return result;
    }

    public Map<String, Object> dailyCheckInStatus() {
        Long userId = SecurityUtils.getUserId();
        LocalDate today = LocalDate.now();
        UserDailyCheckin todayCheckin = userDailyCheckinMapper.selectOne(new LambdaQueryWrapper<UserDailyCheckin>()
                .eq(UserDailyCheckin::getUserId, userId)
                .eq(UserDailyCheckin::getCheckinDate, today)
                .last("limit 1"));
        long totalCount = userDailyCheckinMapper.selectCount(new LambdaQueryWrapper<UserDailyCheckin>()
                .eq(UserDailyCheckin::getUserId, userId));
        List<UserDailyCheckin> recentCheckins = userDailyCheckinMapper.selectList(new LambdaQueryWrapper<UserDailyCheckin>()
                .eq(UserDailyCheckin::getUserId, userId)
                .orderByDesc(UserDailyCheckin::getCheckinDate)
                .last("limit 7"));

        Map<String, Object> result = new HashMap<>();
        result.put("checkedInToday", todayCheckin != null);
        result.put("streakDays", todayCheckin == null ? calculateCurrentStreak(userId) : todayCheckin.getStreakDays());
        result.put("totalCheckinCount", totalCount);
        result.put("todayRecord", todayCheckin);
        result.put("recentDates", recentCheckins.stream()
                .map(UserDailyCheckin::getCheckinDate)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkInToday() {
        UserDailyCheckin checkin = ensureDailyCheckIn(SecurityUtils.getUserId(), "MANUAL", null);
        Map<String, Object> result = dailyCheckInStatus();
        result.put("todayRecord", checkin);
        result.put("message", "今日已完成打卡");
        return result;
    }

    public PageResult<Map<String, Object>> pageQuestionPapers(Long pageNo, Long pageSize, String province) {
        Page<QuestionPaper> page = questionPaperMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<QuestionPaper>()
                .eq(QuestionPaper::getStatus, 1)
                .eq(QuestionPaper::getSubject, "行测")
                .eq(QuestionPaper::getQuestionCount, 120)
                .eq(QuestionPaper::getTotalScore, 120)
                .eq(StringUtils.hasText(province) && !"全国".equals(province), QuestionPaper::getRegion, province)
                .orderByDesc(QuestionPaper::getCreateTime)
                .orderByDesc(QuestionPaper::getId));
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(this::toPaperSummary)
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startQuestionPaper(Long paperId) {
        Long userId = SecurityUtils.getUserId();
        QuestionPaper paper = questionPaperMapper.selectById(paperId);
        if (paper == null || paper.getStatus() == null || paper.getStatus() != 1) {
            throw new BizException(404, "试卷不存在或未发布");
        }
        List<QuestionPaperQuestion> relations = questionPaperQuestionMapper.selectList(new LambdaQueryWrapper<QuestionPaperQuestion>()
                .eq(QuestionPaperQuestion::getPaperId, paperId)
                .orderByAsc(QuestionPaperQuestion::getSortNo)
                .orderByAsc(QuestionPaperQuestion::getId));
        if (CollectionUtils.isEmpty(relations)) {
            throw new BizException("试卷暂未配置题目");
        }
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(relations.stream()
                        .map(QuestionPaperQuestion::getQuestionId)
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setPracticeType("TEST_PAPER");
        record.setPracticeMode("PAPER");
        record.setTitle(paper.getTitle());
        record.setSubjectName(paper.getSubject());
        record.setProvince(paper.getRegion());
        record.setQuestionCount(relations.size());
        record.setAnsweredCount(0);
        record.setCorrectCount(0);
        record.setScore(0);
        record.setAccuracyRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        record.setDurationSeconds(0);
        record.setStatus("IN_PROGRESS");
        record.setSourceSnapshot("question-paper:" + paperId + ";durationMinutes:" + paper.getDuration());
        practiceRecordMapper.insert(record);

        for (QuestionPaperQuestion relation : relations) {
            Question question = questionMap.get(relation.getQuestionId());
            if (question == null) {
                continue;
            }
            PracticeAnswer answer = new PracticeAnswer();
            answer.setPracticeRecordId(record.getId());
            answer.setQuestionId(question.getId());
            answer.setCorrectAnswer(question.getAnswerValue());
            answer.setIsCorrect(false);
            answer.setMarked(false);
            answer.setSpentSeconds(0);
            answer.setQuestionScore(relation.getScore() == null ? 1 : relation.getScore());
            practiceAnswerMapper.insert(answer);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("practiceId", record.getId());
        result.put("questionCount", record.getQuestionCount());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startShenlunGroupPractice(Long groupId) {
        Long userId = SecurityUtils.getUserId();
        ShenlunGroup group = getPublishedShenlunGroup(groupId);
        List<Question> questions = loadShenlunGroupQuestions(groupId);
        if (CollectionUtils.isEmpty(questions)) {
            throw new BizException("申论材料组暂未配置题目");
        }

        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setPracticeType("QUESTION_BANK");
        record.setPracticeMode("GROUP");
        record.setTitle(group.getTitle());
        record.setSubjectName(group.getSubjectName());
        record.setProvince(group.getRegion());
        record.setQuestionCount(questions.size());
        record.setAnsweredCount(0);
        record.setCorrectCount(0);
        record.setScore(0);
        record.setAccuracyRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        record.setDurationSeconds(0);
        record.setStatus("IN_PROGRESS");
        record.setSourceSnapshot("shenlun-group:" + groupId);
        practiceRecordMapper.insert(record);

        for (Question question : questions) {
            PracticeAnswer answer = new PracticeAnswer();
            answer.setPracticeRecordId(record.getId());
            answer.setQuestionId(question.getId());
            answer.setCorrectAnswer(question.getAnswerValue());
            answer.setIsCorrect(false);
            answer.setMarked(false);
            answer.setSpentSeconds(0);
            answer.setQuestionScore(resolveQuestionScore(question));
            practiceAnswerMapper.insert(answer);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("practiceId", record.getId());
        result.put("questionCount", questions.size());
        result.put("groupId", groupId);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPractice(PracticeCreateRequest request) {
        if ("申论".equals(request.getSubjectName()) && request.getGroupId() != null) {
            return startShenlunGroupPractice(request.getGroupId());
        }
        Long userId = SecurityUtils.getUserId();
        int requestedCount = request.getQuestionCount() == null ? 10 : request.getQuestionCount();
        List<Question> matches = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true)
                .like(StringUtils.hasText(request.getKeyword()), Question::getStem, request.getKeyword())
                .eq(StringUtils.hasText(request.getProvince()), Question::getProvince, request.getProvince())
                .eq(StringUtils.hasText(request.getSubjectName()), Question::getSubjectName, request.getSubjectName())
                .eq(StringUtils.hasText(request.getCategoryName()), Question::getCategoryName, request.getCategoryName())
                .eq(StringUtils.hasText(request.getQuestionType()), Question::getQuestionType, request.getQuestionType())
                .eq(StringUtils.hasText(request.getDifficultyLevel()), Question::getDifficultyLevel, request.getDifficultyLevel())
                .eq(StringUtils.hasText(request.getSourceName()), Question::getSourceName, request.getSourceName())
                .orderByDesc(Question::getCreatedAt));
        if (CollectionUtils.isEmpty(matches)) {
            throw new BizException("未找到符合条件的题目");
        }
        List<Question> selected = selectPracticeQuestions(matches, userId, requestedCount);

        String practiceMode = resolvePracticeMode(request.getMode());
        PracticeRecord record = createPracticeRecord(userId, selected, "QUESTION_BANK", practiceMode,
                buildPracticeTitle(request.getSubjectName(), selected.size(), practiceMode),
                "question-bank");

        Map<String, Object> result = new HashMap<>();
        result.put("practiceId", record.getId());
        result.put("questionCount", selected.size());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startRecommendPracticeBundle(Long bundleId) {
        Long userId = SecurityUtils.getUserId();
        RecommendPracticeBundle bundle = recommendPracticeBundleMapper.selectById(bundleId);
        if (bundle == null || !Objects.equals(bundle.getUserId(), userId)) {
            throw new BizException(404, "推荐练习包不存在");
        }
        List<Long> questionIds = parseQuestionIds(bundle.getQuestionIds());
        if (questionIds.isEmpty()) {
            throw new BizException("推荐练习包暂无可用题目");
        }
        List<Question> selected = questionMapper.selectBatchIds(questionIds).stream()
                .filter(item -> item != null && Boolean.TRUE.equals(item.getEnabled()))
                .sorted(Comparator.comparingInt(item -> questionIds.indexOf(item.getId())))
                .collect(Collectors.toList());
        if (selected.isEmpty()) {
            throw new BizException("推荐练习包题目已失效");
        }
        PracticeRecord record = createPracticeRecord(userId, selected, "QUESTION_BANK", bundle.getRecommendMode(),
                bundle.getTitle(), "recommend-bundle:" + bundleId + ";strategy:" + bundle.getSourceStrategy());
        Map<String, Object> result = new HashMap<>();
        result.put("practiceId", record.getId());
        result.put("questionCount", selected.size());
        result.put("bundleId", bundleId);
        result.put("reportPath", "/practice/" + record.getId());
        return result;
    }

    private List<Question> selectPracticeQuestions(List<Question> matches, Long userId, int requestedCount) {
        Set<Long> practicedIds = submittedPracticeQuestionIds(userId, matches);
        List<Question> unseen = new ArrayList<>();
        List<Question> repeated = new ArrayList<>();
        for (Question question : matches) {
            if (practicedIds.contains(question.getId())) {
                repeated.add(question);
            } else {
                unseen.add(question);
            }
        }

        Collections.shuffle(unseen);
        if (unseen.size() >= requestedCount) {
            return new ArrayList<>(unseen.subList(0, requestedCount));
        }

        Collections.shuffle(repeated);
        List<Question> selected = new ArrayList<>(unseen);
        int remaining = requestedCount - selected.size();
        if (repeated.size() > remaining) {
            selected.addAll(repeated.subList(0, remaining));
        } else {
            selected.addAll(repeated);
        }
        return selected;
    }

    private Set<Long> submittedPracticeQuestionIds(Long userId, List<Question> matches) {
        Set<Long> matchIds = matches.stream().map(Question::getId).collect(Collectors.toSet());
        if (matchIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<PracticeRecord> submittedRecords = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getPracticeType, "QUESTION_BANK")
                .eq(PracticeRecord::getStatus, "SUBMITTED"));
        if (CollectionUtils.isEmpty(submittedRecords)) {
            return Collections.emptySet();
        }
        List<PracticeAnswer> answers = practiceAnswerMapper.selectList(new LambdaQueryWrapper<PracticeAnswer>()
                .in(PracticeAnswer::getPracticeRecordId, submittedRecords.stream()
                        .map(PracticeRecord::getId)
                        .collect(Collectors.toList()))
                .in(PracticeAnswer::getQuestionId, matchIds));
        if (CollectionUtils.isEmpty(answers)) {
            return Collections.emptySet();
        }
        return answers.stream()
                .map(PracticeAnswer::getQuestionId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private PracticeRecord createPracticeRecord(Long userId,
                                                List<Question> selected,
                                                String practiceType,
                                                String practiceMode,
                                                String title,
                                                String sourceSnapshot) {
        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setPracticeType(practiceType);
        record.setPracticeMode(practiceMode);
        record.setTitle(title);
        record.setSubjectName(resolveSubject(selected));
        record.setProvince(resolveProvince(selected));
        record.setQuestionCount(selected.size());
        record.setAnsweredCount(0);
        record.setCorrectCount(0);
        record.setScore(0);
        record.setAccuracyRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        record.setDurationSeconds(0);
        record.setStatus("IN_PROGRESS");
        record.setSourceSnapshot(sourceSnapshot);
        practiceRecordMapper.insert(record);
        for (Question question : selected) {
            PracticeAnswer answer = new PracticeAnswer();
            answer.setPracticeRecordId(record.getId());
            answer.setQuestionId(question.getId());
            answer.setCorrectAnswer(question.getAnswerValue());
            answer.setIsCorrect(false);
            answer.setMarked(false);
            answer.setSpentSeconds(0);
            answer.setQuestionScore(resolveQuestionScore(question));
            practiceAnswerMapper.insert(answer);
        }
        return record;
    }

    private List<Long> parseQuestionIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : raw.split("[,，]+")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // ignore invalid id
            }
        }
        return ids;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createWrongBookPractice() {
        Long userId = SecurityUtils.getUserId();
        List<UserWrongQuestion> wrongQuestions = userWrongQuestionMapper.selectList(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getRemoved, false)
                .orderByDesc(UserWrongQuestion::getLastWrongAt)
                .last("limit 20"));
        if (CollectionUtils.isEmpty(wrongQuestions)) {
            throw new BizException("当前没有可练习的错题");
        }
        List<Question> questions = questionMapper.selectBatchIds(wrongQuestions.stream()
                .map(UserWrongQuestion::getQuestionId)
                .collect(Collectors.toList()));
        if (CollectionUtils.isEmpty(questions)) {
            throw new BizException("错题数据异常");
        }
        questions.sort(Comparator.comparingInt(question ->
                wrongQuestions.stream().map(UserWrongQuestion::getQuestionId).collect(Collectors.toList()).indexOf(question.getId())));

        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setPracticeType("WRONG_BOOK");
        record.setPracticeMode("WRONG_BOOK");
        record.setTitle("错题再练");
        record.setSubjectName(resolveSubject(questions));
        record.setProvince(resolveProvince(questions));
        record.setQuestionCount(questions.size());
        record.setAnsweredCount(0);
        record.setCorrectCount(0);
        record.setScore(0);
        record.setAccuracyRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        record.setDurationSeconds(0);
        record.setStatus("IN_PROGRESS");
        record.setSourceSnapshot("wrong-book");
        practiceRecordMapper.insert(record);

        for (Question question : questions) {
            PracticeAnswer answer = new PracticeAnswer();
            answer.setPracticeRecordId(record.getId());
            answer.setQuestionId(question.getId());
            answer.setCorrectAnswer(question.getAnswerValue());
            answer.setIsCorrect(false);
            answer.setMarked(false);
            answer.setSpentSeconds(0);
            answer.setQuestionScore(resolveQuestionScore(question));
            practiceAnswerMapper.insert(answer);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("practiceId", record.getId());
        result.put("questionCount", questions.size());
        return result;
    }

    public Map<String, Object> practiceDetail(Long practiceId) {
        PracticeRecord record = getOwnedPractice(practiceId);
        List<PracticeAnswer> answers = practiceAnswerMapper.selectList(new LambdaQueryWrapper<PracticeAnswer>()
                .eq(PracticeAnswer::getPracticeRecordId, practiceId)
                .orderByAsc(PracticeAnswer::getId));
        if (CollectionUtils.isEmpty(answers)) {
            throw new BizException("练习内容不存在");
        }
        return buildPracticePayload(record, answers, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitPractice(Long practiceId, PracticeSubmitRequest request) {
        PracticeRecord record = getOwnedPractice(practiceId);
        if ("SUBMITTED".equals(record.getStatus())) {
            throw new BizException("该练习已提交");
        }
        List<PracticeAnswer> storedAnswers = practiceAnswerMapper.selectList(new LambdaQueryWrapper<PracticeAnswer>()
                .eq(PracticeAnswer::getPracticeRecordId, practiceId));
        Map<Long, PracticeAnswer> answerMap = storedAnswers.stream()
                .collect(Collectors.toMap(PracticeAnswer::getQuestionId, Function.identity()));

        int answeredCount = 0;
        int correctCount = 0;
        for (PracticeSubmitRequest.AnswerItem item : request.getAnswers()) {
            PracticeAnswer stored = answerMap.get(item.getQuestionId());
            if (stored == null) {
                throw new BizException("存在不属于本次练习的题目");
            }
            String normalizedUserAnswer = normalizeAnswer(item.getUserAnswer());
            String normalizedCorrectAnswer = normalizeAnswer(stored.getCorrectAnswer());
            boolean correct = StringUtils.hasText(normalizedUserAnswer) && normalizedCorrectAnswer.equals(normalizedUserAnswer);
            if (StringUtils.hasText(normalizedUserAnswer)) {
                answeredCount++;
            }
            if (correct) {
                correctCount++;
            }
            stored.setUserAnswer(item.getUserAnswer());
            stored.setIsCorrect(correct);
            stored.setMarked(Boolean.TRUE.equals(item.getMarked()));
            stored.setSpentSeconds(item.getSpentSeconds() == null ? 0 : item.getSpentSeconds());
            practiceAnswerMapper.updateById(stored);
            syncWrongQuestion(record.getUserId(), item.getQuestionId(), record.getPracticeType(), record.getId(), item.getUserAnswer(), correct);
        }

        record.setAnsweredCount(answeredCount);
        record.setCorrectCount(correctCount);
        record.setScore(correctCount);
        record.setAccuracyRate(calculateAccuracy(correctCount, record.getQuestionCount()));
        record.setDurationSeconds(request.getDurationSeconds());
        record.setStatus("SUBMITTED");
        record.setSubmittedAt(LocalDateTime.now());
        practiceRecordMapper.updateById(record);
        ensureDailyCheckIn(record.getUserId(), "PRACTICE", record.getId());
        updateGrowthAssessmentSession(record);
        recommendEngineService.enqueueBehaviorRefresh(record.getUserId(), "PRACTICE_SUBMIT");

        Map<String, Object> result = new HashMap<>();
        result.put("reportId", record.getId());
        result.put("score", record.getScore());
        result.put("accuracyRate", record.getAccuracyRate());
        return result;
    }

    public Map<String, Object> practiceReport(Long practiceId) {
        PracticeRecord record = getOwnedPractice(practiceId);
        if (!"SUBMITTED".equals(record.getStatus())) {
            throw new BizException("练习尚未提交");
        }
        List<PracticeAnswer> answers = practiceAnswerMapper.selectList(new LambdaQueryWrapper<PracticeAnswer>()
                .eq(PracticeAnswer::getPracticeRecordId, practiceId)
                .orderByAsc(PracticeAnswer::getId));
        return buildPracticePayload(record, answers, true);
    }

    public PageResult<Map<String, Object>> wrongQuestions(Long pageNo, Long pageSize, String subjectName, String categoryName) {
        Long userId = SecurityUtils.getUserId();
        String subjectFilter = StringUtils.hasText(subjectName) ? subjectName.trim() : null;
        String categoryFilter = StringUtils.hasText(categoryName) ? categoryName.trim() : null;
        Page<UserWrongQuestion> page = userWrongQuestionMapper.pageActiveWrongQuestions(new Page<>(pageNo, pageSize),
                userId,
                subjectFilter,
                categoryFilter);
        List<Question> questions = questionMapper.selectBatchIds(page.getRecords().stream()
                .map(UserWrongQuestion::getQuestionId)
                .collect(Collectors.toList()));
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, List<QuestionOption>> optionMap = loadOptionMap(questionMap.keySet());
        Map<Long, UserQuestionFavorite> favoriteMap = loadFavoriteMap(questionMap.keySet());

        List<Map<String, Object>> records = page.getRecords().stream()
                .map(item -> {
                    Question question = questionMap.get(item.getQuestionId());
                    if (question == null) {
                        return null;
                    }
                    Map<String, Object> wrong = toQuestionDetail(question, optionMap.get(question.getId()), true, favoriteMap.containsKey(question.getId()));
                    wrong.put("wrongCount", item.getWrongCount());
                    wrong.put("lastWrongAt", item.getLastWrongAt());
                    wrong.put("latestAnswer", item.getLatestAnswer());
                    return wrong;
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    public PageResult<Map<String, Object>> favoriteQuestions(Long pageNo, Long pageSize, String subjectName, String questionType) {
        Long userId = SecurityUtils.getUserId();
        List<UserQuestionFavorite> favorites = userQuestionFavoriteMapper.selectList(new LambdaQueryWrapper<UserQuestionFavorite>()
                .eq(UserQuestionFavorite::getUserId, userId)
                .orderByDesc(UserQuestionFavorite::getCreatedAt));
        if (CollectionUtils.isEmpty(favorites)) {
            return new PageResult<>(0L, pageNo, pageSize, new ArrayList<>());
        }
        Map<Long, UserQuestionFavorite> favoriteMap = favorites.stream()
                .collect(Collectors.toMap(UserQuestionFavorite::getQuestionId, Function.identity(), (left, right) -> left));
        if (CollectionUtils.isEmpty(favoriteMap)) {
            return new PageResult<>(0L, pageNo, pageSize, new ArrayList<>());
        }
        List<Map<String, Object>> records = questionMapper.selectBatchIds(favoriteMap.keySet()).stream()
                .filter(question -> !StringUtils.hasText(subjectName) || subjectName.equals(question.getSubjectName()))
                .filter(question -> !StringUtils.hasText(questionType) || questionType.equals(question.getQuestionType()))
                .map(question -> {
                    Map<String, Object> item = toQuestionSummary(question, true);
                    item.put("favoriteCreatedAt", favoriteMap.get(question.getId()).getCreatedAt());
                    return item;
                })
                .sorted((left, right) -> String.valueOf(right.get("favoriteCreatedAt")).compareTo(String.valueOf(left.get("favoriteCreatedAt"))))
                .collect(Collectors.toList());
        int fromIndex = Math.max(0, (int) ((pageNo - 1) * pageSize));
        int toIndex = Math.min(records.size(), fromIndex + pageSize.intValue());
        List<Map<String, Object>> pageRecords = fromIndex >= records.size() ? new ArrayList<>() : records.subList(fromIndex, toIndex);
        return new PageResult<>((long) records.size(), pageNo, pageSize, pageRecords);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addFavorite(Long questionId) {
        if (questionMapper.selectById(questionId) == null) {
            throw new BizException(404, "题目不存在");
        }
        Long userId = SecurityUtils.getUserId();
        UserQuestionFavorite favorite = userQuestionFavoriteMapper.selectOne(new LambdaQueryWrapper<UserQuestionFavorite>()
                .eq(UserQuestionFavorite::getUserId, userId)
                .eq(UserQuestionFavorite::getQuestionId, questionId));
        if (favorite == null) {
            favorite = new UserQuestionFavorite();
            favorite.setUserId(userId);
            favorite.setQuestionId(questionId);
            userQuestionFavoriteMapper.insert(favorite);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("questionId", questionId);
        result.put("favorite", true);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> removeFavorite(Long questionId) {
        userQuestionFavoriteMapper.delete(new LambdaQueryWrapper<UserQuestionFavorite>()
                .eq(UserQuestionFavorite::getUserId, SecurityUtils.getUserId())
                .eq(UserQuestionFavorite::getQuestionId, questionId));
        Map<String, Object> result = new HashMap<>();
        result.put("questionId", questionId);
        result.put("favorite", false);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeWrongQuestion(Long questionId) {
        Long userId = SecurityUtils.getUserId();
        UserWrongQuestion wrongQuestion = userWrongQuestionMapper.selectOne(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getQuestionId, questionId));
        if (wrongQuestion == null) {
            throw new BizException("错题不存在");
        }
        wrongQuestion.setRemoved(true);
        userWrongQuestionMapper.updateById(wrongQuestion);
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

    private Map<String, Object> buildPracticePayload(PracticeRecord record, List<PracticeAnswer> answers, boolean withAnalysis) {
        List<Long> questionIds = answers.stream().map(PracticeAnswer::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, List<QuestionOption>> optionMap = loadOptionMap(questionIds);
        Map<Long, UserQuestionFavorite> favoriteMap = loadFavoriteMap(questionIds);
        List<Map<String, Object>> items = new ArrayList<>();
        for (PracticeAnswer answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                continue;
            }
            Map<String, Object> item = toQuestionDetail(question, optionMap.get(question.getId()), withAnalysis, favoriteMap.containsKey(question.getId()));
            item.put("userAnswer", answer.getUserAnswer());
            item.put("marked", answer.getMarked());
            item.put("isCorrect", answer.getIsCorrect());
            item.put("spentSeconds", answer.getSpentSeconds());
            item.put("questionScore", answer.getQuestionScore());
            items.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("record", record);
        result.put("questions", items);
        result.put("shenlunGroup", buildPracticeShenlunGroup(record, questions));
        result.put("practiceModeLabel", resolvePracticeModeLabel(record.getPracticeMode()));
        result.put("timeLimitSeconds", resolveTimeLimitSeconds(record));
        result.put("dailyCheckIn", dailyCheckInStatus());
        result.put("assessmentSummary", buildAssessmentSummary(record));
        return result;
    }

    private Map<Long, List<QuestionOption>> loadOptionMap(Iterable<Long> questionIds) {
        List<Long> ids = new ArrayList<>();
        questionIds.forEach(ids::add);
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .in(QuestionOption::getQuestionId, ids)
                        .orderByAsc(QuestionOption::getQuestionId)
                        .orderByAsc(QuestionOption::getSortNo))
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, Object> toQuestionSummary(Question question, boolean favorite) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", question.getId());
        item.put("groupId", question.getGroupId());
        item.put("groupQuestionNo", question.getGroupQuestionNo());
        item.put("isGroupSharedMaterial", Boolean.TRUE.equals(question.getIsGroupSharedMaterial()));
        item.put("groupMaterialRefs", parseMaterialRefNumbers(question.getGroupMaterialRefs()));
        item.put("subjectName", question.getSubjectName());
        item.put("categoryName", question.getCategoryName());
        item.put("province", question.getProvince());
        item.put("questionType", question.getQuestionType());
        item.put("difficultyLevel", question.getDifficultyLevel());
        item.put("score", resolveQuestionScore(question));
        item.put("sourceName", question.getSourceName());
        item.put("material", question.getMaterial());
        item.put("stem", question.getStem());
        item.put("knowledgePoints", question.getKnowledgePoints());
        item.put("favorite", favorite);
        item.put("videoExplainTitle", question.getVideoExplainTitle());
        item.put("videoExplainUrl", question.getVideoExplainUrl());
        item.put("videoExplainAvailable", StringUtils.hasText(question.getVideoExplainUrl()));
        return item;
    }

    private int resolveQuestionScore(Question question) {
        return question.getScore() == null ? 1 : question.getScore();
    }

    private Map<String, Object> toQuestionDetail(Question question, List<QuestionOption> options, boolean withAnalysis, boolean favorite) {
        Map<String, Object> item = toQuestionSummary(question, favorite);
        item.put("options", options == null ? Collections.emptyList() : options);
        if (withAnalysis) {
            item.put("material", question.getMaterial());
            item.put("analysis", question.getAnalysis());
            item.put("correctAnswer", question.getAnswerValue());
        }
        return item;
    }

    private Map<String, Object> toPaperSummary(QuestionPaper paper) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", paper.getId());
        item.put("title", paper.getTitle());
        item.put("province", paper.getRegion());
        item.put("subjectName", paper.getSubject());
        item.put("questionCount", paper.getQuestionCount());
        item.put("totalScore", paper.getTotalScore());
        item.put("durationMinutes", paper.getDuration());
        item.put("status", paper.getStatus() != null && paper.getStatus() == 1 ? "PUBLISHED" : "DRAFT");
        item.put("actionText", "我要答题");
        return item;
    }

    private Map<String, Object> toShenlunGroupSummary(ShenlunGroup group) {
        List<Question> questions = loadShenlunGroupQuestions(group.getId());
        List<ShenlunGroupMaterial> materials = loadShenlunGroupMaterials(group.getId());
        Map<String, Object> item = new HashMap<>();
        item.put("id", group.getId());
        item.put("title", group.getTitle());
        item.put("subjectName", group.getSubjectName());
        item.put("year", group.getYear());
        item.put("province", group.getRegion());
        item.put("paperType", group.getPaperType());
        item.put("sourceName", group.getSourceName());
        item.put("materialMode", group.getMaterialMode());
        item.put("questionCount", group.getTotalQuestionCount());
        item.put("totalScore", group.getTotalScore());
        item.put("materialCount", materials.size());
        item.put("categories", questions.stream()
                .map(Question::getCategoryName)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList()));
        item.put("previewStem", questions.isEmpty() ? null : questions.get(0).getStem());
        item.put("status", group.getStatus() != null && group.getStatus() == 1 ? "PUBLISHED" : "DRAFT");
        return item;
    }

    private Map<String, Object> toShenlunGroupDetail(ShenlunGroup group, List<Question> questions, List<ShenlunGroupMaterial> materials) {
        Map<String, Object> item = toShenlunGroupSummary(group);
        item.put("materials", materials.stream().map(this::toGroupMaterialSummary).collect(Collectors.toList()));
        item.put("questions", questions.stream()
                .map(question -> toQuestionSummary(question, false))
                .collect(Collectors.toList()));
        return item;
    }

    private Map<String, Object> toGroupMaterialSummary(ShenlunGroupMaterial material) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", material.getId());
        item.put("groupId", material.getGroupId());
        item.put("sortNo", material.getSortNo());
        item.put("label", material.getMaterialLabel());
        item.put("title", material.getTitle());
        item.put("content", material.getContent());
        return item;
    }

    private Map<Long, UserQuestionFavorite> loadFavoriteMap(Iterable<Long> questionIds) {
        if (SecurityUtils.getRoles().isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = new ArrayList<>();
        questionIds.forEach(ids::add);
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return userQuestionFavoriteMapper.selectList(new LambdaQueryWrapper<UserQuestionFavorite>()
                        .eq(UserQuestionFavorite::getUserId, SecurityUtils.getUserId())
                        .in(UserQuestionFavorite::getQuestionId, ids))
                .stream()
                .collect(Collectors.toMap(UserQuestionFavorite::getQuestionId, Function.identity(), (left, right) -> left));
    }

    private Map<String, Object> buildFilterOptions(List<Question> questions) {
        Map<String, Object> options = new HashMap<>();
        options.put("provinces", distinctValues(questions.stream().map(Question::getProvince).collect(Collectors.toList())));
        options.put("subjects", distinctValues(questions.stream().map(Question::getSubjectName).collect(Collectors.toList())));
        options.put("categories", distinctValues(questions.stream().map(Question::getCategoryName).collect(Collectors.toList())));
        options.put("questionTypes", distinctValues(questions.stream().map(Question::getQuestionType).collect(Collectors.toList())));
        options.put("difficultyLevels", distinctValues(questions.stream().map(Question::getDifficultyLevel).collect(Collectors.toList())));
        options.put("sources", distinctValues(questions.stream().map(Question::getSourceName).collect(Collectors.toList())));
        return options;
    }

    private List<String> distinctValues(List<String> values) {
        return values.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildPracticeTrend(List<PracticeRecord> records) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            List<PracticeRecord> dailyRecords = records.stream()
                    .filter(record -> record.getSubmittedAt() != null && !record.getSubmittedAt().isBefore(start) && record.getSubmittedAt().isBefore(end))
                    .collect(Collectors.toList());
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("practiceCount", dailyRecords.size());
            item.put("averageAccuracy", dailyRecords.isEmpty()
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : dailyRecords.stream()
                    .map(record -> record.getAccuracyRate() == null ? BigDecimal.ZERO : record.getAccuracyRate())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(dailyRecords.size()), 2, RoundingMode.HALF_UP));
            trend.add(item);
        }
        return trend;
    }

    private void syncWrongQuestion(Long userId, Long questionId, String sourceType, Long recordId, String userAnswer, boolean correct) {
        if (correct) {
            return;
        }
        UserWrongQuestion wrongQuestion = userWrongQuestionMapper.selectOne(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getQuestionId, questionId));
        if (wrongQuestion == null) {
            wrongQuestion = new UserWrongQuestion();
            wrongQuestion.setUserId(userId);
            wrongQuestion.setQuestionId(questionId);
            wrongQuestion.setWrongCount(1);
        } else {
            wrongQuestion.setWrongCount((wrongQuestion.getWrongCount() == null ? 0 : wrongQuestion.getWrongCount()) + 1);
        }
        wrongQuestion.setLatestSourceType(sourceType);
        wrongQuestion.setLatestRecordId(recordId);
        wrongQuestion.setLatestAnswer(userAnswer);
        wrongQuestion.setRemoved(false);
        wrongQuestion.setLastWrongAt(LocalDateTime.now());
        if (wrongQuestion.getId() == null) {
            userWrongQuestionMapper.insert(wrongQuestion);
        } else {
            userWrongQuestionMapper.updateById(wrongQuestion);
        }
    }

    private BigDecimal calculateAccuracy(int correctCount, int questionCount) {
        if (questionCount <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(correctCount * 100.0 / questionCount).setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveSubject(List<Question> questions) {
        return questions.isEmpty() ? null : questions.get(0).getSubjectName();
    }

    private String resolveProvince(List<Question> questions) {
        return questions.isEmpty() ? null : questions.get(0).getProvince();
    }

    private String buildPracticeTitle(String subjectName, int questionCount, String practiceMode) {
        String prefix = StringUtils.hasText(subjectName) ? subjectName : "题库";
        if ("ASSESSMENT".equals(practiceMode)) {
            return prefix + "独立入学评测（" + questionCount + "题）";
        }
        if ("GROUP".equals(practiceMode)) {
            return prefix + "材料组练习（" + questionCount + "题）";
        }
        if ("MOCK_EXAM".equals(practiceMode)) {
            return prefix + "模考模式练习（" + questionCount + "题）";
        }
        if ("TOPIC".equals(practiceMode)) {
            return prefix + "专题练习（" + questionCount + "题）";
        }
        return prefix + "练习（" + questionCount + "题）";
    }

    private String normalizeAnswer(String answer) {
        return QuestionAnswerUtils.normalizeAnswer(answer);
    }

    private String resolvePracticeMode(String mode) {
        if ("TOPIC".equalsIgnoreCase(mode)) {
            return "TOPIC";
        }
        if ("MOCK_EXAM".equalsIgnoreCase(mode)) {
            return "MOCK_EXAM";
        }
        if ("GROUP".equalsIgnoreCase(mode)) {
            return "GROUP";
        }
        if ("ASSESSMENT".equalsIgnoreCase(mode)) {
            return "ASSESSMENT";
        }
        return "RANDOM";
    }

    private String resolvePracticeModeLabel(String mode) {
        if ("ASSESSMENT".equals(mode)) {
            return "独立入学评测";
        }
        if ("TOPIC".equals(mode)) {
            return "专题练习";
        }
        if ("MOCK_EXAM".equals(mode)) {
            return "模考模式";
        }
        if ("PAPER".equals(mode)) {
            return "试卷练习";
        }
        if ("GROUP".equals(mode)) {
            return "材料组练习";
        }
        if ("WRONG_BOOK".equals(mode)) {
            return "错题再练";
        }
        return "随机练习";
    }

    private ShenlunGroup getPublishedShenlunGroup(Long groupId) {
        ShenlunGroup group = shenlunGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == null || group.getStatus() != 1) {
            throw new BizException(404, "申论材料组不存在或未发布");
        }
        return group;
    }

    private List<Question> loadShenlunGroupQuestions(Long groupId) {
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true)
                .eq(Question::getGroupId, groupId)
                .orderByAsc(Question::getGroupQuestionNo)
                .orderByAsc(Question::getId));
    }

    private List<ShenlunGroupMaterial> loadShenlunGroupMaterials(Long groupId) {
        return shenlunGroupMaterialMapper.selectList(new LambdaQueryWrapper<ShenlunGroupMaterial>()
                .eq(ShenlunGroupMaterial::getGroupId, groupId)
                .orderByAsc(ShenlunGroupMaterial::getSortNo)
                .orderByAsc(ShenlunGroupMaterial::getId));
    }

    private Map<String, Object> buildPracticeShenlunGroup(PracticeRecord record, List<Question> questions) {
        if (!"申论".equals(record.getSubjectName())) {
            return null;
        }
        Long groupId = resolvePracticeGroupId(record, questions);
        if (groupId == null) {
            return null;
        }
        ShenlunGroup group = shenlunGroupMapper.selectById(groupId);
        if (group == null) {
            return buildFallbackShenlunGroup(record, questions);
        }
        List<ShenlunGroupMaterial> materials = loadShenlunGroupMaterials(groupId);
        return toShenlunGroupDetail(group, questions, materials);
    }

    private Map<String, Object> buildFallbackShenlunGroup(PracticeRecord record, List<Question> questions) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", null);
        item.put("title", record.getTitle());
        item.put("subjectName", record.getSubjectName());
        item.put("year", null);
        item.put("province", record.getProvince());
        item.put("paperType", null);
        item.put("sourceName", questions.isEmpty() ? null : questions.get(0).getSourceName());
        item.put("materialMode", "QUESTION_LEVEL");
        item.put("questionCount", questions.size());
        item.put("totalScore", questions.stream().map(this::resolveQuestionScore).reduce(0, Integer::sum));
        item.put("materialCount", questions.size());
        item.put("categories", questions.stream().map(Question::getCategoryName).filter(StringUtils::hasText).distinct().collect(Collectors.toList()));
        item.put("previewStem", questions.isEmpty() ? null : questions.get(0).getStem());
        item.put("status", "PUBLISHED");
        item.put("questions", questions.stream().map(question -> toQuestionSummary(question, false)).collect(Collectors.toList()));
        item.put("materials", questions.stream().map(question -> {
            Map<String, Object> material = new HashMap<>();
            material.put("id", question.getId());
            material.put("groupId", null);
            material.put("sortNo", question.getGroupQuestionNo() == null ? 1 : question.getGroupQuestionNo());
            material.put("label", "给定资料" + (question.getGroupQuestionNo() == null ? "" : question.getGroupQuestionNo()));
            material.put("title", null);
            material.put("content", question.getMaterial());
            return material;
        }).collect(Collectors.toList()));
        return item;
    }

    private Long resolvePracticeGroupId(PracticeRecord record, List<Question> questions) {
        Long groupId = parseGroupId(record.getSourceSnapshot());
        if (groupId != null) {
            return groupId;
        }
        Set<Long> groupIds = questions.stream()
                .map(Question::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return groupIds.size() == 1 ? groupIds.iterator().next() : null;
    }

    private Long parseGroupId(String sourceSnapshot) {
        if (!StringUtils.hasText(sourceSnapshot)) {
            return null;
        }
        for (String part : sourceSnapshot.split(";")) {
            if (part.startsWith("shenlun-group:")) {
                try {
                    return Long.parseLong(part.substring("shenlun-group:".length()));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private List<Integer> parseMaterialRefNumbers(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return Collections.emptyList();
        }
        return Arrays.stream(rawValue.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Integer resolveTimeLimitSeconds(PracticeRecord record) {
        if ("TEST_PAPER".equals(record.getPracticeType()) && "PAPER".equals(record.getPracticeMode())) {
            return resolvePaperDurationMinutes(record) * 60;
        }
        if ("ASSESSMENT".equals(record.getPracticeMode())) {
            int questionCount = record.getQuestionCount() == null ? 0 : record.getQuestionCount();
            return Math.max(questionCount * 90, 1800);
        }
        if (!"MOCK_EXAM".equals(record.getPracticeMode())) {
            return null;
        }
        int questionCount = record.getQuestionCount() == null ? 0 : record.getQuestionCount();
        return Math.max(questionCount * 90, 300);
    }

    private int resolvePaperDurationMinutes(PracticeRecord record) {
        String source = record.getSourceSnapshot();
        if (StringUtils.hasText(source)) {
            for (String part : source.split(";")) {
                if (part.startsWith("durationMinutes:")) {
                    try {
                        return Integer.parseInt(part.substring("durationMinutes:".length()));
                    } catch (NumberFormatException ignored) {
                        return 120;
                    }
                }
            }
        }
        return 120;
    }

    private Map<String, Object> buildAssessmentSummary(PracticeRecord record) {
        if (!"GROWTH_ASSESSMENT".equals(record.getPracticeType())) {
            return null;
        }
        UserGrowthAssessmentSession session = userGrowthAssessmentSessionMapper.selectOne(new LambdaQueryWrapper<UserGrowthAssessmentSession>()
                .eq(UserGrowthAssessmentSession::getPracticeRecordId, record.getId())
                .last("limit 1"));
        if (session == null) {
            return null;
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("sessionId", session.getId());
        summary.put("assessmentType", session.getAssessmentType());
        summary.put("assessmentTypeLabel", resolveAssessmentTypeLabel(session.getAssessmentType()));
        summary.put("predictedPassRate", session.getPredictedPassRate());
        summary.put("predictedConfidence", session.getPredictedConfidence());
        summary.put("predictionStage", session.getPredictionStage());
        summary.put("predictionStageLabel", resolveAssessmentStageLabel(session.getPredictionStage()));
        summary.put("questionCount", session.getQuestionCount());
        summary.put("weakKnowledgePoints", session.getWeakKnowledgePoints());
        summary.put("recommendationText", session.getRecommendationText());
        summary.put("assessmentStatus", session.getAssessmentStatus());
        summary.put("assessmentStatusLabel", resolveAssessmentStatusLabel(session.getAssessmentStatus()));
        return summary;
    }

    private String resolveAssessmentTypeLabel(String assessmentType) {
        if ("ENTRY".equals(assessmentType)) {
            return "入学评测";
        }
        if ("FOLLOW_UP".equals(assessmentType)) {
            return "阶段复测";
        }
        return "成长评测";
    }

    private String resolveAssessmentStageLabel(String stage) {
        if ("STABLE".equals(stage)) {
            return "通过把握较高";
        }
        if ("PROMISING".equals(stage)) {
            return "通过前景较好";
        }
        if ("TRACKING".equals(stage)) {
            return "仍需持续补强";
        }
        return "当前需要重点关注";
    }

    private String resolveAssessmentStatusLabel(String status) {
        if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("IN_PROGRESS".equals(status)) {
            return "进行中";
        }
        return "待开始";
    }

    private void updateGrowthAssessmentSession(PracticeRecord record) {
        if (!"GROWTH_ASSESSMENT".equals(record.getPracticeType())) {
            return;
        }
        UserGrowthAssessmentSession session = userGrowthAssessmentSessionMapper.selectOne(new LambdaQueryWrapper<UserGrowthAssessmentSession>()
                .eq(UserGrowthAssessmentSession::getPracticeRecordId, record.getId())
                .last("limit 1"));
        if (session == null) {
            return;
        }
        session.setAssessmentStatus("SUBMITTED".equals(record.getStatus()) ? "COMPLETED" : "IN_PROGRESS");
        userGrowthAssessmentSessionMapper.updateById(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDailyCheckin ensureDailyCheckIn(Long userId, String sourceType, Long sourceRecordId) {
        LocalDate today = LocalDate.now();
        UserDailyCheckin existing = userDailyCheckinMapper.selectOne(new LambdaQueryWrapper<UserDailyCheckin>()
                .eq(UserDailyCheckin::getUserId, userId)
                .eq(UserDailyCheckin::getCheckinDate, today)
                .last("limit 1"));
        if (existing != null) {
            if (!StringUtils.hasText(existing.getSourceType()) && StringUtils.hasText(sourceType)) {
                existing.setSourceType(sourceType);
                existing.setSourceRecordId(sourceRecordId);
                userDailyCheckinMapper.updateById(existing);
            }
            return existing;
        }

        UserDailyCheckin previous = userDailyCheckinMapper.selectOne(new LambdaQueryWrapper<UserDailyCheckin>()
                .eq(UserDailyCheckin::getUserId, userId)
                .lt(UserDailyCheckin::getCheckinDate, today)
                .orderByDesc(UserDailyCheckin::getCheckinDate)
                .last("limit 1"));
        int streakDays = 1;
        if (previous != null && today.minusDays(1).equals(previous.getCheckinDate())) {
            streakDays = (previous.getStreakDays() == null ? 0 : previous.getStreakDays()) + 1;
        }

        UserDailyCheckin checkin = new UserDailyCheckin();
        checkin.setUserId(userId);
        checkin.setCheckinDate(today);
        checkin.setSourceType(StringUtils.hasText(sourceType) ? sourceType : "MANUAL");
        checkin.setSourceRecordId(sourceRecordId);
        checkin.setStreakDays(streakDays);
        userDailyCheckinMapper.insert(checkin);
        return checkin;
    }

    private int calculateCurrentStreak(Long userId) {
        List<UserDailyCheckin> checkins = userDailyCheckinMapper.selectList(new LambdaQueryWrapper<UserDailyCheckin>()
                .eq(UserDailyCheckin::getUserId, userId)
                .orderByDesc(UserDailyCheckin::getCheckinDate)
                .last("limit 1"));
        if (CollectionUtils.isEmpty(checkins)) {
            return 0;
        }
        UserDailyCheckin latest = checkins.get(0);
        return latest.getStreakDays() == null ? 0 : latest.getStreakDays();
    }
}
