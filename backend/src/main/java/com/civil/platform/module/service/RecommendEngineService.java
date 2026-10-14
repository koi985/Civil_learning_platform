package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.module.dto.ExamRegionSaveRequest;
import com.civil.platform.module.dto.RecommendFeedbackRequest;
import com.civil.platform.module.entity.*;
import com.civil.platform.module.mapper.*;
import com.civil.platform.module.service.recommend.UserCfService;
import com.civil.platform.security.LoginUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendEngineService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String GUEST_KEY = "recommend:guest:overview";
    private static final String HOT_KEY = "recommend:pool:hot";
    private static final String HOT_AT_KEY = "recommend:pool:hot:at";
    private static final String USER_AT_KEY = "recommend:pool:user:at";
    private static final String DEFAULT_REGION = "全国";

    private final RecommendConfigMapper configMapper;
    private final RecommendFeedbackMapper feedbackMapper;
    private final RecommendPinnedResourceMapper pinnedMapper;
    private final RecommendRefreshLogMapper refreshLogMapper;
    private final SysUserMapper userMapper;
    private final CourseMapper courseMapper;
    private final NewsMapper newsMapper;
    private final NewsCategoryMapper newsCategoryMapper;
    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final QuestionIrtStatMapper questionIrtStatMapper;
    private final PracticeRecordMapper practiceMapper;
    private final PracticeAnswerMapper practiceAnswerMapper;
    private final ExamRecordMapper examRecordMapper;
    private final UserWrongQuestionMapper wrongMapper;
    private final UserCourseMapper userCourseMapper;
    private final UserCourseStudyProgressMapper progressMapper;
    private final UserCourseLearningRecordMapper learningRecordMapper;
    private final UserKnowledgeMasteryMapper userKnowledgeMasteryMapper;
    private final UserKnowledgeAbilityMapper userKnowledgeAbilityMapper;
    private final UserAbilityProfileMapper userAbilityProfileMapper;
    private final UserStudyPlanMapper userStudyPlanMapper;
    private final RecommendPracticeBundleMapper recommendPracticeBundleMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RecommendRefreshAsyncService asyncService;
    private final IrtCalibrationService irtCalibrationService;
    private final UserCfService userCfService;

    public RecommendEngineService(RecommendConfigMapper configMapper,
                                  RecommendFeedbackMapper feedbackMapper,
                                  RecommendPinnedResourceMapper pinnedMapper,
                                  RecommendRefreshLogMapper refreshLogMapper,
                                  SysUserMapper userMapper,
                                  CourseMapper courseMapper,
                                  NewsMapper newsMapper,
                                  NewsCategoryMapper newsCategoryMapper,
                                  ExamMapper examMapper,
                                  QuestionMapper questionMapper,
                                  QuestionIrtStatMapper questionIrtStatMapper,
                                  PracticeRecordMapper practiceMapper,
                                  PracticeAnswerMapper practiceAnswerMapper,
                                  ExamRecordMapper examRecordMapper,
                                  UserWrongQuestionMapper wrongMapper,
                                  UserCourseMapper userCourseMapper,
                                  UserCourseStudyProgressMapper progressMapper,
                                  UserCourseLearningRecordMapper learningRecordMapper,
                                  UserKnowledgeMasteryMapper userKnowledgeMasteryMapper,
                                  UserKnowledgeAbilityMapper userKnowledgeAbilityMapper,
                                  UserAbilityProfileMapper userAbilityProfileMapper,
                                  UserStudyPlanMapper userStudyPlanMapper,
                                  RecommendPracticeBundleMapper recommendPracticeBundleMapper,
                                  StringRedisTemplate redis,
                                  ObjectMapper objectMapper,
                                  IrtCalibrationService irtCalibrationService,
                                  UserCfService userCfService,
                                  @Lazy RecommendRefreshAsyncService asyncService) {
        this.configMapper = configMapper;
        this.feedbackMapper = feedbackMapper;
        this.pinnedMapper = pinnedMapper;
        this.refreshLogMapper = refreshLogMapper;
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.newsMapper = newsMapper;
        this.newsCategoryMapper = newsCategoryMapper;
        this.examMapper = examMapper;
        this.questionMapper = questionMapper;
        this.questionIrtStatMapper = questionIrtStatMapper;
        this.practiceMapper = practiceMapper;
        this.practiceAnswerMapper = practiceAnswerMapper;
        this.examRecordMapper = examRecordMapper;
        this.wrongMapper = wrongMapper;
        this.userCourseMapper = userCourseMapper;
        this.progressMapper = progressMapper;
        this.learningRecordMapper = learningRecordMapper;
        this.userKnowledgeMasteryMapper = userKnowledgeMasteryMapper;
        this.userKnowledgeAbilityMapper = userKnowledgeAbilityMapper;
        this.userAbilityProfileMapper = userAbilityProfileMapper;
        this.userStudyPlanMapper = userStudyPlanMapper;
        this.recommendPracticeBundleMapper = recommendPracticeBundleMapper;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.irtCalibrationService = irtCalibrationService;
        this.userCfService = userCfService;
        this.asyncService = asyncService;
    }

    public Map<String, Object> overview() {
        return overview(null);
    }

    public Map<String, Object> overview(String explicitRegion) {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            String targetRegion = normalizeRegion(explicitRegion);
            if (!StringUtils.hasText(targetRegion)) {
                targetRegion = DEFAULT_REGION;
            }
            Map<String, Object> cached = readMap(guestKey(targetRegion));
            return cached != null ? withRegionContext(cached, explicitRegion, targetRegion, null) : rebuildGuestCache("AUTO_GUEST", false, targetRegion);
        }
        SysUser user = userMapper.selectById(userId);
        String targetRegion = targetRegion(user, explicitRegion);
        Map<String, Object> cached = readMap(userKey(userId, "overview"));
        if (cached != null && cachedMatchesRegion(cached, targetRegion)) {
            return withRegionContext(cached, explicitRegion, targetRegion, user);
        }
        return rebuildUserCache(userId, "AUTO_USER", false, explicitRegion);
    }

    public Map<String, Object> profile() {
        return profile(null);
    }

    public Map<String, Object> profile(String explicitRegion) {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            return buildProfilePayload(overview(explicitRegion));
        }
        SysUser user = userMapper.selectById(userId);
        String targetRegion = targetRegion(user, explicitRegion);
        Map<String, Object> cached = readMap(userKey(userId, "profile"));
        if (cached != null && cachedMatchesRegion(cached, targetRegion)) {
            return withRegionContext(cached, explicitRegion, targetRegion, user);
        }
        return buildProfilePayload(rebuildUserCache(userId, "AUTO_PROFILE", false, explicitRegion));
    }

    public Map<Long, Map<String, Object>> courseRecommendationMeta(String explicitRegion) {
        List<Map<String, Object>> cards = new ArrayList<>();
        Map<String, Object> cachedOverview = overview(explicitRegion);
        castList(cachedOverview.get("recommendCards")).stream()
                .filter(item -> "COURSE".equals(item.get("resourceType")))
                .forEach(item -> cards.add(new LinkedHashMap<>(item)));

        Long userId = currentUserIdOrNull();
        if (userId == null) {
            cards.addAll(buildGuestCourseRecommendationCards(explicitRegion));
        } else {
            cards.addAll(buildUserCourseRecommendationCards(userId, explicitRegion));
        }

        return deduplicate(cards).stream()
                .filter(item -> "COURSE".equals(item.get("resourceType")))
                .filter(item -> !Boolean.TRUE.equals(item.get("hiddenByFeedback")))
                .sorted((a, b) -> scoreValue(b).compareTo(scoreValue(a)))
                .collect(Collectors.toMap(
                        item -> ((Number) item.get("id")).longValue(),
                        this::courseMeta,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    public Map<String, Object> refresh() {
        return refresh(null);
    }

    public Map<String, Object> refresh(String explicitRegion) {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            insertRefreshLog(null, "MANUAL_GUEST");
            return rebuildGuestCache("MANUAL_GUEST", false, explicitRegion);
        }
        return rebuildUserCache(userId, "MANUAL_USER", true, explicitRegion);
    }

    public Map<String, Object> saveExamRegion(ExamRegionSaveRequest request) {
        Long userId = requireCurrentUserId();
        String province = normalizeRegion(request.getProvince());
        if (!StringUtils.hasText(province)) {
            throw new BizException("报考地区不能为空");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        user.setProvince(province);
        userMapper.updateById(user);
        evictUserCache(userId);
        Map<String, Object> overview = rebuildUserCache(userId, "EXAM_REGION", true, province);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("province", province);
        result.put("portrait", overview.get("portrait"));
        result.put("refreshAt", overview.get("refreshAt"));
        result.put("recommendCards", overview.get("recommendCards"));
        return result;
    }

    public void feedback(RecommendFeedbackRequest request) {
        Long userId = requireCurrentUserId();
        RecommendFeedback existed = feedbackMapper.selectOne(new LambdaQueryWrapper<RecommendFeedback>()
                .eq(RecommendFeedback::getUserId, userId)
                .eq(RecommendFeedback::getResourceType, request.getResourceType())
                .eq(RecommendFeedback::getResourceId, request.getResourceId())
                .eq(RecommendFeedback::getFeedbackType, "NOT_INTERESTED")
                .last("limit 1"));
        if (existed == null) {
            RecommendFeedback feedback = new RecommendFeedback();
            feedback.setUserId(userId);
            feedback.setResourceType(request.getResourceType());
            feedback.setResourceId(request.getResourceId());
            feedback.setFeedbackType("NOT_INTERESTED");
            feedback.setSourcePage(StringUtils.hasText(request.getSourcePage()) ? request.getSourcePage() : "RECOMMEND");
            feedback.setReasonText(request.getReasonText());
            feedbackMapper.insert(feedback);
        }
        enqueueBehaviorRefresh(userId, "FEEDBACK");
    }

    public void enqueueBehaviorRefresh(Long userId, String source) {
        if (userId != null) {
            asyncService.refreshUserAsync(userId, "BEHAVIOR_" + source);
        }
    }

    public Map<String, Object> adminFeedbackSummary() {
        List<RecommendFeedback> feedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<RecommendFeedback>().orderByDesc(RecommendFeedback::getCreatedAt));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("feedbackCount", feedbacks.size());
        result.put("distinctUserCount", feedbacks.stream().map(RecommendFeedback::getUserId).filter(Objects::nonNull).distinct().count());
        result.put("todayFeedbackCount", feedbacks.stream().filter(item -> item.getCreatedAt() != null && item.getCreatedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate())).count());
        result.put("refreshCount", refreshLogMapper.selectCount(null));
        result.put("hotPoolUpdatedAt", readString(HOT_AT_KEY));
        result.put("userPoolUpdatedAt", readString(USER_AT_KEY));
        result.put("pinnedCount", pinnedMapper.selectCount(new LambdaQueryWrapper<RecommendPinnedResource>().eq(RecommendPinnedResource::getEnabled, true)));
        result.put("resourceTypeStats", Arrays.asList(typeStat("COURSE", feedbacks), typeStat("PRACTICE", feedbacks), typeStat("EXAM", feedbacks), typeStat("NEWS", feedbacks)));
        result.put("regionalBehaviorStats", regionalBehaviorStats());
        return result;
    }

    public PageResult<Map<String, Object>> adminFeedbackPage(Long pageNo, Long pageSize, String resourceType) {
        Page<RecommendFeedback> page = feedbackMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<RecommendFeedback>()
                .eq(StringUtils.hasText(resourceType), RecommendFeedback::getResourceType, resourceType)
                .orderByDesc(RecommendFeedback::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(item -> {
            SysUser user = userMapper.selectById(item.getUserId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("nickname", user == null ? "未知用户" : user.getNickname());
            map.put("resourceType", item.getResourceType());
            map.put("resourceTitle", resolveTitle(item.getResourceType(), item.getResourceId()));
            map.put("reasonText", item.getReasonText());
            map.put("createdAt", item.getCreatedAt() == null ? null : item.getCreatedAt().format(FMT));
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    public List<Map<String, Object>> listPinnedResourceView() {
        return pinnedMapper.selectList(new LambdaQueryWrapper<RecommendPinnedResource>()
                        .orderByAsc(RecommendPinnedResource::getSortNo)
                        .orderByAsc(RecommendPinnedResource::getId))
                .stream()
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", item.getId());
                    map.put("resourceType", item.getResourceType());
                    map.put("resourceId", item.getResourceId());
                    map.put("resourceTitle", resolveTitle(item.getResourceType(), item.getResourceId()));
                    map.put("displayTitle", item.getDisplayTitle());
                    map.put("displayReason", item.getDisplayReason());
                    map.put("displayTags", item.getDisplayTags());
                    map.put("actionText", item.getActionText());
                    map.put("regionScope", safeText(item.getRegionScope(), "ALL"));
                    map.put("province", item.getProvince());
                    map.put("fallbackEnabled", item.getFallbackEnabled());
                    map.put("sortNo", item.getSortNo());
                    map.put("enabled", item.getEnabled());
                    map.put("startTime", item.getStartTime());
                    map.put("endTime", item.getEndTime());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public void refreshHotPool() {
        insertRefreshLog(null, "QUARTZ_HOT_POOL");
        Map<String, BigDecimal> weights = loadWeights();
        List<Map<String, Object>> cards = new ArrayList<>(buildPinnedCards(DEFAULT_REGION, Collections.emptySet(), Collections.emptyMap()));
        courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Course::getStatus, "PUBLISHED").orderByDesc(Course::getLearnCount).last("limit 4"))
                .forEach(course -> cards.add(score(
                        buildCourseCard(course, "热门课程推荐", "/course/detail/" + course.getId(), courseAction(course)),
                        weights,
                        0.0,
                        0.0,
                        hotScore(course.getLearnCount()).doubleValue(),
                        BigDecimal.ZERO,
                        Collections.singletonList("热门池兜底"),
                        splitTags(course.getTags())
                )));
        examMapper.selectList(new LambdaQueryWrapper<Exam>().eq(Exam::getStatus, "OPEN").orderByDesc(Exam::getCreatedAt).last("limit 3"))
                .forEach(exam -> cards.add(score(
                        buildCard("EXAM", exam.getId(), exam.getTitle(), null, safeText(exam.getProvince(), exam.getSubjectName()), "近期模考推荐", "/exam/detail/" + exam.getId(), "查看模考"),
                        weights,
                        0.0,
                        0.0,
                        recencyScore(exam.getCreatedAt(), 45).doubleValue(),
                        BigDecimal.ZERO,
                        Collections.singletonList("热门池兜底"),
                        Collections.singletonList(safeText(exam.getProvince(), exam.getSubjectName()))
                )));
        newsMapper.selectList(new LambdaQueryWrapper<News>().eq(News::getStatus, "PUBLISHED").orderByDesc(News::getReadCount).last("limit 3"))
                .forEach(news -> cards.add(score(
                        buildNewsCard(news, "热门资讯推荐", "热门资讯"),
                        weights,
                        0.0,
                        0.0,
                        newsHotScore(news).doubleValue(),
                        BigDecimal.ZERO,
                        Collections.singletonList("热门池兜底"),
                        splitTags(news.getTags())
                )));
        cards.add(score(
                buildPracticeCard(9001L, "近 7 天热门刷题入口", "题库训练,热门练习", "冷启动热门题组，用于没有足够个人行为时快速开始练习", "/search?questionCount=20", "开始练习", 20, 18, "热门题组"),
                weights,
                0.0,
                0.45,
                practiceHotScore(null).doubleValue(),
                BigDecimal.ZERO,
                Arrays.asList("热门池兜底", "热门题组"),
                Arrays.asList("题库训练", "热门练习")
        ));
        writeJson(HOT_KEY, selectRecommendedCards(cards), Duration.ofHours(2));
        writeString(HOT_AT_KEY, nowText(), Duration.ofHours(2));
        rebuildGuestCache("HOT_POOL_REFRESH", false, null);
    }

    public void refreshActiveUserRecommendPool() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        Set<Long> userIds = new LinkedHashSet<>();
        practiceMapper.selectList(new LambdaQueryWrapper<PracticeRecord>().eq(PracticeRecord::getStatus, "SUBMITTED").ge(PracticeRecord::getSubmittedAt, since))
                .forEach(item -> userIds.add(item.getUserId()));
        examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getStatus, "SUBMITTED").ge(ExamRecord::getSubmittedAt, since))
                .forEach(item -> userIds.add(item.getUserId()));
        learningRecordMapper.selectList(new LambdaQueryWrapper<UserCourseLearningRecord>().ge(UserCourseLearningRecord::getCreatedAt, since))
                .forEach(item -> userIds.add(item.getUserId()));
        feedbackMapper.selectList(new LambdaQueryWrapper<RecommendFeedback>().ge(RecommendFeedback::getCreatedAt, since))
                .forEach(item -> userIds.add(item.getUserId()));
        for (Long userId : userIds) {
            rebuildUserCache(userId, "QUARTZ_USER_POOL", true);
        }
        writeString(USER_AT_KEY, nowText(), Duration.ofHours(2));
    }

    public Map<String, Object> rebuildUserCache(Long userId, String source, boolean log) {
        return rebuildUserCache(userId, source, log, null);
    }

    public Map<String, Object> rebuildUserCache(Long userId, String source, boolean log, String explicitRegion) {
        if (log) {
            insertRefreshLog(userId, source);
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        String targetRegion = targetRegion(user, explicitRegion);
        user.setProvince(targetRegion);
        List<PracticeRecord> practices = practiceMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getStatus, "SUBMITTED")
                .orderByDesc(PracticeRecord::getSubmittedAt));
        List<ExamRecord> exams = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getStatus, "SUBMITTED")
                .orderByDesc(ExamRecord::getSubmittedAt));
        List<UserWrongQuestion> wrongs = wrongMapper.selectList(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getRemoved, false));
        List<UserCourse> owned = userCourseMapper.selectList(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId));
        List<UserCourseStudyProgress> progresses = progressMapper.selectList(new LambdaQueryWrapper<UserCourseStudyProgress>()
                .eq(UserCourseStudyProgress::getUserId, userId)
                .orderByDesc(UserCourseStudyProgress::getLastStudyAt));
        List<UserCourseLearningRecord> learningRecords = learningRecordMapper.selectList(new LambdaQueryWrapper<UserCourseLearningRecord>()
                .eq(UserCourseLearningRecord::getUserId, userId)
                .orderByDesc(UserCourseLearningRecord::getCreatedAt)
                .last("limit 30"));
        List<RecommendFeedback> feedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<RecommendFeedback>()
                .eq(RecommendFeedback::getUserId, userId)
                .eq(RecommendFeedback::getFeedbackType, "NOT_INTERESTED"));
        Set<String> feedbackKeys = feedbacks.stream()
                .map(item -> cardKey(item.getResourceType(), item.getResourceId()))
                .collect(Collectors.toSet());
        Map<String, Long> feedbackTypeCounts = feedbacks.stream()
                .collect(Collectors.groupingBy(RecommendFeedback::getResourceType, Collectors.counting()));

        Map<String, BigDecimal> weights = loadWeights();
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("weights", weights);
        overview.put("regionContext", buildRegionContext(explicitRegion, targetRegion, user));
        overview.put("portrait", buildPortrait(user, practices, exams, wrongs, progresses, learningRecords, feedbacks.size()));
        overview.put("continueLearningCourses", buildContinueLearningCourses(owned, progresses));
        overview.put("recommendCards", buildPersonalizedCards(user, practices, exams, wrongs, owned, progresses, feedbackKeys, feedbackTypeCounts, weights));
        overview.put("loggedIn", true);
        overview.put("feedbackEnabled", true);
        overview.put("refreshAt", nowText());
        overview.put("refreshCount", currentRefreshCount(userId));
        overview.put("refreshSource", source);
        overview.put("cacheMode", "REDIS");
        overview.put("hotPoolUpdatedAt", readString(HOT_AT_KEY));
        overview.put("userPoolUpdatedAt", readString(USER_AT_KEY));
        writeJson(userKey(userId, "overview"), overview, Duration.ofHours(6));
        writeJson(userKey(userId, "profile"), buildProfilePayload(overview), Duration.ofHours(6));
        writeJson(userKey(userId, "portrait"), overview.get("portrait"), Duration.ofHours(6));
        return overview;
    }

    private Map<String, Object> rebuildGuestCache(String source, boolean log, String explicitRegion) {
        if (log) {
            insertRefreshLog(null, source);
        }
        String targetRegion = normalizeRegion(explicitRegion);
        if (!StringUtils.hasText(targetRegion)) {
            targetRegion = DEFAULT_REGION;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("weights", loadWeights());
        map.put("regionContext", buildRegionContext(explicitRegion, targetRegion, null));
        map.put("portrait", guestPortrait(targetRegion));
        map.put("continueLearningCourses", new ArrayList<>());
        map.put("recommendCards", guestCards(targetRegion));
        map.put("loggedIn", false);
        map.put("feedbackEnabled", false);
        map.put("refreshAt", nowText());
        map.put("refreshCount", currentRefreshCount(null));
        map.put("refreshSource", source);
        map.put("cacheMode", "REDIS");
        map.put("hotPoolUpdatedAt", readString(HOT_AT_KEY));
        map.put("userPoolUpdatedAt", readString(USER_AT_KEY));
        writeJson(guestKey(targetRegion), map, Duration.ofMinutes(30));
        return map;
    }

    private List<Map<String, Object>> buildPersonalizedCards(SysUser user,
                                                             List<PracticeRecord> practices,
                                                             List<ExamRecord> exams,
                                                             List<UserWrongQuestion> wrongs,
                                                             List<UserCourse> owned,
                                                             List<UserCourseStudyProgress> progresses,
                                                             Set<String> feedbackKeys,
                                                             Map<String, Long> feedbackTypeCounts,
                                                             Map<String, BigDecimal> weights) {
        String targetRegion = targetRegion(user, null);
        List<Map<String, Object>> cards = new ArrayList<>(buildPinnedCards(targetRegion, feedbackKeys, feedbackTypeCounts));
        Set<Long> ownedCourseIds = owned.stream().map(UserCourse::getCourseId).collect(Collectors.toSet());
        String weak = weakSubject(user, practices);
        Map<String, BigDecimal> userTagWeights = buildUserTagWeights(user, practices, exams, wrongs, progresses, weak);
        Map<Long, BigDecimal> collaborativeCourseScores = collaborativeCourseScores(user.getId(), owned, progresses, practices, exams);
        Map<Long, BigDecimal> collaborativeExamScores = collaborativeExamScores(user.getId(), owned, progresses, practices, exams);
        Map<Long, BigDecimal> collaborativeNewsScores = collaborativeNewsScores(user.getId(), userTagWeights, owned, progresses, practices, exams);
        BigDecimal collaborativePracticeScore = collaborativePracticeScore(userTagWeights, weak);
        Map<Long, Long> examPopularity = examPopularityMap();

        progresses.stream()
                .filter(item -> progressBetween(item, 0, 100))
                .limit(3)
                .forEach(item -> {
                    Course course = courseMapper.selectById(item.getCourseId());
                    if (course == null) {
                        return;
                    }
                    Map<String, BigDecimal> resourceTags = courseTagWeights(course);
                    BigDecimal collaborative = collaborativeCourseScores.getOrDefault(course.getId(), BigDecimal.ZERO);
                    BigDecimal content = resourceTagSimilarity(userTagWeights, resourceTags);
                    BigDecimal hot = hotScore(course.getLearnCount());
                    List<String> matchedTags = matchingTags(userTagWeights, resourceTags);
                    cards.add(score(
                            buildCourseCard(course, "基于你最近的学习进度，建议继续完成形成稳定节奏", "/course/play/" + course.getId(), "继续学习"),
                            weights,
                            collaborative.doubleValue(),
                            content.doubleValue(),
                            hot.doubleValue(),
                            penalty("COURSE", course.getId(), feedbackKeys, feedbackTypeCounts),
                            buildExplainReasons(collaborative, content, hot, matchedTags, "学习进度命中"),
                            matchedTags
                    ));
                });

        courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "PUBLISHED")
                        .orderByDesc(Course::getLearnCount)
                        .last("limit 18"))
                .stream()
                .filter(item -> !ownedCourseIds.contains(item.getId()))
                .forEach(course -> {
                    Map<String, BigDecimal> resourceTags = courseTagWeights(course);
                    BigDecimal collaborative = collaborativeCourseScores.getOrDefault(course.getId(), BigDecimal.ZERO);
                    BigDecimal content = resourceTagSimilarity(userTagWeights, resourceTags);
                    BigDecimal contentWithRegion = content.add(regionBoost(course.getProvince(), targetRegion, weights)).min(BigDecimal.ONE);
                    BigDecimal hot = hotScore(course.getLearnCount());
                    List<String> matchedTags = matchingTags(userTagWeights, resourceTags);
                    List<String> reasons = buildExplainReasons(collaborative, contentWithRegion, hot, matchedTags, null);
                    addRegionReason(reasons, "课程", course.getProvince(), targetRegion);
                    String reason = weak.equals(course.getSubjectName())
                            ? "薄弱科目、地区偏好与相似学员行为共同命中的补强课程"
                            : "根据你的备考方向、标签匹配和相似学员学习行为推荐";
                    cards.add(score(
                            buildCourseCard(course, reason, "/course/detail/" + course.getId(), courseAction(course)),
                            weights,
                            collaborative.doubleValue(),
                            contentWithRegion.doubleValue(),
                            hot.doubleValue(),
                            penalty("COURSE", course.getId(), feedbackKeys, feedbackTypeCounts).add(regionPenalty(course.getProvince(), targetRegion, weights)),
                            reasons,
                            matchedTags
                    ));
                });

        cards.addAll(buildPracticeRecommendationCards(user, practices, exams, wrongs, userTagWeights, weak,
                collaborativePracticeScore, feedbackKeys, feedbackTypeCounts, weights));

        examMapper.selectList(new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getStatus, "OPEN")
                        .orderByDesc(Exam::getCreatedAt)
                        .last("limit 10"))
                .forEach(exam -> {
                    Map<String, BigDecimal> resourceTags = examTagWeights(exam);
                    BigDecimal collaborative = collaborativeExamScores.getOrDefault(exam.getId(), BigDecimal.ZERO);
                    BigDecimal content = resourceTagSimilarity(userTagWeights, resourceTags);
                    BigDecimal contentWithRegion = content.add(regionBoost(exam.getProvince(), targetRegion, weights)).min(BigDecimal.ONE);
                    BigDecimal hot = examHotScore(exam, examPopularity);
                    List<String> matchedTags = matchingTags(userTagWeights, resourceTags);
                    List<String> reasons = buildExplainReasons(collaborative, contentWithRegion, hot, matchedTags, null);
                    addRegionReason(reasons, "模考", exam.getProvince(), targetRegion);
                    String reason = exams.isEmpty()
                            ? "结合你的地区、考试类型和相似学员行为，优先推荐入门模考"
                            : "结合你的备考标签、近期模考热度和相似学员参与情况推荐";
                    cards.add(score(
                            buildCard("EXAM", exam.getId(), exam.getTitle(), null, safeText(exam.getProvince(), exam.getSubjectName()), reason, "/exam/detail/" + exam.getId(), "查看模考"),
                            weights,
                            collaborative.doubleValue(),
                            contentWithRegion.doubleValue(),
                            hot.doubleValue(),
                            penalty("EXAM", exam.getId(), feedbackKeys, feedbackTypeCounts).add(regionPenalty(exam.getProvince(), targetRegion, weights)),
                            reasons,
                            matchedTags
                    ));
                });

        cards.addAll(buildNewsRecommendationCards(user, practices, exams, wrongs, userTagWeights,
                collaborativeNewsScores, feedbackKeys, feedbackTypeCounts, weights));

        guestCards(targetRegion).forEach(item -> cards.add(score(
                new LinkedHashMap<>(item),
                weights,
                0.0,
                0.0,
                0.60,
                penalty(String.valueOf(item.get("resourceType")), ((Number) item.get("id")).longValue(), feedbackKeys, feedbackTypeCounts),
                Collections.singletonList("热门池兜底"),
                splitTags(String.valueOf(item.get("tags")))
        )));
        return selectRecommendedCards(cards).stream()
                .filter(item -> !Boolean.TRUE.equals(item.get("hiddenByFeedback")))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildUserCourseRecommendationCards(Long userId, String explicitRegion) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return buildGuestCourseRecommendationCards(explicitRegion);
        }
        String targetRegion = targetRegion(user, explicitRegion);
        List<PracticeRecord> practices = practiceMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getStatus, "SUBMITTED")
                .orderByDesc(PracticeRecord::getSubmittedAt));
        List<ExamRecord> exams = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getStatus, "SUBMITTED")
                .orderByDesc(ExamRecord::getSubmittedAt));
        List<UserWrongQuestion> wrongs = wrongMapper.selectList(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getRemoved, false));
        List<UserCourse> owned = userCourseMapper.selectList(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId));
        List<UserCourseStudyProgress> progresses = progressMapper.selectList(new LambdaQueryWrapper<UserCourseStudyProgress>()
                .eq(UserCourseStudyProgress::getUserId, userId)
                .orderByDesc(UserCourseStudyProgress::getLastStudyAt));
        List<RecommendFeedback> feedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<RecommendFeedback>()
                .eq(RecommendFeedback::getUserId, userId)
                .eq(RecommendFeedback::getFeedbackType, "NOT_INTERESTED"));
        Set<String> feedbackKeys = feedbacks.stream()
                .map(item -> cardKey(item.getResourceType(), item.getResourceId()))
                .collect(Collectors.toSet());
        Map<String, Long> feedbackTypeCounts = feedbacks.stream()
                .collect(Collectors.groupingBy(RecommendFeedback::getResourceType, Collectors.counting()));
        Set<Long> ownedCourseIds = owned.stream().map(UserCourse::getCourseId).collect(Collectors.toSet());
        String weak = weakSubject(user, practices);
        Map<String, BigDecimal> userTagWeights = buildUserTagWeights(user, practices, exams, wrongs, progresses, weak);
        Map<Long, BigDecimal> collaborativeCourseScores = collaborativeCourseScores(user.getId(), owned, progresses, practices, exams);
        Map<String, BigDecimal> weights = loadWeights();
        List<Map<String, Object>> cards = new ArrayList<>(buildPinnedCards(targetRegion, feedbackKeys, feedbackTypeCounts));

        progresses.stream()
                .filter(item -> progressBetween(item, 0, 100))
                .limit(8)
                .forEach(item -> {
                    Course course = courseMapper.selectById(item.getCourseId());
                    if (course == null || !"PUBLISHED".equals(course.getStatus())) {
                        return;
                    }
                    Map<String, BigDecimal> resourceTags = courseTagWeights(course);
                    BigDecimal collaborative = collaborativeCourseScores.getOrDefault(course.getId(), BigDecimal.ZERO);
                    BigDecimal content = resourceTagSimilarity(userTagWeights, resourceTags);
                    BigDecimal hot = hotScore(course.getLearnCount());
                    List<String> matchedTags = matchingTags(userTagWeights, resourceTags);
                    cards.add(score(
                            buildCourseCard(course, "基于你最近的学习进度，建议继续完成形成稳定节奏", "/course/play/" + course.getId(), "继续学习"),
                            weights,
                            collaborative.doubleValue(),
                            content.doubleValue(),
                            hot.doubleValue(),
                            penalty("COURSE", course.getId(), feedbackKeys, feedbackTypeCounts),
                            buildExplainReasons(collaborative, content, hot, matchedTags, "学习进度命中"),
                            matchedTags
                    ));
                });

        courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "PUBLISHED")
                        .orderByDesc(Course::getLearnCount)
                        .orderByDesc(Course::getCreatedAt)
                        .last("limit 80"))
                .stream()
                .filter(item -> !ownedCourseIds.contains(item.getId()))
                .forEach(course -> {
                    Map<String, BigDecimal> resourceTags = courseTagWeights(course);
                    BigDecimal collaborative = collaborativeCourseScores.getOrDefault(course.getId(), BigDecimal.ZERO);
                    BigDecimal content = resourceTagSimilarity(userTagWeights, resourceTags);
                    BigDecimal contentWithRegion = content.add(regionBoost(course.getProvince(), targetRegion, weights)).min(BigDecimal.ONE);
                    BigDecimal hot = hotScore(course.getLearnCount());
                    List<String> matchedTags = matchingTags(userTagWeights, resourceTags);
                    List<String> reasons = buildExplainReasons(collaborative, contentWithRegion, hot, matchedTags, null);
                    addRegionReason(reasons, "课程", course.getProvince(), targetRegion);
                    String reason = weak.equals(course.getSubjectName())
                            ? "薄弱科目、地区偏好与相似学员行为共同命中的补强课程"
                            : "根据你的备考方向、标签匹配和相似学员学习行为推荐";
                    cards.add(score(
                            buildCourseCard(course, reason, "/course/detail/" + course.getId(), courseAction(course)),
                            weights,
                            collaborative.doubleValue(),
                            contentWithRegion.doubleValue(),
                            hot.doubleValue(),
                            penalty("COURSE", course.getId(), feedbackKeys, feedbackTypeCounts).add(regionPenalty(course.getProvince(), targetRegion, weights)),
                            reasons,
                            matchedTags
                    ));
                });
        return cards;
    }

    private List<Map<String, Object>> buildGuestCourseRecommendationCards(String explicitRegion) {
        String targetRegion = normalizeRegion(explicitRegion);
        if (!StringUtils.hasText(targetRegion)) {
            targetRegion = DEFAULT_REGION;
        }
        Map<String, BigDecimal> weights = loadWeights();
        List<Map<String, Object>> cards = new ArrayList<>(buildPinnedCards(targetRegion, Collections.emptySet(), Collections.emptyMap()));
        String normalizedRegion = targetRegion;
        courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "PUBLISHED")
                        .orderByDesc(Course::getLearnCount)
                        .orderByDesc(Course::getCreatedAt)
                        .last("limit 80"))
                .forEach(course -> {
                    BigDecimal regionBoost = regionBoost(course.getProvince(), normalizedRegion, weights);
                    BigDecimal penalty = regionPenalty(course.getProvince(), normalizedRegion, weights);
                    List<String> reasons = new ArrayList<>(Collections.singletonList("热门池兜底"));
                    addRegionReason(reasons, "课程", course.getProvince(), normalizedRegion);
                    cards.add(score(
                            buildCourseCard(course, regionAwareReason("热门课程兜底推荐", "课程", course.getProvince(), normalizedRegion), "/course/detail/" + course.getId(), courseAction(course)),
                            weights,
                            0.0,
                            regionBoost.doubleValue(),
                            hotScore(course.getLearnCount()).doubleValue(),
                            penalty,
                            reasons,
                            splitTags(course.getTags())
                    ));
                });
        return cards;
    }

    private List<Map<String, Object>> buildPracticeRecommendationCards(SysUser user,
                                                                       List<PracticeRecord> practices,
                                                                       List<ExamRecord> exams,
                                                                       List<UserWrongQuestion> wrongs,
                                                                       Map<String, BigDecimal> userTagWeights,
                                                                       String weak,
                                                                       BigDecimal collaborativePracticeScore,
                                                                       Set<String> feedbackKeys,
                                                                       Map<String, Long> feedbackTypeCounts,
                                                                       Map<String, BigDecimal> weights) {
        ensureIrtCalibration(user.getId());
        List<Map<String, Object>> practiceCards = buildAdaptivePracticeCards(user, practices, exams, wrongs, userTagWeights,
                collaborativePracticeScore, feedbackKeys, feedbackTypeCounts, weights);
        if (!practiceCards.isEmpty()) {
            return practiceCards;
        }
        String province = safeText(user.getProvince(), "");
        String preferred = safeText(user.getPreferredSubject(), "行测");
        int hotCount = recommendedQuestionCount(preferred, "", null, 20);
        return Collections.singletonList(score(
                buildPracticeCard(1006L, "热门刷题包 " + hotCount + " 题", tagText("全国", preferred, "热门练习"),
                        "当前 IRT 校准数据不足，先按掌握度和热门题组兜底推荐", practicePath("", preferred, null, hotCount),
                        "开始练习", hotCount, estimateMinutes(hotCount), "热门兜底"),
                weights,
                0.0,
                resourceTagSimilarity(userTagWeights, practiceTagWeights(user, preferred)).doubleValue(),
                practiceHotScore(preferred).max(BigDecimal.valueOf(0.35)).doubleValue(),
                penalty("PRACTICE", 1006L, feedbackKeys, feedbackTypeCounts),
                Arrays.asList("IRT 数据不足", "热门题组兜底"),
                Arrays.asList(preferred, "热门练习")
        ));
    }

    private List<Map<String, Object>> buildAdaptivePracticeCards(SysUser user,
                                                                 List<PracticeRecord> practices,
                                                                 List<ExamRecord> exams,
                                                                 List<UserWrongQuestion> wrongs,
                                                                 Map<String, BigDecimal> userTagWeights,
                                                                 BigDecimal collaborativePracticeScore,
                                                                 Set<String> feedbackKeys,
                                                                 Map<String, Long> feedbackTypeCounts,
                                                                 Map<String, BigDecimal> weights) {
        List<UserKnowledgeMastery> masteries = userKnowledgeMasteryMapper.selectList(new LambdaQueryWrapper<UserKnowledgeMastery>()
                .eq(UserKnowledgeMastery::getUserId, user.getId())
                .orderByDesc(UserKnowledgeMastery::getPriorityScore)
                .orderByAsc(UserKnowledgeMastery::getMasteryScore)
                .last("limit 18"));
        List<UserKnowledgeAbility> abilities = userKnowledgeAbilityMapper.selectList(new LambdaQueryWrapper<UserKnowledgeAbility>()
                .eq(UserKnowledgeAbility::getUserId, user.getId()));
        UserAbilityProfile abilityProfile = userAbilityProfileMapper.selectOne(new LambdaQueryWrapper<UserAbilityProfile>()
                .eq(UserAbilityProfile::getUserId, user.getId())
                .last("limit 1"));
        Map<Long, BigDecimal> questionDifficulty = loadQuestionDifficultyMap();
        Map<Long, QuestionIrtStat> questionStatMap = questionIrtStatMapper.selectList(null).stream()
                .collect(Collectors.toMap(QuestionIrtStat::getQuestionId, item -> item, (left, right) -> left));
        Map<String, UserKnowledgeAbility> abilityMap = abilities.stream()
                .collect(Collectors.toMap(item -> abilityKey(item.getSubjectName(), item.getKnowledgePoint()),
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new));
        Map<Long, Question> questionMap = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                        .eq(Question::getEnabled, true))
                .stream()
                .collect(Collectors.toMap(Question::getId, item -> item, (left, right) -> left));
        Map<Long, Integer> wrongCountMap = wrongs.stream()
                .collect(Collectors.toMap(UserWrongQuestion::getQuestionId,
                        item -> item.getWrongCount() == null ? 1 : item.getWrongCount(),
                        Integer::sum));
        Set<String> wrongKnowledge = wrongs.isEmpty()
                ? Collections.emptySet()
                : questionMapper.selectBatchIds(wrongs.stream().map(UserWrongQuestion::getQuestionId).collect(Collectors.toSet())).stream()
                .flatMap(item -> splitKnowledge(item.getKnowledgePoints()).stream())
                .collect(Collectors.toSet());
        Set<Long> recentQuestionIds = recentPracticeQuestionIds(user.getId(), 7);
        List<String> planKnowledge = loadPlanKnowledge(user.getId());

        List<TargetKnowledge> weakTargets = masteries.stream()
                .filter(item -> "WEAK".equals(item.getKnowledgeStatus()) || "FOUNDATION".equals(item.getKnowledgeStatus()))
                .map(item -> toTargetKnowledge(item, abilityMap))
                .collect(Collectors.toList());
        List<TargetKnowledge> lowConfidenceTargets = masteries.stream()
                .filter(item -> item.getMasteryConfidence() != null && item.getMasteryConfidence().compareTo(BigDecimal.valueOf(40)) < 0)
                .map(item -> toTargetKnowledge(item, abilityMap))
                .collect(Collectors.toList());
        List<TargetKnowledge> stableTargets = masteries.stream()
                .filter(item -> "IMPROVING".equals(item.getKnowledgeStatus()) || "MASTERED".equals(item.getKnowledgeStatus()))
                .sorted(Comparator.comparing(UserKnowledgeMastery::getMasteryScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> toTargetKnowledge(item, abilityMap))
                .collect(Collectors.toList());

        List<Map<String, Object>> cards = new ArrayList<>();
        if (!weakTargets.isEmpty()) {
            cards.add(buildAdaptivePracticeCard(user, "WEAKNESS", weakTargets.subList(0, Math.min(3, weakTargets.size())),
                    chooseQuestionCount(abilityProfile, "WEAKNESS"), userTagWeights, collaborativePracticeScore,
                    feedbackKeys, feedbackTypeCounts, weights, questionMap, questionDifficulty, questionStatMap, recentQuestionIds,
                    wrongCountMap, wrongKnowledge, "优先补齐当前高优先级薄弱知识点"));
        }
        if (!lowConfidenceTargets.isEmpty() || masteries.isEmpty()) {
            List<TargetKnowledge> diagnosisTargets = !lowConfidenceTargets.isEmpty()
                    ? lowConfidenceTargets.subList(0, Math.min(3, lowConfidenceTargets.size()))
                    : planOrSubjectTargets(user, planKnowledge, questionMap, abilityMap);
            if (!diagnosisTargets.isEmpty()) {
                cards.add(buildAdaptivePracticeCard(user, "DIAGNOSIS", diagnosisTargets,
                        12, userTagWeights, collaborativePracticeScore.multiply(BigDecimal.valueOf(0.82)),
                        feedbackKeys, feedbackTypeCounts, weights, questionMap, questionDifficulty, questionStatMap, recentQuestionIds,
                        wrongCountMap, wrongKnowledge, "当前若干知识点样本不足，先做诊断型刷题校准能力"));
            }
        }
        if (!stableTargets.isEmpty()) {
            cards.add(buildAdaptivePracticeCard(user, "CONSOLIDATION", stableTargets.subList(0, Math.min(2, stableTargets.size())),
                    chooseQuestionCount(abilityProfile, "CONSOLIDATION"), userTagWeights,
                    collaborativePracticeScore.multiply(BigDecimal.valueOf(0.76)), feedbackKeys, feedbackTypeCounts, weights,
                    questionMap, questionDifficulty, questionStatMap, recentQuestionIds, wrongCountMap, wrongKnowledge,
                    "对已进入提升区间的知识点做稳定性复现"));
        }
        if (abilityProfile != null
                && abilityProfile.getAbilityConfidence() != null
                && abilityProfile.getAbilityConfidence().compareTo(BigDecimal.valueOf(60)) >= 0
                && !stableTargets.isEmpty()) {
            cards.add(buildAdaptivePracticeCard(user, "CHALLENGE", stableTargets.subList(0, Math.min(2, stableTargets.size())),
                    chooseQuestionCount(abilityProfile, "CHALLENGE"), userTagWeights,
                    collaborativePracticeScore.multiply(BigDecimal.valueOf(0.68)), feedbackKeys, feedbackTypeCounts, weights,
                    questionMap, questionDifficulty, questionStatMap, recentQuestionIds, wrongCountMap, wrongKnowledge,
                    "按当前能力上浮难度，给稳定知识点加入提分挑战"));
        }
        return cards.stream()
                .filter(Objects::nonNull)
                .limit(4)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildAdaptivePracticeCard(SysUser user,
                                                          String recommendMode,
                                                          List<TargetKnowledge> targets,
                                                          int questionCount,
                                                          Map<String, BigDecimal> userTagWeights,
                                                          BigDecimal collaborativePracticeScore,
                                                          Set<String> feedbackKeys,
                                                          Map<String, Long> feedbackTypeCounts,
                                                          Map<String, BigDecimal> weights,
                                                          Map<Long, Question> questionMap,
                                                          Map<Long, BigDecimal> questionDifficulty,
                                                          Map<Long, QuestionIrtStat> questionStatMap,
                                                          Set<Long> recentQuestionIds,
                                                          Map<Long, Integer> wrongCountMap,
                                                          Set<String> wrongKnowledge,
                                                          String baseReason) {
        if (targets == null || targets.isEmpty()) {
            return null;
        }
        String province = safeText(user.getProvince(), "");
        String subjectName = targets.stream().map(item -> item.subjectName).filter(StringUtils::hasText).findFirst()
                .orElse(safeText(user.getPreferredSubject(), "行测"));
        List<QuestionScore> selected = selectAdaptiveQuestions(recommendMode, subjectName, province, questionCount, targets, questionMap,
                questionDifficulty, questionStatMap, recentQuestionIds, wrongCountMap, wrongKnowledge);
        if (selected.isEmpty()) {
            return null;
        }
        List<Long> questionIds = selected.stream().map(item -> item.question.getId()).collect(Collectors.toList());
        double averageB = selected.stream().mapToDouble(item -> item.difficulty.doubleValue()).average().orElse(0.0d);
        double avgAbilityFit = selected.stream().mapToDouble(item -> item.matchScore).average().orElse(0.0d);
        List<String> knowledgePoints = targets.stream().map(item -> item.knowledgePoint).filter(StringUtils::hasText).distinct()
                .limit(5).collect(Collectors.toList());
        String title = buildAdaptivePracticeTitle(recommendMode, subjectName, knowledgePoints, questionIds.size());
        String reason = buildAdaptiveReason(recommendMode, baseReason, knowledgePoints);
        BigDecimal contentScore = adaptiveContentScore(userTagWeights, user, subjectName, knowledgePoints, avgAbilityFit);
        BigDecimal hotScore = adaptiveHotScore(subjectName, selected, questionStatMap);
        List<String> matchedTags = adaptiveTags(user, subjectName, knowledgePoints, recommendMode);
        RecommendPracticeBundle bundle = persistPracticeBundle(user.getId(), title, recommendMode, subjectName, province, knowledgePoints,
                questionIds, selected.size(), thetaRange(targets), difficultyRange(selected), decimal(avgAbilityFit), baseReason);

        Map<String, Object> card = buildPracticeCard(bundle.getId(), title, tagText(province, subjectName, recommendMode),
                reason, null, actionTextForMode(recommendMode), selected.size(), estimateMinutes(selected.size()),
                sourceLabelForMode(recommendMode));
        card.put("practiceBundleId", bundle.getId());
        card.put("path", null);
        card.put("recommendMode", recommendMode);
        card.put("knowledgePoints", knowledgePoints);
        card.put("thetaRange", thetaRange(targets));
        card.put("difficultyRange", difficultyRange(selected));
        card.put("abilityFitScore", decimal(avgAbilityFit));
        card.put("targetQuestionIds", questionIds);
        card.put("regionMatched", selected.stream().anyMatch(item -> province.equals(item.question.getProvince())));
        List<String> explainReasons = buildAdaptiveExplainReasons(recommendMode, targets, averageB, knowledgePoints, province, selected,
                recentQuestionIds);
        return score(card, weights,
                collaborativePracticeScore.doubleValue(),
                contentScore.doubleValue(),
                hotScore.doubleValue(),
                penalty("PRACTICE", bundle.getId(), feedbackKeys, feedbackTypeCounts),
                explainReasons,
                matchedTags);
    }

    private List<QuestionScore> selectAdaptiveQuestions(String recommendMode,
                                                        String subjectName,
                                                        String province,
                                                        int questionCount,
                                                        List<TargetKnowledge> targets,
                                                        Map<Long, Question> questionMap,
                                                        Map<Long, BigDecimal> questionDifficulty,
                                                        Map<Long, QuestionIrtStat> questionStatMap,
                                                        Set<Long> recentQuestionIds,
                                                        Map<Long, Integer> wrongCountMap,
                                                        Set<String> wrongKnowledge) {
        List<Question> regionCandidates = questionMap.values().stream()
                .filter(this::isObjectiveQuestion)
                .filter(item -> subjectName.equals(item.getSubjectName()))
                .filter(item -> !StringUtils.hasText(province) || province.equals(item.getProvince()))
                .collect(Collectors.toList());
        List<Question> candidates = regionCandidates.size() >= questionCount
                ? regionCandidates
                : questionMap.values().stream()
                .filter(this::isObjectiveQuestion)
                .filter(item -> subjectName.equals(item.getSubjectName()))
                .collect(Collectors.toList());
        List<QuestionScore> scored = new ArrayList<>();
        for (Question question : candidates) {
            BigDecimal difficulty = questionDifficulty.getOrDefault(question.getId(), fallbackDifficulty(question.getDifficultyLevel()));
            QuestionIrtStat stat = questionStatMap.get(question.getId());
            List<String> knowledge = splitKnowledge(question.getKnowledgePoints());
            double bestKnowledgeMatch = 0.0d;
            double bestAbilityFit = 0.0d;
            for (TargetKnowledge target : targets) {
                if (!knowledge.contains(target.knowledgePoint)) {
                    continue;
                }
                bestKnowledgeMatch = Math.max(bestKnowledgeMatch, 1.0d);
                bestAbilityFit = Math.max(bestAbilityFit, abilityFitForMode(recommendMode, target.thetaValue(), difficulty.doubleValue()));
            }
            if ("DIAGNOSIS".equals(recommendMode) && bestKnowledgeMatch == 0.0d && targets.stream().anyMatch(item -> item.subjectName.equals(question.getSubjectName()))) {
                bestKnowledgeMatch = 0.55d;
                bestAbilityFit = Math.max(bestAbilityFit, diagnosisFit(targets, difficulty.doubleValue()));
            }
            if (bestKnowledgeMatch == 0.0d) {
                continue;
            }
            double freshness = recentQuestionIds.contains(question.getId()) ? 0.15d : 1.0d;
            double novelty = (stat != null && stat.getSampleCount() != null && stat.getSampleCount() >= 1) ? 0.75d : 0.55d;
            double regionFit = !StringUtils.hasText(province) || province.equals(question.getProvince()) ? 1.0d : 0.65d;
            double wrongBoost = wrongCountMap.getOrDefault(question.getId(), 0) > 0 ? 1.0d
                    : knowledge.stream().anyMatch(wrongKnowledge::contains) ? 0.65d : 0.0d;
            double discriminationProxy = stat == null || stat.getSampleCount() == null ? 0.45d
                    : Math.min(stat.getSampleCount(), 120) / 120.0d;
            double matchScore = bestKnowledgeMatch * 0.45d
                    + bestAbilityFit * 0.20d
                    + discriminationProxy * 0.15d
                    + wrongBoost * 0.10d
                    + ((freshness + novelty + regionFit) / 3.0d) * 0.10d;
            QuestionScore score = new QuestionScore();
            score.question = question;
            score.difficulty = difficulty;
            score.matchScore = matchScore;
            scored.add(score);
        }
        scored.sort(Comparator.comparingDouble((QuestionScore item) -> item.matchScore).reversed()
                .thenComparing(item -> item.question.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())));
        return applyQuestionQuota(scored, targets, questionCount);
    }

    private List<QuestionScore> applyQuestionQuota(List<QuestionScore> scored, List<TargetKnowledge> targets, int questionCount) {
        if (scored.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> quotas = new LinkedHashMap<>();
        if (!targets.isEmpty()) {
            quotas.put(targets.get(0).knowledgePoint, Math.max(2, Math.round(questionCount * 0.5f)));
        }
        if (targets.size() > 1) {
            quotas.put(targets.get(1).knowledgePoint, Math.max(2, Math.round(questionCount * 0.3f)));
        }
        if (targets.size() > 2) {
            quotas.put(targets.get(2).knowledgePoint, Math.max(2, questionCount - quotas.values().stream().mapToInt(Integer::intValue).sum()));
        }
        List<QuestionScore> selected = new ArrayList<>();
        Set<Long> chosenIds = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> entry : quotas.entrySet()) {
            String knowledge = entry.getKey();
            int quota = entry.getValue();
            for (QuestionScore score : scored) {
                if (selected.size() >= questionCount || quota <= 0 || chosenIds.contains(score.question.getId())) {
                    continue;
                }
                if (splitKnowledge(score.question.getKnowledgePoints()).contains(knowledge)) {
                    selected.add(score);
                    chosenIds.add(score.question.getId());
                    quota--;
                }
            }
        }
        for (QuestionScore score : scored) {
            if (selected.size() >= questionCount) {
                break;
            }
            if (chosenIds.add(score.question.getId())) {
                selected.add(score);
            }
        }
        return selected;
    }

    private RecommendPracticeBundle persistPracticeBundle(Long userId,
                                                          String title,
                                                          String recommendMode,
                                                          String subjectName,
                                                          String province,
                                                          List<String> knowledgePoints,
                                                          List<Long> questionIds,
                                                          int questionCount,
                                                          String thetaRange,
                                                          String difficultyRange,
                                                          BigDecimal abilityFitScore,
                                                          String sourceStrategy) {
        RecommendPracticeBundle bundle = new RecommendPracticeBundle();
        bundle.setUserId(userId);
        bundle.setTitle(title);
        bundle.setRecommendMode(recommendMode);
        bundle.setSubjectName(subjectName);
        bundle.setProvince(province);
        bundle.setKnowledgePoints(String.join(",", knowledgePoints));
        bundle.setQuestionCount(questionCount);
        bundle.setEstimatedMinutes(estimateMinutes(questionCount));
        bundle.setQuestionIds(questionIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        bundle.setThetaRange(thetaRange);
        bundle.setDifficultyRange(difficultyRange);
        bundle.setAbilityFitScore(abilityFitScore);
        bundle.setExplainReasons(sourceStrategy);
        bundle.setSourceStrategy("IRT + MASTERY");
        bundle.setStatus("ACTIVE");
        bundle.setGeneratedAt(LocalDateTime.now());
        recommendPracticeBundleMapper.insert(bundle);
        return bundle;
    }

    private List<String> buildAdaptiveExplainReasons(String recommendMode,
                                                     List<TargetKnowledge> targets,
                                                     double averageB,
                                                     List<String> knowledgePoints,
                                                     String province,
                                                     List<QuestionScore> selected,
                                                     Set<Long> recentQuestionIds) {
        List<String> reasons = new ArrayList<>();
        if (!knowledgePoints.isEmpty()) {
            reasons.add("命中知识点: " + String.join(" / ", knowledgePoints.subList(0, Math.min(3, knowledgePoints.size()))));
        }
        if ("WEAKNESS".equals(recommendMode)) {
            reasons.add("题目难度略高于当前能力，适合补弱提分");
        } else if ("CONSOLIDATION".equals(recommendMode)) {
            reasons.add("题目难度贴近当前能力，用于巩固稳定性");
        } else if ("CHALLENGE".equals(recommendMode)) {
            reasons.add("题目难度上浮，用于稳定知识点的提分挑战");
        } else if ("DIAGNOSIS".equals(recommendMode)) {
            reasons.add("覆盖能力区间上下限，用于补充画像与诊断");
        }
        if (StringUtils.hasText(province) && selected.stream().anyMatch(item -> province.equals(item.question.getProvince()))) {
            reasons.add("优先命中报考地区题目");
        } else if (StringUtils.hasText(province)) {
            reasons.add("本地区题量不足，已回退全国题库");
        }
        long recentOverlap = selected.stream().filter(item -> recentQuestionIds.contains(item.question.getId())).count();
        if (recentOverlap == 0) {
            reasons.add("近 7 天未重复");
        }
        return reasons;
    }

    private List<TargetKnowledge> planOrSubjectTargets(SysUser user,
                                                       List<String> planKnowledge,
                                                       Map<Long, Question> questionMap,
                                                       Map<String, UserKnowledgeAbility> abilityMap) {
        List<TargetKnowledge> targets = new ArrayList<>();
        String subject = safeText(user.getPreferredSubject(), "行测");
        for (String knowledge : planKnowledge) {
            TargetKnowledge target = new TargetKnowledge();
            target.subjectName = subject;
            target.knowledgePoint = knowledge;
            UserKnowledgeAbility ability = abilityMap.get(abilityKey(subject, knowledge));
            target.theta = ability == null || ability.getTheta() == null ? BigDecimal.ZERO : ability.getTheta();
            target.priority = BigDecimal.valueOf(60);
            target.confidence = ability == null || ability.getThetaConfidence() == null ? BigDecimal.valueOf(30) : ability.getThetaConfidence();
            targets.add(target);
        }
        if (!targets.isEmpty()) {
            return targets;
        }
        questionMap.values().stream()
                .filter(item -> subject.equals(item.getSubjectName()))
                .map(Question::getKnowledgePoints)
                .flatMap(item -> splitKnowledge(item).stream())
                .distinct()
                .limit(3)
                .forEach(knowledge -> {
                    TargetKnowledge target = new TargetKnowledge();
                    target.subjectName = subject;
                    target.knowledgePoint = knowledge;
                    target.theta = BigDecimal.ZERO;
                    target.priority = BigDecimal.valueOf(50);
                    target.confidence = BigDecimal.valueOf(20);
                    targets.add(target);
                });
        return targets;
    }

    private TargetKnowledge toTargetKnowledge(UserKnowledgeMastery mastery, Map<String, UserKnowledgeAbility> abilityMap) {
        TargetKnowledge target = new TargetKnowledge();
        target.subjectName = mastery.getSubjectName();
        target.knowledgePoint = mastery.getKnowledgePoint();
        target.priority = mastery.getPriorityScore() == null ? BigDecimal.ZERO : mastery.getPriorityScore();
        target.confidence = mastery.getMasteryConfidence() == null ? BigDecimal.ZERO : mastery.getMasteryConfidence();
        UserKnowledgeAbility ability = abilityMap.get(abilityKey(mastery.getSubjectName(), mastery.getKnowledgePoint()));
        target.theta = ability == null || ability.getTheta() == null
                ? BigDecimal.valueOf((mastery.getMasteryScore() == null ? 50 : mastery.getMasteryScore().doubleValue()) - 50)
                .divide(BigDecimal.valueOf(20), 4, RoundingMode.HALF_UP)
                : ability.getTheta();
        return target;
    }

    private Map<Long, BigDecimal> loadQuestionDifficultyMap() {
        Map<Long, BigDecimal> map = questionIrtStatMapper.selectList(null).stream()
                .collect(Collectors.toMap(QuestionIrtStat::getQuestionId,
                        item -> item.getDifficultyB() == null ? BigDecimal.ZERO : item.getDifficultyB(),
                        (left, right) -> right,
                        LinkedHashMap::new));
        if (map.isEmpty()) {
            return irtCalibrationService.loadDifficultyMap();
        }
        return map;
    }

    private Set<Long> recentPracticeQuestionIds(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<PracticeRecord> recentRecords = practiceMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getStatus, "SUBMITTED")
                .ge(PracticeRecord::getSubmittedAt, since));
        if (recentRecords.isEmpty()) {
            return Collections.emptySet();
        }
        return practiceAnswerMapper.selectList(new LambdaQueryWrapper<PracticeAnswer>()
                        .in(PracticeAnswer::getPracticeRecordId, recentRecords.stream().map(PracticeRecord::getId).collect(Collectors.toSet())))
                .stream()
                .map(PracticeAnswer::getQuestionId)
                .collect(Collectors.toSet());
    }

    private List<String> loadPlanKnowledge(Long userId) {
        return userStudyPlanMapper.selectList(new LambdaQueryWrapper<UserStudyPlan>()
                        .eq(UserStudyPlan::getUserId, userId)
                        .eq(UserStudyPlan::getStatus, "ACTIVE")
                        .orderByDesc(UserStudyPlan::getUpdatedAt)
                        .last("limit 3"))
                .stream()
                .map(UserStudyPlan::getFocusKnowledgePoints)
                .flatMap(item -> splitKnowledge(item).stream())
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }

    private int chooseQuestionCount(UserAbilityProfile profile, String mode) {
        int base = "CHALLENGE".equals(mode) ? 12 : 16;
        if (profile == null || profile.getAbilityConfidence() == null) {
            return base;
        }
        return profile.getAbilityConfidence().compareTo(BigDecimal.valueOf(70)) >= 0 ? base + 4 : base;
    }

    private BigDecimal adaptiveContentScore(Map<String, BigDecimal> userTagWeights,
                                            SysUser user,
                                            String subjectName,
                                            List<String> knowledgePoints,
                                            double abilityFit) {
        Map<String, BigDecimal> tags = practiceTagWeights(user, subjectName);
        knowledgePoints.forEach(item -> addTagWeight(tags, item, BigDecimal.valueOf(0.42)));
        BigDecimal similarity = resourceTagSimilarity(userTagWeights, tags);
        return similarity.multiply(BigDecimal.valueOf(0.65)).add(decimal(abilityFit).multiply(BigDecimal.valueOf(0.35)));
    }

    private BigDecimal adaptiveHotScore(String subjectName, List<QuestionScore> selected, Map<Long, QuestionIrtStat> questionStatMap) {
        BigDecimal subjectHot = practiceHotScore(subjectName).max(BigDecimal.valueOf(0.20));
        double support = selected.stream()
                .map(item -> questionStatMap.get(item.question.getId()))
                .filter(Objects::nonNull)
                .mapToInt(item -> item.getSampleCount() == null ? 0 : item.getSampleCount())
                .average()
                .orElse(0.0d);
        return subjectHot.multiply(BigDecimal.valueOf(0.7))
                .add(BigDecimal.valueOf(Math.min(support, 80.0d) / 80.0d * 0.3d))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private List<String> adaptiveTags(SysUser user, String subjectName, List<String> knowledgePoints, String mode) {
        List<String> tags = new ArrayList<>();
        if (StringUtils.hasText(user.getProvince())) {
            tags.add(user.getProvince());
        }
        tags.add(subjectName);
        tags.add(mode);
        knowledgePoints.stream().limit(2).forEach(tags::add);
        return tags;
    }

    private String buildAdaptivePracticeTitle(String mode, String subjectName, List<String> knowledgePoints, int questionCount) {
        String knowledge = knowledgePoints.isEmpty() ? subjectName : knowledgePoints.get(0);
        if ("DIAGNOSIS".equals(mode)) {
            return knowledge + " 诊断刷题 " + questionCount + " 题";
        }
        if ("CONSOLIDATION".equals(mode)) {
            return knowledge + " 巩固刷题 " + questionCount + " 题";
        }
        if ("CHALLENGE".equals(mode)) {
            return knowledge + " 提分挑战 " + questionCount + " 题";
        }
        return knowledge + " 补弱刷题 " + questionCount + " 题";
    }

    private String buildAdaptiveReason(String mode, String baseReason, List<String> knowledgePoints) {
        String suffix = knowledgePoints.isEmpty() ? "" : "，重点围绕 " + String.join(" / ", knowledgePoints.subList(0, Math.min(3, knowledgePoints.size())));
        if ("DIAGNOSIS".equals(mode)) {
            return "按画像低置信度知识点生成诊断型练习" + suffix;
        }
        if ("CONSOLIDATION".equals(mode)) {
            return "按当前提升区间知识点生成巩固型练习" + suffix;
        }
        if ("CHALLENGE".equals(mode)) {
            return "按稳定知识点生成挑战型练习" + suffix;
        }
        return baseReason + suffix;
    }

    private String sourceLabelForMode(String mode) {
        if ("DIAGNOSIS".equals(mode)) {
            return "IRT 诊断";
        }
        if ("CONSOLIDATION".equals(mode)) {
            return "IRT 巩固";
        }
        if ("CHALLENGE".equals(mode)) {
            return "IRT 挑战";
        }
        return "IRT 补弱";
    }

    private String actionTextForMode(String mode) {
        if ("DIAGNOSIS".equals(mode)) {
            return "开始诊断";
        }
        if ("CONSOLIDATION".equals(mode)) {
            return "开始巩固";
        }
        if ("CHALLENGE".equals(mode)) {
            return "开始挑战";
        }
        return "开始练习";
    }

    private String thetaRange(List<TargetKnowledge> targets) {
        double min = targets.stream().map(TargetKnowledge::thetaValue).min(Double::compareTo).orElse(0.0d);
        double max = targets.stream().map(TargetKnowledge::thetaValue).max(Double::compareTo).orElse(0.0d);
        return String.format(Locale.ROOT, "%.2f ~ %.2f", min, max);
    }

    private String difficultyRange(List<QuestionScore> selected) {
        double min = selected.stream().map(item -> item.difficulty.doubleValue()).min(Double::compareTo).orElse(0.0d);
        double max = selected.stream().map(item -> item.difficulty.doubleValue()).max(Double::compareTo).orElse(0.0d);
        return String.format(Locale.ROOT, "%.2f ~ %.2f", min, max);
    }

    private double abilityFitForMode(String mode, double theta, double b) {
        if ("CHALLENGE".equals(mode)) {
            return centeredScore(b - theta, 0.85d, 0.75d);
        }
        if ("CONSOLIDATION".equals(mode)) {
            return centeredScore(b - theta, 0.0d, 0.35d);
        }
        if ("DIAGNOSIS".equals(mode)) {
            return centeredScore(Math.abs(b - theta), 0.75d, 0.6d);
        }
        return centeredScore(b - theta, 0.35d, 0.55d);
    }

    private double diagnosisFit(List<TargetKnowledge> targets, double b) {
        double avgTheta = targets.stream().map(TargetKnowledge::thetaValue).mapToDouble(Double::doubleValue).average().orElse(0.0d);
        return centeredScore(Math.abs(b - avgTheta), 0.65d, 0.75d);
    }

    private double centeredScore(double value, double expected, double tolerance) {
        double delta = Math.abs(value - expected);
        return Math.max(0.0d, 1.0d - delta / Math.max(tolerance, 0.1d));
    }

    private List<String> splitKnowledge(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split("[,，/|；;、\\n\\r]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private String abilityKey(String subjectName, String knowledgePoint) {
        return safeText(subjectName, "") + "::" + safeText(knowledgePoint, "");
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal fallbackDifficulty(String difficultyLevel) {
        String text = safeText(difficultyLevel, "").toLowerCase(Locale.ROOT);
        if (text.contains("难") || text.contains("hard")) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        if (text.contains("易") || text.contains("simple") || text.contains("easy")) {
            return BigDecimal.valueOf(-1.0d).setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    private boolean isObjectiveQuestion(Question question) {
        if (question == null || !Boolean.TRUE.equals(question.getEnabled())) {
            return false;
        }
        String subject = safeText(question.getSubjectName(), "");
        String type = safeText(question.getQuestionType(), "");
        return !subject.contains("申论")
                && !type.contains("主观")
                && !type.contains("写作")
                && !type.contains("简答")
                && !type.contains("论述");
    }

    private void ensureIrtCalibration(Long userId) {
        if (userId == null) {
            return;
        }
        if (questionIrtStatMapper.selectCount(null) == 0
                || userAbilityProfileMapper.selectCount(new LambdaQueryWrapper<UserAbilityProfile>()
                .eq(UserAbilityProfile::getUserId, userId)) == 0) {
            irtCalibrationService.refreshUser(userId);
        }
    }

    private static class TargetKnowledge {
        private String subjectName;
        private String knowledgePoint;
        private BigDecimal theta = BigDecimal.ZERO;
        private BigDecimal priority = BigDecimal.ZERO;
        private BigDecimal confidence = BigDecimal.ZERO;

        private double thetaValue() {
            return theta == null ? 0.0d : theta.doubleValue();
        }
    }

    private static class QuestionScore {
        private Question question;
        private BigDecimal difficulty;
        private double matchScore;
    }

    private List<Map<String, Object>> buildNewsRecommendationCards(SysUser user,
                                                                   List<PracticeRecord> practices,
                                                                   List<ExamRecord> exams,
                                                                   List<UserWrongQuestion> wrongs,
                                                                   Map<String, BigDecimal> userTagWeights,
                                                                   Map<Long, BigDecimal> collaborativeNewsScores,
                                                                   Set<String> feedbackKeys,
                                                                   Map<String, Long> feedbackTypeCounts,
                                                                   Map<String, BigDecimal> weights) {
        List<Map<String, Object>> newsCards = new ArrayList<>();
        String province = safeText(user.getProvince(), "");
        String targetRegion = targetRegion(user, province);
        String examType = safeText(user.getExamType(), "");
        String preferred = safeText(user.getPreferredSubject(), "");
        String weak = weakSubject(user, practices);

        List<News> regionPool = new ArrayList<>();
        if (!isNationalRegion(targetRegion)) {
            regionPool = newsMapper.selectList(new LambdaQueryWrapper<News>()
                    .eq(News::getStatus, "PUBLISHED")
                    .in(News::getProvince, Arrays.asList(targetRegion, DEFAULT_REGION))
                    .orderByDesc(News::getReadCount)
                    .orderByDesc(News::getPublishedAt)
                    .last("limit 12"));
        }
        List<News> pool = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, "PUBLISHED")
                .orderByDesc(News::getReadCount)
                .orderByDesc(News::getPublishedAt)
                .last("limit 24"));
        List<News> latestPool = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, "PUBLISHED")
                .orderByDesc(News::getPublishedAt)
                .last("limit 8"));
        Map<Long, News> candidates = new LinkedHashMap<>();
        regionPool.forEach(item -> candidates.put(item.getId(), item));
        pool.forEach(item -> candidates.put(item.getId(), item));
        latestPool.forEach(item -> candidates.put(item.getId(), item));

        for (News news : candidates.values()) {
            Map<String, BigDecimal> resourceTags = newsTagWeights(news);
            BigDecimal collaborative = collaborativeNewsScores.getOrDefault(news.getId(), resourceTagSimilarity(userTagWeights, resourceTags));
            BigDecimal content = resourceTagSimilarity(userTagWeights, resourceTags);
            BigDecimal contentWithRegion = content.add(regionBoost(news.getProvince(), targetRegion, weights)).min(BigDecimal.ONE);
            BigDecimal hot = newsHotScore(news);
            List<String> matchedTags = matchingTags(userTagWeights, resourceTags);
            String newsType = newsType(news, weak);
            String reason = newsReason(newsType, matchedTags, province, examType, preferred, weak, wrongs, exams);
            BigDecimal boost = newsBoost(newsType, news, matchedTags, province, examType, preferred, weak);
            List<String> reasons = buildExplainReasons(collaborative, contentWithRegion.add(boost).min(BigDecimal.ONE), hot, matchedTags, newsType);
            addRegionReason(reasons, "资讯", news.getProvince(), targetRegion);
            newsCards.add(score(
                    buildNewsCard(news, reason, newsType),
                    weights,
                    collaborative.doubleValue(),
                    contentWithRegion.add(boost).min(BigDecimal.ONE).doubleValue(),
                    hot.doubleValue(),
                    penalty("NEWS", news.getId(), feedbackKeys, feedbackTypeCounts).add(regionPenalty(news.getProvince(), targetRegion, weights)),
                    reasons,
                    matchedTags
            ));
        }
        return newsCards;
    }

    private List<Map<String, Object>> selectRecommendedCards(List<Map<String, Object>> cards) {
        List<Map<String, Object>> sorted = deduplicate(cards).stream()
                .filter(item -> !Boolean.TRUE.equals(item.get("hiddenByFeedback")))
                .sorted((a, b) -> new BigDecimal(String.valueOf(b.get("finalScore"))).compareTo(new BigDecimal(String.valueOf(a.get("finalScore")))))
                .collect(Collectors.toList());
        List<Map<String, Object>> selected = sorted.stream().limit(8).collect(Collectors.toCollection(ArrayList::new));
        ensureType(selected, sorted, "PRACTICE");
        ensureType(selected, sorted, "NEWS");
        return selected;
    }

    private void ensureType(List<Map<String, Object>> selected, List<Map<String, Object>> sorted, String resourceType) {
        boolean existed = selected.stream().anyMatch(item -> resourceType.equals(item.get("resourceType")));
        if (existed) {
            return;
        }
        sorted.stream()
                .filter(item -> resourceType.equals(item.get("resourceType")))
                .findFirst()
                .ifPresent(candidate -> {
                    if (selected.size() >= 8) {
                        int removeIndex = selected.size() - 1;
                        for (int i = selected.size() - 1; i >= 0; i--) {
                            Object type = selected.get(i).get("resourceType");
                            if (!"PRACTICE".equals(type) && !"NEWS".equals(type)) {
                                removeIndex = i;
                                break;
                            }
                        }
                        selected.remove(removeIndex);
                    }
                    selected.add(candidate);
                    selected.sort((a, b) -> new BigDecimal(String.valueOf(b.get("finalScore"))).compareTo(new BigDecimal(String.valueOf(a.get("finalScore")))));
                });
    }

    private Map<String, Object> buildPortrait(SysUser user,
                                              List<PracticeRecord> practices,
                                              List<ExamRecord> exams,
                                              List<UserWrongQuestion> wrongs,
                                              List<UserCourseStudyProgress> progresses,
                                              List<UserCourseLearningRecord> records,
                                              int feedbackCount) {
        Map<String, BigDecimal> accuracy = subjectAccuracy(practices);
        String weak = weakSubject(user, practices);
        String strong = accuracy.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(safeText(user.getPreferredSubject(), "综合能力"));
        Set<String> activeDays = records.stream()
                .map(UserCourseLearningRecord::getCreatedAt)
                .filter(Objects::nonNull)
                .map(item -> item.toLocalDate().toString())
                .collect(Collectors.toSet());
        int totalStudySeconds = progresses.stream()
                .mapToInt(item -> item.getTotalStudySeconds() == null ? 0 : item.getTotalStudySeconds())
                .sum();
        List<String> labels = new ArrayList<>();
        if (StringUtils.hasText(user.getProvince())) labels.add(user.getProvince() + "备考");
        if (StringUtils.hasText(user.getPreferredSubject())) labels.add(user.getPreferredSubject() + "主攻");
        if (wrongs.size() >= 3) labels.add("错题需强化");
        if (progresses.stream().anyMatch(item -> progressBetween(item, 30, 100))) labels.add("有继续学习课程");
        if (feedbackCount > 0) labels.add("反馈已回流推荐");
        if (labels.isEmpty()) labels.add("正在建立学习画像");
        Map<String, Object> portrait = new LinkedHashMap<>();
        portrait.put("title", safeText(user.getNickname(), user.getUsername()) + " 的学习画像");
        portrait.put("summary", "画像摘要与推荐结果已缓存到 Redis，并会在学习行为和推荐反馈后异步刷新。");
        portrait.put("labels", labels);
        portrait.put("targetProvince", safeText(user.getProvince(), "全国"));
        portrait.put("preferredSubject", safeText(user.getPreferredSubject(), "综合"));
        portrait.put("examType", safeText(user.getExamType(), "公考通用"));
        portrait.put("activeDays", activeDays.size());
        portrait.put("totalStudyHours", BigDecimal.valueOf(totalStudySeconds).divide(BigDecimal.valueOf(3600), 1, RoundingMode.HALF_UP));
        portrait.put("weakSubject", weak);
        portrait.put("strongSubject", strong);
        portrait.put("practiceCount", practices.size());
        portrait.put("examCount", exams.size());
        portrait.put("wrongQuestionCount", wrongs.size());
        portrait.put("feedbackCount", feedbackCount);
        return portrait;
    }

    private List<Map<String, Object>> buildContinueLearningCourses(List<UserCourse> owned, List<UserCourseStudyProgress> progresses) {
        if (owned.isEmpty() || progresses.isEmpty()) return new ArrayList<>();
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(owned.stream().map(UserCourse::getCourseId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Course::getId, item -> item, (a, b) -> a));
        return progresses.stream()
                .filter(item -> progressBetween(item, 0, 100))
                .sorted(Comparator.comparing(UserCourseStudyProgress::getLastStudyAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .map(item -> {
                    Course course = courseMap.get(item.getCourseId());
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("courseId", item.getCourseId());
                    map.put("title", course == null ? "课程已下架" : course.getTitle());
                    map.put("coverUrl", course == null ? null : course.getCoverUrl());
                    map.put("teacherName", course == null ? null : course.getTeacherName());
                    map.put("subjectName", course == null ? null : course.getSubjectName());
                    map.put("progressPercent", item.getProgressPercent() == null ? BigDecimal.ZERO : item.getProgressPercent());
                    map.put("lastStudyAt", item.getLastStudyAt() == null ? null : item.getLastStudyAt().format(FMT));
                    map.put("path", "/course/play/" + item.getCourseId());
                    return map;
                }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> guestCards(String targetRegion) {
        String normalizedRegion = StringUtils.hasText(targetRegion) ? targetRegion : DEFAULT_REGION;
        List<Map<String, Object>> cached = isNationalRegion(normalizedRegion) ? readList(HOT_KEY) : null;
        if (!CollectionUtils.isEmpty(cached)) return cached;
        Map<String, BigDecimal> weights = loadWeights();
        List<Map<String, Object>> cards = new ArrayList<>(buildPinnedCards(normalizedRegion, Collections.emptySet(), Collections.emptyMap()));
        courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Course::getStatus, "PUBLISHED").orderByDesc(Course::getLearnCount).last("limit 4"))
                .forEach(course -> {
                    BigDecimal regionBoost = regionBoost(course.getProvince(), normalizedRegion, weights);
                    BigDecimal penalty = regionPenalty(course.getProvince(), normalizedRegion, weights);
                    List<String> reasons = new ArrayList<>(Collections.singletonList("热门池兜底"));
                    addRegionReason(reasons, "课程", course.getProvince(), normalizedRegion);
                    cards.add(score(
                        buildCourseCard(course, regionAwareReason("热门课程兜底推荐", "课程", course.getProvince(), normalizedRegion), "/course/detail/" + course.getId(), courseAction(course)),
                        weights,
                        0.0,
                        regionBoost.doubleValue(),
                        hotScore(course.getLearnCount()).doubleValue(),
                        penalty,
                        reasons,
                        splitTags(course.getTags())
                    ));
                });
        examMapper.selectList(new LambdaQueryWrapper<Exam>().eq(Exam::getStatus, "OPEN").orderByDesc(Exam::getCreatedAt).last("limit 3"))
                .forEach(exam -> {
                    BigDecimal regionBoost = regionBoost(exam.getProvince(), normalizedRegion, weights);
                    BigDecimal penalty = regionPenalty(exam.getProvince(), normalizedRegion, weights);
                    List<String> reasons = new ArrayList<>(Collections.singletonList("热门池兜底"));
                    addRegionReason(reasons, "模考", exam.getProvince(), normalizedRegion);
                    cards.add(score(
                        buildCard("EXAM", exam.getId(), exam.getTitle(), null, safeText(exam.getProvince(), exam.getSubjectName()), regionAwareReason("近期开放模考", "模考", exam.getProvince(), normalizedRegion), "/exam/detail/" + exam.getId(), "查看模考"),
                        weights,
                        0.0,
                        regionBoost.doubleValue(),
                        recencyScore(exam.getCreatedAt(), 45).doubleValue(),
                        penalty,
                        reasons,
                        Collections.singletonList(safeText(exam.getProvince(), exam.getSubjectName()))
                    ));
                });
        LambdaQueryWrapper<News> newsWrapper = new LambdaQueryWrapper<News>().eq(News::getStatus, "PUBLISHED");
        if (!isNationalRegion(normalizedRegion)) {
            newsWrapper.in(News::getProvince, Arrays.asList(normalizedRegion, DEFAULT_REGION));
        }
        newsMapper.selectList(newsWrapper.orderByDesc(News::getReadCount).last("limit 3"))
                .forEach(news -> {
                    BigDecimal regionBoost = regionBoost(news.getProvince(), normalizedRegion, weights);
                    BigDecimal penalty = regionPenalty(news.getProvince(), normalizedRegion, weights);
                    List<String> reasons = new ArrayList<>(Collections.singletonList("热门池兜底"));
                    addRegionReason(reasons, "资讯", news.getProvince(), normalizedRegion);
                    cards.add(score(
                        buildNewsCard(news, regionAwareReason("热门资讯推荐", "资讯", news.getProvince(), normalizedRegion), "热门资讯"),
                        weights,
                        0.0,
                        regionBoost.doubleValue(),
                        newsHotScore(news).doubleValue(),
                        penalty,
                        reasons,
                        splitTags(news.getTags())
                    ));
                });
        cards.add(score(
                buildPracticeCard(9001L, "热门专项刷题", "题库训练,热门练习", "热门题组兜底，登录后会结合画像生成更精准的刷题包", "/search?questionCount=20", "开始练习", 20, 18, "热门题组"),
                weights,
                0.0,
                0.42,
                practiceHotScore(null).doubleValue(),
                BigDecimal.ZERO,
                Arrays.asList("热门池兜底", "登录后更精准"),
                Arrays.asList("题库训练", "热门练习")
        ));
        return selectRecommendedCards(cards);
    }

    private Map<String, Object> guestPortrait(String targetRegion) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", "游客模式");
        map.put("summary", "当前展示热门推荐池、后台置顶资源和已选择报考地区的全国兜底内容。登录后会同步到用户画像。");
        map.put("labels", Arrays.asList("热门推荐池", targetRegion + "地区上下文", "登录后更精准"));
        map.put("targetProvince", targetRegion);
        map.put("preferredSubject", "未登录");
        map.put("examType", "未登录");
        map.put("activeDays", 0);
        map.put("totalStudyHours", 0);
        map.put("weakSubject", "登录后分析");
        map.put("strongSubject", "登录后分析");
        return map;
    }

    private Map<String, Object> buildProfilePayload(Map<String, Object> overview) {
        Map<String, Object> result = new LinkedHashMap<>(overview);
        List<Map<String, Object>> cards = castList(overview.get("recommendCards"));
        Map<String, Object> portrait = castMap(overview.get("portrait"));
        result.put("recommendReasonStats", stats(cards, "resourceType"));
        result.put("preferenceSubjects", statsByTags(cards, String.valueOf(portrait.get("preferredSubject"))));
        result.put("weaknessFocus", weakFocus(cards, String.valueOf(portrait.get("weakSubject"))));
        return result;
    }

    private List<Map<String, Object>> buildPinnedCards(String targetRegion, Set<String> feedbackKeys, Map<String, Long> feedbackTypeCounts) {
        LocalDateTime now = LocalDateTime.now();
        return pinnedMapper.selectList(new LambdaQueryWrapper<RecommendPinnedResource>()
                        .eq(RecommendPinnedResource::getEnabled, true)
                        .orderByAsc(RecommendPinnedResource::getSortNo)
                        .orderByAsc(RecommendPinnedResource::getId))
                .stream()
                .filter(item -> item.getStartTime() == null || !item.getStartTime().isAfter(now))
                .filter(item -> item.getEndTime() == null || !item.getEndTime().isBefore(now))
                .filter(item -> pinnedMatchesRegion(item, targetRegion))
                .map(item -> {
                    Map<String, Object> map = resourceCard(item.getResourceType(), item.getResourceId());
                    if (map == null) return null;
                    if (StringUtils.hasText(item.getDisplayTitle())) map.put("title", item.getDisplayTitle());
                    if (StringUtils.hasText(item.getDisplayReason())) map.put("reason", item.getDisplayReason());
                    if (StringUtils.hasText(item.getDisplayTags())) map.put("tags", item.getDisplayTags());
                    if (StringUtils.hasText(item.getActionText())) map.put("actionText", item.getActionText());
                    map.put("pinned", true);
                    map.put("regionScope", safeText(item.getRegionScope(), "ALL"));
                    map.put("pinnedProvince", item.getProvince());
                    return score(
                            map,
                            loadWeights(),
                            0.95,
                            0.95,
                            0.85,
                            penalty(item.getResourceType(), item.getResourceId(), feedbackKeys, feedbackTypeCounts),
                            Collections.singletonList("后台置顶资源"),
                            splitTags(String.valueOf(map.get("tags")))
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, Object> resourceCard(String resourceType, Long resourceId) {
        if ("COURSE".equals(resourceType)) {
            Course course = courseMapper.selectById(resourceId);
            return course == null ? null : buildCourseCard(course, "后台强制置顶课程", "/course/detail/" + course.getId(), courseAction(course));
        }
        if ("EXAM".equals(resourceType)) {
            Exam exam = examMapper.selectById(resourceId);
            return exam == null ? null : buildCard("EXAM", exam.getId(), exam.getTitle(), null, safeText(exam.getProvince(), exam.getSubjectName()), "后台强制置顶模考", "/exam/detail/" + exam.getId(), "查看模考");
        }
        if ("NEWS".equals(resourceType)) {
            News news = newsMapper.selectById(resourceId);
            return news == null ? null : buildNewsCard(news, "后台强制置顶资讯", newsType(news, null));
        }
        return "PRACTICE".equals(resourceType) ? buildCard("PRACTICE", resourceId == null ? 0L : resourceId, "专项刷题入口", null, "题库训练", "后台强制置顶刷题入口", "/question-bank", "开始练习") : null;
    }

    private Map<String, BigDecimal> loadWeights() {
        Map<String, BigDecimal> weights = configMapper.selectList(new LambdaQueryWrapper<RecommendConfig>().orderByAsc(RecommendConfig::getId))
                .stream()
                .collect(Collectors.toMap(RecommendConfig::getConfigKey, RecommendConfig::getConfigValue, (a, b) -> a, LinkedHashMap::new));
        weights.putIfAbsent("collaborative_weight", BigDecimal.valueOf(0.4));
        weights.putIfAbsent("content_weight", BigDecimal.valueOf(0.4));
        weights.putIfAbsent("hot_weight", BigDecimal.valueOf(0.2));
        weights.putIfAbsent("pinned_boost", BigDecimal.valueOf(0.5));
        weights.putIfAbsent("region_match_boost", BigDecimal.valueOf(0.28));
        weights.putIfAbsent("national_region_boost", BigDecimal.valueOf(0.12));
        weights.putIfAbsent("outside_region_penalty", BigDecimal.valueOf(0.18));
        return weights;
    }

    private Map<String, Object> score(Map<String, Object> card,
                                      Map<String, BigDecimal> weights,
                                      double collaborative,
                                      double content,
                                      double hot,
                                      BigDecimal penalty,
                                      List<String> explainReasons,
                                      List<String> explainTags) {
        BigDecimal finalScore = weights.get("collaborative_weight").multiply(BigDecimal.valueOf(collaborative))
                .add(weights.get("content_weight").multiply(BigDecimal.valueOf(content)))
                .add(weights.get("hot_weight").multiply(BigDecimal.valueOf(hot)))
                .subtract(penalty);
        if (Boolean.TRUE.equals(card.get("pinned"))) {
            finalScore = finalScore.add(weights.get("pinned_boost"));
        }
        card.put("collaborativeScore", BigDecimal.valueOf(collaborative).setScale(4, RoundingMode.HALF_UP));
        card.put("contentScore", BigDecimal.valueOf(content).setScale(4, RoundingMode.HALF_UP));
        card.put("hotScore", BigDecimal.valueOf(hot).setScale(4, RoundingMode.HALF_UP));
        card.put("finalScore", finalScore.setScale(4, RoundingMode.HALF_UP));
        card.put("feedbackPenalty", penalty.setScale(4, RoundingMode.HALF_UP));
        card.put("hiddenByFeedback", penalty.compareTo(BigDecimal.valueOf(0.95)) >= 0);
        card.put("explainReasons", explainReasons == null ? Collections.emptyList() : explainReasons);
        card.put("explainTags", explainTags == null ? Collections.emptyList() : explainTags);
        return card;
    }

    private BigDecimal penalty(String type, Long id, Set<String> feedbackKeys, Map<String, Long> feedbackTypeCounts) {
        BigDecimal penalty = feedbackKeys.contains(cardKey(type, id)) ? BigDecimal.ONE : BigDecimal.ZERO;
        long count = feedbackTypeCounts.getOrDefault(type, 0L);
        return penalty.add(BigDecimal.valueOf(Math.min(count, 3)).multiply(BigDecimal.valueOf(0.12)));
    }

    private String weakSubject(SysUser user, List<PracticeRecord> practices) {
        return subjectAccuracy(practices).entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(safeText(user.getPreferredSubject(), "行测"));
    }

    private Map<String, BigDecimal> subjectAccuracy(List<PracticeRecord> practices) {
        Map<String, List<PracticeRecord>> grouped = practices.stream()
                .filter(item -> StringUtils.hasText(item.getSubjectName()))
                .collect(Collectors.groupingBy(PracticeRecord::getSubjectName));
        Map<String, BigDecimal> result = new HashMap<>();
        grouped.forEach((subject, list) -> result.put(subject, list.stream()
                .map(item -> item.getAccuracyRate() == null ? BigDecimal.ZERO : item.getAccuracyRate())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP)));
        return result;
    }

    private boolean progressBetween(UserCourseStudyProgress item, int min, int maxExclusive) {
        BigDecimal percent = item.getProgressPercent() == null ? BigDecimal.ZERO : item.getProgressPercent();
        return percent.compareTo(BigDecimal.valueOf(min)) > 0 && percent.compareTo(BigDecimal.valueOf(maxExclusive)) < 0;
    }

    private BigDecimal hotScore(Integer count) {
        return BigDecimal.valueOf(Math.min(count == null ? 0 : count, 200)).divide(BigDecimal.valueOf(200), 4, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> buildUserTagWeights(SysUser user,
                                                        List<PracticeRecord> practices,
                                                        List<ExamRecord> exams,
                                                        List<UserWrongQuestion> wrongs,
                                                        List<UserCourseStudyProgress> progresses,
                                                        String weakSubject) {
        Map<String, BigDecimal> tags = new LinkedHashMap<>();
        addTagWeight(tags, safeText(user.getProvince(), null), BigDecimal.valueOf(1.00));
        addTagWeight(tags, safeText(user.getExamType(), null), BigDecimal.valueOf(0.95));
        addTagWeight(tags, safeText(user.getPreferredSubject(), null), BigDecimal.valueOf(1.00));
        addTagWeight(tags, weakSubject, BigDecimal.valueOf(1.10));
        subjectAccuracy(practices).forEach((subject, score) -> addTagWeight(tags, subject, BigDecimal.valueOf(0.35)));
        practices.stream().limit(10).forEach(item -> {
            addTagWeight(tags, item.getProvince(), BigDecimal.valueOf(0.25));
            addTagWeight(tags, item.getPracticeMode(), BigDecimal.valueOf(0.20));
        });
        if (!wrongs.isEmpty()) {
            addTagWeight(tags, "错题强化", BigDecimal.valueOf(0.45));
        }
        if (progresses.stream().anyMatch(item -> progressBetween(item, 30, 100))) {
            addTagWeight(tags, "继续学习", BigDecimal.valueOf(0.38));
        }
        exams.stream().limit(6).forEach(item -> addTagWeight(tags, item.getTitle(), BigDecimal.valueOf(0.05)));
        return tags;
    }

    private Map<Long, BigDecimal> collaborativeCourseScores(Long userId,
                                                            List<UserCourse> owned,
                                                            List<UserCourseStudyProgress> progresses,
                                                            List<PracticeRecord> practices,
                                                            List<ExamRecord> exams) {
        Map<Long, BigDecimal> similarUsers = similarUserWeights(userId, owned, progresses, practices, exams);
        if (similarUsers.isEmpty()) {
            return Collections.emptyMap();
        }
        BigDecimal similaritySum = similarUsers.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (similaritySum.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> candidateCourses = courseInteractionStrength(similarUsers.keySet());
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Long courseId : candidateCourses.keySet()) {
            BigDecimal weighted = BigDecimal.ZERO;
            for (Map.Entry<Long, BigDecimal> entry : similarUsers.entrySet()) {
                BigDecimal strength = courseInteractionStrength(entry.getKey(), courseId);
                if (strength.compareTo(BigDecimal.ZERO) > 0) {
                    weighted = weighted.add(entry.getValue().multiply(strength));
                }
            }
            if (weighted.compareTo(BigDecimal.ZERO) > 0) {
                result.put(courseId, weighted.divide(similaritySum, 4, RoundingMode.HALF_UP));
            }
        }
        return result;
    }

    private Map<Long, BigDecimal> collaborativeExamScores(Long userId,
                                                          List<UserCourse> owned,
                                                          List<UserCourseStudyProgress> progresses,
                                                          List<PracticeRecord> practices,
                                                          List<ExamRecord> exams) {
        Map<Long, BigDecimal> similarUsers = similarUserWeights(userId, owned, progresses, practices, exams);
        if (similarUsers.isEmpty()) {
            return Collections.emptyMap();
        }
        BigDecimal similaritySum = similarUsers.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (similaritySum.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyMap();
        }
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .in(ExamRecord::getUserId, similarUsers.keySet())
                .eq(ExamRecord::getStatus, "SUBMITTED"));
        Set<Long> examIds = records.stream().map(ExamRecord::getExamId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Long examId : examIds) {
            BigDecimal weighted = BigDecimal.ZERO;
            for (Map.Entry<Long, BigDecimal> entry : similarUsers.entrySet()) {
                boolean interacted = records.stream().anyMatch(item -> Objects.equals(item.getUserId(), entry.getKey()) && Objects.equals(item.getExamId(), examId));
                if (interacted) {
                    weighted = weighted.add(entry.getValue());
                }
            }
            result.put(examId, weighted.divide(similaritySum, 4, RoundingMode.HALF_UP));
        }
        return result;
    }

    private Map<Long, BigDecimal> collaborativeNewsScores(Long userId,
                                                          Map<String, BigDecimal> userTagWeights,
                                                          List<UserCourse> owned,
                                                          List<UserCourseStudyProgress> progresses,
                                                          List<PracticeRecord> practices,
                                                          List<ExamRecord> exams) {
        // 优先：真实 UserCF —— 取相似用户的资讯交互聚合得分
        Map<Long, BigDecimal> similarUsers = similarUserWeights(userId, owned, progresses, practices, exams);
        if (!similarUsers.isEmpty()) {
            BigDecimal simSum = similarUsers.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (simSum.compareTo(BigDecimal.ZERO) > 0) {
                List<UserItemInteraction> rows = userCfService.loadInteractions(similarUsers.keySet(), UserCfService.ITEM_NEWS);
                if (!rows.isEmpty()) {
                    Map<Long, BigDecimal> scores = new HashMap<>();
                    for (UserItemInteraction row : rows) {
                        if (row.getWeight() == null || row.getWeight().doubleValue() <= 0) continue;
                        BigDecimal sim = similarUsers.get(row.getUserId());
                        if (sim == null) continue;
                        BigDecimal idf = BigDecimal.valueOf(userCfService.idfFor(UserCfService.ITEM_NEWS, row.getItemId()));
                        scores.merge(row.getItemId(),
                                sim.multiply(row.getWeight()).multiply(idf),
                                BigDecimal::add);
                    }
                    // 归一化到 [0,1]
                    BigDecimal max = scores.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
                    if (max.compareTo(BigDecimal.ZERO) > 0) {
                        Map<Long, BigDecimal> normalized = new HashMap<>();
                        for (Map.Entry<Long, BigDecimal> e : scores.entrySet()) {
                            normalized.put(e.getKey(), e.getValue().divide(max, 4, RoundingMode.HALF_UP));
                        }
                        return normalized;
                    }
                }
            }
        }
        // Fallback：内容画像（仅在 CF 无相似用户或无资讯交互时使用）
        Map<Long, BigDecimal> result = new HashMap<>();
        newsMapper.selectList(new LambdaQueryWrapper<News>()
                        .eq(News::getStatus, "PUBLISHED")
                        .orderByDesc(News::getReadCount)
                        .last("limit 20"))
                .forEach(news -> result.put(news.getId(), resourceTagSimilarity(userTagWeights, newsTagWeights(news))));
        return result;
    }

    private BigDecimal collaborativePracticeScore(Map<String, BigDecimal> userTagWeights, String weakSubject) {
        Map<String, BigDecimal> practiceTags = new LinkedHashMap<>();
        addTagWeight(practiceTags, weakSubject, BigDecimal.valueOf(1.10));
        addTagWeight(practiceTags, "专项刷题", BigDecimal.valueOf(0.22));
        return resourceTagSimilarity(userTagWeights, practiceTags);
    }

    private Map<Long, BigDecimal> similarUserWeights(Long userId,
                                                     List<UserCourse> owned,
                                                     List<UserCourseStudyProgress> progresses,
                                                     List<PracticeRecord> practices,
                                                     List<ExamRecord> exams) {
        // 优先读 Redis ZSET（真实 UserCF：余弦相似度 + IDF + 倒排剪枝）
        LinkedHashMap<Long, BigDecimal> cached = userCfService.readSimilarUsersFromCache(userId, UserCfService.ONLINE_TOP_K);
        if (cached != null && cached.size() >= 3) {
            return cached;
        }
        // Fallback：原行为话题 Jaccard 相似度（保留作为 Redis 不可用时降级路径）
        return legacySimilarUserWeights(userId, owned, progresses, practices, exams);
    }

    private Map<Long, BigDecimal> legacySimilarUserWeights(Long userId,
                                                           List<UserCourse> owned,
                                                           List<UserCourseStudyProgress> progresses,
                                                           List<PracticeRecord> practices,
                                                           List<ExamRecord> exams) {
        Map<String, BigDecimal> targetBehavior = behaviorTopicProfile(owned, progresses, practices, exams);
        if (targetBehavior.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> candidates = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEnabled, true));
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (SysUser candidate : candidates) {
            if (candidate.getId().equals(userId)) {
                continue;
            }
            List<UserCourse> candidateOwned = userCourseMapper.selectList(new LambdaQueryWrapper<UserCourse>().eq(UserCourse::getUserId, candidate.getId()));
            List<UserCourseStudyProgress> candidateProgress = progressMapper.selectList(new LambdaQueryWrapper<UserCourseStudyProgress>().eq(UserCourseStudyProgress::getUserId, candidate.getId()));
            List<PracticeRecord> candidatePractice = practiceMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                    .eq(PracticeRecord::getUserId, candidate.getId())
                    .eq(PracticeRecord::getStatus, "SUBMITTED")
                    .last("limit 20"));
            List<ExamRecord> candidateExams = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getUserId, candidate.getId())
                    .eq(ExamRecord::getStatus, "SUBMITTED")
                    .last("limit 20"));
            Map<String, BigDecimal> candidateBehavior = behaviorTopicProfile(candidateOwned, candidateProgress, candidatePractice, candidateExams);
            BigDecimal similarity = resourceTagSimilarity(targetBehavior, candidateBehavior);
            if (similarity.compareTo(BigDecimal.valueOf(0.18)) >= 0) {
                result.put(candidate.getId(), similarity);
            }
        }
        return result.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(12)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, BigDecimal> behaviorTopicProfile(List<UserCourse> owned,
                                                         List<UserCourseStudyProgress> progresses,
                                                         List<PracticeRecord> practices,
                                                         List<ExamRecord> exams) {
        Map<String, BigDecimal> profile = new LinkedHashMap<>();
        Set<Long> courseIds = new LinkedHashSet<>();
        owned.forEach(item -> courseIds.add(item.getCourseId()));
        progresses.forEach(item -> courseIds.add(item.getCourseId()));
        if (!courseIds.isEmpty()) {
            courseMapper.selectBatchIds(courseIds).forEach(course -> {
                addTagWeight(profile, course.getProvince(), BigDecimal.valueOf(0.30));
                addTagWeight(profile, course.getExamType(), BigDecimal.valueOf(0.42));
                addTagWeight(profile, course.getSubjectName(), BigDecimal.valueOf(0.55));
                splitTags(course.getTags()).forEach(tag -> addTagWeight(profile, tag, BigDecimal.valueOf(0.18)));
            });
        }
        practices.forEach(item -> {
            addTagWeight(profile, item.getProvince(), BigDecimal.valueOf(0.22));
            addTagWeight(profile, item.getSubjectName(), BigDecimal.valueOf(0.70));
            addTagWeight(profile, item.getPracticeMode(), BigDecimal.valueOf(0.18));
        });
        if (!exams.isEmpty()) {
            Set<Long> examIds = exams.stream().map(ExamRecord::getExamId).filter(Objects::nonNull).collect(Collectors.toSet());
            examMapper.selectBatchIds(examIds).forEach(exam -> {
                addTagWeight(profile, exam.getProvince(), BigDecimal.valueOf(0.26));
                addTagWeight(profile, exam.getExamType(), BigDecimal.valueOf(0.48));
                addTagWeight(profile, exam.getSubjectName(), BigDecimal.valueOf(0.55));
                addTagWeight(profile, exam.getSeasonName(), BigDecimal.valueOf(0.12));
            });
        }
        return profile;
    }

    private Map<Long, BigDecimal> courseInteractionStrength(Set<Long> similarUserIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (similarUserIds.isEmpty()) {
            return result;
        }
        userCourseMapper.selectList(new LambdaQueryWrapper<UserCourse>().in(UserCourse::getUserId, similarUserIds))
                .forEach(item -> result.put(item.getCourseId(), BigDecimal.ONE));
        progressMapper.selectList(new LambdaQueryWrapper<UserCourseStudyProgress>().in(UserCourseStudyProgress::getUserId, similarUserIds))
                .forEach(item -> result.merge(item.getCourseId(), BigDecimal.valueOf(0.85), BigDecimal::max));
        learningRecordMapper.selectList(new LambdaQueryWrapper<UserCourseLearningRecord>().in(UserCourseLearningRecord::getUserId, similarUserIds))
                .forEach(item -> result.merge(item.getCourseId(), BigDecimal.valueOf(0.60), BigDecimal::max));
        return result;
    }

    private BigDecimal courseInteractionStrength(Long userId, Long courseId) {
        if (userCourseMapper.selectCount(new LambdaQueryWrapper<UserCourse>().eq(UserCourse::getUserId, userId).eq(UserCourse::getCourseId, courseId)) > 0) {
            return BigDecimal.ONE;
        }
        if (progressMapper.selectCount(new LambdaQueryWrapper<UserCourseStudyProgress>().eq(UserCourseStudyProgress::getUserId, userId).eq(UserCourseStudyProgress::getCourseId, courseId)) > 0) {
            return BigDecimal.valueOf(0.85);
        }
        if (learningRecordMapper.selectCount(new LambdaQueryWrapper<UserCourseLearningRecord>().eq(UserCourseLearningRecord::getUserId, userId).eq(UserCourseLearningRecord::getCourseId, courseId)) > 0) {
            return BigDecimal.valueOf(0.60);
        }
        return BigDecimal.ZERO;
    }

    private Map<Long, Long> examPopularityMap() {
        return examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getStatus, "SUBMITTED"))
                .stream()
                .collect(Collectors.groupingBy(ExamRecord::getExamId, Collectors.counting()));
    }

    private BigDecimal examHotScore(Exam exam, Map<Long, Long> popularityMap) {
        BigDecimal participation = BigDecimal.valueOf(Math.min(popularityMap.getOrDefault(exam.getId(), 0L), 100L))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal recency = recencyScore(exam.getCreatedAt(), 45);
        return participation.multiply(BigDecimal.valueOf(0.65))
                .add(recency.multiply(BigDecimal.valueOf(0.35)))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal newsHotScore(News news) {
        BigDecimal read = hotScore(news.getReadCount());
        BigDecimal share = BigDecimal.valueOf(Math.min(news.getShareCount() == null ? 0 : news.getShareCount(), 100))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return read.multiply(BigDecimal.valueOf(0.7))
                .add(share.multiply(BigDecimal.valueOf(0.3)))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal practiceHotScore(String subjectName) {
        long count = practiceMapper.selectCount(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getStatus, "SUBMITTED")
                .eq(StringUtils.hasText(subjectName), PracticeRecord::getSubjectName, subjectName));
        return BigDecimal.valueOf(Math.min(count, 100L)).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal newsBoost(String newsType, News news, List<String> matchedTags, String province, String examType, String preferred, String weak) {
        BigDecimal boost = BigDecimal.ZERO;
        if (!matchedTags.isEmpty()) boost = boost.add(BigDecimal.valueOf(0.08));
        String raw = String.join(" ", safeText(news.getTags(), ""), safeText(news.getTitle(), ""), safeText(news.getSummary(), ""));
        if (StringUtils.hasText(province) && raw.contains(province)) boost = boost.add(BigDecimal.valueOf(0.08));
        if (StringUtils.hasText(examType) && raw.contains(examType)) boost = boost.add(BigDecimal.valueOf(0.08));
        if (StringUtils.hasText(preferred) && raw.contains(preferred)) boost = boost.add(BigDecimal.valueOf(0.05));
        if (StringUtils.hasText(weak) && raw.contains(weak)) boost = boost.add(BigDecimal.valueOf(0.08));
        if ("考试动态".equals(newsType) || "复盘素材".equals(newsType)) boost = boost.add(BigDecimal.valueOf(0.04));
        return boost.min(BigDecimal.valueOf(0.25));
    }

    private String newsReason(String newsType,
                              List<String> matchedTags,
                              String province,
                              String examType,
                              String preferred,
                              String weak,
                              List<UserWrongQuestion> wrongs,
                              List<ExamRecord> exams) {
        if (!matchedTags.isEmpty()) {
            return "命中你的 " + String.join(" / ", matchedTags) + " 标签，作为" + newsType + "补充";
        }
        if ("复盘素材".equals(newsType) && (!wrongs.isEmpty() || StringUtils.hasText(weak))) {
            return "围绕 " + safeText(weak, "薄弱项") + " 补充复盘素材和热点表达";
        }
        if ("考试动态".equals(newsType)) {
            return "结合 " + safeText(province, "目标地区") + " " + safeText(examType, "考试") + " 方向补充公告动态";
        }
        if ("备考策略".equals(newsType)) {
            return "结合 " + safeText(preferred, "主攻科目") + " 学习路径补充备考方法";
        }
        if (!exams.isEmpty()) {
            return "结合近期模考阶段补充复盘和政策资讯";
        }
        return "热门资讯兜底，优先补充公告、热点和备考策略";
    }

    private String newsType(News news, String weakSubject) {
        String raw = String.join(" ", safeText(news.getTitle(), ""), safeText(news.getSummary(), ""), safeText(news.getTags(), ""));
        if (raw.contains("公告") || raw.contains("时间") || raw.contains("政策") || raw.contains("报名")) {
            return "考试动态";
        }
        if (raw.contains("热点") || raw.contains("面试") || raw.contains("申论") || (StringUtils.hasText(weakSubject) && raw.contains(weakSubject))) {
            return "复盘素材";
        }
        if (raw.contains("复习") || raw.contains("备考") || raw.contains("优先级") || raw.contains("技巧")) {
            return "备考策略";
        }
        return "热门资讯";
    }

    private String targetRegion(SysUser user, String explicitRegion) {
        String selected = normalizeRegion(explicitRegion);
        if (StringUtils.hasText(selected)) {
            return selected;
        }
        if (user != null && StringUtils.hasText(user.getProvince())) {
            return user.getProvince().trim();
        }
        return DEFAULT_REGION;
    }

    private String normalizeRegion(String region) {
        return StringUtils.hasText(region) ? region.trim() : "";
    }

    private boolean isNationalRegion(String region) {
        return !StringUtils.hasText(region) || DEFAULT_REGION.equals(region.trim());
    }

    private BigDecimal regionBoost(String resourceProvince, String targetRegion, Map<String, BigDecimal> weights) {
        if (isNationalRegion(targetRegion)) {
            return BigDecimal.ZERO;
        }
        if (!StringUtils.hasText(resourceProvince) || DEFAULT_REGION.equals(resourceProvince.trim())) {
            return weights.getOrDefault("national_region_boost", BigDecimal.valueOf(0.12));
        }
        return resourceProvince.trim().equals(targetRegion)
                ? weights.getOrDefault("region_match_boost", BigDecimal.valueOf(0.28))
                : BigDecimal.ZERO;
    }

    private BigDecimal regionPenalty(String resourceProvince, String targetRegion, Map<String, BigDecimal> weights) {
        if (isNationalRegion(targetRegion) || !StringUtils.hasText(resourceProvince) || DEFAULT_REGION.equals(resourceProvince.trim())) {
            return BigDecimal.ZERO;
        }
        return resourceProvince.trim().equals(targetRegion)
                ? BigDecimal.ZERO
                : weights.getOrDefault("outside_region_penalty", BigDecimal.valueOf(0.18));
    }

    private void addRegionReason(List<String> reasons, String resourceName, String resourceProvince, String targetRegion) {
        if (reasons == null || isNationalRegion(targetRegion)) {
            return;
        }
        if (StringUtils.hasText(resourceProvince) && resourceProvince.trim().equals(targetRegion)) {
            reasons.add("与你选择的报考地区匹配");
        } else if (!StringUtils.hasText(resourceProvince) || DEFAULT_REGION.equals(resourceProvince.trim())) {
            reasons.add("全国" + resourceName + "兜底保留");
        }
    }

    private String regionAwareReason(String fallback, String resourceName, String resourceProvince, String targetRegion) {
        if (isNationalRegion(targetRegion)) {
            return fallback;
        }
        if (StringUtils.hasText(resourceProvince) && resourceProvince.trim().equals(targetRegion)) {
            return targetRegion + resourceName + "与你选择的报考地区匹配";
        }
        if (!StringUtils.hasText(resourceProvince) || DEFAULT_REGION.equals(resourceProvince.trim())) {
            return "全国" + resourceName + "兜底推荐，适配多地区备考";
        }
        return fallback;
    }

    private boolean pinnedMatchesRegion(RecommendPinnedResource item, String targetRegion) {
        String scope = safeText(item.getRegionScope(), "ALL");
        if ("ALL".equalsIgnoreCase(scope)) {
            return true;
        }
        if ("NATIONAL".equalsIgnoreCase(scope)) {
            return Boolean.TRUE.equals(item.getFallbackEnabled()) || isNationalRegion(targetRegion);
        }
        if ("PROVINCE".equalsIgnoreCase(scope) || "REGION".equalsIgnoreCase(scope)) {
            return StringUtils.hasText(item.getProvince()) && item.getProvince().trim().equals(targetRegion);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean cachedMatchesRegion(Map<String, Object> cached, String targetRegion) {
        Object portraitValue = cached.get("portrait");
        if (!(portraitValue instanceof Map)) {
            return true;
        }
        Object cachedRegion = ((Map<String, Object>) portraitValue).get("targetProvince");
        return Objects.equals(String.valueOf(cachedRegion), targetRegion);
    }

    private Map<String, Object> withRegionContext(Map<String, Object> payload, String explicitRegion, String targetRegion, SysUser user) {
        Map<String, Object> result = new LinkedHashMap<>(payload);
        result.put("regionContext", buildRegionContext(explicitRegion, targetRegion, user));
        return result;
    }

    private Map<String, Object> buildRegionContext(String explicitRegion, String targetRegion, SysUser user) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("selectedRegion", normalizeRegion(explicitRegion));
        context.put("profileProvince", user == null ? null : user.getProvince());
        context.put("targetRegion", targetRegion);
        context.put("priority", StringUtils.hasText(explicitRegion) ? "SELECTED_REGION" : user != null && StringUtils.hasText(user.getProvince()) ? "USER_PROFILE" : "DEFAULT_NATIONAL");
        return context;
    }

    private BigDecimal recencyScore(LocalDateTime time, int windowDays) {
        if (time == null) {
            return BigDecimal.ZERO;
        }
        long ageDays = Math.max(0, Duration.between(time, LocalDateTime.now()).toDays());
        BigDecimal score = BigDecimal.ONE.subtract(BigDecimal.valueOf(Math.min(ageDays, windowDays)).divide(BigDecimal.valueOf(windowDays), 4, RoundingMode.HALF_UP));
        return score.max(BigDecimal.ZERO);
    }

    private Map<String, BigDecimal> courseTagWeights(Course course) {
        Map<String, BigDecimal> tags = new LinkedHashMap<>();
        addTagWeight(tags, course.getProvince(), BigDecimal.valueOf(0.95));
        addTagWeight(tags, course.getRegionScope(), BigDecimal.valueOf(0.25));
        addTagWeight(tags, course.getExamType(), BigDecimal.valueOf(0.90));
        addTagWeight(tags, course.getSubjectName(), BigDecimal.valueOf(1.05));
        addTagWeight(tags, course.getCourseType(), BigDecimal.valueOf(0.55));
        splitTags(course.getTags()).forEach(tag -> addTagWeight(tags, tag, BigDecimal.valueOf(0.32)));
        splitTags(course.getTargetUsers()).forEach(tag -> addTagWeight(tags, tag, BigDecimal.valueOf(0.20)));
        return tags;
    }

    private Map<String, BigDecimal> examTagWeights(Exam exam) {
        Map<String, BigDecimal> tags = new LinkedHashMap<>();
        addTagWeight(tags, exam.getProvince(), BigDecimal.valueOf(0.95));
        addTagWeight(tags, exam.getExamType(), BigDecimal.valueOf(0.90));
        addTagWeight(tags, exam.getSubjectName(), BigDecimal.valueOf(1.00));
        addTagWeight(tags, exam.getSeasonName(), BigDecimal.valueOf(0.25));
        splitTags(exam.getIntro()).forEach(tag -> addTagWeight(tags, tag, BigDecimal.valueOf(0.12)));
        return tags;
    }

    private Map<String, BigDecimal> newsTagWeights(News news) {
        Map<String, BigDecimal> tags = new LinkedHashMap<>();
        addTagWeight(tags, news.getProvince(), BigDecimal.valueOf(0.95));
        addTagWeight(tags, news.getInfoCategory(), BigDecimal.valueOf(0.45));
        splitTags(news.getTags()).forEach(tag -> addTagWeight(tags, tag, BigDecimal.valueOf(0.70)));
        splitTags(news.getSummary()).forEach(tag -> addTagWeight(tags, tag, BigDecimal.valueOf(0.12)));
        splitTags(news.getAuthorName()).forEach(tag -> addTagWeight(tags, tag, BigDecimal.valueOf(0.08)));
        return tags;
    }

    private Map<String, BigDecimal> practiceTagWeights(SysUser user, String weakSubject) {
        Map<String, BigDecimal> tags = new LinkedHashMap<>();
        addTagWeight(tags, user.getProvince(), BigDecimal.valueOf(0.75));
        addTagWeight(tags, user.getExamType(), BigDecimal.valueOf(0.55));
        addTagWeight(tags, weakSubject, BigDecimal.valueOf(1.05));
        addTagWeight(tags, "专项刷题", BigDecimal.valueOf(0.20));
        return tags;
    }

    private int recommendedQuestionCount(String subjectName, String province, String categoryName, int targetCount) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true)
                .eq(StringUtils.hasText(subjectName), Question::getSubjectName, subjectName)
                .eq(StringUtils.hasText(categoryName), Question::getCategoryName, categoryName);
        if (StringUtils.hasText(province)) {
            wrapper.and(item -> item.eq(Question::getProvince, province).or().eq(Question::getProvince, DEFAULT_REGION));
        }
        long count = questionMapper.selectCount(wrapper);
        if (count <= 0 && StringUtils.hasText(province)) {
            count = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                    .eq(Question::getEnabled, true)
                    .eq(StringUtils.hasText(subjectName), Question::getSubjectName, subjectName)
                    .eq(StringUtils.hasText(categoryName), Question::getCategoryName, categoryName));
        }
        if (count <= 0) {
            return targetCount;
        }
        return (int) Math.max(5, Math.min(targetCount, count));
    }

    private int estimateMinutes(int questionCount) {
        return Math.max(8, (int) Math.ceil(questionCount * 0.9));
    }

    private String practicePath(String province, String subjectName, String categoryName, int questionCount) {
        List<String> params = new ArrayList<>();
        String pathProvince = province;
        if (StringUtils.hasText(pathProvince) && !hasQuestions(subjectName, pathProvince, categoryName)) {
            pathProvince = "";
        }
        if (StringUtils.hasText(pathProvince)) params.add("province=" + urlValue(pathProvince));
        if (StringUtils.hasText(subjectName)) params.add("subjectName=" + urlValue(subjectName));
        if (StringUtils.hasText(categoryName)) params.add("categoryName=" + urlValue(categoryName));
        params.add("questionCount=" + questionCount);
        return "/search?" + String.join("&", params);
    }

    private boolean hasQuestions(String subjectName, String province, String categoryName) {
        return questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true)
                .eq(StringUtils.hasText(subjectName), Question::getSubjectName, subjectName)
                .eq(StringUtils.hasText(province), Question::getProvince, province)
                .eq(StringUtils.hasText(categoryName), Question::getCategoryName, categoryName)) > 0;
    }

    private String urlValue(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    private String tagText(String... values) {
        return Arrays.stream(values)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private BigDecimal resourceTagSimilarity(Map<String, BigDecimal> userTags, Map<String, BigDecimal> resourceTags) {
        if (userTags.isEmpty() || resourceTags.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal overlap = BigDecimal.ZERO;
        BigDecimal union = BigDecimal.ZERO;
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(userTags.keySet());
        keys.addAll(resourceTags.keySet());
        for (String key : keys) {
            BigDecimal userWeight = userTags.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal resourceWeight = resourceTags.getOrDefault(key, BigDecimal.ZERO);
            overlap = overlap.add(userWeight.min(resourceWeight));
            union = union.add(userWeight.max(resourceWeight));
        }
        if (union.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return overlap.divide(union, 4, RoundingMode.HALF_UP);
    }

    private List<String> matchingTags(Map<String, BigDecimal> userTags, Map<String, BigDecimal> resourceTags) {
        return userTags.keySet().stream()
                .filter(resourceTags::containsKey)
                .filter(StringUtils::hasText)
                .limit(4)
                .collect(Collectors.toList());
    }

    private List<String> buildExplainReasons(BigDecimal collaborative,
                                             BigDecimal content,
                                             BigDecimal hot,
                                             List<String> matchedTags,
                                             String extraReason) {
        List<String> reasons = new ArrayList<>();
        if (collaborative.compareTo(BigDecimal.valueOf(0.20)) >= 0) {
            reasons.add("相似学员行为命中");
        }
        if (content.compareTo(BigDecimal.valueOf(0.20)) >= 0) {
            reasons.add(matchedTags.isEmpty() ? "画像标签匹配" : "命中标签：" + String.join(" / ", matchedTags));
        }
        if (hot.compareTo(BigDecimal.valueOf(0.35)) >= 0) {
            reasons.add("当前热度较高");
        }
        if (StringUtils.hasText(extraReason)) {
            reasons.add(extraReason);
        }
        if (reasons.isEmpty()) {
            reasons.add("综合排序推荐");
        }
        return reasons;
    }

    private void addTagWeight(Map<String, BigDecimal> tags, String rawTag, BigDecimal weight) {
        if (!StringUtils.hasText(rawTag)) {
            return;
        }
        String tag = rawTag.trim();
        if (!StringUtils.hasText(tag)) {
            return;
        }
        tags.merge(tag, weight, BigDecimal::add);
    }

    private List<String> splitTags(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new ArrayList<>();
        }
        return Arrays.stream(raw.split("[、,，|/\\s]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> deduplicate(List<Map<String, Object>> cards) {
        return new ArrayList<>(cards.stream().collect(Collectors.toMap(
                item -> cardKey(String.valueOf(item.get("resourceType")), ((Number) item.get("id")).longValue()),
                item -> item,
                (a, b) -> new BigDecimal(String.valueOf(a.getOrDefault("finalScore", 0))).compareTo(new BigDecimal(String.valueOf(b.getOrDefault("finalScore", 0)))) >= 0 ? a : b,
                LinkedHashMap::new)).values());
    }

    private Map<String, Object> buildCard(String type, Long id, String title, String coverUrl, String tags, String reason, String path, String actionText) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("resourceType", type);
        map.put("id", id);
        map.put("title", title);
        map.put("coverUrl", coverUrl);
        map.put("tags", tags);
        map.put("reason", reason);
        map.put("path", path);
        map.put("actionText", actionText);
        return map;
    }

    private Map<String, Object> buildPracticeCard(Long id,
                                                  String title,
                                                  String tags,
                                                  String reason,
                                                  String path,
                                                  String actionText,
                                                  int questionCount,
                                                  int estimatedMinutes,
                                                  String sourceLabel) {
        Map<String, Object> map = buildCard("PRACTICE", id, title, null, tags, reason, path, actionText);
        map.put("questionCount", questionCount);
        map.put("estimatedMinutes", estimatedMinutes);
        map.put("sourceLabel", sourceLabel);
        map.put("practicePack", true);
        return map;
    }

    private Map<String, Object> buildNewsCard(News news, String reason, String newsType) {
        Map<String, Object> map = buildCard("NEWS", news.getId(), news.getTitle(), news.getCoverUrl(), news.getTags(), reason, "/news/detail/" + news.getId(), "阅读资讯");
        map.put("newsType", newsType);
        map.put("readCount", news.getReadCount() == null ? 0 : news.getReadCount());
        map.put("shareCount", news.getShareCount() == null ? 0 : news.getShareCount());
        map.put("sourceName", safeText(news.getSourceName(), "平台资讯"));
        map.put("publishedAt", news.getPublishedAt() == null ? null : news.getPublishedAt().format(FMT));
        if (news.getCategoryId() != null) {
            NewsCategory category = newsCategoryMapper.selectById(news.getCategoryId());
            if (category != null) {
                map.put("categoryName", category.getName());
            }
        }
        return map;
    }

    private Map<String, Object> buildCourseCard(Course course, String reason, String path, String actionText) {
        return buildCard("COURSE", course.getId(), course.getTitle(), course.getCoverUrl(), safeText(course.getTags(), course.getSubjectName()), reason, path, actionText);
    }

    private Map<String, Object> courseMeta(Map<String, Object> card) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("recommendReason", card.get("reason"));
        meta.put("explainTags", card.get("explainTags"));
        meta.put("finalScore", card.get("finalScore"));
        meta.put("recommendActionText", card.get("actionText"));
        meta.put("recommended", true);
        return meta;
    }

    private BigDecimal scoreValue(Map<String, Object> card) {
        Object value = card.get("finalScore");
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Map<String, Object> typeStat(String type, List<RecommendFeedback> feedbacks) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("resourceType", type);
        item.put("count", feedbacks.stream().filter(feedback -> type.equals(feedback.getResourceType())).count());
        return item;
    }

    private Map<String, Object> regionalBehaviorStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseLearning", courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Course::getStatus, "PUBLISHED"))
                .stream()
                .collect(Collectors.groupingBy(item -> safeText(item.getProvince(), DEFAULT_REGION), LinkedHashMap::new,
                        Collectors.summingInt(item -> item.getLearnCount() == null ? 0 : item.getLearnCount()))));
        result.put("newsClicks", newsMapper.selectList(new LambdaQueryWrapper<News>().eq(News::getStatus, "PUBLISHED"))
                .stream()
                .collect(Collectors.groupingBy(item -> safeText(item.getProvince(), DEFAULT_REGION), LinkedHashMap::new,
                        Collectors.summingInt(item -> item.getReadCount() == null ? 0 : item.getReadCount()))));
        result.put("practiceParticipation", practiceMapper.selectList(new LambdaQueryWrapper<PracticeRecord>().eq(PracticeRecord::getStatus, "SUBMITTED"))
                .stream()
                .collect(Collectors.groupingBy(item -> safeText(item.getProvince(), DEFAULT_REGION), LinkedHashMap::new, Collectors.counting())));
        result.put("examParticipation", examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getStatus, "SUBMITTED"))
                .stream()
                .map(item -> item.getExamId() == null ? null : examMapper.selectById(item.getExamId()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(item -> safeText(item.getProvince(), DEFAULT_REGION), LinkedHashMap::new, Collectors.counting())));
        return result;
    }

    private List<Map<String, Object>> stats(List<Map<String, Object>> cards, String field) {
        return cards.stream()
                .collect(Collectors.groupingBy(item -> String.valueOf(item.get(field)), LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put(field, entry.getKey());
                    map.put("resourceType", entry.getKey());
                    map.put("count", entry.getValue());
                    return map;
                }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> statsByTags(List<Map<String, Object>> cards, String fallback) {
        return cards.stream()
                .map(item -> StringUtils.hasText(String.valueOf(item.get("tags"))) ? String.valueOf(item.get("tags")) : fallback)
                .collect(Collectors.groupingBy(item -> item, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .limit(5)
                .map(entry -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("label", entry.getKey());
                    map.put("count", entry.getValue());
                    return map;
                }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> weakFocus(List<Map<String, Object>> cards, String weak) {
        List<Map<String, Object>> result = cards.stream()
                .filter(item -> String.valueOf(item.get("reason")).contains("薄弱") || String.valueOf(item.get("title")).contains(weak))
                .limit(4)
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("title", item.get("title"));
                    map.put("reason", item.get("reason"));
                    map.put("path", item.get("path"));
                    return map;
                }).collect(Collectors.toList());
        if (result.isEmpty()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("title", weak + " 补强建议");
            map.put("reason", "当前画像识别出的薄弱科目是 " + weak + "，建议优先进入搜题页和专项练习补齐短板。");
            map.put("path", "/search?subjectName=" + weak);
            result.add(map);
        }
        return result;
    }

    private String resolveTitle(String type, Long id) {
        Map<String, Object> card = resourceCard(type, id);
        return card == null ? "资源已删除" : String.valueOf(card.get("title"));
    }

    private void insertRefreshLog(Long userId, String source) {
        RecommendRefreshLog log = new RecommendRefreshLog();
        log.setUserId(userId);
        log.setRefreshSource(source);
        refreshLogMapper.insert(log);
    }

    private long currentRefreshCount(Long userId) {
        LambdaQueryWrapper<RecommendRefreshLog> wrapper = new LambdaQueryWrapper<>();
        if (userId == null) wrapper.isNull(RecommendRefreshLog::getUserId); else wrapper.eq(RecommendRefreshLog::getUserId, userId);
        return refreshLogMapper.selectCount(wrapper);
    }

    private String userKey(Long userId, String suffix) {
        return "recommend:user:" + userId + ":" + suffix;
    }

    private String guestKey(String region) {
        String targetRegion = StringUtils.hasText(region) ? region.trim() : DEFAULT_REGION;
        return isNationalRegion(targetRegion) ? GUEST_KEY : GUEST_KEY + ":" + targetRegion;
    }

    private void evictUserCache(Long userId) {
        try {
            redis.delete(Arrays.asList(userKey(userId, "overview"), userKey(userId, "profile"), userKey(userId, "portrait")));
        } catch (Exception ignored) {
        }
    }

    private String nowText() {
        return LocalDateTime.now().format(FMT);
    }

    private String cardKey(String type, Long id) {
        return type + "-" + id;
    }

    private String courseAction(Course course) {
        return course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) <= 0 ? "免费学习" : "查看课程";
    }

    private String safeText(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return value == null ? new ArrayList<>() : (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value == null ? new LinkedHashMap<>() : (Map<String, Object>) value;
    }

    private void writeJson(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ignored) {
        }
    }

    private void writeString(String key, String value, Duration ttl) {
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (Exception ignored) {
        }
    }

    private Map<String, Object> readMap(String key) {
        try {
            String raw = redis.opsForValue().get(key);
            return StringUtils.hasText(raw) ? objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {}) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> readList(String key) {
        try {
            String raw = redis.opsForValue().get(key);
            return StringUtils.hasText(raw) ? objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {}) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String readString(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (Exception e) {
            return null;
        }
    }

    private Long requireCurrentUserId() {
        Long userId = currentUserIdOrNull();
        if (userId == null) throw new BizException(401, "请先登录");
        return userId;
    }

    private Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) return null;
        return ((LoginUser) authentication.getPrincipal()).getUserId();
    }
}
