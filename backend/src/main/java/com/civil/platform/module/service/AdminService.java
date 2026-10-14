package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.QuestionAnswerUtils;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.*;
import com.civil.platform.module.entity.*;
import com.civil.platform.module.mapper.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final List<String> PAPER_SECTIONS = Arrays.asList("常识判断", "言语理解与表达", "资料分析", "判断推理", "政治理论", "数量关系");
    private static final String SHENLUN_SUBJECT = "申论";
    private static final String SHENLUN_PAPER_SOURCE_PREFIX = "shenlun-group:";
    private static final int SHENLUN_GROUP_STATUS_DRAFT = 0;
    private static final int SHENLUN_GROUP_STATUS_PUBLISHED = 1;
    private static final int SHENLUN_GROUP_STATUS_ARCHIVED = 2;

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final TeacherService teacherService;
    private final CourseService courseService;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final ShenlunGroupMapper shenlunGroupMapper;
    private final ShenlunGroupMaterialMapper shenlunGroupMaterialMapper;
    private final QuestionPaperMapper questionPaperMapper;
    private final QuestionPaperQuestionMapper questionPaperQuestionMapper;
    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final NewsService newsService;
    private final NoticeMapper noticeMapper;
    private final BannerMapper bannerMapper;
    private final RecommendConfigMapper recommendConfigMapper;
    private final RecommendPinnedResourceMapper recommendPinnedResourceMapper;
    private final CourseMapper courseMapper;
    private final ClassroomMapper classroomMapper;
    private final ClassroomMemberMapper classroomMemberMapper;
    private final NewsMapper newsMapper;
    private final CourseOrderMapper courseOrderMapper;
    private final CourseOrderItemMapper courseOrderItemMapper;
    private final OrderRefundService orderRefundService;
    private final PaymentLogMapper paymentLogMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final UserStudyPlanMapper userStudyPlanMapper;
    private final UserDailyTaskMapper userDailyTaskMapper;
    private final UserKnowledgeMasteryMapper userKnowledgeMasteryMapper;
    private final UserGrowthAchievementMapper userGrowthAchievementMapper;
    private final UserGrowthDiagnosisReportMapper userGrowthDiagnosisReportMapper;
    private final AdminGrowthFocusFollowMapper adminGrowthFocusFollowMapper;
    private final AdminGrowthFocusFollowHistoryMapper adminGrowthFocusFollowHistoryMapper;
    private final AdminGrowthDashboardPreferenceMapper adminGrowthDashboardPreferenceMapper;
    private final TeacherGrowthFocusFollowMapper teacherGrowthFocusFollowMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final SysOperationLogMapper sysOperationLogMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final RecommendEngineService recommendEngineService;
    private final HomeService homeService;
    private final UserCourseMapper userCourseMapper;
    private final CourseLearnCountService courseLearnCountService;

    public AdminService(SysUserMapper sysUserMapper,
                        SysRoleMapper sysRoleMapper,
                        SysUserRoleMapper sysUserRoleMapper,
                        TeacherService teacherService,
                        CourseService courseService,
                        QuestionMapper questionMapper,
                        QuestionOptionMapper questionOptionMapper,
                        ShenlunGroupMapper shenlunGroupMapper,
                        ShenlunGroupMaterialMapper shenlunGroupMaterialMapper,
                        QuestionPaperMapper questionPaperMapper,
                        QuestionPaperQuestionMapper questionPaperQuestionMapper,
                        ExamMapper examMapper,
                        ExamQuestionMapper examQuestionMapper,
                        NewsService newsService,
                        NoticeMapper noticeMapper,
                        BannerMapper bannerMapper,
                        RecommendConfigMapper recommendConfigMapper,
                        RecommendPinnedResourceMapper recommendPinnedResourceMapper,
                        CourseMapper courseMapper,
                        ClassroomMapper classroomMapper,
                        ClassroomMemberMapper classroomMemberMapper,
                        NewsMapper newsMapper,
                        CourseOrderMapper courseOrderMapper,
                        CourseOrderItemMapper courseOrderItemMapper,
                        OrderRefundService orderRefundService,
                        PaymentLogMapper paymentLogMapper,
                        PracticeRecordMapper practiceRecordMapper,
                        UserStudyPlanMapper userStudyPlanMapper,
                        UserDailyTaskMapper userDailyTaskMapper,
                        UserKnowledgeMasteryMapper userKnowledgeMasteryMapper,
                        UserGrowthAchievementMapper userGrowthAchievementMapper,
                        UserGrowthDiagnosisReportMapper userGrowthDiagnosisReportMapper,
                        AdminGrowthFocusFollowMapper adminGrowthFocusFollowMapper,
                        AdminGrowthFocusFollowHistoryMapper adminGrowthFocusFollowHistoryMapper,
                        AdminGrowthDashboardPreferenceMapper adminGrowthDashboardPreferenceMapper,
                        TeacherGrowthFocusFollowMapper teacherGrowthFocusFollowMapper,
                        TeacherProfileMapper teacherProfileMapper,
                        SysOperationLogMapper sysOperationLogMapper,
                        SystemConfigMapper systemConfigMapper,
                        RecommendEngineService recommendEngineService,
                        HomeService homeService,
                        UserCourseMapper userCourseMapper,
                        CourseLearnCountService courseLearnCountService) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.teacherService = teacherService;
        this.courseService = courseService;
        this.questionMapper = questionMapper;
        this.questionOptionMapper = questionOptionMapper;
        this.shenlunGroupMapper = shenlunGroupMapper;
        this.shenlunGroupMaterialMapper = shenlunGroupMaterialMapper;
        this.questionPaperMapper = questionPaperMapper;
        this.questionPaperQuestionMapper = questionPaperQuestionMapper;
        this.examMapper = examMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.newsService = newsService;
        this.noticeMapper = noticeMapper;
        this.bannerMapper = bannerMapper;
        this.recommendConfigMapper = recommendConfigMapper;
        this.recommendPinnedResourceMapper = recommendPinnedResourceMapper;
        this.courseMapper = courseMapper;
        this.classroomMapper = classroomMapper;
        this.classroomMemberMapper = classroomMemberMapper;
        this.newsMapper = newsMapper;
        this.courseOrderMapper = courseOrderMapper;
        this.courseOrderItemMapper = courseOrderItemMapper;
        this.orderRefundService = orderRefundService;
        this.paymentLogMapper = paymentLogMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.userStudyPlanMapper = userStudyPlanMapper;
        this.userDailyTaskMapper = userDailyTaskMapper;
        this.userKnowledgeMasteryMapper = userKnowledgeMasteryMapper;
        this.userGrowthAchievementMapper = userGrowthAchievementMapper;
        this.userGrowthDiagnosisReportMapper = userGrowthDiagnosisReportMapper;
        this.adminGrowthFocusFollowMapper = adminGrowthFocusFollowMapper;
        this.adminGrowthFocusFollowHistoryMapper = adminGrowthFocusFollowHistoryMapper;
        this.adminGrowthDashboardPreferenceMapper = adminGrowthDashboardPreferenceMapper;
        this.teacherGrowthFocusFollowMapper = teacherGrowthFocusFollowMapper;
        this.teacherProfileMapper = teacherProfileMapper;
        this.sysOperationLogMapper = sysOperationLogMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.recommendEngineService = recommendEngineService;
        this.homeService = homeService;
        this.userCourseMapper = userCourseMapper;
        this.courseLearnCountService = courseLearnCountService;
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> result = new HashMap<>();
        result.put("todayOrders", courseOrderMapper.selectCount(new LambdaQueryWrapper<CourseOrder>()
                .ge(CourseOrder::getCreatedAt, LocalDateTime.now().toLocalDate().atStartOfDay())));
        result.put("newUsers", sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .ge(SysUser::getCreatedAt, LocalDateTime.now().toLocalDate().atStartOfDay())));
        result.put("newTeacherApplies", teacherService.auditPage(1L, 1000L, "PENDING").getTotal());
        result.put("classroomCount", classroomMapper.selectCount(null));
        result.put("courseCount", courseMapper.selectCount(null));
        result.put("practiceQuestionCount", questionMapper.selectCount(null));
        List<Map<String, Object>> trends = new ArrayList<>();
        trends.add(buildTrend("课程", courseMapper.selectCount(null)));
        trends.add(buildTrend("题库", questionMapper.selectCount(null)));
        trends.add(buildTrend("资讯", newsMapper.selectCount(null)));
        trends.add(buildTrend("班级", classroomMapper.selectCount(null)));
        result.put("trends", trends);
        return result;
    }

    public Map<String, Object> growthDashboard(String segment,
                                               BigDecimal minCompletionRate,
                                               String followStatus) {
        Long operatorId = SecurityUtils.getUserId();
        AdminGrowthDashboardPreference preference = loadAdminGrowthDashboardPreference(operatorId);
        String resolvedSegment = StringUtils.hasText(segment) ? segment : (preference == null ? null : preference.getSegment());
        BigDecimal resolvedMinCompletionRate = minCompletionRate != null
                ? minCompletionRate
                : (preference == null || preference.getMinCompletionRate() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : preference.getMinCompletionRate());
        String resolvedFollowStatus = StringUtils.hasText(followStatus) ? followStatus : (preference == null ? null : preference.getFollowStatus());
        Map<String, Object> result = new HashMap<>();
        List<UserStudyPlan> allPlans = userStudyPlanMapper.selectList(new LambdaQueryWrapper<UserStudyPlan>()
                .orderByDesc(UserStudyPlan::getId));
        List<UserDailyTask> allTasks = userDailyTaskMapper.selectList(null);
        List<UserGrowthAchievement> achievements = userGrowthAchievementMapper.selectList(null);
        List<UserKnowledgeMastery> masteryList = userKnowledgeMasteryMapper.selectList(null);
        List<UserGrowthDiagnosisReport> diagnosisReports = userGrowthDiagnosisReportMapper.selectList(new LambdaQueryWrapper<UserGrowthDiagnosisReport>()
                .orderByDesc(UserGrowthDiagnosisReport::getDiagnosisDate)
                .orderByDesc(UserGrowthDiagnosisReport::getId));
        List<AdminGrowthFocusFollow> focusFollows = adminGrowthFocusFollowMapper.selectList(new LambdaQueryWrapper<AdminGrowthFocusFollow>()
                .orderByDesc(AdminGrowthFocusFollow::getUpdatedAt)
                .orderByDesc(AdminGrowthFocusFollow::getId));
        List<PracticeRecord> completedTestPapers = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getPracticeType, "TEST_PAPER")
                .eq(PracticeRecord::getStatus, "SUBMITTED")
                .orderByDesc(PracticeRecord::getSubmittedAt));

        result.put("overview", buildGrowthOverview(allPlans, allTasks, achievements, completedTestPapers));
        result.put("subjectPlanStats", buildSubjectPlanStats(allPlans));
        result.put("taskTypeStats", buildGrowthTaskTypeStats(allTasks, achievements));
        result.put("masteryLevelStats", buildMasteryLevelStats(masteryList));
        result.put("testPaperTrend", buildTestPaperTrend(completedTestPapers));
        result.put("weakKnowledgeTop", buildWeakKnowledgeTop(masteryList));
        result.put("archivedPlans", buildArchivedPlans(allPlans));
        result.put("segmentStats", buildSegmentStats(allPlans, diagnosisReports));
        result.put("focusUsers", buildFocusUsers(allPlans, diagnosisReports, focusFollows, resolvedMinCompletionRate, resolvedSegment, resolvedFollowStatus));
        result.put("classroomGrowthStats", buildClassroomGrowthStats(allPlans, diagnosisReports));
        result.put("teacherCollaborationStats", buildTeacherCollaborationStats(allPlans, diagnosisReports));
        result.put("followHistory", buildAdminFollowHistory());
        result.put("reminderSummary", buildAdminReminderSummary(focusFollows));
        result.put("followReminders", buildAdminFollowReminders(focusFollows, allPlans, diagnosisReports));
        result.put("filters", buildGrowthFilterState(resolvedSegment, resolvedMinCompletionRate, resolvedFollowStatus));
        return result;
    }

    public byte[] exportGrowthDashboard(String segment,
                                        BigDecimal minCompletionRate,
                                        String followStatus) {
        Map<String, Object> dashboard = growthDashboard(segment, minCompletionRate, followStatus);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> focusUsers = (List<Map<String, Object>>) dashboard.get("focusUsers");
        List<String> headers = Arrays.asList("用户名", "昵称", "当前计划", "科目", "成长分层", "完成率", "成长活力", "知识掌握度", "测卷平均准确率", "跟进状态", "优先级", "薄弱知识点", "跟进备注", "下次回访时间", "提醒状态", "最近诊断日期");
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> item : focusUsers) {
            List<Object> row = new ArrayList<>();
            row.add(item.get("username"));
            row.add(item.get("nickname"));
            row.add(item.get("planName"));
            row.add(item.get("subjectName"));
            row.add(item.get("segmentLabel"));
            row.add(item.get("completionRate"));
            row.add(item.get("vitalityScore"));
            row.add(item.get("knowledgeMasteryRate"));
            row.add(item.get("testPaperAverageScore"));
            row.add(item.get("followStatusLabel"));
            row.add(item.get("priorityLevelLabel"));
            @SuppressWarnings("unchecked")
            List<String> weakPoints = (List<String>) item.get("weakKnowledgePoints");
            row.add(CollectionUtils.isEmpty(weakPoints) ? "" : String.join(" / ", weakPoints));
            row.add(item.get("followRemark"));
            row.add(item.get("nextFollowUpAt"));
            row.add(item.get("reminderStatusLabel"));
            row.add(item.get("diagnosisDate"));
            rows.add(row);
        }
        return com.civil.platform.common.util.ExcelUtils.buildWorkbook("成长重点人群", headers, rows);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveGrowthFocusFollow(AdminGrowthFocusFollowRequest request) {
        Long operatorId = SecurityUtils.getUserId();
        saveGrowthFocusFollowInternal(request.getUserId(), request.getPlanId(), request.getLatestDiagnosisId(),
                request.getFollowStatus(), request.getPriorityLevel(), request.getRemark(), request.getNextFollowUpAt(), operatorId, "SAVE");
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchSaveGrowthFocusFollow(AdminGrowthBatchFollowRequest request) {
        Long operatorId = SecurityUtils.getUserId();
        for (AdminGrowthBatchFollowRequest.Item item : request.getItems()) {
            saveGrowthFocusFollowInternal(item.getUserId(), item.getPlanId(), item.getLatestDiagnosisId(),
                    request.getFollowStatus(), request.getPriorityLevel(), request.getRemark(), request.getNextFollowUpAt(), operatorId, "BATCH");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveGrowthDashboardPreference(AdminGrowthDashboardPreferenceRequest request) {
        Long userId = SecurityUtils.getUserId();
        AdminGrowthDashboardPreference preference = loadAdminGrowthDashboardPreference(userId);
        if (preference == null) {
            preference = new AdminGrowthDashboardPreference();
            preference.setUserId(userId);
            preference.setMinCompletionRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            adminGrowthDashboardPreferenceMapper.insert(preference);
        }
        preference.setSegment(request.getSegment());
        preference.setMinCompletionRate(request.getMinCompletionRate() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : request.getMinCompletionRate().setScale(2, RoundingMode.HALF_UP));
        preference.setFollowStatus(request.getFollowStatus());
        adminGrowthDashboardPreferenceMapper.updateById(preference);
    }

    public PageResult<Map<String, Object>> pageUsers(Long pageNo, Long pageSize, String keyword) {
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword))
                .orderByDesc(SysUser::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(user -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("nickname", user.getNickname());
            item.put("province", user.getProvince());
            item.put("examType", user.getExamType());
            item.put("preferredSubject", user.getPreferredSubject());
            item.put("enabled", user.getEnabled());
            item.put("locked", user.getLocked());
            item.put("vipExpireAt", user.getVipExpireAt());
            item.put("roles", sysUserMapper.selectRoleCodesByUserId(user.getId()));
            return item;
        }).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateRequest request) {
        SysUser user = sysUserMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setNickname(request.getNickname());
        user.setProvince(request.getProvince());
        user.setExamType(request.getExamType());
        user.setPreferredSubject(request.getPreferredSubject());
        user.setEnabled(request.getEnabled());
        user.setLocked(request.getLocked());
        user.setVipExpireAt(request.getVipExpireAt());
        sysUserMapper.updateById(user);

        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        List<String> roleCodes = CollectionUtils.isEmpty(request.getRoleCodes())
                ? Collections.singletonList(request.getRoleCode())
                : request.getRoleCodes();
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getCode, roleCodes));
        if (roles.isEmpty()) {
            throw new BizException("角色不存在");
        }
        for (SysRole role : roles) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            sysUserRoleMapper.insert(userRole);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeUser(Long id) {
        if (id == null) {
            throw new BizException("用户不存在");
        }
        Long currentUserId = SecurityUtils.getUserId();
        if (id.equals(currentUserId)) {
            throw new BizException("不能删除当前登录账号");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        sysUserMapper.deleteById(id);
    }

    public PageResult<Course> pageCourses(Long pageNo, Long pageSize, String keyword) {
        return courseService.adminPage(pageNo, pageSize, keyword);
    }

    public PageResult<Map<String, Object>> pageTeachers(Long pageNo, Long pageSize, String keyword, String auditStatus) {
        String effectiveAuditStatus = StringUtils.hasText(auditStatus) ? auditStatus : "APPROVED";
        Page<TeacherProfile> page = teacherProfileMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getAuditStatus, effectiveAuditStatus)
                .orderByDesc(TeacherProfile::getUpdatedAt));
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(profile -> {
                    SysUser user = sysUserMapper.selectById(profile.getUserId());
                    boolean keywordMatched = !StringUtils.hasText(keyword)
                            || (user != null && (String.valueOf(user.getUsername()).contains(keyword)
                            || String.valueOf(user.getNickname()).contains(keyword)
                            || String.valueOf(user.getRealName()).contains(keyword)));
                    if (!keywordMatched) {
                        return null;
                    }
                    Map<String, Object> item = new HashMap<>();
                    item.put("teacherProfileId", profile.getId());
                    item.put("userId", profile.getUserId());
                    item.put("username", user == null ? null : user.getUsername());
                    item.put("nickname", user == null ? null : user.getNickname());
                    item.put("realName", user == null ? null : user.getRealName());
                    item.put("email", user == null ? profile.getEmail() : user.getEmail());
                    item.put("phone", user == null ? null : user.getPhone());
                    item.put("enabled", user != null && Boolean.TRUE.equals(user.getEnabled()));
                    item.put("locked", user != null && Boolean.TRUE.equals(user.getLocked()));
                    item.put("auditStatus", profile.getAuditStatus());
                    item.put("auditRemark", profile.getAuditRemark());
                    item.put("intro", profile.getIntro());
                    item.put("certificateFileId", profile.getCertificateFileId());
                    item.put("classroomCount", classroomMapper.selectCount(new LambdaQueryWrapper<Classroom>()
                            .eq(Classroom::getTeacherId, profile.getUserId())));
                    item.put("updatedAt", profile.getUpdatedAt());
                    return item;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTeacher(AdminTeacherUpdateRequest request) {
        TeacherProfile profile = teacherProfileMapper.selectById(request.getTeacherProfileId());
        if (profile == null) {
            throw new BizException("教师资料不存在");
        }
        SysUser user = sysUserMapper.selectById(profile.getUserId());
        if (user == null) {
            throw new BizException("教师账号不存在");
        }
        user.setNickname(request.getNickname());
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setEnabled(request.getEnabled());
        user.setLocked(request.getLocked());
        sysUserMapper.updateById(user);

        profile.setEmail(request.getEmail());
        profile.setIntro(request.getIntro());
        profile.setAuditStatus(request.getAuditStatus());
        profile.setAuditRemark(request.getAuditRemark());
        teacherProfileMapper.updateById(profile);
        syncTeacherRoleByAuditStatus(profile.getUserId(), request.getAuditStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeTeacher(Long id) {
        if (id == null) {
            throw new BizException("教师资料不存在");
        }
        TeacherProfile profile = teacherProfileMapper.selectById(id);
        if (profile == null) {
            throw new BizException("教师资料不存在");
        }
        Long classroomCount = classroomMapper.selectCount(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getTeacherId, profile.getUserId()));
        if (classroomCount > 0) {
            throw new BizException("该教师已关联班级，请先处理班级后再删除");
        }
        teacherProfileMapper.deleteById(id);
        syncTeacherRoleByAuditStatus(profile.getUserId(), "REJECTED");
    }

    public Map<String, Object> courseDetail(Long courseId) {
        return courseService.adminDetail(courseId);
    }

    public void syncCourseLearnCounts() {
        courseLearnCountService.syncAllCourses();
    }

    public void saveCourse(CourseSaveRequest request) {
        courseService.save(request);
        refreshCoursePublicationCaches();
    }

    public void removeCourse(Long id) {
        courseService.remove(id);
        refreshCoursePublicationCaches();
    }

    private void refreshCoursePublicationCaches() {
        homeService.clearPublicOverviewCache();
        try {
            recommendEngineService.refreshHotPool();
        } catch (Exception ignored) {
        }
    }

    public PageResult<Map<String, Object>> pageQuestions(Long pageNo, Long pageSize, String keyword) {
        Page<Question> page = questionMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<Question>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Question::getStem, keyword)
                        .or()
                        .like(Question::getMaterial, keyword))
                .orderByDesc(Question::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(question -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", question.getId());
            item.put("subjectName", question.getSubjectName());
            item.put("categoryName", question.getCategoryName());
            item.put("province", question.getProvince());
            item.put("questionType", question.getQuestionType());
            item.put("difficultyLevel", question.getDifficultyLevel());
            item.put("sourceName", question.getSourceName());
            item.put("material", question.getMaterial());
            item.put("stem", question.getStem());
            item.put("analysis", question.getAnalysis());
            item.put("answerValue", question.getAnswerValue());
            item.put("knowledgePoints", question.getKnowledgePoints());
            item.put("enabled", question.getEnabled());
            item.put("options", questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, question.getId())
                    .orderByAsc(QuestionOption::getSortNo)));
            return item;
        }).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    public List<Map<String, Object>> randomQuestions(Integer count,
                                                     String province,
                                                     String subjectName,
                                                     String questionType,
                                                     String difficultyLevel) {
        int resolvedCount = count == null ? 120 : Math.min(Math.max(count, 1), 120);
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true)
                .eq(StringUtils.hasText(province), Question::getProvince, province)
                .eq(StringUtils.hasText(subjectName), Question::getSubjectName, subjectName)
                .eq(StringUtils.hasText(questionType), Question::getQuestionType, questionType)
                .eq(StringUtils.hasText(difficultyLevel), Question::getDifficultyLevel, difficultyLevel)
                .last("ORDER BY RAND() LIMIT " + resolvedCount));
        if (questions.size() < resolvedCount) {
            throw new BizException("符合条件的题目不足 " + resolvedCount + " 道，请放宽筛选条件或补充题库");
        }
        return questions.stream().map(this::buildQuestionSummary).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveQuestion(QuestionSaveRequest request) {
        Question question = new Question();
        BeanUtils.copyProperties(request, question);
        question.setQuestionType(StringUtils.hasText(question.getQuestionType()) ? question.getQuestionType().trim().toUpperCase() : null);
        validateQuestionRequest(request, question);
        if (question.getEnabled() == null) {
            question.setEnabled(Boolean.TRUE);
        }
        if (question.getId() == null) {
            questionMapper.insert(question);
        } else {
            questionMapper.updateById(question);
            questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, question.getId()));
        }
        if (!CollectionUtils.isEmpty(request.getOptions())) {
            int index = 1;
            for (QuestionSaveRequest.OptionItem optionItem : request.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setQuestionId(question.getId());
                option.setOptionKey(optionItem.getOptionKey());
                option.setOptionContent(optionItem.getOptionContent());
                option.setSortNo(index++);
                questionOptionMapper.insert(option);
            }
        }
    }

    public void removeQuestion(Long id) {
        questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, id));
        questionMapper.deleteById(id);
    }

    private void validateQuestionRequest(QuestionSaveRequest request, Question question) {
        List<QuestionSaveRequest.OptionItem> options = request.getOptions() == null ? Collections.emptyList() : request.getOptions();
        List<String> optionKeys = new ArrayList<>();
        Set<String> uniqueKeys = new LinkedHashSet<>();
        int nonBlankOptionCount = 0;
        for (QuestionSaveRequest.OptionItem option : options) {
            if (option == null || !StringUtils.hasText(option.getOptionKey()) || !StringUtils.hasText(option.getOptionContent())) {
                continue;
            }
            nonBlankOptionCount++;
            String optionKey = option.getOptionKey().trim().toUpperCase();
            option.setOptionKey(optionKey);
            option.setOptionContent(option.getOptionContent().trim());
            if (!uniqueKeys.add(optionKey)) {
                throw new BizException("选项标识不能重复");
            }
            optionKeys.add(optionKey);
        }
        if (QuestionAnswerUtils.isSingleChoice(question.getQuestionType()) || QuestionAnswerUtils.isMultiChoice(question.getQuestionType())) {
            if (nonBlankOptionCount < 2) {
                throw new BizException("客观题至少需要两个有效选项");
            }
            List<String> answers = QuestionAnswerUtils.parseAnswerValues(request.getAnswerValue());
            if (answers.isEmpty()) {
                throw new BizException("请设置正确答案");
            }
            if (QuestionAnswerUtils.isSingleChoice(question.getQuestionType()) && answers.size() != 1) {
                throw new BizException("单选题只能设置一个正确答案");
            }
            if (QuestionAnswerUtils.isMultiChoice(question.getQuestionType()) && answers.size() < 2) {
                throw new BizException("多选题至少需要两个正确答案");
            }
            for (String answer : answers) {
                if (!uniqueKeys.contains(answer)) {
                    throw new BizException("正确答案必须来自已配置选项");
                }
            }
            question.setAnswerValue(String.join(",", answers));
        } else {
            question.setAnswerValue(QuestionAnswerUtils.normalizeAnswer(request.getAnswerValue()));
        }
        request.setOptions(options.stream()
                .filter(option -> option != null && StringUtils.hasText(option.getOptionKey()) && StringUtils.hasText(option.getOptionContent()))
                .collect(Collectors.toList()));
    }

    public PageResult<QuestionPaper> pageQuestionPapers(Long pageNo, Long pageSize, String keyword) {
        Page<QuestionPaper> page = questionPaperMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<QuestionPaper>()
                .like(StringUtils.hasText(keyword), QuestionPaper::getTitle, keyword)
                .and(wrapper -> wrapper.notLikeRight(QuestionPaper::getSource, SHENLUN_PAPER_SOURCE_PREFIX)
                        .or()
                        .ne(QuestionPaper::getStatus, SHENLUN_GROUP_STATUS_ARCHIVED))
                .orderByDesc(QuestionPaper::getCreateTime)
                .orderByDesc(QuestionPaper::getId));
        return new PageResult<>(page.getTotal(), pageNo, pageSize, page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveQuestionPaper(QuestionPaperSaveRequest request) {
        QuestionPaper paper = new QuestionPaper();
        paper.setId(request.getId());
        paper.setPaperNo(request.getId() == null ? "PAPER-" + System.currentTimeMillis() : null);
        paper.setTitle(request.getTitle());
        paper.setPaperType("QUESTION_BANK");
        paper.setExamType("公务员");
        paper.setSubject(StringUtils.hasText(request.getSubjectName()) ? request.getSubjectName() : "行测");
        paper.setRegion(StringUtils.hasText(request.getProvince()) ? request.getProvince() : "全国");
        paper.setYear(LocalDateTime.now().getYear());
        paper.setSource("平台组卷");
        paper.setDescription("行测六个专项各20题，每题1分，总分120分。");
        paper.setDuration(request.getDurationMinutes() == null ? 120 : request.getDurationMinutes());
        paper.setTotalScore(request.getTotalScore() == null ? 120 : request.getTotalScore());
        paper.setQuestionCount(120);
        paper.setDifficulty(2);
        paper.setStatus("PUBLISHED".equals(request.getStatus()) ? 1 : 0);

        if (paper.getId() == null) {
            questionPaperMapper.insert(paper);
        } else {
            questionPaperMapper.updateById(paper);
            questionPaperQuestionMapper.delete(new LambdaQueryWrapper<QuestionPaperQuestion>().eq(QuestionPaperQuestion::getPaperId, paper.getId()));
        }
        attachDefaultPaperQuestions(paper.getId());
    }

    public void removeQuestionPaper(Long id) {
        QuestionPaper paper = questionPaperMapper.selectById(id);
        if (paper != null && isGeneratedShenlunPaper(paper)) {
            throw new BizException("申论材料组自动生成的试卷请在材料组管理中删除");
        }
        questionPaperQuestionMapper.delete(new LambdaQueryWrapper<QuestionPaperQuestion>().eq(QuestionPaperQuestion::getPaperId, id));
        questionPaperMapper.deleteById(id);
    }

    public PageResult<Map<String, Object>> pageAdminShenlunGroups(Long pageNo,
                                                                  Long pageSize,
                                                                  String keyword,
                                                                  Integer year,
                                                                  String region,
                                                                  String sourceName,
                                                                  String status) {
        Page<ShenlunGroup> page = shenlunGroupMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<ShenlunGroup>()
                .eq(ShenlunGroup::getSubjectName, SHENLUN_SUBJECT)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(ShenlunGroup::getTitle, keyword)
                        .or()
                        .like(ShenlunGroup::getSourceName, keyword))
                .eq(year != null, ShenlunGroup::getYear, year)
                .eq(StringUtils.hasText(region), ShenlunGroup::getRegion, region)
                .eq(StringUtils.hasText(sourceName), ShenlunGroup::getSourceName, sourceName)
                .eq(StringUtils.hasText(status), ShenlunGroup::getStatus, resolveShenlunGroupStatus(status))
                .in(!StringUtils.hasText(status), ShenlunGroup::getStatus,
                        Arrays.asList(SHENLUN_GROUP_STATUS_DRAFT, SHENLUN_GROUP_STATUS_PUBLISHED))
                .orderByDesc(ShenlunGroup::getYear)
                .orderByDesc(ShenlunGroup::getId));
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(this::toAdminShenlunGroupSummary)
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    public Map<String, Object> adminShenlunGroupDetail(Long id) {
        ShenlunGroup group = requireShenlunGroup(id);
        Map<String, Object> result = new HashMap<>();
        result.put("group", toAdminShenlunGroupDetail(group, loadAdminShenlunGroupMaterials(id), loadAdminShenlunGroupQuestions(id)));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveShenlunGroup(AdminShenlunGroupSaveRequest request) {
        validateShenlunGroupRequest(request);
        ShenlunGroup group = request.getId() == null ? new ShenlunGroup() : requireShenlunGroup(request.getId());
        fillShenlunGroup(group, request);
        if (group.getId() == null) {
            shenlunGroupMapper.insert(group);
        } else {
            shenlunGroupMapper.updateById(group);
        }

        List<ShenlunGroupMaterial> oldMaterials = loadAdminShenlunGroupMaterials(group.getId());
        Map<Long, ShenlunGroupMaterial> oldMaterialMap = oldMaterials.stream()
                .collect(Collectors.toMap(ShenlunGroupMaterial::getId, Function.identity(), (left, right) -> left));
        Map<Integer, ShenlunGroupMaterial> materialMap = saveShenlunGroupMaterials(group.getId(), request.getMaterials(), oldMaterialMap);
        removeUnusedGroupMaterials(group.getId(), request.getMaterials(), oldMaterials);

        List<Question> oldQuestions = loadAdminShenlunGroupQuestions(group.getId());
        Map<Long, Question> oldQuestionMap = oldQuestions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (left, right) -> left));
        saveShenlunGroupQuestions(group, request.getQuestions(), oldQuestionMap, materialMap);
        removeUnusedGroupQuestions(group.getId(), request.getQuestions(), oldQuestions);

        updateShenlunGroupStats(group.getId());
        if (shouldSyncGeneratedShenlunPaper(group) || findGeneratedShenlunPaper(group.getId()) != null) {
            syncGeneratedShenlunPaper(group.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishShenlunGroup(Long id) {
        ShenlunGroup group = requireShenlunGroup(id);
        if (isArchivedShenlunGroup(group)) {
            throw new BizException("已归档的材料组请先恢复后再发布");
        }
        if (loadAdminShenlunGroupMaterials(id).isEmpty()) {
            throw new BizException("请先配置材料后再发布");
        }
        if (loadAdminShenlunGroupQuestions(id).isEmpty()) {
            throw new BizException("请先配置题目后再发布");
        }
        group.setStatus(SHENLUN_GROUP_STATUS_PUBLISHED);
        shenlunGroupMapper.updateById(group);
        updateShenlunGroupStats(id);
        syncGeneratedShenlunPaper(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeShenlunGroup(Long id) {
        ShenlunGroup group = requireShenlunGroup(id);
        if (isArchivedShenlunGroup(group)) {
            return;
        }
        group.setStatus(SHENLUN_GROUP_STATUS_ARCHIVED);
        shenlunGroupMapper.updateById(group);
        QuestionPaper generatedPaper = findGeneratedShenlunPaper(id);
        if (generatedPaper != null) {
            generatedPaper.setStatus(SHENLUN_GROUP_STATUS_ARCHIVED);
            questionPaperMapper.updateById(generatedPaper);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void restoreShenlunGroup(Long id, String status) {
        ShenlunGroup group = requireShenlunGroup(id);
        if (!isArchivedShenlunGroup(group)) {
            throw new BizException("仅已归档的材料组支持恢复");
        }
        group.setStatus(resolveRestoredShenlunGroupStatus(status));
        shenlunGroupMapper.updateById(group);
        updateShenlunGroupStats(id);
        syncGeneratedShenlunPaper(id);
    }

    private void attachDefaultPaperQuestions(Long paperId) {
        int sortNo = 1;
        for (String section : PAPER_SECTIONS) {
            List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                    .eq(Question::getEnabled, true)
                    .eq(Question::getSubjectName, "行测")
                    .eq(Question::getCategoryName, section)
                    .orderByDesc(Question::getId)
                    .last("limit 20"));
            if (questions.size() < 20) {
                throw new BizException(section + "题量不足 20 题，无法组卷");
            }
            for (Question question : questions) {
                QuestionPaperQuestion relation = new QuestionPaperQuestion();
                relation.setPaperId(paperId);
                relation.setQuestionId(question.getId());
                relation.setSectionName(section);
                relation.setScore(1);
                relation.setSortNo(sortNo++);
                questionPaperQuestionMapper.insert(relation);
            }
        }
    }

    private void validateShenlunGroupRequest(AdminShenlunGroupSaveRequest request) {
        Set<String> materialLabels = new HashSet<>();
        for (AdminShenlunGroupSaveRequest.MaterialItem item : request.getMaterials()) {
            String label = normalizeText(item.getMaterialLabel());
            if (!materialLabels.add(label)) {
                throw new BizException("材料标签不能重复");
            }
        }
    }

    private void fillShenlunGroup(ShenlunGroup group, AdminShenlunGroupSaveRequest request) {
        group.setTitle(normalizeText(request.getTitle()));
        group.setSubjectName(SHENLUN_SUBJECT);
        group.setYear(request.getYear());
        group.setRegion(normalizeText(request.getRegion()));
        group.setPaperType(normalizeText(request.getPaperType()));
        group.setSourceName(normalizeText(request.getSourceName()));
        group.setMaterialMode(StringUtils.hasText(request.getMaterialMode()) ? request.getMaterialMode().trim() : "GROUP_SHARED");
        group.setStatus(resolveShenlunGroupStatus(request.getStatus()));
    }

    private Map<Integer, ShenlunGroupMaterial> saveShenlunGroupMaterials(Long groupId,
                                                                         List<AdminShenlunGroupSaveRequest.MaterialItem> items,
                                                                         Map<Long, ShenlunGroupMaterial> oldMaterialMap) {
        Map<Integer, ShenlunGroupMaterial> materialMap = new LinkedHashMap<>();
        int sortNo = 1;
        for (AdminShenlunGroupSaveRequest.MaterialItem item : items) {
            ShenlunGroupMaterial material = item.getId() == null ? new ShenlunGroupMaterial() : oldMaterialMap.get(item.getId());
            if (material == null) {
                material = new ShenlunGroupMaterial();
            }
            material.setGroupId(groupId);
            material.setSortNo(sortNo);
            material.setMaterialLabel(normalizeText(item.getMaterialLabel()));
            material.setTitle(normalizeNullableText(item.getTitle()));
            material.setContent(normalizeText(item.getContent()));
            if (material.getId() == null) {
                shenlunGroupMaterialMapper.insert(material);
            } else {
                shenlunGroupMaterialMapper.updateById(material);
            }
            item.setId(material.getId());
            item.setSortNo(sortNo);
            materialMap.put(sortNo, material);
            sortNo++;
        }
        return materialMap;
    }

    private void removeUnusedGroupMaterials(Long groupId,
                                            List<AdminShenlunGroupSaveRequest.MaterialItem> requestItems,
                                            List<ShenlunGroupMaterial> oldMaterials) {
        Set<Long> keepIds = requestItems.stream()
                .map(AdminShenlunGroupSaveRequest.MaterialItem::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Long> removeIds = oldMaterials.stream()
                .map(ShenlunGroupMaterial::getId)
                .filter(id -> !keepIds.contains(id))
                .collect(Collectors.toList());
        if (!removeIds.isEmpty()) {
            shenlunGroupMaterialMapper.delete(new LambdaQueryWrapper<ShenlunGroupMaterial>()
                    .eq(ShenlunGroupMaterial::getGroupId, groupId)
                    .in(ShenlunGroupMaterial::getId, removeIds));
        }
    }

    private void saveShenlunGroupQuestions(ShenlunGroup group,
                                           List<AdminShenlunGroupSaveRequest.QuestionItem> items,
                                           Map<Long, Question> oldQuestionMap,
                                           Map<Integer, ShenlunGroupMaterial> materialMap) {
        int sortNo = 1;
        for (AdminShenlunGroupSaveRequest.QuestionItem item : items) {
            Question question = item.getId() == null ? new Question() : oldQuestionMap.get(item.getId());
            if (question == null) {
                question = new Question();
            }
            List<Integer> refs = normalizeMaterialRefs(item.getMaterialRefs(), materialMap.keySet());
            question.setGroupId(group.getId());
            question.setGroupQuestionNo(sortNo);
            question.setIsGroupSharedMaterial(true);
            question.setGroupMaterialRefs(joinMaterialRefs(refs));
            question.setSubjectName(SHENLUN_SUBJECT);
            question.setCategoryName(normalizeText(item.getCategoryName()));
            question.setProvince(group.getRegion());
            question.setQuestionType("SHORT_ANSWER");
            question.setDifficultyLevel(normalizeText(item.getDifficultyLevel()));
            question.setScore(item.getScore());
            question.setSourceName(group.getSourceName());
            question.setMaterial(resolveQuestionMaterialSnapshot(refs, materialMap));
            question.setStem(normalizeText(item.getStem()));
            question.setAnalysis(normalizeText(item.getAnalysis()));
            question.setAnswerValue(QuestionAnswerUtils.normalizeAnswer(item.getAnswerValue()));
            question.setKnowledgePoints(normalizeNullableText(item.getKnowledgePoints()));
            question.setEnabled(true);
            if (question.getId() == null) {
                questionMapper.insert(question);
            } else {
                questionMapper.updateById(question);
            }
            item.setId(question.getId());
            item.setSortNo(sortNo);
            sortNo++;
        }
    }

    private void removeUnusedGroupQuestions(Long groupId,
                                            List<AdminShenlunGroupSaveRequest.QuestionItem> requestItems,
                                            List<Question> oldQuestions) {
        Set<Long> keepIds = requestItems.stream()
                .map(AdminShenlunGroupSaveRequest.QuestionItem::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Long> removeIds = oldQuestions.stream()
                .map(Question::getId)
                .filter(id -> !keepIds.contains(id))
                .collect(Collectors.toList());
        if (!removeIds.isEmpty()) {
            questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>().in(QuestionOption::getQuestionId, removeIds));
            questionMapper.delete(new LambdaQueryWrapper<Question>().eq(Question::getGroupId, groupId).in(Question::getId, removeIds));
        }
    }

    private void updateShenlunGroupStats(Long groupId) {
        ShenlunGroup group = requireShenlunGroup(groupId);
        List<Question> questions = loadAdminShenlunGroupQuestions(groupId);
        group.setTotalQuestionCount(questions.size());
        group.setTotalScore(questions.stream().map(Question::getScore).filter(Objects::nonNull).reduce(0, Integer::sum));
        shenlunGroupMapper.updateById(group);
    }

    private void syncGeneratedShenlunPaper(Long groupId) {
        ShenlunGroup group = requireShenlunGroup(groupId);
        List<Question> questions = loadAdminShenlunGroupQuestions(groupId);
        if (questions.isEmpty()) {
            return;
        }
        QuestionPaper paper = findGeneratedShenlunPaper(groupId);
        if (paper == null) {
            paper = new QuestionPaper();
            paper.setPaperNo("SL-PAPER-" + groupId);
        }
        paper.setTitle(group.getTitle());
        paper.setPaperType("QUESTION_BANK");
        paper.setExamType("公务员");
        paper.setSubject(SHENLUN_SUBJECT);
        paper.setRegion(group.getRegion());
        paper.setYear(group.getYear());
        paper.setSource(SHENLUN_PAPER_SOURCE_PREFIX + groupId);
        paper.setDescription(group.getSourceName());
        paper.setDuration(180);
        paper.setTotalScore(group.getTotalScore() == null ? 0 : group.getTotalScore());
        paper.setQuestionCount(group.getTotalQuestionCount() == null ? questions.size() : group.getTotalQuestionCount());
        paper.setDifficulty(3);
        paper.setStatus(group.getStatus() == null ? SHENLUN_GROUP_STATUS_DRAFT : group.getStatus());
        if (paper.getId() == null) {
            questionPaperMapper.insert(paper);
        } else {
            questionPaperMapper.updateById(paper);
            questionPaperQuestionMapper.delete(new LambdaQueryWrapper<QuestionPaperQuestion>().eq(QuestionPaperQuestion::getPaperId, paper.getId()));
        }
        int sortNo = 1;
        for (Question question : questions) {
            QuestionPaperQuestion relation = new QuestionPaperQuestion();
            relation.setPaperId(paper.getId());
            relation.setQuestionId(question.getId());
            relation.setSectionName(StringUtils.hasText(question.getCategoryName()) ? question.getCategoryName() : ("第" + sortNo + "题"));
            relation.setScore(question.getScore() == null ? 0 : question.getScore());
            relation.setSortNo(sortNo++);
            questionPaperQuestionMapper.insert(relation);
        }
    }

    private ShenlunGroup requireShenlunGroup(Long id) {
        ShenlunGroup group = shenlunGroupMapper.selectById(id);
        if (group == null || !SHENLUN_SUBJECT.equals(group.getSubjectName())) {
            throw new BizException("申论材料组不存在");
        }
        return group;
    }

    private List<ShenlunGroupMaterial> loadAdminShenlunGroupMaterials(Long groupId) {
        return shenlunGroupMaterialMapper.selectList(new LambdaQueryWrapper<ShenlunGroupMaterial>()
                .eq(ShenlunGroupMaterial::getGroupId, groupId)
                .orderByAsc(ShenlunGroupMaterial::getSortNo)
                .orderByAsc(ShenlunGroupMaterial::getId));
    }

    private List<Question> loadAdminShenlunGroupQuestions(Long groupId) {
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getGroupId, groupId)
                .orderByAsc(Question::getGroupQuestionNo)
                .orderByAsc(Question::getId));
    }

    private List<Integer> normalizeMaterialRefs(List<String> refs, Set<Integer> validSortNos) {
        LinkedHashSet<Integer> normalized = new LinkedHashSet<>();
        for (String ref : refs) {
            if (!StringUtils.hasText(ref)) {
                continue;
            }
            int value;
            try {
                value = Integer.parseInt(ref.trim());
            } catch (NumberFormatException exception) {
                throw new BizException("题目关联材料编号格式不正确");
            }
            if (!validSortNos.contains(value)) {
                throw new BizException("题目关联了不存在的材料编号：" + value);
            }
            normalized.add(value);
        }
        if (normalized.isEmpty()) {
            throw new BizException("每题至少关联一份材料");
        }
        return new ArrayList<>(normalized);
    }

    private String joinMaterialRefs(List<Integer> refs) {
        return refs.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private String resolveQuestionMaterialSnapshot(List<Integer> refs, Map<Integer, ShenlunGroupMaterial> materialMap) {
        for (Integer ref : refs) {
            ShenlunGroupMaterial material = materialMap.get(ref);
            if (material != null && StringUtils.hasText(material.getContent())) {
                return material.getContent();
            }
        }
        return materialMap.isEmpty() ? null : materialMap.values().iterator().next().getContent();
    }

    private QuestionPaper findGeneratedShenlunPaper(Long groupId) {
        return questionPaperMapper.selectOne(new LambdaQueryWrapper<QuestionPaper>()
                .eq(QuestionPaper::getSource, SHENLUN_PAPER_SOURCE_PREFIX + groupId)
                .last("limit 1"));
    }

    private boolean isGeneratedShenlunPaper(QuestionPaper paper) {
        return paper != null && StringUtils.hasText(paper.getSource()) && paper.getSource().startsWith(SHENLUN_PAPER_SOURCE_PREFIX);
    }

    private Map<String, Object> toAdminShenlunGroupSummary(ShenlunGroup group) {
        List<ShenlunGroupMaterial> materials = loadAdminShenlunGroupMaterials(group.getId());
        QuestionPaper generatedPaper = findGeneratedShenlunPaper(group.getId());
        Map<String, Object> item = new HashMap<>();
        item.put("id", group.getId());
        item.put("title", group.getTitle());
        item.put("year", group.getYear());
        item.put("region", group.getRegion());
        item.put("paperType", group.getPaperType());
        item.put("sourceName", group.getSourceName());
        item.put("materialMode", group.getMaterialMode());
        item.put("materialCount", materials.size());
        item.put("questionCount", group.getTotalQuestionCount());
        item.put("totalScore", group.getTotalScore());
        item.put("status", formatShenlunGroupStatus(group.getStatus()));
        item.put("generatedPaperId", generatedPaper == null ? null : generatedPaper.getId());
        item.put("generatedPaperTitle", generatedPaper == null ? null : generatedPaper.getTitle());
        return item;
    }

    private Map<String, Object> toAdminShenlunGroupDetail(ShenlunGroup group,
                                                          List<ShenlunGroupMaterial> materials,
                                                          List<Question> questions) {
        Map<String, Object> item = toAdminShenlunGroupSummary(group);
        item.put("materials", materials.stream().map(this::toAdminShenlunMaterialItem).collect(Collectors.toList()));
        item.put("questions", questions.stream().map(this::toAdminShenlunQuestionItem).collect(Collectors.toList()));
        QuestionPaper generatedPaper = findGeneratedShenlunPaper(group.getId());
        if (generatedPaper != null) {
            item.put("generatedPaper", generatedPaper);
        }
        return item;
    }

    private Map<String, Object> toAdminShenlunMaterialItem(ShenlunGroupMaterial material) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", material.getId());
        item.put("sortNo", material.getSortNo());
        item.put("materialLabel", material.getMaterialLabel());
        item.put("title", material.getTitle());
        item.put("content", material.getContent());
        return item;
    }

    private Map<String, Object> toAdminShenlunQuestionItem(Question question) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", question.getId());
        item.put("sortNo", question.getGroupQuestionNo());
        item.put("categoryName", question.getCategoryName());
        item.put("questionType", question.getQuestionType());
        item.put("difficultyLevel", question.getDifficultyLevel());
        item.put("score", question.getScore());
        item.put("stem", question.getStem());
        item.put("analysis", question.getAnalysis());
        item.put("answerValue", question.getAnswerValue());
        item.put("knowledgePoints", question.getKnowledgePoints());
        item.put("materialRefs", parseGroupMaterialRefs(question.getGroupMaterialRefs()));
        return item;
    }

    private List<String> parseGroupMaterialRefs(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean shouldSyncGeneratedShenlunPaper(ShenlunGroup group) {
        return group != null && group.getStatus() != null
                && (group.getStatus() == SHENLUN_GROUP_STATUS_PUBLISHED || group.getStatus() == SHENLUN_GROUP_STATUS_ARCHIVED);
    }

    private boolean isArchivedShenlunGroup(ShenlunGroup group) {
        return group != null && group.getStatus() != null && group.getStatus() == SHENLUN_GROUP_STATUS_ARCHIVED;
    }

    private int resolveRestoredShenlunGroupStatus(String status) {
        int resolved = resolveShenlunGroupStatus(status);
        if (resolved == SHENLUN_GROUP_STATUS_ARCHIVED) {
            throw new BizException("恢复状态不能为已归档");
        }
        return resolved;
    }

    private int resolveShenlunGroupStatus(String status) {
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            return SHENLUN_GROUP_STATUS_PUBLISHED;
        }
        if ("ARCHIVED".equalsIgnoreCase(status)) {
            return SHENLUN_GROUP_STATUS_ARCHIVED;
        }
        return SHENLUN_GROUP_STATUS_DRAFT;
    }

    private String formatShenlunGroupStatus(Integer status) {
        if (status != null && status == SHENLUN_GROUP_STATUS_PUBLISHED) {
            return "PUBLISHED";
        }
        if (status != null && status == SHENLUN_GROUP_STATUS_ARCHIVED) {
            return "ARCHIVED";
        }
        return "DRAFT";
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public PageResult<Exam> pageExams(Long pageNo, Long pageSize, String keyword) {
        Page<Exam> page = examMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<Exam>()
                .like(StringUtils.hasText(keyword), Exam::getTitle, keyword)
                .orderByDesc(Exam::getCreatedAt));
        page.getRecords().forEach(exam -> exam.setQuestionIds(loadExamQuestionIds(exam.getId())));
        return new PageResult<>(page.getTotal(), pageNo, pageSize, page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveExam(ExamSaveRequest request) {
        List<Long> questionIds = request.getQuestionIds() == null
                ? Collections.emptyList()
                : request.getQuestionIds().stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        validateExamQuestions(questionIds);
        Exam exam = new Exam();
        BeanUtils.copyProperties(request, exam);
        exam.setQuestionCount(questionIds.size());
        if (exam.getRankingEnabled() == null) {
            exam.setRankingEnabled(Boolean.TRUE);
        }
        if (exam.getId() == null) {
            examMapper.insert(exam);
        } else {
            examMapper.updateById(exam);
            examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, exam.getId()));
        }
        if (!CollectionUtils.isEmpty(questionIds)) {
            int index = 1;
            int everyScore = Math.max(1, request.getTotalScore() / questionIds.size());
            for (Long questionId : questionIds) {
                ExamQuestion relation = new ExamQuestion();
                relation.setExamId(exam.getId());
                relation.setQuestionId(questionId);
                relation.setScore(everyScore);
                relation.setSortNo(index++);
                examQuestionMapper.insert(relation);
            }
        }
    }

    public void removeExam(Long id) {
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id));
        examMapper.deleteById(id);
    }

    private Map<String, Object> buildQuestionSummary(Question question) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", question.getId());
        item.put("subjectName", question.getSubjectName());
        item.put("categoryName", question.getCategoryName());
        item.put("province", question.getProvince());
        item.put("questionType", question.getQuestionType());
        item.put("difficultyLevel", question.getDifficultyLevel());
        item.put("sourceName", question.getSourceName());
        item.put("material", question.getMaterial());
        item.put("stem", question.getStem());
        return item;
    }

    private List<Long> loadExamQuestionIds(Long examId) {
        if (examId == null) {
            return Collections.emptyList();
        }
        return examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, examId)
                        .orderByAsc(ExamQuestion::getSortNo))
                .stream()
                .map(ExamQuestion::getQuestionId)
                .collect(Collectors.toList());
    }

    private void validateExamQuestions(List<Long> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return;
        }
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new BizException("模考题目包含不存在的题目，请重新选择");
        }
        boolean hasDisabled = questions.stream().anyMatch(question -> Boolean.FALSE.equals(question.getEnabled()));
        if (hasDisabled) {
            throw new BizException("模考题目包含已停用题目，请重新选择");
        }
    }

    public PageResult<News> pageNews(Long pageNo, Long pageSize, String keyword, Long categoryId, String infoCategory, String province) {
        return newsService.adminPage(pageNo, pageSize, keyword, categoryId, infoCategory, province);
    }

    public void saveNews(NewsSaveRequest request) {
        newsService.save(request);
    }

    public void removeNews(Long id) {
        newsService.remove(id);
    }

    public PageResult<Notice> pageNotices(Long pageNo, Long pageSize, String keyword) {
        Page<Notice> page = noticeMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<Notice>()
                .like(StringUtils.hasText(keyword), Notice::getTitle, keyword)
                .orderByDesc(Notice::getPublishedAt));
        return new PageResult<>(page.getTotal(), pageNo, pageSize, page.getRecords());
    }

    public void saveNotice(NoticeSaveRequest request) {
        Notice notice = new Notice();
        BeanUtils.copyProperties(request, notice);
        if (notice.getPublishedAt() == null) {
            notice.setPublishedAt(LocalDateTime.now());
        }
        if (!StringUtils.hasText(notice.getStatus())) {
            notice.setStatus("PUBLISHED");
        }
        notice.setCreatedBy(SecurityUtils.getUserId());
        if (notice.getId() == null) {
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
    }

    public void removeNotice(Long id) {
        noticeMapper.deleteById(id);
    }

    public List<RecommendConfig> listRecommendConfigs() {
        return recommendConfigMapper.selectList(new LambdaQueryWrapper<RecommendConfig>().orderByAsc(RecommendConfig::getId));
    }

    public List<Banner> listBanners() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSortNo)
                .orderByDesc(Banner::getId));
    }

    public void saveBanner(BannerSaveRequest request) {
        Banner banner = request.getId() == null ? null : bannerMapper.selectById(request.getId());
        if (banner == null) {
            banner = new Banner();
            banner.setCreatedAt(LocalDateTime.now());
        }
        BeanUtils.copyProperties(request, banner);
        if (banner.getEnabled() == null) {
            banner.setEnabled(true);
        }
        if (banner.getId() == null) {
            bannerMapper.insert(banner);
        } else {
            bannerMapper.updateById(banner);
        }
    }

    public void removeBanner(Long id) {
        bannerMapper.deleteById(id);
    }

    public void saveRecommendConfig(RecommendConfigSaveRequest request) {
        RecommendConfig config = recommendConfigMapper.selectOne(new LambdaQueryWrapper<RecommendConfig>()
                .eq(RecommendConfig::getConfigKey, request.getConfigKey()));
        if (config == null) {
            config = new RecommendConfig();
        }
        BeanUtils.copyProperties(request, config);
        if (config.getId() == null) {
            recommendConfigMapper.insert(config);
        } else {
            recommendConfigMapper.updateById(config);
        }
    }

    public List<Map<String, Object>> listRecommendPinnedResources() {
        return recommendEngineService.listPinnedResourceView();
    }

    public void saveRecommendPinnedResource(RecommendPinnedResourceSaveRequest request) {
        RecommendPinnedResource resource = request.getId() == null ? null : recommendPinnedResourceMapper.selectById(request.getId());
        if (resource == null) {
            resource = new RecommendPinnedResource();
        }
        BeanUtils.copyProperties(request, resource);
        if (resource.getSortNo() == null) {
            resource.setSortNo(0);
        }
        if (resource.getEnabled() == null) {
            resource.setEnabled(true);
        }
        if (!StringUtils.hasText(resource.getRegionScope())) {
            resource.setRegionScope("ALL");
        }
        if (resource.getFallbackEnabled() == null) {
            resource.setFallbackEnabled(true);
        }
        if (resource.getId() == null) {
            recommendPinnedResourceMapper.insert(resource);
        } else {
            recommendPinnedResourceMapper.updateById(resource);
        }
        recommendEngineService.refreshHotPool();
    }

    public void removeRecommendPinnedResource(Long id) {
        recommendPinnedResourceMapper.deleteById(id);
        recommendEngineService.refreshHotPool();
    }

    public PageResult<Map<String, Object>> pageOrders(Long pageNo, Long pageSize, String keyword, String orderStatus, String paymentStatus) {
        Page<CourseOrder> page = courseOrderMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<CourseOrder>()
                .like(StringUtils.hasText(keyword), CourseOrder::getOrderNo, keyword)
                .eq(StringUtils.hasText(orderStatus), CourseOrder::getOrderStatus, orderStatus)
                .eq(StringUtils.hasText(paymentStatus), CourseOrder::getPaymentStatus, paymentStatus)
                .orderByDesc(CourseOrder::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(order -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", order.getId());
            item.put("orderNo", order.getOrderNo());
            item.put("userId", order.getUserId());
            item.put("nickname", Optional.ofNullable(sysUserMapper.selectById(order.getUserId())).map(SysUser::getNickname).orElse("未知用户"));
            item.put("totalAmount", order.getTotalAmount());
            item.put("payAmount", order.getPayAmount());
            item.put("itemCount", order.getItemCount());
            item.put("orderStatus", order.getOrderStatus());
            item.put("paymentStatus", order.getPaymentStatus());
            item.put("paymentChannel", order.getPaymentChannel());
            item.put("createdAt", order.getCreatedAt());
            item.put("paidAt", order.getPaidAt());
            item.put("refund", orderRefundService.refundSummary(order.getId()));
            item.put("items", courseOrderItemMapper.selectList(new LambdaQueryWrapper<CourseOrderItem>()
                    .eq(CourseOrderItem::getOrderId, order.getId())
                    .orderByAsc(CourseOrderItem::getId)));
            return item;
        }).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(AdminOrderUpdateRequest request) {
        CourseOrder order = courseOrderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BizException("订单不存在");
        }
        String previousStatus = order.getOrderStatus();
        order.setOrderStatus(request.getOrderStatus());
        order.setPaymentStatus(request.getPaymentStatus());
        if ("PAID".equals(request.getOrderStatus()) && order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }
        courseOrderMapper.updateById(order);
        if ("PAID".equals(request.getOrderStatus()) && !"PAID".equals(previousStatus)) {
            grantOrderCourses(order);
        }
    }

    private void grantOrderCourses(CourseOrder order) {
        List<CourseOrderItem> items = courseOrderItemMapper.selectList(new LambdaQueryWrapper<CourseOrderItem>()
                .eq(CourseOrderItem::getOrderId, order.getId()));
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        List<Long> changedCourseIds = new ArrayList<>();
        for (CourseOrderItem item : items) {
            if (userCourseMapper.selectCount(new LambdaQueryWrapper<UserCourse>()
                    .eq(UserCourse::getUserId, order.getUserId())
                    .eq(UserCourse::getCourseId, item.getCourseId())) > 0) {
                continue;
            }
            Course course = courseMapper.selectById(item.getCourseId());
            UserCourse userCourse = new UserCourse();
            userCourse.setUserId(order.getUserId());
            userCourse.setCourseId(item.getCourseId());
            userCourse.setSourceOrderId(order.getId());
            userCourse.setExpireAt(course == null || course.getValidityDays() == null
                    ? null
                    : LocalDateTime.now().plusDays(course.getValidityDays()));
            userCourseMapper.insert(userCourse);
            changedCourseIds.add(item.getCourseId());
        }
        courseLearnCountService.syncCourses(changedCourseIds);
        recommendEngineService.enqueueBehaviorRefresh(order.getUserId(), "ADMIN_ORDER_PAID");
    }

    public PageResult<PaymentLog> pagePaymentLogs(Long pageNo, Long pageSize, String paymentChannel, String paymentStatus) {
        Page<PaymentLog> page = paymentLogMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<PaymentLog>()
                .eq(StringUtils.hasText(paymentChannel), PaymentLog::getPaymentChannel, paymentChannel)
                .eq(StringUtils.hasText(paymentStatus), PaymentLog::getPaymentStatus, paymentStatus)
                .orderByDesc(PaymentLog::getCreatedAt));
        return new PageResult<>(page.getTotal(), pageNo, pageSize, page.getRecords());
    }

    public Map<String, Object> statistics() {
        Map<String, Object> result = new HashMap<>();
        List<CourseOrder> paidOrders = courseOrderMapper.selectList(new LambdaQueryWrapper<CourseOrder>()
                .eq(CourseOrder::getOrderStatus, "PAID"));
        List<CourseOrderItem> orderItems = courseOrderItemMapper.selectList(null);
        result.put("overview", buildStatsOverview(paidOrders));
        result.put("salesTrend", buildSalesTrend(paidOrders));
        result.put("orderStatusStats", buildOrderStatusStats());
        result.put("subjectSales", buildSubjectSales(orderItems));
        return result;
    }

    public Map<String, Object> systemConfigs() {
        List<SystemConfig> configs = systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .orderByAsc(SystemConfig::getConfigGroup)
                .orderByAsc(SystemConfig::getSortNo)
                .orderByAsc(SystemConfig::getId));
        Map<String, List<SystemConfig>> groupedConfigs = configs.stream()
                .collect(Collectors.groupingBy(SystemConfig::getConfigGroup, LinkedHashMap::new, Collectors.toList()));
        Map<String, Object> result = new HashMap<>();
        result.put("groups", groupedConfigs);
        result.put("summary", java.util.Arrays.asList(
                buildStatusStat("参数配置", (long) groupedConfigs.getOrDefault("PARAM", Collections.emptyList()).size()),
                buildStatusStat("导出配置", (long) groupedConfigs.getOrDefault("EXPORT", Collections.emptyList()).size()),
                buildStatusStat("广播配置", (long) groupedConfigs.getOrDefault("BROADCAST", Collections.emptyList()).size())
        ));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveSystemConfigs(SystemConfigSaveRequest request) {
        for (SystemConfigSaveRequest.Item item : request.getItems()) {
            SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getConfigGroup, request.getConfigGroup())
                    .eq(SystemConfig::getConfigKey, item.getConfigKey())
                    .last("LIMIT 1"));
            if (config == null) {
                throw new BizException("配置项不存在: " + item.getConfigKey());
            }
            config.setConfigValue(item.getConfigValue());
            systemConfigMapper.updateById(config);
        }
    }

    public PageResult<Map<String, Object>> pageOperationLogs(Long pageNo,
                                                             Long pageSize,
                                                             String moduleName,
                                                             String success,
                                                             String keyword,
                                                             LocalDateTime startTime,
                                                             LocalDateTime endTime,
                                                             Boolean sensitiveOnly) {
        Page<SysOperationLog> page = sysOperationLogMapper.selectPage(new Page<>(pageNo, pageSize), buildOperationLogWrapper(moduleName, success, keyword, startTime, endTime, sensitiveOnly)
                .orderByDesc(SysOperationLog::getCreatedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(log -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", log.getId());
            item.put("moduleName", log.getModuleName());
            item.put("operationName", log.getOperationName());
            item.put("requestMethod", log.getRequestMethod());
            item.put("requestUri", log.getRequestUri());
            item.put("operatorName", log.getOperatorName());
            item.put("operatorRole", log.getOperatorRole());
            item.put("success", log.getSuccess());
            item.put("errorMessage", log.getErrorMessage());
            item.put("requestBody", log.getRequestBody());
            item.put("responseBody", log.getResponseBody());
            item.put("createdAt", log.getCreatedAt());
            item.put("sensitive", isSensitiveOperation(log));
            return item;
        }).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    public Map<String, Object> operationLogSummary(String moduleName,
                                                   String success,
                                                   String keyword,
                                                   LocalDateTime startTime,
                                                   LocalDateTime endTime,
                                                   Boolean sensitiveOnly) {
        List<SysOperationLog> logs = sysOperationLogMapper.selectList(buildOperationLogWrapper(moduleName, success, keyword, startTime, endTime, sensitiveOnly));
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", logs.size());
        summary.put("todayCount", logs.stream().filter(item -> item.getCreatedAt() != null && !item.getCreatedAt().isBefore(todayStart)).count());
        summary.put("failureCount", logs.stream().filter(item -> Boolean.FALSE.equals(item.getSuccess())).count());
        summary.put("sensitiveCount", logs.stream().filter(this::isSensitiveOperation).count());
        summary.put("activeOperatorCount", logs.stream().map(SysOperationLog::getOperatorName).filter(StringUtils::hasText).distinct().count());
        summary.put("modules", logs.stream()
                .map(SysOperationLog::getModuleName)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList()));
        return summary;
    }

    public byte[] exportOperationLogs(String moduleName,
                                      String success,
                                      String keyword,
                                      LocalDateTime startTime,
                                      LocalDateTime endTime,
                                      Boolean sensitiveOnly) {
        List<Map<String, Object>> records = pageOperationLogs(1L, 500L, moduleName, success, keyword, startTime, endTime, sensitiveOnly).getRecords();
        List<List<Object>> rows = records.stream()
                .map(item -> java.util.Arrays.asList(
                        item.get("moduleName"),
                        item.get("operationName"),
                        item.get("requestMethod"),
                        item.get("requestUri"),
                        item.get("operatorName"),
                        Boolean.TRUE.equals(item.get("success")) ? "成功" : "失败",
                        Boolean.TRUE.equals(item.get("sensitive")) ? "敏感操作" : "普通操作",
                        item.get("errorMessage"),
                        item.get("createdAt")
                ))
                .collect(Collectors.toList());
        return com.civil.platform.common.util.ExcelUtils.buildWorkbook("操作日志",
                java.util.Arrays.asList("模块", "操作", "请求方式", "请求地址", "操作人", "结果", "风险标记", "错误信息", "时间"),
                rows);
    }

    private LambdaQueryWrapper<SysOperationLog> buildOperationLogWrapper(String moduleName,
                                                                         String success,
                                                                         String keyword,
                                                                         LocalDateTime startTime,
                                                                         LocalDateTime endTime,
                                                                         Boolean sensitiveOnly) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<SysOperationLog>()
                .eq(StringUtils.hasText(moduleName), SysOperationLog::getModuleName, moduleName)
                .eq(StringUtils.hasText(success), SysOperationLog::getSuccess, Boolean.valueOf(success))
                .ge(startTime != null, SysOperationLog::getCreatedAt, startTime)
                .le(endTime != null, SysOperationLog::getCreatedAt, endTime)
                .and(StringUtils.hasText(keyword), query -> query.like(SysOperationLog::getOperationName, keyword)
                        .or().like(SysOperationLog::getRequestUri, keyword)
                        .or().like(SysOperationLog::getOperatorName, keyword)
                        .or().like(SysOperationLog::getModuleName, keyword));
        if (Boolean.TRUE.equals(sensitiveOnly)) {
            wrapper.and(query -> query.like(SysOperationLog::getOperationName, "删除")
                    .or().like(SysOperationLog::getOperationName, "审核")
                    .or().like(SysOperationLog::getOperationName, "退款")
                    .or().like(SysOperationLog::getOperationName, "权限")
                    .or().like(SysOperationLog::getOperationName, "角色")
                    .or().like(SysOperationLog::getOperationName, "系统设置")
                    .or().like(SysOperationLog::getOperationName, "配置")
                    .or().like(SysOperationLog::getRequestUri, "/refund")
                    .or().like(SysOperationLog::getRequestUri, "/role")
                    .or().like(SysOperationLog::getRequestUri, "/system-config"));
        }
        return wrapper;
    }

    private boolean isSensitiveOperation(SysOperationLog log) {
        if (log == null) {
            return false;
        }
        String text = (log.getOperationName() == null ? "" : log.getOperationName())
                + " "
                + (log.getRequestUri() == null ? "" : log.getRequestUri());
        return text.contains("删除")
                || text.contains("审核")
                || text.contains("退款")
                || text.contains("权限")
                || text.contains("角色")
                || text.contains("系统设置")
                || text.contains("配置")
                || text.contains("/refund")
                || text.contains("/role")
                || text.contains("/system-config");
    }

    private Map<String, Object> buildGrowthOverview(List<UserStudyPlan> allPlans,
                                                    List<UserDailyTask> allTasks,
                                                    List<UserGrowthAchievement> achievements,
                                                    List<PracticeRecord> completedTestPapers) {
        Map<String, Object> overview = new HashMap<>();
        long activePlanCount = allPlans.stream().filter(item -> "ACTIVE".equals(item.getStatus())).count();
        List<UserStudyPlan> archivedPlans = allPlans.stream()
                .filter(item -> "ARCHIVED".equals(item.getStatus()))
                .collect(Collectors.toList());
        overview.put("activePlanCount", activePlanCount);
        overview.put("archivedPlanCount", archivedPlans.size());
        overview.put("todayCompletedTaskCount", achievements.stream()
                .filter(item -> "TASK_COMPLETED".equals(item.getAchievementType()))
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().isAfter(LocalDateTime.now().toLocalDate().atStartOfDay()))
                .count());
        overview.put("achievementCount", achievements.size());
        overview.put("completedTestPaperCount", completedTestPapers.size());
        overview.put("averagePlanCompletionRate", archivedPlans.isEmpty()
                ? java.math.BigDecimal.ZERO
                : archivedPlans.stream()
                .map(item -> item.getCompletionRate() == null ? java.math.BigDecimal.ZERO : item.getCompletionRate())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .divide(java.math.BigDecimal.valueOf(archivedPlans.size()), 2, java.math.RoundingMode.HALF_UP));
        overview.put("totalTaskCount", allTasks.size());
        return overview;
    }

    private List<Map<String, Object>> buildSubjectPlanStats(List<UserStudyPlan> allPlans) {
        Map<String, Long> grouped = allPlans.stream()
                .collect(Collectors.groupingBy(item -> StringUtils.hasText(item.getSubjectName()) ? item.getSubjectName() : "未分类",
                        LinkedHashMap::new,
                        Collectors.counting()));
        return grouped.entrySet().stream().map(entry -> {
            Map<String, Object> item = new HashMap<>();
            item.put("subjectName", entry.getKey());
            item.put("planCount", entry.getValue());
            return item;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildGrowthTaskTypeStats(List<UserDailyTask> allTasks, List<UserGrowthAchievement> achievements) {
        Map<Long, UserDailyTask> taskMap = allTasks.stream()
                .collect(Collectors.toMap(UserDailyTask::getId, Function.identity(), (left, right) -> left));
        Map<String, Long> completedMap = achievements.stream()
                .filter(item -> "TASK_COMPLETED".equals(item.getAchievementType()))
                .map(item -> taskMap.get(item.getTaskId()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(UserDailyTask::getTaskType, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> totalMap = allTasks.stream()
                .collect(Collectors.groupingBy(UserDailyTask::getTaskType, LinkedHashMap::new, Collectors.counting()));
        List<String> taskTypes = Arrays.asList("LEARN", "PRACTICE", "EXAM", "TEST_PAPER");
        List<Map<String, Object>> result = new ArrayList<>();
        for (String taskType : taskTypes) {
            Map<String, Object> item = new HashMap<>();
            item.put("taskType", taskType);
            item.put("taskTypeLabel", resolveTaskTypeLabel(taskType));
            item.put("totalCount", totalMap.getOrDefault(taskType, 0L));
            item.put("completedCount", completedMap.getOrDefault(taskType, 0L));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildMasteryLevelStats(List<UserKnowledgeMastery> masteryList) {
        List<String> levels = Arrays.asList("WEAK", "FOUNDATION", "IMPROVING", "MASTERED");
        List<Map<String, Object>> result = new ArrayList<>();
        for (String level : levels) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", resolveMasteryLevelLabel(level));
            item.put("value", masteryList.stream().filter(record -> level.equals(record.getMasteryLevel())).count());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildTestPaperTrend(List<PracticeRecord> completedTestPapers) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime start = LocalDateTime.now().toLocalDate().minusDays(i).atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            Map<String, Object> item = new HashMap<>();
            item.put("date", start.toLocalDate().toString());
            item.put("count", completedTestPapers.stream()
                    .filter(record -> between(record.getSubmittedAt(), start, end))
                    .count());
            item.put("averageScore", completedTestPapers.stream()
                    .filter(record -> between(record.getSubmittedAt(), start, end))
                    .map(record -> record.getScore() == null ? 0 : record.getScore())
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0));
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> buildWeakKnowledgeTop(List<UserKnowledgeMastery> masteryList) {
        return masteryList.stream()
                .sorted(Comparator.comparing(UserKnowledgeMastery::getPendingWrongCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UserKnowledgeMastery::getMasteryScore, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8)
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("subjectName", item.getSubjectName());
                    map.put("knowledgePoint", item.getKnowledgePoint());
                    map.put("pendingWrongCount", item.getPendingWrongCount());
                    map.put("masteryScore", item.getMasteryScore());
                    map.put("masteryLevelLabel", resolveMasteryLevelLabel(item.getMasteryLevel()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildClassroomGrowthStats(List<UserStudyPlan> allPlans,
                                                                List<UserGrowthDiagnosisReport> diagnosisReports) {
        List<Classroom> classrooms = classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                .orderByDesc(Classroom::getMemberCount)
                .orderByDesc(Classroom::getId));
        if (CollectionUtils.isEmpty(classrooms)) {
            return Collections.emptyList();
        }
        Map<Long, UserGrowthDiagnosisReport> latestDiagnosisMap = diagnosisReports.stream()
                .collect(Collectors.toMap(UserGrowthDiagnosisReport::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, UserStudyPlan> activePlanMap = allPlans.stream()
                .filter(item -> "ACTIVE".equals(item.getStatus()))
                .collect(Collectors.toMap(UserStudyPlan::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, SysUser> userMap = sysUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        List<ClassroomMember> members = classroomMemberMapper.selectList(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getRoleCode, "STUDENT"));
        Map<Long, Long> teacherPendingMap = teacherGrowthFocusFollowMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(TeacherGrowthFocusFollow::getClassroomId,
                        LinkedHashMap::new,
                        Collectors.counting()));

        return classrooms.stream()
                .map(classroom -> {
                    List<ClassroomMember> classroomStudents = members.stream()
                            .filter(item -> classroom.getId().equals(item.getClassroomId()))
                            .collect(Collectors.toList());
                    List<Map<String, Object>> activeStudents = classroomStudents.stream()
                            .map(member -> buildClassroomStudentMetric(member, activePlanMap, latestDiagnosisMap, userMap))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    Map<String, Object> item = new HashMap<>();
                    item.put("classroomId", classroom.getId());
                    item.put("classroomName", classroom.getName());
                    item.put("teacherName", classroom.getTeacherName());
                    item.put("subjectName", classroom.getSubjectName());
                    item.put("studentCount", classroomStudents.size());
                    item.put("activePlanStudentCount", activeStudents.size());
                    item.put("focusStudentCount", activeStudents.stream()
                            .filter(student -> !"STABLE".equals(student.get("segment")))
                            .count());
                    item.put("atRiskCount", activeStudents.stream()
                            .filter(student -> "AT_RISK".equals(student.get("segment")))
                            .count());
                    item.put("averageCompletionRate", averageDecimal(activeStudents, "completionRate"));
                    item.put("averageVitalityScore", averageDecimal(activeStudents, "vitalityScore"));
                    item.put("teacherFollowCount", teacherPendingMap.getOrDefault(classroom.getId(), 0L));
                    return item;
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTeacherCollaborationStats(List<UserStudyPlan> allPlans,
                                                                     List<UserGrowthDiagnosisReport> diagnosisReports) {
        List<Classroom> classrooms = classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                .orderByDesc(Classroom::getId));
        if (CollectionUtils.isEmpty(classrooms)) {
            return Collections.emptyList();
        }
        Map<Long, UserGrowthDiagnosisReport> latestDiagnosisMap = diagnosisReports.stream()
                .collect(Collectors.toMap(UserGrowthDiagnosisReport::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, UserStudyPlan> activePlanMap = allPlans.stream()
                .filter(item -> "ACTIVE".equals(item.getStatus()))
                .collect(Collectors.toMap(UserStudyPlan::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<ClassroomMember> members = classroomMemberMapper.selectList(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getRoleCode, "STUDENT"));
        Map<String, TeacherGrowthFocusFollow> teacherFollowMap = teacherGrowthFocusFollowMapper.selectList(new LambdaQueryWrapper<TeacherGrowthFocusFollow>()
                        .orderByDesc(TeacherGrowthFocusFollow::getUpdatedAt)
                        .orderByDesc(TeacherGrowthFocusFollow::getId))
                .stream()
                .collect(Collectors.toMap(item -> item.getClassroomId() + "_" + item.getUserId() + "_" + (item.getPlanId() == null ? 0 : item.getPlanId()),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        return classrooms.stream()
                .map(classroom -> {
                    List<ClassroomMember> classroomStudents = members.stream()
                            .filter(item -> classroom.getId().equals(item.getClassroomId()))
                            .collect(Collectors.toList());
                    long pendingCount = 0;
                    long doneCount = 0;
                    long inProgressCount = 0;
                    long atRiskCount = 0;
                    for (ClassroomMember student : classroomStudents) {
                        UserStudyPlan plan = activePlanMap.get(student.getUserId());
                        if (plan == null) {
                            continue;
                        }
                        UserGrowthDiagnosisReport diagnosis = latestDiagnosisMap.get(student.getUserId());
                        String segment = resolveUserSegment(diagnosis, plan);
                        if ("AT_RISK".equals(segment)) {
                            atRiskCount++;
                        }
                        TeacherGrowthFocusFollow follow = teacherFollowMap.get(classroom.getId() + "_" + student.getUserId() + "_" + plan.getId());
                        String status = follow == null ? "PENDING" : follow.getFollowStatus();
                        if ("DONE".equals(status)) {
                            doneCount++;
                        } else if ("IN_PROGRESS".equals(status)) {
                            inProgressCount++;
                        } else {
                            pendingCount++;
                        }
                    }
                    Map<String, Object> item = new HashMap<>();
                    item.put("classroomId", classroom.getId());
                    item.put("classroomName", classroom.getName());
                    item.put("teacherName", classroom.getTeacherName());
                    item.put("pendingCount", pendingCount);
                    item.put("inProgressCount", inProgressCount);
                    item.put("doneCount", doneCount);
                    item.put("atRiskCount", atRiskCount);
                    return item;
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildAdminFollowHistory() {
        List<AdminGrowthFocusFollowHistory> histories = adminGrowthFocusFollowHistoryMapper.selectList(new LambdaQueryWrapper<AdminGrowthFocusFollowHistory>()
                .orderByDesc(AdminGrowthFocusFollowHistory::getCreatedAt)
                .orderByDesc(AdminGrowthFocusFollowHistory::getId)
                .last("LIMIT 12"));
        if (CollectionUtils.isEmpty(histories)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = histories.stream().map(AdminGrowthFocusFollowHistory::getUserId).collect(Collectors.toSet());
        Set<Long> operatorIds = histories.stream().map(AdminGrowthFocusFollowHistory::getOperatorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        Map<Long, SysUser> operatorMap = operatorIds.isEmpty() ? Collections.emptyMap() : sysUserMapper.selectBatchIds(operatorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        return histories.stream().map(item -> {
            SysUser user = userMap.get(item.getUserId());
            SysUser operator = operatorMap.get(item.getOperatorId());
            Map<String, Object> map = new HashMap<>();
            map.put("userId", item.getUserId());
            map.put("username", user == null ? "未知用户" : user.getUsername());
            map.put("nickname", user == null ? "未知用户" : user.getNickname());
            map.put("followStatus", item.getFollowStatus());
            map.put("followStatusLabel", resolveFollowStatusLabel(item.getFollowStatus()));
            map.put("priorityLevel", item.getPriorityLevel());
            map.put("priorityLevelLabel", resolvePriorityLabel(item.getPriorityLevel()));
            map.put("remark", item.getRemark());
            map.put("nextFollowUpAt", item.getNextFollowUpAt());
            map.put("actionType", item.getActionType());
            map.put("createdAt", item.getCreatedAt());
            map.put("operatorName", operator == null ? "系统" : operator.getNickname());
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildArchivedPlans(List<UserStudyPlan> allPlans) {
        Map<Long, SysUser> userMap = sysUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        return allPlans.stream()
                .filter(item -> "ARCHIVED".equals(item.getStatus()))
                .sorted(Comparator.comparing(UserStudyPlan::getArchivedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("planName", item.getPlanName());
                    map.put("username", Optional.ofNullable(userMap.get(item.getUserId())).map(SysUser::getUsername).orElse("未知用户"));
                    map.put("subjectName", item.getSubjectName());
                    map.put("completionRate", item.getCompletionRate());
                    map.put("completedTaskCount", item.getCompletedTaskCount());
                    map.put("totalTaskCount", item.getTotalTaskCount());
                    map.put("archivedAt", item.getArchivedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildSegmentStats(List<UserStudyPlan> allPlans,
                                                        List<UserGrowthDiagnosisReport> diagnosisReports) {
        Map<Long, UserGrowthDiagnosisReport> latestDiagnosisMap = diagnosisReports.stream()
                .collect(Collectors.toMap(UserGrowthDiagnosisReport::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<String, Long> grouped = allPlans.stream()
                .filter(item -> "ACTIVE".equals(item.getStatus()))
                .collect(Collectors.groupingBy(item -> resolveUserSegment(latestDiagnosisMap.get(item.getUserId()), item),
                        LinkedHashMap::new,
                        Collectors.counting()));
        List<String> segments = Arrays.asList("AT_RISK", "TRACKING", "RISING", "STABLE");
        List<Map<String, Object>> result = new ArrayList<>();
        for (String segment : segments) {
            Map<String, Object> item = new HashMap<>();
            item.put("segment", segment);
            item.put("segmentLabel", resolveSegmentLabel(segment));
            item.put("count", grouped.getOrDefault(segment, 0L));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildFocusUsers(List<UserStudyPlan> allPlans,
                                                      List<UserGrowthDiagnosisReport> diagnosisReports,
                                                      List<AdminGrowthFocusFollow> focusFollows,
                                                      BigDecimal minCompletionRate,
                                                      String segment,
                                                      String followStatus) {
        Map<Long, SysUser> userMap = sysUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        Map<Long, UserGrowthDiagnosisReport> latestDiagnosisMap = diagnosisReports.stream()
                .collect(Collectors.toMap(UserGrowthDiagnosisReport::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<String, AdminGrowthFocusFollow> followMap = focusFollows.stream()
                .collect(Collectors.toMap(item -> buildFollowKey(item.getUserId(), item.getPlanId()), Function.identity(), (left, right) -> left, LinkedHashMap::new));
        BigDecimal threshold = minCompletionRate == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : minCompletionRate;

        return allPlans.stream()
                .filter(item -> "ACTIVE".equals(item.getStatus()))
                .map(plan -> {
                    UserGrowthDiagnosisReport diagnosis = latestDiagnosisMap.get(plan.getUserId());
                    AdminGrowthFocusFollow follow = followMap.get(buildFollowKey(plan.getUserId(), plan.getId()));
                    SysUser user = userMap.get(plan.getUserId());
                    String userSegment = resolveUserSegment(diagnosis, plan);
                    BigDecimal completionRate = diagnosis == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                            : Optional.ofNullable(diagnosis.getPlanCompletionRate()).orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", plan.getUserId());
                    map.put("username", user == null ? "未知用户" : user.getUsername());
                    map.put("nickname", user == null ? "未知用户" : user.getNickname());
                    map.put("subjectName", plan.getSubjectName());
                    map.put("planId", plan.getId());
                    map.put("planName", plan.getPlanName());
                    map.put("segment", userSegment);
                    map.put("segmentLabel", resolveSegmentLabel(userSegment));
                    map.put("completionRate", completionRate);
                    map.put("vitalityScore", diagnosis == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : diagnosis.getVitalityScore());
                    map.put("knowledgeMasteryRate", diagnosis == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : diagnosis.getKnowledgeMasteryRate());
                    map.put("testPaperAverageScore", diagnosis == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : diagnosis.getTestPaperAverageScore());
                    map.put("weakKnowledgePoints", diagnosis == null ? new ArrayList<>() : splitCsv(diagnosis.getWeakKnowledgePoints()));
                    map.put("followStatus", follow == null ? "PENDING" : follow.getFollowStatus());
                    map.put("followStatusLabel", resolveFollowStatusLabel(follow == null ? "PENDING" : follow.getFollowStatus()));
                    map.put("priorityLevel", follow == null ? "MEDIUM" : follow.getPriorityLevel());
                    map.put("priorityLevelLabel", resolvePriorityLabel(follow == null ? "MEDIUM" : follow.getPriorityLevel()));
                    map.put("followRemark", follow == null ? null : follow.getRemark());
                    map.put("nextFollowUpAt", follow == null ? null : follow.getNextFollowUpAt());
                    map.put("followUpdatedAt", follow == null ? null : follow.getUpdatedAt());
                    map.put("latestDiagnosisId", diagnosis == null ? null : diagnosis.getId());
                    map.put("diagnosisDate", diagnosis == null ? null : diagnosis.getDiagnosisDate());
                    String reminderStatus = resolveReminderStatus(follow == null ? null : follow.getNextFollowUpAt(), follow == null ? "PENDING" : follow.getFollowStatus());
                    map.put("reminderStatus", reminderStatus);
                    map.put("reminderStatusLabel", resolveReminderStatusLabel(reminderStatus));
                    return map;
                })
                .filter(item -> !StringUtils.hasText(segment) || segment.equals(item.get("segment")))
                .filter(item -> !StringUtils.hasText(followStatus) || followStatus.equals(item.get("followStatus")))
                .filter(item -> ((BigDecimal) item.get("completionRate")).compareTo(threshold) >= 0)
                .sorted((left, right) -> {
                    int segmentOrderCompare = Integer.compare(segmentOrder(String.valueOf(left.get("segment"))), segmentOrder(String.valueOf(right.get("segment"))));
                    if (segmentOrderCompare != 0) {
                        return segmentOrderCompare;
                    }
                    return ((BigDecimal) left.get("vitalityScore")).compareTo((BigDecimal) right.get("vitalityScore"));
                })
                .limit(20)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildGrowthFilterState(String segment,
                                                       BigDecimal minCompletionRate,
                                                       String followStatus) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("segment", segment);
        filters.put("minCompletionRate", minCompletionRate == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : minCompletionRate);
        filters.put("followStatus", followStatus);
        return filters;
    }

    private Map<String, Object> buildAdminReminderSummary(List<AdminGrowthFocusFollow> focusFollows) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("overdueCount", focusFollows.stream()
                .filter(item -> "OVERDUE".equals(resolveReminderStatus(item.getNextFollowUpAt(), item.getFollowStatus())))
                .count());
        summary.put("todayCount", focusFollows.stream()
                .filter(item -> "TODAY".equals(resolveReminderStatus(item.getNextFollowUpAt(), item.getFollowStatus())))
                .count());
        summary.put("upcomingCount", focusFollows.stream()
                .filter(item -> "UPCOMING".equals(resolveReminderStatus(item.getNextFollowUpAt(), item.getFollowStatus())))
                .count());
        summary.put("trackedCount", focusFollows.stream().filter(item -> item.getNextFollowUpAt() != null).count());
        return summary;
    }

    private List<Map<String, Object>> buildAdminFollowReminders(List<AdminGrowthFocusFollow> focusFollows,
                                                                List<UserStudyPlan> allPlans,
                                                                List<UserGrowthDiagnosisReport> diagnosisReports) {
        if (CollectionUtils.isEmpty(focusFollows)) {
            return Collections.emptyList();
        }
        Map<Long, SysUser> userMap = sysUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        Map<Long, UserStudyPlan> planMap = allPlans.stream()
                .collect(Collectors.toMap(UserStudyPlan::getId, Function.identity(), (left, right) -> left));
        Map<Long, UserGrowthDiagnosisReport> diagnosisMap = diagnosisReports.stream()
                .collect(Collectors.toMap(UserGrowthDiagnosisReport::getId, Function.identity(), (left, right) -> left));
        return focusFollows.stream()
                .filter(item -> item.getNextFollowUpAt() != null)
                .map(item -> {
                    SysUser user = userMap.get(item.getUserId());
                    UserStudyPlan plan = item.getPlanId() == null ? null : planMap.get(item.getPlanId());
                    UserGrowthDiagnosisReport diagnosis = item.getLatestDiagnosisId() == null ? null : diagnosisMap.get(item.getLatestDiagnosisId());
                    String reminderStatus = resolveReminderStatus(item.getNextFollowUpAt(), item.getFollowStatus());
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", item.getUserId());
                    map.put("username", user == null ? "未知用户" : user.getUsername());
                    map.put("nickname", user == null ? "未知用户" : user.getNickname());
                    map.put("planName", plan == null ? "未命名计划" : plan.getPlanName());
                    map.put("subjectName", plan == null ? null : plan.getSubjectName());
                    map.put("segmentLabel", diagnosis == null ? "跟踪补强" : resolveSegmentLabel(diagnosis.getDiagnosisStage()));
                    map.put("followStatusLabel", resolveFollowStatusLabel(item.getFollowStatus()));
                    map.put("priorityLevelLabel", resolvePriorityLabel(item.getPriorityLevel()));
                    map.put("nextFollowUpAt", item.getNextFollowUpAt());
                    map.put("remark", item.getRemark());
                    map.put("reminderStatus", reminderStatus);
                    map.put("reminderStatusLabel", resolveReminderStatusLabel(reminderStatus));
                    return map;
                })
                .sorted((left, right) -> {
                    int statusCompare = Integer.compare(reminderStatusOrder(String.valueOf(left.get("reminderStatus"))), reminderStatusOrder(String.valueOf(right.get("reminderStatus"))));
                    if (statusCompare != 0) {
                        return statusCompare;
                    }
                    return ((LocalDateTime) left.get("nextFollowUpAt")).compareTo((LocalDateTime) right.get("nextFollowUpAt"));
                })
                .limit(12)
                .collect(Collectors.toList());
    }

    private AdminGrowthDashboardPreference loadAdminGrowthDashboardPreference(Long userId) {
        if (userId == null) {
            return null;
        }
        return adminGrowthDashboardPreferenceMapper.selectOne(new LambdaQueryWrapper<AdminGrowthDashboardPreference>()
                .eq(AdminGrowthDashboardPreference::getUserId, userId)
                .last("LIMIT 1"));
    }

    private Map<String, Object> buildTrend(String name, Long value) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        return item;
    }

    private void syncTeacherRoleByAuditStatus(Long userId, String auditStatus) {
        SysRole teacherRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, "TEACHER")
                .last("limit 1"));
        if (teacherRole == null) {
            throw new BizException("教师角色不存在");
        }
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, teacherRole.getId());
        if ("APPROVED".equals(auditStatus)) {
            if (sysUserRoleMapper.selectCount(wrapper) == 0) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(teacherRole.getId());
                sysUserRoleMapper.insert(userRole);
            }
            return;
        }
        sysUserRoleMapper.delete(wrapper);
    }

    private String resolveUserSegment(UserGrowthDiagnosisReport diagnosis, UserStudyPlan plan) {
        if (diagnosis != null && StringUtils.hasText(diagnosis.getDiagnosisStage())) {
            return diagnosis.getDiagnosisStage();
        }
        if (plan.getCompletionRate() != null && plan.getCompletionRate().compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "RISING";
        }
        return "TRACKING";
    }

    private String resolveSegmentLabel(String segment) {
        if ("STABLE".equals(segment)) {
            return "稳定冲刺";
        }
        if ("RISING".equals(segment)) {
            return "持续上升";
        }
        if ("TRACKING".equals(segment)) {
            return "跟踪补强";
        }
        return "重点预警";
    }

    private String resolveFollowStatusLabel(String followStatus) {
        if ("DONE".equals(followStatus)) {
            return "已跟进";
        }
        if ("IN_PROGRESS".equals(followStatus)) {
            return "跟进中";
        }
        return "待跟进";
    }

    private String resolvePriorityLabel(String priorityLevel) {
        if ("HIGH".equals(priorityLevel)) {
            return "高优先级";
        }
        if ("LOW".equals(priorityLevel)) {
            return "低优先级";
        }
        return "中优先级";
    }

    private int segmentOrder(String segment) {
        if ("AT_RISK".equals(segment)) {
            return 0;
        }
        if ("TRACKING".equals(segment)) {
            return 1;
        }
        if ("RISING".equals(segment)) {
            return 2;
        }
        return 3;
    }

    private String buildFollowKey(Long userId, Long planId) {
        return userId + "_" + (planId == null ? 0 : planId);
    }

    private void saveGrowthFocusFollowInternal(Long userId,
                                               Long planId,
                                               Long latestDiagnosisId,
                                               String followStatus,
                                               String priorityLevel,
                                               String remark,
                                               LocalDateTime nextFollowUpAt,
                                               Long operatorId,
                                               String actionType) {
        LambdaQueryWrapper<AdminGrowthFocusFollow> wrapper = new LambdaQueryWrapper<AdminGrowthFocusFollow>()
                .eq(AdminGrowthFocusFollow::getUserId, userId)
                .last("LIMIT 1");
        if (planId == null) {
            wrapper.isNull(AdminGrowthFocusFollow::getPlanId);
        } else {
            wrapper.eq(AdminGrowthFocusFollow::getPlanId, planId);
        }
        AdminGrowthFocusFollow follow = adminGrowthFocusFollowMapper.selectOne(wrapper);
        if (follow == null) {
            follow = new AdminGrowthFocusFollow();
            follow.setUserId(userId);
            follow.setPlanId(planId);
            follow.setCreatedBy(operatorId);
        }
        follow.setLatestDiagnosisId(latestDiagnosisId);
        follow.setFollowStatus(followStatus);
        follow.setPriorityLevel(priorityLevel);
        follow.setRemark(remark);
        follow.setNextFollowUpAt(nextFollowUpAt);
        follow.setUpdatedBy(operatorId);
        if (follow.getId() == null) {
            adminGrowthFocusFollowMapper.insert(follow);
        } else {
            adminGrowthFocusFollowMapper.updateById(follow);
        }
        AdminGrowthFocusFollowHistory history = new AdminGrowthFocusFollowHistory();
        history.setAdminFollowId(follow.getId());
        history.setUserId(userId);
        history.setPlanId(planId);
        history.setLatestDiagnosisId(latestDiagnosisId);
        history.setFollowStatus(followStatus);
        history.setPriorityLevel(priorityLevel);
        history.setRemark(remark);
        history.setNextFollowUpAt(nextFollowUpAt);
        history.setOperatorId(operatorId);
        history.setActionType(actionType);
        adminGrowthFocusFollowHistoryMapper.insert(history);
    }

    private String resolveReminderStatus(LocalDateTime nextFollowUpAt, String followStatus) {
        if ("DONE".equals(followStatus)) {
            return "DONE";
        }
        if (nextFollowUpAt == null) {
            return "UNSCHEDULED";
        }
        LocalDateTime now = LocalDateTime.now();
        if (nextFollowUpAt.isBefore(now)) {
            return "OVERDUE";
        }
        if (nextFollowUpAt.toLocalDate().isEqual(now.toLocalDate())) {
            return "TODAY";
        }
        if (!nextFollowUpAt.isAfter(now.plusDays(7))) {
            return "UPCOMING";
        }
        return "SCHEDULED";
    }

    private String resolveReminderStatusLabel(String reminderStatus) {
        if ("DONE".equals(reminderStatus)) {
            return "已完成";
        }
        if ("OVERDUE".equals(reminderStatus)) {
            return "已逾期";
        }
        if ("TODAY".equals(reminderStatus)) {
            return "今日回访";
        }
        if ("UPCOMING".equals(reminderStatus)) {
            return "近 7 天";
        }
        if ("SCHEDULED".equals(reminderStatus)) {
            return "已排期";
        }
        return "待安排";
    }

    private int reminderStatusOrder(String reminderStatus) {
        if ("OVERDUE".equals(reminderStatus)) {
            return 0;
        }
        if ("TODAY".equals(reminderStatus)) {
            return 1;
        }
        if ("UPCOMING".equals(reminderStatus)) {
            return 2;
        }
        if ("SCHEDULED".equals(reminderStatus)) {
            return 3;
        }
        if ("UNSCHEDULED".equals(reminderStatus)) {
            return 4;
        }
        return 5;
    }

    private Map<String, Object> buildClassroomStudentMetric(ClassroomMember member,
                                                            Map<Long, UserStudyPlan> activePlanMap,
                                                            Map<Long, UserGrowthDiagnosisReport> latestDiagnosisMap,
                                                            Map<Long, SysUser> userMap) {
        UserStudyPlan plan = activePlanMap.get(member.getUserId());
        if (plan == null) {
            return null;
        }
        UserGrowthDiagnosisReport diagnosis = latestDiagnosisMap.get(member.getUserId());
        SysUser user = userMap.get(member.getUserId());
        Map<String, Object> item = new HashMap<>();
        item.put("userId", member.getUserId());
        item.put("username", user == null ? "未知用户" : user.getUsername());
        item.put("segment", resolveUserSegment(diagnosis, plan));
        item.put("completionRate", diagnosis == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : diagnosis.getPlanCompletionRate());
        item.put("vitalityScore", diagnosis == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : diagnosis.getVitalityScore());
        return item;
    }

    private BigDecimal averageDecimal(List<Map<String, Object>> records, String field) {
        if (CollectionUtils.isEmpty(records)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = records.stream()
                .map(item -> (BigDecimal) item.get(field))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
    }

    private List<String> splitCsv(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return new ArrayList<>();
        }
        return Arrays.stream(rawValue.split("[,，]"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private String resolveTaskTypeLabel(String taskType) {
        if ("LEARN".equals(taskType)) {
            return "学习时长";
        }
        if ("PRACTICE".equals(taskType)) {
            return "专项刷题";
        }
        if ("EXAM".equals(taskType)) {
            return "模考检验";
        }
        return "个性化测卷";
    }

    private String resolveMasteryLevelLabel(String masteryLevel) {
        if ("MASTERED".equals(masteryLevel)) {
            return "掌握稳定";
        }
        if ("IMPROVING".equals(masteryLevel)) {
            return "持续提升";
        }
        if ("FOUNDATION".equals(masteryLevel)) {
            return "基础建立";
        }
        return "重点补强";
    }

    private Map<String, Object> buildStatsOverview(List<CourseOrder> paidOrders) {
        Map<String, Object> overview = new HashMap<>();
        overview.put("paidOrderCount", paidOrders.size());
        overview.put("salesAmount", paidOrders.stream().map(CourseOrder::getTotalAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        overview.put("userCount", sysUserMapper.selectCount(null));
        overview.put("practiceCount", practiceRecordMapper.selectCount(new LambdaQueryWrapper<PracticeRecord>().eq(PracticeRecord::getStatus, "SUBMITTED")));
        return overview;
    }

    private List<Map<String, Object>> buildSalesTrend(List<CourseOrder> paidOrders) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime start = LocalDateTime.now().toLocalDate().minusDays(i).atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            Map<String, Object> item = new HashMap<>();
            item.put("date", start.toLocalDate().toString());
            item.put("orderCount", paidOrders.stream().filter(order -> between(order.getPaidAt(), start, end)).count());
            item.put("salesAmount", paidOrders.stream()
                    .filter(order -> between(order.getPaidAt(), start, end))
                    .map(CourseOrder::getTotalAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> buildOrderStatusStats() {
        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(buildStatusStat("PENDING", courseOrderMapper.selectCount(new LambdaQueryWrapper<CourseOrder>().eq(CourseOrder::getOrderStatus, "PENDING"))));
        stats.add(buildStatusStat("PAID", courseOrderMapper.selectCount(new LambdaQueryWrapper<CourseOrder>().eq(CourseOrder::getOrderStatus, "PAID"))));
        stats.add(buildStatusStat("CANCELLED", courseOrderMapper.selectCount(new LambdaQueryWrapper<CourseOrder>().eq(CourseOrder::getOrderStatus, "CANCELLED"))));
        return stats;
    }

    private List<Map<String, Object>> buildSubjectSales(List<CourseOrderItem> orderItems) {
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(orderItems.stream().map(CourseOrderItem::getCourseId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<String, java.math.BigDecimal> grouped = new LinkedHashMap<>();
        for (CourseOrderItem item : orderItems) {
            Course course = courseMap.get(item.getCourseId());
            String subject = course == null ? "未知科目" : course.getSubjectName();
            grouped.put(subject, grouped.getOrDefault(subject, java.math.BigDecimal.ZERO).add(item.getUnitPrice()));
        }
        return grouped.entrySet().stream().map(entry -> {
            Map<String, Object> item = new HashMap<>();
            item.put("subjectName", entry.getKey());
            item.put("salesAmount", entry.getValue());
            return item;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> buildStatusStat(String name, Long value) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        return item;
    }

    private boolean between(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }
}
