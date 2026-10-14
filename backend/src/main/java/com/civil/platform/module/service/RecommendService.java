package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.module.dto.RecommendFeedbackRequest;
import com.civil.platform.module.entity.Course;
import com.civil.platform.module.entity.Exam;
import com.civil.platform.module.entity.ExamRecord;
import com.civil.platform.module.entity.News;
import com.civil.platform.module.entity.PracticeRecord;
import com.civil.platform.module.entity.RecommendConfig;
import com.civil.platform.module.entity.RecommendFeedback;
import com.civil.platform.module.entity.RecommendRefreshLog;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.entity.UserCourse;
import com.civil.platform.module.entity.UserCourseLearningRecord;
import com.civil.platform.module.entity.UserCourseStudyProgress;
import com.civil.platform.module.entity.UserWrongQuestion;
import com.civil.platform.module.mapper.CourseMapper;
import com.civil.platform.module.mapper.ExamMapper;
import com.civil.platform.module.mapper.ExamRecordMapper;
import com.civil.platform.module.mapper.NewsMapper;
import com.civil.platform.module.mapper.PracticeRecordMapper;
import com.civil.platform.module.mapper.RecommendConfigMapper;
import com.civil.platform.module.mapper.RecommendFeedbackMapper;
import com.civil.platform.module.mapper.RecommendRefreshLogMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.UserCourseLearningRecordMapper;
import com.civil.platform.module.mapper.UserCourseMapper;
import com.civil.platform.module.mapper.UserCourseStudyProgressMapper;
import com.civil.platform.module.mapper.UserWrongQuestionMapper;
import com.civil.platform.module.service.recommend.UserCfRebuildService;
import com.civil.platform.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendService {

    private static final String REFRESH_SOURCE = "RECOMMEND";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RecommendConfigMapper recommendConfigMapper;
    private final RecommendFeedbackMapper recommendFeedbackMapper;
    private final RecommendRefreshLogMapper recommendRefreshLogMapper;
    private final SysUserMapper sysUserMapper;
    private final CourseMapper courseMapper;
    private final NewsMapper newsMapper;
    private final ExamMapper examMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final ExamRecordMapper examRecordMapper;
    private final UserWrongQuestionMapper userWrongQuestionMapper;
    private final UserCourseMapper userCourseMapper;
    private final UserCourseStudyProgressMapper userCourseStudyProgressMapper;
    private final UserCourseLearningRecordMapper userCourseLearningRecordMapper;
    private final UserCfRebuildService userCfRebuildService;

    public RecommendService(RecommendConfigMapper recommendConfigMapper,
                            RecommendFeedbackMapper recommendFeedbackMapper,
                            RecommendRefreshLogMapper recommendRefreshLogMapper,
                            SysUserMapper sysUserMapper,
                            CourseMapper courseMapper,
                            NewsMapper newsMapper,
                            ExamMapper examMapper,
                            PracticeRecordMapper practiceRecordMapper,
                            ExamRecordMapper examRecordMapper,
                            UserWrongQuestionMapper userWrongQuestionMapper,
                            UserCourseMapper userCourseMapper,
                            UserCourseStudyProgressMapper userCourseStudyProgressMapper,
                            UserCourseLearningRecordMapper userCourseLearningRecordMapper,
                            UserCfRebuildService userCfRebuildService) {
        this.recommendConfigMapper = recommendConfigMapper;
        this.recommendFeedbackMapper = recommendFeedbackMapper;
        this.recommendRefreshLogMapper = recommendRefreshLogMapper;
        this.sysUserMapper = sysUserMapper;
        this.courseMapper = courseMapper;
        this.newsMapper = newsMapper;
        this.examMapper = examMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.examRecordMapper = examRecordMapper;
        this.userWrongQuestionMapper = userWrongQuestionMapper;
        this.userCourseMapper = userCourseMapper;
        this.userCourseStudyProgressMapper = userCourseStudyProgressMapper;
        this.userCourseLearningRecordMapper = userCourseLearningRecordMapper;
        this.userCfRebuildService = userCfRebuildService;
    }

    public Map<String, Object> overview() {
        return buildOverview(currentUserIdOrNull(), false);
    }

    public Map<String, Object> profile() {
        Long userId = currentUserIdOrNull();
        Map<String, Object> overview = buildOverview(userId, false);
        @SuppressWarnings("unchecked")
        Map<String, Object> portrait = (Map<String, Object>) overview.get("portrait");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recommendCards = (List<Map<String, Object>>) overview.get("recommendCards");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> continueLearningCourses = (List<Map<String, Object>>) overview.get("continueLearningCourses");
        Map<String, Object> result = new HashMap<>();
        result.put("portrait", portrait);
        result.put("weights", overview.get("weights"));
        result.put("refreshAt", overview.get("refreshAt"));
        result.put("refreshCount", overview.get("refreshCount"));
        result.put("loggedIn", overview.get("loggedIn"));
        result.put("recommendCards", recommendCards);
        result.put("continueLearningCourses", continueLearningCourses);
        result.put("recommendReasonStats", buildRecommendReasonStats(recommendCards));
        result.put("preferenceSubjects", buildPreferenceSubjects(recommendCards, portrait));
        result.put("weaknessFocus", buildWeaknessFocus(recommendCards, portrait));
        return result;
    }

    public Map<String, Object> refresh() {
        Long userId = currentUserIdOrNull();
        RecommendRefreshLog refreshLog = new RecommendRefreshLog();
        refreshLog.setUserId(userId);
        refreshLog.setRefreshSource(REFRESH_SOURCE);
        recommendRefreshLogMapper.insert(refreshLog);
        return buildOverview(userId, true);
    }

    public void feedback(RecommendFeedbackRequest request) {
        Long userId = requireCurrentUserId();
        RecommendFeedback existed = recommendFeedbackMapper.selectOne(new LambdaQueryWrapper<RecommendFeedback>()
                .eq(RecommendFeedback::getUserId, userId)
                .eq(RecommendFeedback::getResourceType, request.getResourceType())
                .eq(RecommendFeedback::getResourceId, request.getResourceId())
                .eq(RecommendFeedback::getFeedbackType, "NOT_INTERESTED"));
        if (existed != null) {
            return;
        }
        RecommendFeedback feedback = new RecommendFeedback();
        feedback.setUserId(userId);
        feedback.setResourceType(request.getResourceType());
        feedback.setResourceId(request.getResourceId());
        feedback.setFeedbackType("NOT_INTERESTED");
        feedback.setSourcePage(StringUtils.hasText(request.getSourcePage()) ? request.getSourcePage() : REFRESH_SOURCE);
        feedback.setReasonText(request.getReasonText());
        recommendFeedbackMapper.insert(feedback);
        // 触发 UserCF 单用户异步增量刷新（写完反馈后立即让相似度回流）
        userCfRebuildService.refreshUserAsync(userId);
    }

    public Map<String, Object> adminFeedbackSummary() {
        List<RecommendFeedback> feedbacks = recommendFeedbackMapper.selectList(new LambdaQueryWrapper<RecommendFeedback>()
                .orderByDesc(RecommendFeedback::getCreatedAt));
        Map<String, Object> result = new HashMap<>();
        result.put("feedbackCount", feedbacks.size());
        result.put("distinctUserCount", feedbacks.stream().map(RecommendFeedback::getUserId).filter(Objects::nonNull).distinct().count());
        result.put("todayFeedbackCount", feedbacks.stream()
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count());
        result.put("refreshCount", recommendRefreshLogMapper.selectCount(new LambdaQueryWrapper<RecommendRefreshLog>()
                .eq(RecommendRefreshLog::getRefreshSource, REFRESH_SOURCE)));
        result.put("latestFeedbackAt", feedbacks.isEmpty() || feedbacks.get(0).getCreatedAt() == null
                ? null : feedbacks.get(0).getCreatedAt().format(DATE_TIME_FORMATTER));
        result.put("resourceTypeStats", Arrays.asList(
                buildTypeStat("COURSE", feedbacks),
                buildTypeStat("PRACTICE", feedbacks),
                buildTypeStat("EXAM", feedbacks),
                buildTypeStat("NEWS", feedbacks)
        ));
        return result;
    }

    public PageResult<Map<String, Object>> adminFeedbackPage(Long pageNo, Long pageSize, String resourceType) {
        Page<RecommendFeedback> page = recommendFeedbackMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<RecommendFeedback>()
                .eq(StringUtils.hasText(resourceType), RecommendFeedback::getResourceType, resourceType)
                .orderByDesc(RecommendFeedback::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(this::buildAdminFeedbackItem).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    private Map<String, Object> buildOverview(Long userId, boolean refreshed) {
        Map<String, Object> result = new HashMap<>();
        result.put("weights", recommendConfigMapper.selectList(new LambdaQueryWrapper<RecommendConfig>()
                        .orderByAsc(RecommendConfig::getId))
                .stream()
                .collect(Collectors.toMap(RecommendConfig::getConfigKey, RecommendConfig::getConfigValue, (left, right) -> left, LinkedHashMap::new)));
        long refreshCount = currentRefreshCount(userId);
        result.put("refreshCount", refreshCount);
        result.put("refreshed", refreshed);
        result.put("refreshAt", LocalDateTime.now().format(DATE_TIME_FORMATTER));

        if (userId == null) {
            result.put("loggedIn", false);
            result.put("portrait", buildGuestPortrait());
            result.put("continueLearningCourses", new ArrayList<>());
            result.put("recommendCards", buildGuestRecommendations(refreshCount));
            result.put("feedbackEnabled", false);
            return result;
        }

        SysUser user = sysUserMapper.selectById(userId);
        List<PracticeRecord> practices = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getStatus, "SUBMITTED"));
        List<ExamRecord> exams = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getStatus, "SUBMITTED"));
        List<UserWrongQuestion> wrongQuestions = userWrongQuestionMapper.selectList(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getRemoved, false));
        List<UserCourse> ownedCourses = userCourseMapper.selectList(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId));
        List<UserCourseStudyProgress> studyProgressList = userCourseStudyProgressMapper.selectList(new LambdaQueryWrapper<UserCourseStudyProgress>()
                .eq(UserCourseStudyProgress::getUserId, userId)
                .orderByDesc(UserCourseStudyProgress::getLastStudyAt));
        List<UserCourseLearningRecord> learningRecords = userCourseLearningRecordMapper.selectList(new LambdaQueryWrapper<UserCourseLearningRecord>()
                .eq(UserCourseLearningRecord::getUserId, userId)
                .orderByDesc(UserCourseLearningRecord::getCreatedAt)
                .last("limit 30"));
        Set<String> feedbackKeys = recommendFeedbackMapper.selectList(new LambdaQueryWrapper<RecommendFeedback>()
                        .eq(RecommendFeedback::getUserId, userId)
                        .eq(RecommendFeedback::getFeedbackType, "NOT_INTERESTED"))
                .stream()
                .map(item -> buildCardKey(item.getResourceType(), item.getResourceId()))
                .collect(Collectors.toSet());

        result.put("loggedIn", true);
        result.put("portrait", buildPortrait(user, practices, exams, wrongQuestions, studyProgressList, learningRecords));
        result.put("continueLearningCourses", buildContinueLearningCourses(ownedCourses, studyProgressList));
        result.put("recommendCards", buildPersonalizedRecommendations(user, practices, exams, wrongQuestions, ownedCourses, studyProgressList, feedbackKeys, refreshCount));
        result.put("feedbackEnabled", true);
        return result;
    }

    private List<Map<String, Object>> buildGuestRecommendations(long refreshCount) {
        List<Map<String, Object>> cards = new ArrayList<>();
        courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "PUBLISHED")
                        .orderByDesc(Course::getLearnCount)
                        .last("limit 4"))
                .forEach(course -> cards.add(buildCard("COURSE", course.getId(), course.getTitle(), course.getCoverUrl(),
                        safeText(course.getTags(), course.getSubjectName()), "热门课程兜底推荐", "/course/detail/" + course.getId(), "查看课程")));
        examMapper.selectList(new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getStatus, "OPEN")
                        .orderByDesc(Exam::getCreatedAt)
                        .last("limit 3"))
                .forEach(exam -> cards.add(buildCard("EXAM", exam.getId(), exam.getTitle(), null,
                        safeText(exam.getProvince(), exam.getSubjectName()), "近期开放模考", "/exam/detail/" + exam.getId(), "开始模考")));
        newsMapper.selectList(new LambdaQueryWrapper<News>()
                        .eq(News::getStatus, "PUBLISHED")
                        .orderByDesc(News::getReadCount)
                        .last("limit 3"))
                .forEach(news -> cards.add(buildCard("NEWS", news.getId(), news.getTitle(), news.getCoverUrl(),
                        news.getTags(), "热门资讯推荐", "/news/detail/" + news.getId(), "阅读资讯")));
        return applyRefreshWindow(cards, refreshCount, 7);
    }

    private Map<String, Object> buildGuestPortrait() {
        Map<String, Object> portrait = new HashMap<>();
        portrait.put("title", "游客模式");
        portrait.put("summary", "当前展示热门课程、模考和资讯。登录后可基于你的学习进度、练习记录和错题情况生成个性化推荐。");
        portrait.put("labels", Arrays.asList("热门兜底", "冷启动友好", "登录后更精准"));
        portrait.put("targetProvince", "未登录");
        portrait.put("preferredSubject", "未登录");
        portrait.put("examType", "未登录");
        portrait.put("activeDays", 0);
        portrait.put("totalStudyHours", 0);
        portrait.put("weakSubject", "登录后分析");
        portrait.put("strongSubject", "登录后分析");
        return portrait;
    }

    private Map<String, Object> buildPortrait(SysUser user,
                                              List<PracticeRecord> practices,
                                              List<ExamRecord> exams,
                                              List<UserWrongQuestion> wrongQuestions,
                                              List<UserCourseStudyProgress> studyProgressList,
                                              List<UserCourseLearningRecord> learningRecords) {
        Map<String, Object> portrait = new HashMap<>();
        Map<String, BigDecimal> subjectAccuracy = buildSubjectAccuracy(practices);
        String weakSubject = subjectAccuracy.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(StringUtils.hasText(user.getPreferredSubject()) ? user.getPreferredSubject() : "行测");
        String strongSubject = subjectAccuracy.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(StringUtils.hasText(user.getPreferredSubject()) ? user.getPreferredSubject() : "综合能力");
        Set<String> activeDays = learningRecords.stream()
                .map(UserCourseLearningRecord::getCreatedAt)
                .filter(Objects::nonNull)
                .map(item -> item.toLocalDate().toString())
                .collect(Collectors.toSet());
        int totalStudySeconds = studyProgressList.stream()
                .mapToInt(item -> item.getTotalStudySeconds() == null ? 0 : item.getTotalStudySeconds())
                .sum();

        List<String> labels = new ArrayList<>();
        if (StringUtils.hasText(user.getProvince())) {
            labels.add(user.getProvince() + "备考");
        }
        if (StringUtils.hasText(user.getPreferredSubject())) {
            labels.add(user.getPreferredSubject() + "主攻");
        }
        if (wrongQuestions.size() >= 3) {
            labels.add("错题需强化");
        }
        if (studyProgressList.stream().anyMatch(item -> {
            BigDecimal percent = item.getProgressPercent() == null ? BigDecimal.ZERO : item.getProgressPercent();
            return percent.compareTo(BigDecimal.valueOf(30)) >= 0 && percent.compareTo(BigDecimal.valueOf(100)) < 0;
        })) {
            labels.add("有继续学习课程");
        }
        if (labels.isEmpty()) {
            labels.add("正在建立学习画像");
        }

        portrait.put("title", safeText(user.getNickname(), user.getUsername()) + " 的学习画像");
        portrait.put("summary", "结合课程学习进度、刷题表现、错题记录和近期活跃度生成，推荐会随着你的学习行为持续刷新。");
        portrait.put("labels", labels);
        portrait.put("targetProvince", safeText(user.getProvince(), "全国"));
        portrait.put("preferredSubject", safeText(user.getPreferredSubject(), "综合"));
        portrait.put("examType", safeText(user.getExamType(), "公考通用"));
        portrait.put("activeDays", activeDays.size());
        portrait.put("totalStudyHours", BigDecimal.valueOf(totalStudySeconds)
                .divide(BigDecimal.valueOf(3600), 1, RoundingMode.HALF_UP));
        portrait.put("weakSubject", weakSubject);
        portrait.put("strongSubject", strongSubject);
        portrait.put("practiceCount", practices.size());
        portrait.put("examCount", exams.size());
        portrait.put("wrongQuestionCount", wrongQuestions.size());
        return portrait;
    }

    private List<Map<String, Object>> buildContinueLearningCourses(List<UserCourse> ownedCourses,
                                                                   List<UserCourseStudyProgress> studyProgressList) {
        if (ownedCourses.isEmpty() || studyProgressList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(ownedCourses.stream()
                        .map(UserCourse::getCourseId)
                        .distinct()
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Course::getId, item -> item, (left, right) -> left));
        return studyProgressList.stream()
                .filter(item -> {
                    BigDecimal percent = item.getProgressPercent() == null ? BigDecimal.ZERO : item.getProgressPercent();
                    return percent.compareTo(BigDecimal.ZERO) > 0 && percent.compareTo(BigDecimal.valueOf(100)) < 0;
                })
                .sorted(Comparator.comparing(UserCourseStudyProgress::getLastStudyAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .map(item -> {
                    Course course = courseMap.get(item.getCourseId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("courseId", item.getCourseId());
                    map.put("title", course == null ? "课程已下架" : course.getTitle());
                    map.put("coverUrl", course == null ? null : course.getCoverUrl());
                    map.put("teacherName", course == null ? null : course.getTeacherName());
                    map.put("subjectName", course == null ? null : course.getSubjectName());
                    map.put("progressPercent", item.getProgressPercent() == null ? BigDecimal.ZERO : item.getProgressPercent());
                    map.put("completedChapterCount", item.getCompletedChapterCount() == null ? 0 : item.getCompletedChapterCount());
                    map.put("totalStudySeconds", item.getTotalStudySeconds() == null ? 0 : item.getTotalStudySeconds());
                    map.put("lastStudyAt", item.getLastStudyAt());
                    map.put("path", "/course/play/" + item.getCourseId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildPersonalizedRecommendations(SysUser user,
                                                                       List<PracticeRecord> practices,
                                                                       List<ExamRecord> exams,
                                                                       List<UserWrongQuestion> wrongQuestions,
                                                                       List<UserCourse> ownedCourses,
                                                                       List<UserCourseStudyProgress> studyProgressList,
                                                                       Set<String> feedbackKeys,
                                                                       long refreshCount) {
        List<Map<String, Object>> cards = new ArrayList<>();
        Set<Long> ownedCourseIds = ownedCourses.stream().map(UserCourse::getCourseId).collect(Collectors.toSet());
        Map<String, BigDecimal> accuracyBySubject = buildSubjectAccuracy(practices);
        String weakSubject = accuracyBySubject.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(StringUtils.hasText(user.getPreferredSubject()) ? user.getPreferredSubject() : "行测");

        studyProgressList.stream()
                .filter(item -> {
                    BigDecimal percent = item.getProgressPercent() == null ? BigDecimal.ZERO : item.getProgressPercent();
                    return percent.compareTo(BigDecimal.ZERO) > 0 && percent.compareTo(BigDecimal.valueOf(100)) < 0;
                })
                .limit(3)
                .forEach(item -> {
                    Course course = courseMapper.selectById(item.getCourseId());
                    if (course != null) {
                        cards.add(buildCard("COURSE", course.getId(), course.getTitle(), course.getCoverUrl(),
                                safeText(course.getTeacherName(), course.getSubjectName()),
                                "你最近正在学习这门课，继续完成更容易形成稳定节奏",
                                "/course/play/" + course.getId(), "继续学习"));
                    }
                });

        courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "PUBLISHED")
                        .eq(StringUtils.hasText(user.getProvince()), Course::getProvince, user.getProvince())
                        .eq(StringUtils.hasText(weakSubject), Course::getSubjectName, weakSubject)
                        .orderByDesc(Course::getLearnCount)
                        .last("limit 5"))
                .stream()
                .filter(item -> !ownedCourseIds.contains(item.getId()))
                .forEach(course -> cards.add(buildCard("COURSE", course.getId(), course.getTitle(), course.getCoverUrl(),
                        safeText(course.getProvince(), course.getSubjectName()),
                        "根据你的薄弱科目和地区偏好推荐补强课程",
                        "/course/detail/" + course.getId(), course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) <= 0 ? "免费学习" : "查看课程")));

        cards.add(buildCard("PRACTICE", 0L,
                weakSubject + " 专项刷题", null,
                safeText(user.getProvince(), weakSubject),
                "你在该科目的正确率相对更低，建议优先做专项练习巩固薄弱点",
                "/question-bank?subject=" + weakSubject, "开始练习"));

        examMapper.selectList(new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getStatus, "OPEN")
                        .eq(StringUtils.hasText(user.getProvince()), Exam::getProvince, user.getProvince())
                        .orderByDesc(Exam::getCreatedAt)
                        .last("limit 3"))
                .forEach(exam -> cards.add(buildCard("EXAM", exam.getId(), exam.getTitle(), null,
                        safeText(exam.getProvince(), exam.getSubjectName()),
                        exams.isEmpty() ? "你还没有完成模考，建议尽快做一套检验阶段学习效果" : "结合你的备考地区，推荐最近开放的模考",
                        "/exam/detail/" + exam.getId(), "查看模考")));

        newsMapper.selectList(new LambdaQueryWrapper<News>()
                        .eq(News::getStatus, "PUBLISHED")
                        .like(StringUtils.hasText(user.getProvince()), News::getTags, user.getProvince())
                        .orderByDesc(News::getPublishedAt)
                        .last("limit 4"))
                .forEach(news -> cards.add(buildCard("NEWS", news.getId(), news.getTitle(), news.getCoverUrl(),
                        news.getTags(),
                        wrongQuestions.isEmpty() ? "保持资讯输入，及时跟进考试动态和热点素材" : "结合你当前学习阶段，补充相关资讯素材",
                        "/news/detail/" + news.getId(), "阅读资讯")));

        List<Map<String, Object>> uniqueCards = cards.stream()
                .collect(Collectors.collectingAndThen(Collectors.toMap(
                        item -> buildCardKey((String) item.get("resourceType"), ((Number) item.get("id")).longValue()),
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ), map -> new ArrayList<>(map.values())));
        uniqueCards.removeIf(item -> feedbackKeys.contains(buildCardKey((String) item.get("resourceType"), ((Number) item.get("id")).longValue())));
        return applyRefreshWindow(uniqueCards, refreshCount, 6);
    }

    private Map<String, BigDecimal> buildSubjectAccuracy(List<PracticeRecord> practices) {
        Map<String, List<PracticeRecord>> grouped = practices.stream()
                .filter(item -> StringUtils.hasText(item.getSubjectName()))
                .collect(Collectors.groupingBy(PracticeRecord::getSubjectName));
        Map<String, BigDecimal> result = new HashMap<>();
        grouped.forEach((subject, records) -> {
            BigDecimal total = records.stream()
                    .map(item -> item.getAccuracyRate() == null ? BigDecimal.ZERO : item.getAccuracyRate())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put(subject, total.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP));
        });
        return result;
    }

    private Map<String, Object> buildCard(String type,
                                          Long id,
                                          String title,
                                          String coverUrl,
                                          String tags,
                                          String reason,
                                          String path,
                                          String actionText) {
        Map<String, Object> map = new HashMap<>();
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

    private Map<String, Object> buildTypeStat(String resourceType, List<RecommendFeedback> feedbacks) {
        Map<String, Object> item = new HashMap<>();
        item.put("resourceType", resourceType);
        item.put("count", feedbacks.stream().filter(feedback -> resourceType.equals(feedback.getResourceType())).count());
        return item;
    }

    private Map<String, Object> buildAdminFeedbackItem(RecommendFeedback feedback) {
        Map<String, Object> item = new HashMap<>();
        SysUser user = sysUserMapper.selectById(feedback.getUserId());
        item.put("id", feedback.getId());
        item.put("userId", feedback.getUserId());
        item.put("nickname", user == null ? "未知用户" : user.getNickname());
        item.put("resourceType", feedback.getResourceType());
        item.put("resourceId", feedback.getResourceId());
        item.put("resourceTitle", resolveResourceTitle(feedback.getResourceType(), feedback.getResourceId()));
        item.put("feedbackType", feedback.getFeedbackType());
        item.put("sourcePage", feedback.getSourcePage());
        item.put("reasonText", feedback.getReasonText());
        item.put("createdAt", feedback.getCreatedAt());
        return item;
    }

    private String resolveResourceTitle(String resourceType, Long resourceId) {
        if ("COURSE".equals(resourceType)) {
            Course course = courseMapper.selectById(resourceId);
            return course == null ? "课程已删除" : course.getTitle();
        }
        if ("EXAM".equals(resourceType)) {
            Exam exam = examMapper.selectById(resourceId);
            return exam == null ? "模考已删除" : exam.getTitle();
        }
        if ("NEWS".equals(resourceType)) {
            News news = newsMapper.selectById(resourceId);
            return news == null ? "资讯已删除" : news.getTitle();
        }
        if ("PRACTICE".equals(resourceType)) {
            return "专项刷题入口";
        }
        return "未知资源";
    }

    private List<Map<String, Object>> applyRefreshWindow(List<Map<String, Object>> cards, long refreshCount, int limit) {
        if (cards.isEmpty()) {
            return cards;
        }
        int size = cards.size();
        int offset = (int) (refreshCount % size);
        List<Map<String, Object>> rotated = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            rotated.add(cards.get((offset + index) % size));
        }
        if (limit > 0 && rotated.size() > limit) {
            return new ArrayList<>(rotated.subList(0, limit));
        }
        return rotated;
    }

    private List<Map<String, Object>> buildRecommendReasonStats(List<Map<String, Object>> cards) {
        Map<String, Long> grouped = cards.stream()
                .collect(Collectors.groupingBy(item -> String.valueOf(item.get("resourceType")), LinkedHashMap::new, Collectors.counting()));
        return grouped.entrySet().stream().map(entry -> {
            Map<String, Object> item = new HashMap<>();
            item.put("resourceType", entry.getKey());
            item.put("count", entry.getValue());
            return item;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildPreferenceSubjects(List<Map<String, Object>> cards, Map<String, Object> portrait) {
        String preferredSubject = portrait == null ? null : (String) portrait.get("preferredSubject");
        Map<String, Long> grouped = cards.stream()
                .map(item -> {
                    String tags = (String) item.get("tags");
                    if (!StringUtils.hasText(tags)) {
                        return safeText(preferredSubject, "综合能力");
                    }
                    return tags;
                })
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(item -> item, LinkedHashMap::new, Collectors.counting()));
        return grouped.entrySet().stream().limit(5).map(entry -> {
            Map<String, Object> item = new HashMap<>();
            item.put("label", entry.getKey());
            item.put("count", entry.getValue());
            return item;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildWeaknessFocus(List<Map<String, Object>> cards, Map<String, Object> portrait) {
        String weakSubject = portrait == null ? null : (String) portrait.get("weakSubject");
        List<Map<String, Object>> result = cards.stream()
                .filter(item -> String.valueOf(item.get("reason")).contains("薄弱")
                        || (StringUtils.hasText(weakSubject) && String.valueOf(item.get("title")).contains(weakSubject)))
                .limit(4)
                .map(item -> {
                    Map<String, Object> focus = new HashMap<>();
                    focus.put("title", item.get("title"));
                    focus.put("reason", item.get("reason"));
                    focus.put("path", item.get("path"));
                    return focus;
                }).collect(Collectors.toList());
        if (result.isEmpty() && StringUtils.hasText(weakSubject)) {
            Map<String, Object> focus = new HashMap<>();
            focus.put("title", weakSubject + " 补强建议");
            focus.put("reason", "当前画像识别出的薄弱科目是 " + weakSubject + "，建议优先进入搜题页和专项练习补齐短板。");
            focus.put("path", "/search?subjectName=" + weakSubject);
            result.add(focus);
        }
        return result;
    }

    private String buildCardKey(String resourceType, Long resourceId) {
        return resourceType + "-" + resourceId;
    }

    private long currentRefreshCount(Long userId) {
        LambdaQueryWrapper<RecommendRefreshLog> wrapper = new LambdaQueryWrapper<RecommendRefreshLog>()
                .eq(RecommendRefreshLog::getRefreshSource, REFRESH_SOURCE);
        if (userId == null) {
            wrapper.isNull(RecommendRefreshLog::getUserId);
        } else {
            wrapper.eq(RecommendRefreshLog::getUserId, userId);
        }
        return recommendRefreshLogMapper.selectCount(wrapper);
    }

    private String safeText(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
    }

    private Long requireCurrentUserId() {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        return userId;
    }

    private Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return ((LoginUser) authentication.getPrincipal()).getUserId();
    }
}
