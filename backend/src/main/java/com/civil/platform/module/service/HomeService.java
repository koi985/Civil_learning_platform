package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.entity.*;
import com.civil.platform.module.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HomeService {
    private static final long PUBLIC_HOME_CACHE_TTL_MILLIS = 60_000L;

    private final BannerMapper bannerMapper;
    private final NoticeMapper noticeMapper;
    private final CourseMapper courseMapper;
    private final NewsMapper newsMapper;
    private final ClassroomMapper classroomMapper;
    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final UserCourseMapper userCourseMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final ExamRecordMapper examRecordMapper;
    private final RecommendConfigMapper recommendConfigMapper;
    private final RecommendEngineService recommendEngineService;
    private final Map<String, CachedHomePayload> publicHomeCache = new java.util.concurrent.ConcurrentHashMap<>();

    public HomeService(BannerMapper bannerMapper,
                       NoticeMapper noticeMapper,
                       CourseMapper courseMapper,
                       NewsMapper newsMapper,
                       ClassroomMapper classroomMapper,
                       ExamMapper examMapper,
                       QuestionMapper questionMapper,
                       TeacherProfileMapper teacherProfileMapper,
                       SysUserMapper sysUserMapper,
                       UserCourseMapper userCourseMapper,
                       PracticeRecordMapper practiceRecordMapper,
                       ExamRecordMapper examRecordMapper,
                       RecommendConfigMapper recommendConfigMapper,
                       RecommendEngineService recommendEngineService) {
        this.bannerMapper = bannerMapper;
        this.noticeMapper = noticeMapper;
        this.courseMapper = courseMapper;
        this.newsMapper = newsMapper;
        this.classroomMapper = classroomMapper;
        this.examMapper = examMapper;
        this.questionMapper = questionMapper;
        this.teacherProfileMapper = teacherProfileMapper;
        this.sysUserMapper = sysUserMapper;
        this.userCourseMapper = userCourseMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.examRecordMapper = examRecordMapper;
        this.recommendConfigMapper = recommendConfigMapper;
        this.recommendEngineService = recommendEngineService;
    }

    public Map<String, Object> home() {
        return home(null);
    }

    public Map<String, Object> home(String explicitRegion) {
        Map<String, Object> result = new HashMap<>(publicOverview(explicitRegion));
        result.putAll(personalizedOverview(explicitRegion));
        return result;
    }

    public Map<String, Object> publicOverview(String explicitRegion) {
        String targetRegion = normalizeRegion(explicitRegion);
        boolean cacheable = SecurityUtils.getRoles().isEmpty();
        if (!cacheable) {
            return buildPublicOverview(targetRegion);
        }
        String cacheKey = StringUtils.hasText(targetRegion) ? targetRegion : "全国";
        long now = System.currentTimeMillis();
        CachedHomePayload cached = publicHomeCache.get(cacheKey);
        if (cached != null && now - cached.createdAtMillis < PUBLIC_HOME_CACHE_TTL_MILLIS) {
            return new HashMap<>(cached.payload);
        }
        Map<String, Object> result = buildPublicOverview(targetRegion);
        publicHomeCache.put(cacheKey, new CachedHomePayload(now, new HashMap<>(result)));
        return result;
    }

    public void clearPublicOverviewCache() {
        publicHomeCache.clear();
    }

    public Map<String, Object> personalizedOverview() {
        return personalizedOverview(null);
    }

    public Map<String, Object> personalizedOverview(String explicitRegion) {
        String targetRegion = normalizeRegion(explicitRegion);
        Map<String, Object> result = new HashMap<>();
        result.put("authCard", buildAuthCard());
        result.put("personalizedResources", buildPersonalizedResources());
        if (!SecurityUtils.getRoles().isEmpty()) {
            Map<String, Object> recommendOverview = recommendEngineService.overview(targetRegion);
            List<Map<String, Object>> recommendations = castCardList(recommendOverview.get("recommendCards")).stream()
                    .limit(6)
                    .map(item -> {
                        Map<String, Object> card = new HashMap<>(item);
                        card.put("targetPath", item.get("path"));
                        return card;
                    })
                    .collect(Collectors.toList());
            result.put("recommendCards", recommendations);
        }
        return result;
    }

    private Map<String, Object> buildPublicOverview(String targetRegion) {
        Map<String, Object> result = new HashMap<>();
        result.put("banners", bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getEnabled, true)
                .orderByAsc(Banner::getSortNo)));
        result.put("notices", noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, "PUBLISHED")
                .orderByDesc(Notice::getPublishedAt)
                .last("limit 5")));
        result.put("hotCourses", courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, "PUBLISHED")
                .orderByDesc(Course::getLearnCount)
                .last("limit 4")));
        result.put("latestNews", latestNewsByRegion(targetRegion));
        result.put("hotNews", newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, "PUBLISHED")
                .orderByDesc(News::getReadCount)
                .last("limit 4")));
        result.put("recommendedClassrooms", classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getStatus, "OPEN")
                .eq(Classroom::getIsPublic, true)
                .orderByDesc(Classroom::getMemberCount)
                .last("limit 4")));
        result.put("latestExams", examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getStatus, "OPEN")
                .orderByDesc(Exam::getCreatedAt)
                .last("limit 3")));
        result.put("questionTopics", buildQuestionTopics());
        result.put("teacherHighlights", buildTeacherHighlights());
        result.put("quickEntries", buildQuickEntries());
        result.put("authCard", buildGuestAuthCard());
        result.put("personalizedResources", buildGuestPersonalizedResources());
        result.put("footerSections", buildFooterSections());

        Map<String, Object> recommendOverview = recommendEngineService.overview(targetRegion);
        List<Map<String, Object>> recommendations = castCardList(recommendOverview.get("recommendCards")).stream()
                .limit(6)
                .map(item -> {
                    Map<String, Object> card = new HashMap<>(item);
                    card.put("targetPath", item.get("path"));
                    return card;
                })
                .collect(Collectors.toList());
        result.put("recommendCards", recommendations);
        result.put("recommendConfig", recommendConfigMapper.selectList(null).stream()
                .collect(Collectors.toMap(RecommendConfig::getConfigKey, RecommendConfig::getConfigValue)));
        return result;
    }

    private Map<String, Object> buildGuestAuthCard() {
        Map<String, Object> authCard = new HashMap<>();
        authCard.put("loggedIn", false);
        authCard.put("title", "登录后同步学习资源与成长进度");
        authCard.put("description", "注册即可解锁我的课程、刷题记录、模考成绩与个性化推荐。");
        authCard.put("primaryAction", "/login");
        authCard.put("primaryText", "立即登录");
        authCard.put("secondaryAction", "/register");
        authCard.put("secondaryText", "注册账号");
        return authCard;
    }

    private Map<String, Object> buildGuestPersonalizedResources() {
        Map<String, Object> result = new HashMap<>();
        result.put("loggedIn", false);
        result.put("summary", "登录后可查看我的课程、最近练习和模考记录。");
        result.put("resources", new ArrayList<>());
        return result;
    }

    private List<News> latestNewsByRegion(String targetRegion) {
        if (!StringUtils.hasText(targetRegion) || "全国".equals(targetRegion)) {
            return newsMapper.selectList(new LambdaQueryWrapper<News>()
                    .eq(News::getStatus, "PUBLISHED")
                    .orderByDesc(News::getPublishedAt)
                    .last("limit 4"));
        }
        List<News> regional = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, "PUBLISHED")
                .eq(News::getProvince, targetRegion)
                .orderByDesc(News::getPublishedAt)
                .last("limit 4"));
        if (regional.size() < 4) {
            Set<Long> regionalIds = regional.stream().map(News::getId).collect(Collectors.toSet());
            List<News> national = newsMapper.selectList(new LambdaQueryWrapper<News>()
                    .eq(News::getStatus, "PUBLISHED")
                    .eq(News::getProvince, "全国")
                    .notIn(!regionalIds.isEmpty(), News::getId, regionalIds)
                    .orderByDesc(News::getPublishedAt)
                    .last("limit " + (4 - regional.size())));
            regional.addAll(national);
        }
        if (regional.size() >= 4) {
            return regional;
        }
        Set<Long> existingIds = regional.stream().map(News::getId).collect(Collectors.toSet());
        List<News> fallback = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, "PUBLISHED")
                .notIn(!existingIds.isEmpty(), News::getId, existingIds)
                .orderByDesc(News::getPublishedAt)
                .last("limit " + (4 - regional.size())));
        regional.addAll(fallback);
        return regional;
    }

    private String normalizeRegion(String region) {
        return StringUtils.hasText(region) ? region.trim() : "";
    }

    private Map<String, Object> buildCard(String type, Long id, String title, String coverUrl, String tags, String reason, String targetPath, String actionText) {
        Map<String, Object> map = new HashMap<>();
        map.put("resourceType", type);
        map.put("id", id);
        map.put("title", title);
        map.put("coverUrl", coverUrl);
        map.put("tags", tags);
        map.put("reason", reason);
        map.put("targetPath", targetPath);
        map.put("actionText", actionText);
        return map;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castCardList(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }
        return ((List<?>) value).stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildQuickEntries() {
        List<Map<String, Object>> entries = new ArrayList<>();
        entries.add(buildEntry("搜题", "按关键词快速定位题目与解析", "/search", "QUESTION"));
        entries.add(buildEntry("题库练习", "进入题库筛选并开始刷题", "/question-bank", "QUESTION_BANK"));
        entries.add(buildEntry("独立评测", "20 题入学评测入口，校准当前通过率预测", "/user/assessment", "ASSESSMENT"));
        entries.add(buildEntry("模考专区", "查看最新模考与排行", "/exam", "EXAM"));
        entries.add(buildEntry("智能推荐", "课程、题库、模考混合推荐", "/recommend", "RECOMMEND"));
        entries.add(buildEntry("教师专区", "申请教师、管理班级与报表", "/teacher/apply", "TEACHER"));
        return entries;
    }

    private Map<String, Object> buildEntry(String title, String description, String path, String type) {
        Map<String, Object> item = new HashMap<>();
        item.put("title", title);
        item.put("description", description);
        item.put("path", path);
        item.put("type", type);
        return item;
    }

    private Map<String, Object> buildAuthCard() {
        boolean loggedIn = !SecurityUtils.getRoles().isEmpty();
        Map<String, Object> authCard = new HashMap<>();
        authCard.put("loggedIn", loggedIn);
        if (!loggedIn) {
            authCard.put("title", "登录后同步学习资源与成长进度");
            authCard.put("description", "注册即可解锁我的课程、刷题记录、模考成绩与个性化推荐。");
            authCard.put("primaryAction", "/login");
            authCard.put("primaryText", "立即登录");
            authCard.put("secondaryAction", "/register");
            authCard.put("secondaryText", "注册账号");
            return authCard;
        }
        SysUser user = sysUserMapper.selectById(SecurityUtils.getUserId());
        authCard.put("title", StringUtils.hasText(user.getNickname()) ? user.getNickname() + "，继续今天的备考任务" : "继续今天的备考任务");
        authCard.put("description", "已为你准备课程、练习和模考的连续学习入口。");
        authCard.put("primaryAction", "/my-course");
        authCard.put("primaryText", "进入我的课程");
        authCard.put("secondaryAction", "/user");
        authCard.put("secondaryText", "查看个人中心");
        authCard.put("user", user);
        return authCard;
    }

    private Map<String, Object> buildPersonalizedResources() {
        Map<String, Object> result = new HashMap<>();
        if (SecurityUtils.getRoles().isEmpty()) {
            result.put("loggedIn", false);
            result.put("summary", "登录后可查看我的课程、最近练习和模考记录。");
            result.put("resources", new ArrayList<>());
            return result;
        }
        Long userId = SecurityUtils.getUserId();
        result.put("loggedIn", true);
        result.put("summary", "优先展示你最近使用过的课程、练习和模考入口。");
        List<Map<String, Object>> resources = new ArrayList<>();

        List<UserCourse> userCourses = userCourseMapper.selectList(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId)
                .orderByDesc(UserCourse::getCreatedAt)
                .last("limit 3"));
        Map<Long, Course> courseMap = userCourses.isEmpty()
                ? Collections.emptyMap()
                : courseMapper.selectBatchIds(userCourses.stream().map(UserCourse::getCourseId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Course::getId, item -> item, (left, right) -> left));
        userCourses.forEach(item -> {
            Course course = courseMap.get(item.getCourseId());
            if (course != null) {
                resources.add(buildResource("COURSE", course.getTitle(), course.getSubjectName(), "/course/play/" + course.getId(), "继续学习"));
            }
        });

        practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .orderByDesc(PracticeRecord::getCreatedAt)
                        .last("limit 2"))
                .forEach(item -> resources.add(buildResource("PRACTICE", item.getTitle(), item.getSubjectName(), "/practice/report/" + item.getId(), "查看练习")));

        examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .orderByDesc(ExamRecord::getCreatedAt)
                        .last("limit 2"))
                .forEach(item -> resources.add(buildResource("EXAM", item.getTitle(), "最近模考成绩 " + item.getScore() + " 分", "/exam/report/" + item.getId(), "查看报告")));

        result.put("resources", resources.stream().limit(6).collect(Collectors.toList()));
        return result;
    }

    private Map<String, Object> buildResource(String type, String title, String subtitle, String path, String actionText) {
        Map<String, Object> item = new HashMap<>();
        item.put("type", type);
        item.put("title", title);
        item.put("subtitle", subtitle);
        item.put("path", path);
        item.put("actionText", actionText);
        return item;
    }

    private List<Map<String, Object>> buildQuestionTopics() {
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                        .eq(Question::getEnabled, true)
                        .orderByDesc(Question::getCreatedAt)
                        .last("limit 4"))
                .stream()
                .map(item -> {
                    Map<String, Object> topic = new HashMap<>();
                    topic.put("id", item.getId());
                    topic.put("title", trimTitle(item.getStem()));
                    topic.put("subjectName", item.getSubjectName());
                    topic.put("categoryName", item.getCategoryName());
                    topic.put("difficultyLevel", item.getDifficultyLevel());
                    topic.put("path", "/search");
                    return topic;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTeacherHighlights() {
        List<TeacherProfile> profiles = teacherProfileMapper.selectList(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getAuditStatus, "APPROVED")
                .orderByDesc(TeacherProfile::getAuditedAt)
                .last("limit 20"));
        List<Long> teacherUserIds = profiles.stream()
                .map(TeacherProfile::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Long> publicClassroomCountMap = teacherUserIds.isEmpty()
                ? Collections.emptyMap()
                : classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                .in(Classroom::getTeacherId, teacherUserIds)
                .eq(Classroom::getIsPublic, true)
                .eq(Classroom::getStatus, "OPEN"))
                .stream()
                .collect(Collectors.groupingBy(Classroom::getTeacherId, Collectors.counting()));
        profiles.sort(Comparator
                .comparingLong((TeacherProfile profile) -> publicClassroomCountMap.getOrDefault(profile.getUserId(), 0L))
                .reversed()
                .thenComparing(TeacherProfile::getAuditedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        profiles = profiles.stream().limit(4).collect(Collectors.toList());
        Map<Long, SysUser> userMap = profiles.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(profiles.stream().map(TeacherProfile::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(SysUser::getId, item -> item, (left, right) -> left));
        return profiles.stream().map(profile -> {
            SysUser user = userMap.get(profile.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", profile.getId());
            item.put("nickname", user == null ? "教师" : user.getNickname());
            item.put("avatarUrl", user == null ? null : user.getAvatarUrl());
            item.put("province", user == null ? null : user.getProvince());
            item.put("preferredSubject", user == null ? null : user.getPreferredSubject());
            item.put("intro", profile.getIntro());
            return item;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildFooterSections() {
        List<Map<String, Object>> sections = new ArrayList<>();
        sections.add(buildFooterSection("友情说明", Arrays.asList("课程、题库、模考内容均用于学习训练，不构成官方招录承诺。", "请结合最新公告与招考简章安排复习节奏。")));
        sections.add(buildFooterSection("平台说明", Arrays.asList("提供课程学习、题库练习、模考训练、班级教学与成长分析。", "支持 Web 端统一登录和多角色协同。")));
        sections.add(buildFooterSection("用户协议", Arrays.asList("购买课程后可在有效期内反复学习。", "请勿共享账号或传播受版权保护的课程资料。")));
        sections.add(buildFooterSection("隐私政策", Arrays.asList("仅在教学、推荐和服务保障范围内使用必要数据。", "敏感信息按权限隔离，教师与管理员按角色访问。")));
        sections.add(buildFooterSection("联系方式", Arrays.asList("客服邮箱：support@civil-platform.local", "工作时间：09:00-21:00")));
        return sections;
    }

    private Map<String, Object> buildFooterSection(String title, List<String> items) {
        Map<String, Object> section = new HashMap<>();
        section.put("title", title);
        section.put("items", items);
        return section;
    }

    private String trimTitle(String text) {
        if (!StringUtils.hasText(text)) {
            return "-";
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        return normalized.length() > 28 ? normalized.substring(0, 28) + "..." : normalized;
    }

    private String formatCourseAction(Course course) {
        return course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0 ? "免费学习" : "查看课程";
    }

    private static class CachedHomePayload {
        private final long createdAtMillis;
        private final Map<String, Object> payload;

        private CachedHomePayload(long createdAtMillis, Map<String, Object> payload) {
            this.createdAtMillis = createdAtMillis;
            this.payload = payload;
        }
    }
}
