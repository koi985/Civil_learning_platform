package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.StudyPlanSaveRequest;
import com.civil.platform.module.dto.TestPaperGenerateRequest;
import com.civil.platform.module.entity.ExamAnswer;
import com.civil.platform.module.entity.ExamRecord;
import com.civil.platform.module.entity.PracticeAnswer;
import com.civil.platform.module.entity.PracticeRecord;
import com.civil.platform.module.entity.Question;
import com.civil.platform.module.entity.Classroom;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.entity.AdminGrowthFocusFollow;
import com.civil.platform.module.entity.AdminGrowthFocusFollowHistory;
import com.civil.platform.module.entity.TeacherGrowthFocusFollow;
import com.civil.platform.module.entity.TeacherGrowthFocusFollowHistory;
import com.civil.platform.module.entity.UserDailyCheckin;
import com.civil.platform.module.entity.UserCourseLearningRecord;
import com.civil.platform.module.entity.UserDailyTask;
import com.civil.platform.module.entity.UserAbilityProfile;
import com.civil.platform.module.entity.UserGrowthAchievement;
import com.civil.platform.module.entity.UserGrowthAssessmentSession;
import com.civil.platform.module.entity.UserGrowthDiagnosisReport;
import com.civil.platform.module.entity.UserKnowledgeMastery;
import com.civil.platform.module.entity.UserStudyPlan;
import com.civil.platform.module.entity.UserTestPaper;
import com.civil.platform.module.entity.UserTestPaperQuestion;
import com.civil.platform.module.entity.UserWrongQuestion;
import com.civil.platform.module.mapper.AdminGrowthFocusFollowMapper;
import com.civil.platform.module.mapper.AdminGrowthFocusFollowHistoryMapper;
import com.civil.platform.module.mapper.ClassroomMapper;
import com.civil.platform.module.mapper.ExamAnswerMapper;
import com.civil.platform.module.mapper.ExamRecordMapper;
import com.civil.platform.module.mapper.PracticeAnswerMapper;
import com.civil.platform.module.mapper.PracticeRecordMapper;
import com.civil.platform.module.mapper.QuestionMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.TeacherGrowthFocusFollowMapper;
import com.civil.platform.module.mapper.TeacherGrowthFocusFollowHistoryMapper;
import com.civil.platform.module.mapper.UserDailyCheckinMapper;
import com.civil.platform.module.mapper.UserCourseLearningRecordMapper;
import com.civil.platform.module.mapper.UserDailyTaskMapper;
import com.civil.platform.module.mapper.UserAbilityProfileMapper;
import com.civil.platform.module.mapper.UserGrowthAchievementMapper;
import com.civil.platform.module.mapper.UserGrowthAssessmentSessionMapper;
import com.civil.platform.module.mapper.UserGrowthDiagnosisReportMapper;
import com.civil.platform.module.mapper.UserKnowledgeMasteryMapper;
import com.civil.platform.module.mapper.UserStudyPlanMapper;
import com.civil.platform.module.mapper.UserTestPaperMapper;
import com.civil.platform.module.mapper.UserTestPaperQuestionMapper;
import com.civil.platform.module.mapper.UserWrongQuestionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserGrowthService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final int KNOWLEDGE_RECENT_WINDOW = 20;
    private static final int DEFAULT_EXCLUDE_RECENT_DAYS = 7;
    private static final String PAPER_SOURCE_STRATEGY = "RULE_ENHANCED_V1";
    private static final String GROWTH_PREDICTION_FORMULA_VERSION = "growth-pass-v2";
    private static final int ENTRY_ASSESSMENT_QUESTION_COUNT = 20;
    private static final BigDecimal WEIGHT_PLAN_COMPLETION = BigDecimal.valueOf(0.25D);
    private static final BigDecimal WEIGHT_KNOWLEDGE_MASTERY = BigDecimal.valueOf(0.30D);
    private static final BigDecimal WEIGHT_TEST_PAPER = BigDecimal.valueOf(0.20D);
    private static final BigDecimal WEIGHT_ENTRY_ASSESSMENT = BigDecimal.valueOf(0.25D);
    private static final BigDecimal ABILITY_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(60);
    private static final BigDecimal MAX_ABILITY_CALIBRATION_DELTA = BigDecimal.valueOf(5);

    private static final class KnowledgeAnswerEvent {
        private LocalDateTime occurredAt;
        private boolean correct;
        private String sourceType;
        private String practiceType;
        private Long sessionId;
    }

    private static final class KnowledgePointComputation {
        private UserKnowledgeMastery mastery;
        private List<KnowledgeAnswerEvent> answerEvents = new ArrayList<>();
        private Set<Long> sessionIds = new LinkedHashSet<>();
        private Set<String> sourceTypes = new LinkedHashSet<>();
        private int practiceAttemptCount;
        private int examAttemptCount;
        private int testPaperAttemptCount;
        private int wrongBookAttemptCount;
        private int pendingWrongCount;
        private int recentWrongPressure;
        private boolean planFocused;
    }

    private static final class GrowthScoringSnapshot {
        private BigDecimal completionRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal masteryRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal testPaperAverage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal assessmentReference = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal basePredictedPassRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal abilityCalibrationDelta = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal predictedPassRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal predictionConfidence = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal abilityScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal abilityConfidence = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private boolean assessmentFallback;
        private boolean abilityCalibrationApplied;
        private LocalDateTime abilityCalibratedAt;
        private String formulaVersion = GROWTH_PREDICTION_FORMULA_VERSION;
    }

    private final UserStudyPlanMapper userStudyPlanMapper;
    private final UserDailyTaskMapper userDailyTaskMapper;
    private final UserGrowthAchievementMapper userGrowthAchievementMapper;
    private final UserGrowthAssessmentSessionMapper userGrowthAssessmentSessionMapper;
    private final UserGrowthDiagnosisReportMapper userGrowthDiagnosisReportMapper;
    private final UserKnowledgeMasteryMapper userKnowledgeMasteryMapper;
    private final UserAbilityProfileMapper userAbilityProfileMapper;
    private final UserTestPaperMapper userTestPaperMapper;
    private final UserTestPaperQuestionMapper userTestPaperQuestionMapper;
    private final UserCourseLearningRecordMapper userCourseLearningRecordMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeAnswerMapper practiceAnswerMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final QuestionMapper questionMapper;
    private final UserWrongQuestionMapper userWrongQuestionMapper;
    private final SysUserMapper sysUserMapper;
    private final AdminGrowthFocusFollowMapper adminGrowthFocusFollowMapper;
    private final AdminGrowthFocusFollowHistoryMapper adminGrowthFocusFollowHistoryMapper;
    private final TeacherGrowthFocusFollowMapper teacherGrowthFocusFollowMapper;
    private final TeacherGrowthFocusFollowHistoryMapper teacherGrowthFocusFollowHistoryMapper;
    private final ClassroomMapper classroomMapper;
    private final UserDailyCheckinMapper userDailyCheckinMapper;

    public UserGrowthService(UserStudyPlanMapper userStudyPlanMapper,
                             UserDailyTaskMapper userDailyTaskMapper,
                             UserGrowthAchievementMapper userGrowthAchievementMapper,
                             UserGrowthAssessmentSessionMapper userGrowthAssessmentSessionMapper,
                             UserGrowthDiagnosisReportMapper userGrowthDiagnosisReportMapper,
                             UserKnowledgeMasteryMapper userKnowledgeMasteryMapper,
                             UserAbilityProfileMapper userAbilityProfileMapper,
                             UserTestPaperMapper userTestPaperMapper,
                             UserTestPaperQuestionMapper userTestPaperQuestionMapper,
                             UserCourseLearningRecordMapper userCourseLearningRecordMapper,
                             PracticeRecordMapper practiceRecordMapper,
                             PracticeAnswerMapper practiceAnswerMapper,
                             ExamRecordMapper examRecordMapper,
                             ExamAnswerMapper examAnswerMapper,
                             QuestionMapper questionMapper,
                             UserWrongQuestionMapper userWrongQuestionMapper,
                             SysUserMapper sysUserMapper,
                             AdminGrowthFocusFollowMapper adminGrowthFocusFollowMapper,
                             AdminGrowthFocusFollowHistoryMapper adminGrowthFocusFollowHistoryMapper,
                             TeacherGrowthFocusFollowMapper teacherGrowthFocusFollowMapper,
                             TeacherGrowthFocusFollowHistoryMapper teacherGrowthFocusFollowHistoryMapper,
                             ClassroomMapper classroomMapper,
                             UserDailyCheckinMapper userDailyCheckinMapper) {
        this.userStudyPlanMapper = userStudyPlanMapper;
        this.userDailyTaskMapper = userDailyTaskMapper;
        this.userGrowthAchievementMapper = userGrowthAchievementMapper;
        this.userGrowthAssessmentSessionMapper = userGrowthAssessmentSessionMapper;
        this.userGrowthDiagnosisReportMapper = userGrowthDiagnosisReportMapper;
        this.userKnowledgeMasteryMapper = userKnowledgeMasteryMapper;
        this.userAbilityProfileMapper = userAbilityProfileMapper;
        this.userTestPaperMapper = userTestPaperMapper;
        this.userTestPaperQuestionMapper = userTestPaperQuestionMapper;
        this.userCourseLearningRecordMapper = userCourseLearningRecordMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.practiceAnswerMapper = practiceAnswerMapper;
        this.examRecordMapper = examRecordMapper;
        this.examAnswerMapper = examAnswerMapper;
        this.questionMapper = questionMapper;
        this.userWrongQuestionMapper = userWrongQuestionMapper;
        this.sysUserMapper = sysUserMapper;
        this.adminGrowthFocusFollowMapper = adminGrowthFocusFollowMapper;
        this.adminGrowthFocusFollowHistoryMapper = adminGrowthFocusFollowHistoryMapper;
        this.teacherGrowthFocusFollowMapper = teacherGrowthFocusFollowMapper;
        this.teacherGrowthFocusFollowHistoryMapper = teacherGrowthFocusFollowHistoryMapper;
        this.classroomMapper = classroomMapper;
        this.userDailyCheckinMapper = userDailyCheckinMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> savePlan(StudyPlanSaveRequest request) {
        Long userId = SecurityUtils.getUserId();
        UserStudyPlan currentPlan = loadCurrentPlan(userId);
        if (currentPlan != null) {
            archivePlan(currentPlan);
        }
        UserStudyPlan plan = new UserStudyPlan();
        plan.setUserId(userId);
        LocalDate startDate = request.getStartDate() == null ? LocalDate.now() : request.getStartDate();
        int durationDays = request.getDurationDays() == null ? 30 : request.getDurationDays();
        plan.setPlanName(buildPlanName(request.getTargetExamType(), request.getSubjectName()));
        plan.setTargetExamType(request.getTargetExamType());
        plan.setSubjectName(request.getSubjectName());
        plan.setDailyStudyMinutes(request.getDailyStudyMinutes());
        plan.setDailyQuestionCount(request.getDailyQuestionCount());
        plan.setWeeklyExamCount(request.getWeeklyExamCount());
        plan.setDurationDays(durationDays);
        plan.setStartDate(startDate);
        plan.setEndDate(startDate.plusDays(Math.max(durationDays - 1, 0)));
        plan.setFocusKnowledgePoints(normalizeCsv(request.getFocusKnowledgePoints()));
        plan.setStatus("ACTIVE");
        plan.setArchivedAt(null);
        plan.setTotalTaskCount(0);
        plan.setCompletedTaskCount(0);
        plan.setCompletionRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        userStudyPlanMapper.insert(plan);
        regeneratePlanTasks(plan);
        List<UserKnowledgeMastery> masteryList = refreshKnowledgeMastery(userId, plan);

        Map<String, Object> result = new HashMap<>();
        result.put("planId", plan.getId());
        result.put("planName", plan.getPlanName());
        result.put("durationDays", plan.getDurationDays());
        result.put("taskCount", userDailyTaskMapper.selectCount(new LambdaQueryWrapper<UserDailyTask>()
                .eq(UserDailyTask::getPlanId, plan.getId())));
        result.put("focusKnowledgeCount", splitKnowledgePoints(plan.getFocusKnowledgePoints()).size());
        result.put("weakKnowledgeCount", masteryList.stream().filter(item -> "WEAK".equals(item.getMasteryLevel())).count());
        return result;
    }

    public Map<String, Object> currentPlan() {
        Long userId = SecurityUtils.getUserId();
        UserStudyPlan plan = loadCurrentPlan(userId);
        Map<String, Object> result = new HashMap<>();
        if (plan == null) {
            result.put("plan", null);
            result.put("summary", buildEmptyPlanSummary());
            result.put("todayTasks", new ArrayList<>());
            result.put("upcomingTasks", new ArrayList<>());
            result.put("weakKnowledgePoints", new ArrayList<>());
            return result;
        }

        ensurePlanTasks(plan);
        List<UserDailyTask> allTasks = loadPlanTasks(plan.getId());
        refreshTaskProgress(userId, allTasks);
        allTasks = loadPlanTasks(plan.getId());
        List<UserKnowledgeMastery> masteryList = refreshKnowledgeMastery(userId, plan);

        result.put("plan", toPlanMap(plan));
        result.put("summary", buildPlanSummary(plan, allTasks));
        result.put("todayTasks", allTasks.stream()
                .filter(item -> LocalDate.now().equals(item.getTaskDate()))
                .sorted(Comparator.comparing(UserDailyTask::getTaskType))
                .map(this::toTaskMap)
                .collect(Collectors.toList()));
        result.put("upcomingTasks", allTasks.stream()
                .filter(item -> !item.getTaskDate().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(UserDailyTask::getTaskDate).thenComparing(UserDailyTask::getTaskType))
                .limit(8)
                .map(this::toTaskMap)
                .collect(Collectors.toList()));
        result.put("weakKnowledgePoints", masteryList.stream()
                .sorted(Comparator.comparing(UserKnowledgeMastery::getPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UserKnowledgeMastery::getMasteryScore, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(4)
                .map(this::toKnowledgeMap)
                .collect(Collectors.toList()));
        result.put("achievementSummary", buildAchievementSummary(userId));
        return result;
    }

    public Map<String, Object> growthArchive() {
        Long userId = SecurityUtils.getUserId();
        UserStudyPlan currentPlan = loadCurrentPlan(userId);

        Map<String, Object> result = new HashMap<>();
        if (currentPlan == null) {
            result.put("currentPlan", null);
            result.put("currentPlanSummary", buildEmptyPlanSummary());
        } else {
            ensurePlanTasks(currentPlan);
            List<UserDailyTask> currentTasks = loadPlanTasks(currentPlan.getId());
            refreshTaskProgress(userId, currentTasks);
            currentTasks = loadPlanTasks(currentPlan.getId());
            result.put("currentPlan", toPlanMap(currentPlan));
            result.put("currentPlanSummary", buildPlanSummary(currentPlan, currentTasks));
        }

        List<UserStudyPlan> historyPlans = userStudyPlanMapper.selectList(new LambdaQueryWrapper<UserStudyPlan>()
                .eq(UserStudyPlan::getUserId, userId)
                .ne(UserStudyPlan::getStatus, "ACTIVE")
                .orderByDesc(UserStudyPlan::getArchivedAt)
                .orderByDesc(UserStudyPlan::getId));
        result.put("planHistory", historyPlans.stream()
                .limit(6)
                .map(this::toPlanHistoryMap)
                .collect(Collectors.toList()));

        List<UserGrowthAchievement> achievements = userGrowthAchievementMapper.selectList(new LambdaQueryWrapper<UserGrowthAchievement>()
                .eq(UserGrowthAchievement::getUserId, userId)
                .orderByDesc(UserGrowthAchievement::getCreatedAt)
                .orderByDesc(UserGrowthAchievement::getId));
        result.put("achievementSummary", buildAchievementSummary(userId));
        result.put("recentAchievements", achievements.stream()
                .limit(8)
                .map(this::toAchievementMap)
                .collect(Collectors.toList()));

        @SuppressWarnings("unchecked")
        PageResult<Map<String, Object>> reportPage = (PageResult<Map<String, Object>>) testPapers(1L, 100L, null, "COMPLETED").get("page");
        List<Map<String, Object>> completedReports = reportPage == null ? new ArrayList<>() : reportPage.getRecords();
        result.put("testPaperHighlights", completedReports.stream()
                .limit(6)
                .collect(Collectors.toList()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> growthDiagnosis(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getUserId();
        UserStudyPlan plan = loadCurrentPlan(userId);
        Map<String, Object> result = new HashMap<>();
        if (plan == null) {
            result.put("currentPlan", null);
            result.put("latestDiagnosis", null);
            result.put("trend", new ArrayList<>());
            result.put("diagnosisHistory", new ArrayList<>());
            result.put("historyComparison", buildEmptyDiagnosisComparison());
            result.put("focusKnowledgePoints", new ArrayList<>());
            result.put("actionSuggestions", new ArrayList<>());
            result.put("testPaperReports", new ArrayList<>());
            result.put("teacherFollowUps", new ArrayList<>());
            result.put("adminFollowUp", null);
            result.put("teacherFollowTimeline", new ArrayList<>());
            result.put("adminFollowTimeline", new ArrayList<>());
            result.put("collaborationTimeline", new ArrayList<>());
            result.put("collaborationSummary", buildEmptyCollaborationSummary());
            result.put("stageSummary", buildEmptyDiagnosisStageSummary());
            result.put("assessmentSummary", buildAssessmentSummary(new ArrayList<>()));
            result.put("assessmentPrediction", buildAssessmentPrediction(buildEmptyGrowthScoringSnapshot(), new ArrayList<>()));
            result.put("assessmentEntry", buildAssessmentEntry(null, new ArrayList<>()));
            result.put("assessmentHistory", new ArrayList<>());
            result.put("filters", buildDiagnosisFilterState(startDate, endDate));
            return result;
        }

        ensurePlanTasks(plan);
        List<UserDailyTask> tasks = loadPlanTasks(plan.getId());
        refreshTaskProgress(userId, tasks);
        tasks = loadPlanTasks(plan.getId());
        List<UserKnowledgeMastery> masteryList = refreshKnowledgeMastery(userId, plan);
        List<Map<String, Object>> completedReports = loadCompletedTestPaperReports(userId, null);
        List<Map<String, Object>> assessmentHistory = loadAssessmentSessions(userId, plan.getId(), startDate, endDate);
        GrowthScoringSnapshot scoringSnapshot = buildGrowthScoringSnapshot(userId, plan, tasks, masteryList, completedReports, assessmentHistory);

        UserGrowthDiagnosisReport latestDiagnosis = saveDiagnosisSnapshot(userId, plan, scoringSnapshot, masteryList);
        List<UserGrowthDiagnosisReport> allDiagnosisHistory = userGrowthDiagnosisReportMapper.selectList(new LambdaQueryWrapper<UserGrowthDiagnosisReport>()
                .eq(UserGrowthDiagnosisReport::getUserId, userId)
                .orderByDesc(UserGrowthDiagnosisReport::getDiagnosisDate)
                .orderByDesc(UserGrowthDiagnosisReport::getId));
        List<UserGrowthDiagnosisReport> diagnosisHistory = allDiagnosisHistory.stream()
                .filter(item -> withinDateRange(item.getDiagnosisDate(), startDate, endDate))
                .collect(Collectors.toList());
        Collections.reverse(diagnosisHistory);
        List<Map<String, Object>> filteredReports = completedReports.stream()
                .filter(item -> withinDateRange(toLocalDate(item.get("latestSubmittedAt")), startDate, endDate))
                .collect(Collectors.toList());

        result.put("currentPlan", toPlanMap(plan));
        result.put("latestDiagnosis", toDiagnosisMap(latestDiagnosis));
        result.put("stageSummary", buildDiagnosisStageSummary(tasks, masteryList, completedReports));
        result.put("trend", diagnosisHistory.stream()
                .limit(7)
                .map(this::toDiagnosisTrendMap)
                .collect(Collectors.toList()));
        result.put("diagnosisHistory", diagnosisHistory.stream()
                .sorted(Comparator.comparing(UserGrowthDiagnosisReport::getDiagnosisDate).reversed())
                .limit(6)
                .map(this::toDiagnosisMap)
                .collect(Collectors.toList()));
        result.put("historyComparison", buildDiagnosisComparison(latestDiagnosis, diagnosisHistory.isEmpty() ? allDiagnosisHistory : diagnosisHistory));
        result.put("focusKnowledgePoints", masteryList.stream()
                .sorted(Comparator.comparing(UserKnowledgeMastery::getPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UserKnowledgeMastery::getMasteryScore, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(6)
                .map(this::toKnowledgeMap)
                .collect(Collectors.toList()));
        result.put("testPaperReports", filteredReports.stream().limit(8).collect(Collectors.toList()));
        List<Map<String, Object>> teacherFollowUps = loadTeacherFollowUps(userId, plan.getId());
        Map<String, Object> adminFollowUp = loadAdminFollowUp(userId, plan.getId());
        List<Map<String, Object>> teacherFollowTimeline = loadTeacherFollowTimeline(userId, plan.getId()).stream()
                .filter(item -> withinDateRange(toLocalDate(item.get("createdAt")), startDate, endDate))
                .collect(Collectors.toList());
        List<Map<String, Object>> adminFollowTimeline = loadAdminFollowTimeline(userId, plan.getId()).stream()
                .filter(item -> withinDateRange(toLocalDate(item.get("createdAt")), startDate, endDate))
                .collect(Collectors.toList());
        result.put("teacherFollowUps", teacherFollowUps);
        result.put("adminFollowUp", adminFollowUp);
        result.put("teacherFollowTimeline", teacherFollowTimeline);
        result.put("adminFollowTimeline", adminFollowTimeline);
        result.put("collaborationTimeline", buildCollaborationTimeline(teacherFollowTimeline, adminFollowTimeline));
        result.put("collaborationSummary", buildCollaborationSummary(teacherFollowUps, adminFollowUp, teacherFollowTimeline, adminFollowTimeline));
        result.put("assessmentSummary", buildAssessmentSummary(assessmentHistory));
        result.put("assessmentPrediction", buildAssessmentPrediction(scoringSnapshot, assessmentHistory));
        result.put("assessmentEntry", buildAssessmentEntry(plan, assessmentHistory));
        result.put("assessmentHistory", assessmentHistory);
        result.put("actionSuggestions", buildDiagnosisActions(plan, tasks, masteryList, completedReports, latestDiagnosis, assessmentHistory));
        result.put("filters", buildDiagnosisFilterState(startDate, endDate));
        // G1: 暴露 IRT 校准就绪信号，前端据此显示样本不足提示
        long submittedPracticeCount = practiceRecordMapper.selectCount(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getStatus, "SUBMITTED"));
        long distinctMasteryKnowledge = masteryList.stream()
                .map(UserKnowledgeMastery::getKnowledgePoint)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        Map<String, Object> calibrationStatus = new HashMap<>();
        boolean ready = submittedPracticeCount >= 5 && distinctMasteryKnowledge >= 5;
        calibrationStatus.put("ready", ready);
        calibrationStatus.put("submittedPracticeCount", submittedPracticeCount);
        calibrationStatus.put("knowledgePointCount", distinctMasteryKnowledge);
        calibrationStatus.put("hint", ready ? "" : "练习样本不足，能力诊断仅供参考，建议完成 5 次以上专项练习以获得更精准评估。");
        result.put("calibrationStatus", calibrationStatus);
        return result;
    }

    public Map<String, Object> growthAssessment(LocalDate startDate, LocalDate endDate) {
        return growthDiagnosis(startDate, endDate);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startAssessment() {
        Long userId = SecurityUtils.getUserId();
        UserStudyPlan plan = loadCurrentPlan(userId);
        List<UserKnowledgeMastery> masteryList = refreshKnowledgeMastery(userId, plan);
        List<UserDailyTask> tasks = new ArrayList<>();
        if (plan != null) {
            ensurePlanTasks(plan);
            tasks = loadPlanTasks(plan.getId());
            refreshTaskProgress(userId, tasks);
            tasks = loadPlanTasks(plan.getId());
        }
        List<Map<String, Object>> completedReports = loadCompletedTestPaperReports(userId, null);
        List<Map<String, Object>> assessmentHistory = plan == null
                ? new ArrayList<>()
                : loadAssessmentSessions(userId, plan.getId(), null, null);
        GrowthScoringSnapshot scoringSnapshot = buildGrowthScoringSnapshot(userId, plan, tasks, masteryList, completedReports, assessmentHistory);
        SysUser user = sysUserMapper.selectById(userId);
        String subjectName = plan != null && StringUtils.hasText(plan.getSubjectName())
                ? plan.getSubjectName()
                : user == null ? null : user.getPreferredSubject();
        List<Question> selectedQuestions = resolveAssessmentQuestions(subjectName, plan, masteryList);
        if (selectedQuestions.size() < ENTRY_ASSESSMENT_QUESTION_COUNT) {
            throw new BizException("当前可用于入学评测的客观题不足 " + ENTRY_ASSESSMENT_QUESTION_COUNT + " 题，请先补充题库后再开始评测");
        }

        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setPracticeType("GROWTH_ASSESSMENT");
        record.setPracticeMode("ASSESSMENT");
        record.setTitle("独立入学评测（" + selectedQuestions.size() + "题）");
        record.setSubjectName(StringUtils.hasText(subjectName) ? subjectName : resolveSubject(selectedQuestions));
        record.setProvince(resolveProvince(selectedQuestions));
        record.setQuestionCount(selectedQuestions.size());
        record.setAnsweredCount(0);
        record.setCorrectCount(0);
        record.setScore(0);
        record.setAccuracyRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        record.setDurationSeconds(0);
        record.setStatus("IN_PROGRESS");
        record.setSourceSnapshot("growth-assessment");
        practiceRecordMapper.insert(record);

        for (Question question : selectedQuestions) {
            PracticeAnswer answer = new PracticeAnswer();
            answer.setPracticeRecordId(record.getId());
            answer.setQuestionId(question.getId());
            answer.setCorrectAnswer(question.getAnswerValue());
            answer.setIsCorrect(false);
            answer.setMarked(false);
            answer.setSpentSeconds(0);
            answer.setQuestionScore(1);
            practiceAnswerMapper.insert(answer);
        }

        UserGrowthAssessmentSession session = new UserGrowthAssessmentSession();
        session.setUserId(userId);
        session.setPlanId(plan == null ? null : plan.getId());
        session.setPracticeRecordId(record.getId());
        session.setAssessmentType("ENTRY");
        session.setQuestionCount(selectedQuestions.size());
        session.setPredictedPassRate(scoringSnapshot.predictedPassRate);
        session.setPredictedConfidence(scoringSnapshot.predictionConfidence);
        session.setPredictionStage(resolveAssessmentPredictionStage(session.getPredictedPassRate()));
        session.setWeakKnowledgePoints(joinKnowledgePoints(resolveAssessmentFocusKnowledgePoints(plan, masteryList)));
        session.setRecommendationText(buildAssessmentRecommendation(scoringSnapshot));
        session.setAssessmentStatus("IN_PROGRESS");
        userGrowthAssessmentSessionMapper.insert(session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("practiceId", record.getId());
        result.put("questionCount", record.getQuestionCount());
        result.put("predictionStageLabel", resolveAssessmentPredictionStageLabel(session.getPredictionStage()));
        result.put("predictedPassRate", session.getPredictedPassRate());
        result.put("predictedConfidence", session.getPredictedConfidence());
        result.put("reportPath", "/practice/report/" + record.getId());
        return result;
    }

    public byte[] exportGrowthDiagnosis(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> diagnosis = growthDiagnosis(startDate, endDate);
        List<String> headers = new ArrayList<>();
        headers.add("诊断日期");
        headers.add("阶段判定");
        headers.add("成长活力");
        headers.add("计划完成率");
        headers.add("知识掌握度");
        headers.add("测卷平均准确率");
        headers.add("预测通过率");
        headers.add("预测置信度");
        headers.add("最近评测准确率");
        headers.add("评测次数");
        headers.add("薄弱知识点");
        headers.add("诊断建议");
        headers.add("教师最近跟进");
        headers.add("后台最近跟进");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) diagnosis.get("diagnosisHistory");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> collaborationTimeline = (List<Map<String, Object>>) diagnosis.get("collaborationTimeline");
        @SuppressWarnings("unchecked")
        Map<String, Object> assessmentPrediction = (Map<String, Object>) diagnosis.get("assessmentPrediction");
        @SuppressWarnings("unchecked")
        Map<String, Object> assessmentSummary = (Map<String, Object>) diagnosis.get("assessmentSummary");
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> item : history) {
            List<Object> row = new ArrayList<>();
            row.add(item.get("diagnosisDate"));
            row.add(item.get("diagnosisStageLabel"));
            row.add(item.get("vitalityScore"));
            row.add(item.get("planCompletionRate"));
            row.add(item.get("knowledgeMasteryRate"));
            row.add(item.get("testPaperAverageScore"));
            row.add(item.get("predictedPassRate") == null ? (assessmentPrediction == null ? "" : assessmentPrediction.get("predictedPassRate")) : item.get("predictedPassRate"));
            row.add(item.get("predictionConfidence") == null ? (assessmentPrediction == null ? "" : assessmentPrediction.get("predictionConfidence")) : item.get("predictionConfidence"));
            row.add(assessmentSummary == null ? "" : assessmentSummary.get("latestAssessmentAccuracy"));
            row.add(assessmentSummary == null ? "" : assessmentSummary.get("sessionCount"));
            @SuppressWarnings("unchecked")
            List<String> weakKnowledgePoints = (List<String>) item.get("weakKnowledgePoints");
            row.add(CollectionUtils.isEmpty(weakKnowledgePoints) ? "" : String.join(" / ", weakKnowledgePoints));
            row.add(item.get("recommendationText"));
            row.add(resolveLatestFollowRemark(collaborationTimeline, "TEACHER"));
            row.add(resolveLatestFollowRemark(collaborationTimeline, "ADMIN"));
            rows.add(row);
        }
        return com.civil.platform.common.util.ExcelUtils.buildWorkbook("成长诊断", headers, rows);
    }

    public Map<String, Object> tasks(Long pageNo,
                                     Long pageSize,
                                     LocalDate taskDate,
                                     String status,
                                     String taskType) {
        Long userId = SecurityUtils.getUserId();
        UserStudyPlan plan = loadCurrentPlan(userId);
        if (plan == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("summary", buildTaskSummary(new ArrayList<>()));
            empty.put("page", new PageResult<>(0L, pageNo, pageSize, new ArrayList<>()));
            return empty;
        }
        ensurePlanTasks(plan);
        refreshTaskProgress(userId, loadPlanTasks(plan.getId()));

        Page<UserDailyTask> page = userDailyTaskMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<UserDailyTask>()
                .eq(UserDailyTask::getPlanId, plan.getId())
                .eq(taskDate != null, UserDailyTask::getTaskDate, taskDate)
                .eq(StringUtils.hasText(status), UserDailyTask::getStatus, status)
                .eq(StringUtils.hasText(taskType), UserDailyTask::getTaskType, taskType)
                .orderByAsc(UserDailyTask::getTaskDate)
                .orderByAsc(UserDailyTask::getId));
        List<UserDailyTask> allTasks = userDailyTaskMapper.selectList(new LambdaQueryWrapper<UserDailyTask>()
                .eq(UserDailyTask::getPlanId, plan.getId())
                .orderByAsc(UserDailyTask::getTaskDate)
                .orderByAsc(UserDailyTask::getId));

        Map<String, Object> result = new HashMap<>();
        result.put("summary", buildTaskSummary(allTasks));
        result.put("page", new PageResult<>(page.getTotal(), pageNo, pageSize,
                page.getRecords().stream().map(this::toTaskMap).collect(Collectors.toList())));
        return result;
    }

    public Map<String, Object> knowledgeMastery(Long pageNo,
                                                Long pageSize,
                                                String subjectName,
                                                String keyword,
                                                String masteryLevel) {
        Long userId = SecurityUtils.getUserId();
        List<UserKnowledgeMastery> allRecords = refreshKnowledgeMastery(userId, loadCurrentPlan(userId));
        Page<UserKnowledgeMastery> page = userKnowledgeMasteryMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<UserKnowledgeMastery>()
                        .eq(UserKnowledgeMastery::getUserId, userId)
                        .eq(StringUtils.hasText(subjectName), UserKnowledgeMastery::getSubjectName, subjectName)
                        .like(StringUtils.hasText(keyword), UserKnowledgeMastery::getKnowledgePoint, keyword)
                        .eq(StringUtils.hasText(masteryLevel) && !"TO_DIAGNOSE".equals(masteryLevel), UserKnowledgeMastery::getMasteryLevel, masteryLevel)
                        .eq("TO_DIAGNOSE".equals(masteryLevel), UserKnowledgeMastery::getKnowledgeStatus, masteryLevel)
                        .orderByDesc(UserKnowledgeMastery::getPriorityScore)
                        .orderByAsc(UserKnowledgeMastery::getMasteryConfidence)
                        .orderByAsc(UserKnowledgeMastery::getMasteryScore)
                        .orderByDesc(UserKnowledgeMastery::getUpdatedAt));

        Map<String, Object> result = new HashMap<>();
        result.put("summary", buildKnowledgeSummary(allRecords));
        result.put("page", new PageResult<>(page.getTotal(), pageNo, pageSize,
                page.getRecords().stream().map(this::toKnowledgeMap).collect(Collectors.toList())));
        return result;
    }

    public Map<String, Object> testPapers(Long pageNo,
                                          Long pageSize,
                                          String subjectName,
                                          String status) {
        Long userId = SecurityUtils.getUserId();
        List<UserTestPaper> papers = userTestPaperMapper.selectList(new LambdaQueryWrapper<UserTestPaper>()
                .eq(UserTestPaper::getUserId, userId)
                .orderByDesc(UserTestPaper::getCreatedAt)
                .orderByDesc(UserTestPaper::getId));
        List<PracticeRecord> testPaperPractices = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getPracticeType, "TEST_PAPER")
                .orderByDesc(PracticeRecord::getCreatedAt));
        Map<Long, List<PracticeRecord>> practiceMap = testPaperPractices.stream()
                .filter(item -> parseTestPaperId(item.getSourceSnapshot()) != null)
                .collect(Collectors.groupingBy(item -> parseTestPaperId(item.getSourceSnapshot())));

        List<Map<String, Object>> records = papers.stream()
                .filter(item -> !StringUtils.hasText(subjectName) || subjectName.equals(item.getSubjectName()))
                .map(item -> toTestPaperMap(item, practiceMap.getOrDefault(item.getId(), Collections.emptyList())))
                .filter(item -> !StringUtils.hasText(status) || status.equals(item.get("status")))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("summary", buildTestPaperSummary(records));
        result.put("page", manualPage(records, pageNo, pageSize));
        return result;
    }

    public Map<String, Object> testPaperReports(Long pageNo,
                                                Long pageSize,
                                                String subjectName) {
        Long userId = SecurityUtils.getUserId();
        List<Map<String, Object>> reports = loadCompletedTestPaperReports(userId, subjectName);
        Map<String, Object> result = new HashMap<>();
        result.put("summary", buildTestPaperReportSummary(reports));
        result.put("page", manualPage(reports, pageNo, pageSize));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateTestPaper(TestPaperGenerateRequest request) {
        Long userId = SecurityUtils.getUserId();
        UserStudyPlan plan = loadCurrentPlan(userId);
        List<UserKnowledgeMastery> masteryList = refreshKnowledgeMastery(userId, plan);
        SysUser user = sysUserMapper.selectById(userId);

        String resolvedSubject = StringUtils.hasText(request.getSubjectName())
                ? request.getSubjectName()
                : (plan != null && StringUtils.hasText(plan.getSubjectName()) ? plan.getSubjectName() : user.getPreferredSubject());
        boolean onlyWrong = Boolean.TRUE.equals(request.getOnlyWrong());
        String generateMode = StringUtils.hasText(request.getGenerateMode()) ? request.getGenerateMode() : (onlyWrong ? "WRONG_BOOK" : "WEAKNESS");
        String paperType = resolvePaperType(request, masteryList);
        int questionCount = resolveQuestionCount(request.getQuestionCount(), paperType);
        int excludeRecentDays = request.getExcludeRecentDays() == null ? DEFAULT_EXCLUDE_RECENT_DAYS : Math.max(0, request.getExcludeRecentDays());

        List<Question> baseQuestions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true)
                .eq(StringUtils.hasText(resolvedSubject), Question::getSubjectName, resolvedSubject)
                .eq(StringUtils.hasText(request.getCategoryName()), Question::getCategoryName, request.getCategoryName())
                .eq(StringUtils.hasText(request.getDifficultyLevel()), Question::getDifficultyLevel, request.getDifficultyLevel())
                .orderByDesc(Question::getCreatedAt));
        if (CollectionUtils.isEmpty(baseQuestions)) {
            throw new BizException("当前条件下暂无可生成的题目");
        }

        Set<Long> wrongQuestionIds = userWrongQuestionMapper.selectList(new LambdaQueryWrapper<UserWrongQuestion>()
                        .eq(UserWrongQuestion::getUserId, userId)
                        .eq(UserWrongQuestion::getRemoved, false))
                .stream()
                .map(UserWrongQuestion::getQuestionId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (onlyWrong && wrongQuestionIds.isEmpty()) {
            throw new BizException("当前没有可生成错题强化测卷的题目");
        }

        Set<Long> recentQuestionIds = loadRecentQuestionIds(userId, excludeRecentDays);
        List<String> focusKnowledgePoints = resolveTargetKnowledgePoints(request, plan, masteryList, baseQuestions, paperType);
        BigDecimal subjectMasteryAverage = resolveSubjectMasteryAverage(masteryList, resolvedSubject, focusKnowledgePoints);
        String difficultyStrategy = resolveDifficultyStrategy(request.getDifficultyStrategy(), subjectMasteryAverage);
        List<Question> selected = selectQuestionsForPaper(baseQuestions,
                focusKnowledgePoints,
                masteryList,
                wrongQuestionIds,
                recentQuestionIds,
                questionCount,
                onlyWrong,
                paperType,
                difficultyStrategy);
        if (CollectionUtils.isEmpty(selected)) {
            throw new BizException("当前筛选条件下没有可生成的测卷题目");
        }
        List<String> selectedKnowledgePoints = resolveSelectedKnowledgePoints(selected, focusKnowledgePoints);
        String difficultyDistribution = buildDifficultyDistribution(selected);
        String generationSnapshot = buildGenerationSnapshot(paperType,
                difficultyStrategy,
                excludeRecentDays,
                selectedKnowledgePoints,
                selected,
                recentQuestionIds);

        UserTestPaper paper = new UserTestPaper();
        paper.setUserId(userId);
        paper.setTitle(buildTestPaperTitle(resolvedSubject, selected.size(), generateMode, paperType));
        paper.setSubjectName(resolvedSubject);
        paper.setCategoryName(request.getCategoryName());
        paper.setDifficultyLevel(request.getDifficultyLevel());
        paper.setQuestionCount(selected.size());
        paper.setGenerateMode(generateMode);
        paper.setPaperType(paperType);
        paper.setKnowledgePoints(joinKnowledgePoints(selectedKnowledgePoints));
        paper.setTargetKnowledgePoints(joinKnowledgePoints(focusKnowledgePoints));
        paper.setRecommendationReason(buildPaperReason(generateMode, paperType, onlyWrong, focusKnowledgePoints, plan));
        paper.setGenerationSnapshot(generationSnapshot);
        paper.setDifficultyDistribution(difficultyDistribution);
        paper.setSourceStrategy(PAPER_SOURCE_STRATEGY);
        userTestPaperMapper.insert(paper);

        int sortNo = 1;
        for (Question question : selected) {
            UserTestPaperQuestion relation = new UserTestPaperQuestion();
            relation.setTestPaperId(paper.getId());
            relation.setQuestionId(question.getId());
            relation.setSortNo(sortNo++);
            relation.setQuestionScore(1);
            userTestPaperQuestionMapper.insert(relation);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paperId", paper.getId());
        result.put("title", paper.getTitle());
        result.put("questionCount", paper.getQuestionCount());
        result.put("recommendationReason", paper.getRecommendationReason());
        result.put("paperType", paper.getPaperType());
        result.put("targetKnowledgePoints", splitKnowledgePoints(paper.getTargetKnowledgePoints()));
        result.put("difficultyDistribution", paper.getDifficultyDistribution());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startTestPaper(Long paperId) {
        Long userId = SecurityUtils.getUserId();
        UserTestPaper paper = userTestPaperMapper.selectById(paperId);
        if (paper == null || !userId.equals(paper.getUserId())) {
            throw new BizException(404, "个性化测卷不存在");
        }
        List<UserTestPaperQuestion> relations = userTestPaperQuestionMapper.selectList(new LambdaQueryWrapper<UserTestPaperQuestion>()
                .eq(UserTestPaperQuestion::getTestPaperId, paperId)
                .orderByAsc(UserTestPaperQuestion::getSortNo)
                .orderByAsc(UserTestPaperQuestion::getId));
        if (CollectionUtils.isEmpty(relations)) {
            throw new BizException("当前测卷暂无题目");
        }
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(relations.stream()
                        .map(UserTestPaperQuestion::getQuestionId)
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (left, right) -> left));
        List<Question> questions = relations.stream()
                .map(item -> questionMap.get(item.getQuestionId()))
                .filter(item -> item != null)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(questions)) {
            throw new BizException("测卷题目异常");
        }

        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setPracticeType("TEST_PAPER");
        record.setPracticeMode(paper.getGenerateMode());
        record.setTitle(paper.getTitle());
        record.setSubjectName(StringUtils.hasText(paper.getSubjectName()) ? paper.getSubjectName() : questions.get(0).getSubjectName());
        record.setProvince(resolveProvince(questions));
        record.setQuestionCount(questions.size());
        record.setAnsweredCount(0);
        record.setCorrectCount(0);
        record.setScore(0);
        record.setAccuracyRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        record.setDurationSeconds(0);
        record.setStatus("IN_PROGRESS");
        record.setSourceSnapshot("test-paper:" + paper.getId());
        practiceRecordMapper.insert(record);

        for (UserTestPaperQuestion relation : relations) {
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
            answer.setQuestionScore(relation.getQuestionScore());
            practiceAnswerMapper.insert(answer);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paperId", paper.getId());
        result.put("practiceId", record.getId());
        return result;
    }

    private UserStudyPlan loadCurrentPlan(Long userId) {
        return userStudyPlanMapper.selectOne(new LambdaQueryWrapper<UserStudyPlan>()
                .eq(UserStudyPlan::getUserId, userId)
                .eq(UserStudyPlan::getStatus, "ACTIVE")
                .orderByDesc(UserStudyPlan::getId)
                .last("limit 1"));
    }

    private void archivePlan(UserStudyPlan plan) {
        List<UserDailyTask> tasks = loadPlanTasks(plan.getId());
        refreshTaskProgress(plan.getUserId(), tasks);
        tasks = loadPlanTasks(plan.getId());
        Map<String, Object> summary = buildPlanSummary(plan, tasks);
        plan.setStatus("ARCHIVED");
        plan.setArchivedAt(LocalDateTime.now());
        plan.setTotalTaskCount(tasks.size());
        plan.setCompletedTaskCount(((Long) summary.get("completedTaskCount")).intValue());
        plan.setCompletionRate((BigDecimal) summary.get("completionRate"));
        userStudyPlanMapper.updateById(plan);
        createPlanArchivedAchievement(plan);
    }

    private List<UserDailyTask> loadPlanTasks(Long planId) {
        return userDailyTaskMapper.selectList(new LambdaQueryWrapper<UserDailyTask>()
                .eq(UserDailyTask::getPlanId, planId)
                .orderByAsc(UserDailyTask::getTaskDate)
                .orderByAsc(UserDailyTask::getId));
    }

    private void ensurePlanTasks(UserStudyPlan plan) {
        List<UserDailyTask> expectedTasks = buildPlanTasks(plan);
        long existingCount = userDailyTaskMapper.selectCount(new LambdaQueryWrapper<UserDailyTask>()
                .eq(UserDailyTask::getPlanId, plan.getId()));
        if (existingCount != expectedTasks.size()) {
            regeneratePlanTasks(plan);
        }
    }

    private void regeneratePlanTasks(UserStudyPlan plan) {
        userDailyTaskMapper.delete(new LambdaQueryWrapper<UserDailyTask>()
                .eq(UserDailyTask::getPlanId, plan.getId()));
        List<UserDailyTask> tasks = buildPlanTasks(plan);
        plan.setTotalTaskCount(tasks.size());
        plan.setCompletedTaskCount(0);
        plan.setCompletionRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        userStudyPlanMapper.updateById(plan);
        for (UserDailyTask task : tasks) {
            userDailyTaskMapper.insert(task);
        }
    }

    private List<UserDailyTask> buildPlanTasks(UserStudyPlan plan) {
        List<UserDailyTask> tasks = new ArrayList<>();
        if (plan == null || plan.getStartDate() == null || plan.getEndDate() == null) {
            return tasks;
        }
        boolean hasExamTask = false;
        boolean hasTestPaperTask = false;
        int totalDays = Math.max(plan.getDurationDays() == null ? 0 : plan.getDurationDays(), 0);
        for (int offset = 0; offset < totalDays; offset++) {
            LocalDate taskDate = plan.getStartDate().plusDays(offset);
            if (taskDate.isAfter(plan.getEndDate())) {
                break;
            }
            tasks.add(buildTask(plan, taskDate, "CHECK_IN",
                    "今日学习打卡",
                    "完成 1 次题库学习打卡，形成稳定备考节奏。",
                    1,
                    "/question-bank"));
            tasks.add(buildTask(plan, taskDate, "LEARN",
                    "今日课程学习",
                    "完成课程学习 " + plan.getDailyStudyMinutes() + " 分钟，保持稳定节奏。",
                    plan.getDailyStudyMinutes(),
                    "/my-course"));
            tasks.add(buildTask(plan, taskDate, "PRACTICE",
                    "今日专项刷题",
                    "完成 " + plan.getDailyQuestionCount() + " 道专项练习，强化主攻科目。",
                    plan.getDailyQuestionCount(),
                    "/question-bank"));
            if (shouldCreateTestPaperTask(offset, totalDays)) {
                tasks.add(buildTask(plan, taskDate, "TEST_PAPER",
                        "个性化测卷强化",
                        "围绕薄弱知识点生成 1 套强化测卷并完成练习。",
                        1,
                        "/user/test-paper"));
                hasTestPaperTask = true;
            }
            if (plan.getWeeklyExamCount() != null && plan.getWeeklyExamCount() > 0 && shouldCreateExamTask(offset, totalDays)) {
                tasks.add(buildTask(plan, taskDate, "EXAM",
                        "阶段模考检验",
                        "本周至少完成 " + plan.getWeeklyExamCount() + " 次模考，检验阶段学习效果。",
                        plan.getWeeklyExamCount(),
                        "/exam"));
                hasExamTask = true;
            }
        }
        if (!hasTestPaperTask && totalDays > 0) {
            tasks.add(buildTask(plan, plan.getEndDate(), "TEST_PAPER",
                    "个性化测卷强化",
                    "围绕薄弱知识点生成 1 套强化测卷并完成练习。",
                    1,
                    "/user/test-paper"));
        }
        if (!hasExamTask && plan.getWeeklyExamCount() != null && plan.getWeeklyExamCount() > 0 && totalDays > 0) {
            tasks.add(buildTask(plan, plan.getEndDate(), "EXAM",
                    "阶段模考检验",
                    "本周期至少完成 " + plan.getWeeklyExamCount() + " 次模考，检验阶段学习效果。",
                    plan.getWeeklyExamCount(),
                    "/exam"));
        }
        return tasks;
    }

    private UserDailyTask buildTask(UserStudyPlan plan,
                                    LocalDate taskDate,
                                    String taskType,
                                    String title,
                                    String description,
                                    Integer targetValue,
                                    String actionPath) {
        UserDailyTask task = new UserDailyTask();
        task.setUserId(plan.getUserId());
        task.setPlanId(plan.getId());
        task.setTaskDate(taskDate);
        task.setTaskType(taskType);
        task.setTitle(title);
        task.setDescriptionText(description);
        task.setTargetValue(targetValue);
        task.setCompletedValue(0);
        task.setStatus("PENDING");
        task.setActionPath(actionPath);
        return task;
    }

    private boolean shouldCreateExamTask(int offset, int totalDays) {
        return (offset + 1) % 7 == 0 || offset == totalDays - 1;
    }

    private boolean shouldCreateTestPaperTask(int offset, int totalDays) {
        return (offset + 1) % 3 == 0 || offset == totalDays - 1;
    }

    private void refreshTaskProgress(Long userId, List<UserDailyTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        LocalDate minDate = tasks.stream().map(UserDailyTask::getTaskDate).min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate maxDate = tasks.stream().map(UserDailyTask::getTaskDate).max(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDateTime start = minDate.atStartOfDay();
        LocalDateTime end = maxDate.plusDays(1).atStartOfDay();
        Map<LocalDate, Integer> checkinCountMap = userDailyCheckinMapper.selectList(new LambdaQueryWrapper<UserDailyCheckin>()
                        .eq(UserDailyCheckin::getUserId, userId)
                        .ge(UserDailyCheckin::getCheckinDate, minDate)
                        .le(UserDailyCheckin::getCheckinDate, maxDate))
                .stream()
                .collect(Collectors.groupingBy(UserDailyCheckin::getCheckinDate, Collectors.summingInt(item -> 1)));

        Map<LocalDate, Integer> learnSecondsMap = userCourseLearningRecordMapper.selectList(new LambdaQueryWrapper<UserCourseLearningRecord>()
                        .eq(UserCourseLearningRecord::getUserId, userId)
                        .ge(UserCourseLearningRecord::getCreatedAt, start)
                        .lt(UserCourseLearningRecord::getCreatedAt, end))
                .stream()
                .filter(item -> item.getCreatedAt() != null)
                .collect(Collectors.groupingBy(item -> item.getCreatedAt().toLocalDate(),
                        Collectors.summingInt(item -> item.getWatchSeconds() == null ? 0 : item.getWatchSeconds())));

        List<PracticeRecord> practiceRecords = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getStatus, "SUBMITTED")
                .ge(PracticeRecord::getSubmittedAt, start)
                .lt(PracticeRecord::getSubmittedAt, end));
        Map<LocalDate, Integer> practiceQuestionMap = practiceRecords.stream()
                .filter(item -> item.getSubmittedAt() != null)
                .filter(item -> !"TEST_PAPER".equals(item.getPracticeType()))
                .collect(Collectors.groupingBy(item -> item.getSubmittedAt().toLocalDate(),
                        Collectors.summingInt(item -> item.getQuestionCount() == null ? 0 : item.getQuestionCount())));
        Map<LocalDate, Integer> testPaperCountMap = practiceRecords.stream()
                .filter(item -> item.getSubmittedAt() != null)
                .filter(item -> "TEST_PAPER".equals(item.getPracticeType()))
                .collect(Collectors.groupingBy(item -> item.getSubmittedAt().toLocalDate(),
                        Collectors.summingInt(item -> 1)));

        Map<LocalDate, Integer> examCountMap = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .eq(ExamRecord::getStatus, "SUBMITTED")
                        .ge(ExamRecord::getSubmittedAt, start)
                        .lt(ExamRecord::getSubmittedAt, end))
                .stream()
                .filter(item -> item.getSubmittedAt() != null)
                .collect(Collectors.groupingBy(item -> item.getSubmittedAt().toLocalDate(),
                        Collectors.summingInt(item -> 1)));

        for (UserDailyTask task : tasks) {
            int completedValue;
            if ("CHECK_IN".equals(task.getTaskType())) {
                completedValue = Math.min(checkinCountMap.getOrDefault(task.getTaskDate(), 0), 1);
            } else if ("LEARN".equals(task.getTaskType())) {
                completedValue = learnSecondsMap.getOrDefault(task.getTaskDate(), 0) / 60;
            } else if ("PRACTICE".equals(task.getTaskType())) {
                completedValue = practiceQuestionMap.getOrDefault(task.getTaskDate(), 0);
            } else if ("EXAM".equals(task.getTaskType())) {
                completedValue = examCountMap.getOrDefault(task.getTaskDate(), 0);
            } else {
                completedValue = testPaperCountMap.getOrDefault(task.getTaskDate(), 0);
            }
            String nextStatus = resolveTaskStatus(completedValue, task.getTargetValue());
            if (!Integer.valueOf(completedValue).equals(task.getCompletedValue()) || !nextStatus.equals(task.getStatus())) {
                boolean newlyCompleted = !"COMPLETED".equals(task.getStatus()) && "COMPLETED".equals(nextStatus);
                task.setCompletedValue(completedValue);
                task.setStatus(nextStatus);
                userDailyTaskMapper.updateById(task);
                if (newlyCompleted) {
                    createTaskAchievement(task);
                }
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private List<UserKnowledgeMastery> refreshKnowledgeMastery(Long userId, UserStudyPlan plan) {
        List<PracticeRecord> practiceRecords = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getStatus, "SUBMITTED"));
        List<ExamRecord> examRecords = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getStatus, "SUBMITTED"));
        List<UserWrongQuestion> wrongQuestions = userWrongQuestionMapper.selectList(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getRemoved, false));

        List<PracticeAnswer> practiceAnswers = CollectionUtils.isEmpty(practiceRecords)
                ? new ArrayList<>()
                : practiceAnswerMapper.selectList(new LambdaQueryWrapper<PracticeAnswer>()
                .in(PracticeAnswer::getPracticeRecordId, practiceRecords.stream().map(PracticeRecord::getId).collect(Collectors.toList())));
        List<ExamAnswer> examAnswers = CollectionUtils.isEmpty(examRecords)
                ? new ArrayList<>()
                : examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .in(ExamAnswer::getExamRecordId, examRecords.stream().map(ExamRecord::getId).collect(Collectors.toList())));

        Set<Long> questionIds = new LinkedHashSet<>();
        practiceAnswers.forEach(item -> questionIds.add(item.getQuestionId()));
        examAnswers.forEach(item -> questionIds.add(item.getQuestionId()));
        wrongQuestions.forEach(item -> questionIds.add(item.getQuestionId()));

        Map<Long, Question> questionMap = CollectionUtils.isEmpty(questionIds)
                ? new HashMap<>()
                : questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (left, right) -> left));
        Map<Long, PracticeRecord> practiceRecordMap = practiceRecords.stream()
                .collect(Collectors.toMap(PracticeRecord::getId, Function.identity(), (left, right) -> left));
        Map<Long, ExamRecord> examRecordMap = examRecords.stream()
                .collect(Collectors.toMap(ExamRecord::getId, Function.identity(), (left, right) -> left));

        Map<String, KnowledgePointComputation> snapshotMap = new LinkedHashMap<>();
        List<String> planFocusPoints = plan == null ? new ArrayList<>() : splitKnowledgePoints(plan.getFocusKnowledgePoints());
        for (PracticeAnswer answer : practiceAnswers) {
            Question question = questionMap.get(answer.getQuestionId());
            PracticeRecord record = practiceRecordMap.get(answer.getPracticeRecordId());
            if (question == null || record == null) {
                continue;
            }
            LocalDateTime occurredAt = record.getSubmittedAt() == null ? record.getCreatedAt() : record.getSubmittedAt();
            for (String point : splitKnowledgePoints(question.getKnowledgePoints())) {
                KnowledgePointComputation computation = initKnowledgeSnapshot(snapshotMap, userId, question.getSubjectName(), point);
                addKnowledgeAnswerEvent(computation, occurredAt, Boolean.TRUE.equals(answer.getIsCorrect()), record.getId(), "PRACTICE", record.getPracticeType());
                computation.planFocused = computation.planFocused || planFocusPoints.contains(point);
            }
        }
        for (ExamAnswer answer : examAnswers) {
            Question question = questionMap.get(answer.getQuestionId());
            ExamRecord record = examRecordMap.get(answer.getExamRecordId());
            if (question == null || record == null) {
                continue;
            }
            LocalDateTime occurredAt = record.getSubmittedAt() == null ? record.getCreatedAt() : record.getSubmittedAt();
            for (String point : splitKnowledgePoints(question.getKnowledgePoints())) {
                KnowledgePointComputation computation = initKnowledgeSnapshot(snapshotMap, userId, question.getSubjectName(), point);
                addKnowledgeAnswerEvent(computation, occurredAt, Boolean.TRUE.equals(answer.getIsCorrect()), record.getId(), "EXAM", "EXAM");
                computation.planFocused = computation.planFocused || planFocusPoints.contains(point);
            }
        }
        for (UserWrongQuestion wrongQuestion : wrongQuestions) {
            Question question = questionMap.get(wrongQuestion.getQuestionId());
            if (question == null) {
                continue;
            }
            for (String point : splitKnowledgePoints(question.getKnowledgePoints())) {
                KnowledgePointComputation computation = initKnowledgeSnapshot(snapshotMap, userId, question.getSubjectName(), point);
                computation.pendingWrongCount = computation.pendingWrongCount + 1;
                computation.recentWrongPressure = computation.recentWrongPressure + resolveRecentWrongPressure(wrongQuestion);
                computation.mastery.setLastPracticedAt(maxTime(computation.mastery.getLastPracticedAt(), wrongQuestion.getLastWrongAt()));
                computation.planFocused = computation.planFocused || planFocusPoints.contains(point);
            }
        }
        if (plan != null) {
            for (String point : splitKnowledgePoints(plan.getFocusKnowledgePoints())) {
                KnowledgePointComputation computation = initKnowledgeSnapshot(snapshotMap, userId, plan.getSubjectName(), point);
                computation.planFocused = true;
            }
        }

        userKnowledgeMasteryMapper.delete(new LambdaQueryWrapper<UserKnowledgeMastery>()
                .eq(UserKnowledgeMastery::getUserId, userId));
        List<UserKnowledgeMastery> results = new ArrayList<>();
        for (KnowledgePointComputation computation : snapshotMap.values()) {
            finalizeKnowledgeSnapshot(computation);
            userKnowledgeMasteryMapper.upsert(computation.mastery);
            results.add(computation.mastery);
        }
        return results.stream()
                .sorted(Comparator.comparing(UserKnowledgeMastery::getPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UserKnowledgeMastery::getMasteryScore, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private KnowledgePointComputation initKnowledgeSnapshot(Map<String, KnowledgePointComputation> snapshotMap,
                                                            Long userId,
                                                            String subjectName,
                                                            String knowledgePoint) {
        String resolvedSubject = normalizeKnowledgeSubject(subjectName);
        String resolvedKnowledgePoint = normalizeKnowledgeValue(knowledgePoint);
        String key = resolvedSubject + "::" + resolvedKnowledgePoint;
        return snapshotMap.computeIfAbsent(key, unused -> {
            KnowledgePointComputation computation = new KnowledgePointComputation();
            UserKnowledgeMastery item = new UserKnowledgeMastery();
            item.setUserId(userId);
            item.setSubjectName(resolvedSubject);
            item.setKnowledgePoint(resolvedKnowledgePoint);
            item.setAttemptCount(0);
            item.setCorrectCount(0);
            item.setWrongCount(0);
            item.setPendingWrongCount(0);
            item.setAccuracyRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setMasteryScore(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setMasteryConfidence(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setPriorityScore(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setMasteryLevel("WEAK");
            item.setKnowledgeStatus("TO_DIAGNOSE");
            computation.mastery = item;
            return computation;
        });
    }

    private void addKnowledgeAnswerEvent(KnowledgePointComputation computation,
                                         LocalDateTime occurredAt,
                                         boolean correct,
                                         Long sessionId,
                                         String sourceType,
                                         String practiceType) {
        KnowledgeAnswerEvent event = new KnowledgeAnswerEvent();
        event.occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
        event.correct = correct;
        event.sourceType = sourceType;
        event.practiceType = practiceType;
        event.sessionId = sessionId;
        computation.answerEvents.add(event);
        if (sessionId != null) {
            computation.sessionIds.add(sessionId);
        }
        if (StringUtils.hasText(sourceType)) {
            computation.sourceTypes.add(sourceType);
        }
        if ("EXAM".equals(sourceType)) {
            computation.examAttemptCount = computation.examAttemptCount + 1;
        } else if ("TEST_PAPER".equals(practiceType)) {
            computation.testPaperAttemptCount = computation.testPaperAttemptCount + 1;
        } else if ("WRONG_BOOK".equals(practiceType)) {
            computation.wrongBookAttemptCount = computation.wrongBookAttemptCount + 1;
        } else {
            computation.practiceAttemptCount = computation.practiceAttemptCount + 1;
        }
        computation.mastery.setLastPracticedAt(maxTime(computation.mastery.getLastPracticedAt(), event.occurredAt));
    }

    private void finalizeKnowledgeSnapshot(KnowledgePointComputation computation) {
        UserKnowledgeMastery item = computation.mastery;
        List<KnowledgeAnswerEvent> orderedEvents = computation.answerEvents.stream()
                .sorted(Comparator.comparing(event -> event.occurredAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        int attempts = orderedEvents.size();
        int correctCount = (int) orderedEvents.stream().filter(event -> event.correct).count();
        int wrongCount = attempts - correctCount;
        int recentAttempts = Math.min(attempts, KNOWLEDGE_RECENT_WINDOW);
        List<KnowledgeAnswerEvent> recentWindow = orderedEvents.subList(Math.max(0, attempts - recentAttempts), attempts);
        int recentCorrectCount = (int) recentWindow.stream().filter(event -> event.correct).count();

        item.setAttemptCount(attempts);
        item.setCorrectCount(correctCount);
        item.setWrongCount(wrongCount);
        item.setPendingWrongCount(computation.pendingWrongCount);
        BigDecimal accuracyRate = attempts == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(correctCount * 100.0 / attempts).setScale(2, RoundingMode.HALF_UP);
        item.setAccuracyRate(accuracyRate);

        double accuracyScore = attempts == 0 ? 0D : ((recentCorrectCount + 1D) / (recentAttempts + 2D)) * 100D;
        double stabilityScore = calculateStabilityScore(orderedEvents, recentCorrectCount, recentAttempts, computation.pendingWrongCount);
        double recencyScore = calculateRecencyScore(item.getLastPracticedAt());
        double errorAdjustedScore = calculateErrorAdjustedScore(orderedEvents, computation.pendingWrongCount, computation.recentWrongPressure);
        double masteryScore = accuracyScore * 0.45D
                + stabilityScore * 0.20D
                + recencyScore * 0.20D
                + errorAdjustedScore * 0.15D;
        masteryScore = clampScore(masteryScore);
        item.setMasteryScore(toScore(masteryScore));
        item.setMasteryLevel(resolveMasteryLevel(item.getMasteryScore()));

        double confidenceScore = calculateConfidenceScore(computation, orderedEvents);
        item.setMasteryConfidence(toScore(confidenceScore));
        item.setKnowledgeStatus(resolveKnowledgeStatus(item, computation, orderedEvents));
        item.setPriorityScore(toScore(calculatePriorityScore(item, computation, orderedEvents)));
    }

    private int resolveRecentWrongPressure(UserWrongQuestion wrongQuestion) {
        if (wrongQuestion == null) {
            return 0;
        }
        int pressure = Math.min(50, (wrongQuestion.getWrongCount() == null ? 1 : wrongQuestion.getWrongCount()) * 12);
        if (wrongQuestion.getLastWrongAt() != null && wrongQuestion.getLastWrongAt().isAfter(LocalDateTime.now().minusDays(14))) {
            pressure = pressure + 18;
        }
        return Math.min(100, pressure);
    }

    private double calculateStabilityScore(List<KnowledgeAnswerEvent> orderedEvents,
                                           int recentCorrectCount,
                                           int recentAttempts,
                                           int pendingWrongCount) {
        int attempts = orderedEvents.size();
        if (attempts == 0) {
            return 0D;
        }
        int currentCorrectStreak = 0;
        int transitionCount = 0;
        Boolean previous = null;
        for (KnowledgeAnswerEvent event : orderedEvents) {
            if (event.correct) {
                currentCorrectStreak = currentCorrectStreak + 1;
            } else {
                currentCorrectStreak = 0;
            }
            if (previous != null && previous.booleanValue() != event.correct) {
                transitionCount = transitionCount + 1;
            }
            previous = event.correct;
        }
        double windowAccuracy = recentAttempts == 0 ? 0D : recentCorrectCount * 100D / recentAttempts;
        double streakScore = Math.min(100D, currentCorrectStreak * 18D);
        double transitionPenalty = attempts <= 1 ? 0D : transitionCount * 35D / (attempts - 1D);
        double pendingPenalty = Math.min(35D, pendingWrongCount * 12D);
        double sampleCap = attempts < 5 ? Math.min(60D, attempts * 12D) : 100D;
        return Math.max(0D, Math.min(sampleCap, windowAccuracy * 0.60D + streakScore * 0.25D + (100D - pendingPenalty) * 0.15D - transitionPenalty));
    }

    private double calculateRecencyScore(LocalDateTime lastPracticedAt) {
        if (lastPracticedAt == null) {
            return 0D;
        }
        long days = ChronoUnit.DAYS.between(lastPracticedAt.toLocalDate(), LocalDate.now());
        if (days <= 3) {
            return 100D;
        }
        if (days <= 7) {
            return 85D;
        }
        if (days <= 14) {
            return 65D;
        }
        if (days <= 30) {
            return 40D;
        }
        return 20D;
    }

    private double calculateErrorAdjustedScore(List<KnowledgeAnswerEvent> orderedEvents,
                                               int pendingWrongCount,
                                               int recentWrongPressure) {
        int recentWrongCount = (int) orderedEvents.stream()
                .filter(event -> !event.correct && event.occurredAt != null && event.occurredAt.isAfter(LocalDateTime.now().minusDays(14)))
                .count();
        int recoveryStreak = 0;
        for (int i = orderedEvents.size() - 1; i >= 0; i--) {
            if (orderedEvents.get(i).correct) {
                recoveryStreak = recoveryStreak + 1;
            } else {
                break;
            }
        }
        double pressure = Math.min(100D, pendingWrongCount * 18D + recentWrongCount * 8D + recentWrongPressure * 0.35D);
        double recoveryBonus = Math.min(20D, recoveryStreak * 6D);
        return clampScore(100D - Math.max(0D, pressure - recoveryBonus));
    }

    private double calculateConfidenceScore(KnowledgePointComputation computation,
                                            List<KnowledgeAnswerEvent> orderedEvents) {
        int attempts = orderedEvents.size();
        int recent14Attempts = (int) orderedEvents.stream()
                .filter(event -> event.occurredAt != null && event.occurredAt.isAfter(LocalDateTime.now().minusDays(14)))
                .count();
        double confidence = Math.min(45D, attempts * 6D);
        confidence = confidence + Math.min(20D, recent14Attempts * 4D);
        confidence = confidence + Math.min(15D, computation.sessionIds.size() * 5D);
        confidence = confidence + Math.min(20D, computation.sourceTypes.size() * 8D);
        if (computation.practiceAttemptCount == 0 && computation.testPaperAttemptCount == 0 && computation.examAttemptCount == 0 && computation.wrongBookAttemptCount > 0) {
            confidence = Math.min(confidence, 45D);
        }
        if (attempts < 3) {
            confidence = Math.min(confidence, 35D);
        }
        return clampScore(confidence);
    }

    private String resolveKnowledgeStatus(UserKnowledgeMastery item,
                                          KnowledgePointComputation computation,
                                          List<KnowledgeAnswerEvent> orderedEvents) {
        if (item.getMasteryConfidence() == null || item.getMasteryConfidence().compareTo(BigDecimal.valueOf(40)) < 0) {
            return "TO_DIAGNOSE";
        }
        if (orderedEvents.size() < 3 && computation.wrongBookAttemptCount > 0 && computation.practiceAttemptCount == 0 && computation.testPaperAttemptCount == 0 && computation.examAttemptCount == 0) {
            return "TO_DIAGNOSE";
        }
        return item.getMasteryLevel();
    }

    private double calculatePriorityScore(UserKnowledgeMastery item,
                                          KnowledgePointComputation computation,
                                          List<KnowledgeAnswerEvent> orderedEvents) {
        double pendingWrongPressure = Math.min(100D, computation.pendingWrongCount * 20D + computation.recentWrongPressure * 0.40D);
        double examRelevance = computation.planFocused ? 100D : 35D;
        double recencyDecayNeed = 0D;
        if ("IMPROVING".equals(item.getMasteryLevel()) || "FOUNDATION".equals(item.getMasteryLevel())) {
            recencyDecayNeed = 100D - calculateRecencyScore(item.getLastPracticedAt());
        } else if ("MASTERED".equals(item.getMasteryLevel()) && item.getLastPracticedAt() != null
                && item.getLastPracticedAt().isBefore(LocalDateTime.now().minusDays(21))) {
            recencyDecayNeed = 60D;
        }
        return clampScore((100D - item.getMasteryScore().doubleValue()) * 0.55D
                + pendingWrongPressure * 0.25D
                + examRelevance * 0.15D
                + recencyDecayNeed * 0.05D);
    }

    private String resolveMasteryLevel(BigDecimal masteryScore) {
        if (masteryScore == null) {
            return "WEAK";
        }
        if (masteryScore.compareTo(BigDecimal.valueOf(85)) >= 0) {
            return "MASTERED";
        }
        if (masteryScore.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "IMPROVING";
        }
        if (masteryScore.compareTo(BigDecimal.valueOf(35)) >= 0) {
            return "FOUNDATION";
        }
        return "WEAK";
    }

    private Map<String, Object> toPlanMap(UserStudyPlan plan) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", plan.getId());
        map.put("planName", plan.getPlanName());
        map.put("targetExamType", plan.getTargetExamType());
        map.put("subjectName", plan.getSubjectName());
        map.put("dailyStudyMinutes", plan.getDailyStudyMinutes());
        map.put("dailyQuestionCount", plan.getDailyQuestionCount());
        map.put("weeklyExamCount", plan.getWeeklyExamCount());
        map.put("durationDays", plan.getDurationDays());
        map.put("startDate", plan.getStartDate());
        map.put("endDate", plan.getEndDate());
        map.put("focusKnowledgePoints", splitKnowledgePoints(plan.getFocusKnowledgePoints()));
        map.put("status", plan.getStatus());
        map.put("archivedAt", plan.getArchivedAt());
        map.put("totalTaskCount", plan.getTotalTaskCount());
        map.put("completedTaskCount", plan.getCompletedTaskCount());
        map.put("completionRate", plan.getCompletionRate());
        return map;
    }

    private Map<String, Object> toPlanHistoryMap(UserStudyPlan plan) {
        Map<String, Object> map = toPlanMap(plan);
        int totalTasks = plan.getTotalTaskCount() == null ? 0 : plan.getTotalTaskCount();
        int completedTasks = plan.getCompletedTaskCount() == null ? 0 : plan.getCompletedTaskCount();
        map.put("daysRemaining", 0);
        map.put("completionRate", plan.getCompletionRate() == null
                ? calculatePercent(completedTasks, totalTasks)
                : plan.getCompletionRate());
        map.put("archived", "ARCHIVED".equals(plan.getStatus()));
        return map;
    }

    private Map<String, Object> buildPlanSummary(UserStudyPlan plan, List<UserDailyTask> tasks) {
        long completedTaskCount = tasks.stream().filter(item -> "COMPLETED".equals(item.getStatus())).count();
        long todayTaskCount = tasks.stream().filter(item -> LocalDate.now().equals(item.getTaskDate())).count();
        long todayCompletedTaskCount = tasks.stream()
                .filter(item -> LocalDate.now().equals(item.getTaskDate()))
                .filter(item -> "COMPLETED".equals(item.getStatus()))
                .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("daysRemaining", Math.max(ChronoUnit.DAYS.between(LocalDate.now(), plan.getEndDate()) + 1, 0));
        summary.put("totalTaskCount", tasks.size());
        summary.put("completedTaskCount", completedTaskCount);
        summary.put("completionRate", calculatePercent(completedTaskCount, tasks.size()));
        summary.put("todayTaskCount", todayTaskCount);
        summary.put("todayCompletedTaskCount", todayCompletedTaskCount);
        summary.put("focusKnowledgeCount", splitKnowledgePoints(plan.getFocusKnowledgePoints()).size());
        return summary;
    }

    private Map<String, Object> buildEmptyPlanSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("daysRemaining", 0);
        summary.put("totalTaskCount", 0);
        summary.put("completedTaskCount", 0);
        summary.put("completionRate", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        summary.put("todayTaskCount", 0);
        summary.put("todayCompletedTaskCount", 0);
        summary.put("focusKnowledgeCount", 0);
        return summary;
    }

    private Map<String, Object> buildAchievementSummary(Long userId) {
        List<UserGrowthAchievement> achievements = userGrowthAchievementMapper.selectList(new LambdaQueryWrapper<UserGrowthAchievement>()
                .eq(UserGrowthAchievement::getUserId, userId)
                .orderByDesc(UserGrowthAchievement::getCreatedAt)
                .orderByDesc(UserGrowthAchievement::getId));
        Map<String, Object> summary = new HashMap<>();
        summary.put("achievementCount", achievements.size());
        summary.put("completedTaskCount", achievements.stream().filter(item -> "TASK_COMPLETED".equals(item.getAchievementType())).count());
        summary.put("archivedPlanCount", achievements.stream().filter(item -> "PLAN_ARCHIVED".equals(item.getAchievementType())).count());
        summary.put("latestAchievedAt", achievements.stream()
                .map(UserGrowthAchievement::getCreatedAt)
                .filter(item -> item != null)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        return summary;
    }

    private Map<String, Object> buildTaskSummary(List<UserDailyTask> tasks) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTaskCount", tasks.size());
        summary.put("completedTaskCount", tasks.stream().filter(item -> "COMPLETED".equals(item.getStatus())).count());
        summary.put("todayTaskCount", tasks.stream().filter(item -> LocalDate.now().equals(item.getTaskDate())).count());
        summary.put("todayCompletedTaskCount", tasks.stream()
                .filter(item -> LocalDate.now().equals(item.getTaskDate()))
                .filter(item -> "COMPLETED".equals(item.getStatus()))
                .count());
        summary.put("todayCheckInCount", tasks.stream()
                .filter(item -> LocalDate.now().equals(item.getTaskDate()))
                .filter(item -> "CHECK_IN".equals(item.getTaskType()))
                .mapToInt(item -> item.getCompletedValue() == null ? 0 : item.getCompletedValue())
                .sum());
        summary.put("todayLearnMinutes", tasks.stream()
                .filter(item -> LocalDate.now().equals(item.getTaskDate()))
                .filter(item -> "LEARN".equals(item.getTaskType()))
                .mapToInt(item -> item.getCompletedValue() == null ? 0 : item.getCompletedValue())
                .sum());
        summary.put("todayPracticeQuestions", tasks.stream()
                .filter(item -> LocalDate.now().equals(item.getTaskDate()))
                .filter(item -> "PRACTICE".equals(item.getTaskType()))
                .mapToInt(item -> item.getCompletedValue() == null ? 0 : item.getCompletedValue())
                .sum());
        summary.put("todayExamCount", tasks.stream()
                .filter(item -> LocalDate.now().equals(item.getTaskDate()))
                .filter(item -> "EXAM".equals(item.getTaskType()))
                .mapToInt(item -> item.getCompletedValue() == null ? 0 : item.getCompletedValue())
                .sum());
        summary.put("todayTestPaperCount", tasks.stream()
                .filter(item -> LocalDate.now().equals(item.getTaskDate()))
                .filter(item -> "TEST_PAPER".equals(item.getTaskType()))
                .mapToInt(item -> item.getCompletedValue() == null ? 0 : item.getCompletedValue())
                .sum());
        return summary;
    }

    private Map<String, Object> toTaskMap(UserDailyTask task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("taskDate", task.getTaskDate());
        map.put("taskType", task.getTaskType());
        map.put("taskTypeLabel", resolveTaskTypeLabel(task.getTaskType()));
        map.put("title", task.getTitle());
        map.put("description", task.getDescriptionText());
        map.put("targetValue", task.getTargetValue());
        map.put("completedValue", task.getCompletedValue());
        map.put("status", task.getStatus());
        map.put("statusLabel", resolveTaskStatusLabel(task.getStatus()));
        map.put("progressPercent", calculatePercent(task.getCompletedValue(), task.getTargetValue()));
        map.put("actionPath", task.getActionPath());
        map.put("actionText", resolveTaskActionText(task.getTaskType()));
        map.put("unitLabel", resolveTaskUnit(task.getTaskType()));
        return map;
    }

    private Map<String, Object> toAchievementMap(UserGrowthAchievement item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("planId", item.getPlanId());
        map.put("taskId", item.getTaskId());
        map.put("achievementType", item.getAchievementType());
        map.put("title", item.getTitle());
        map.put("description", item.getDescriptionText());
        map.put("achievementValue", item.getAchievementValue());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private UserGrowthDiagnosisReport saveDiagnosisSnapshot(Long userId,
                                                            UserStudyPlan plan,
                                                            GrowthScoringSnapshot scoringSnapshot,
                                                            List<UserKnowledgeMastery> masteryList) {
        LocalDate diagnosisDate = LocalDate.now();
        UserGrowthDiagnosisReport diagnosis = userGrowthDiagnosisReportMapper.selectOne(new LambdaQueryWrapper<UserGrowthDiagnosisReport>()
                .eq(UserGrowthDiagnosisReport::getUserId, userId)
                .eq(UserGrowthDiagnosisReport::getPlanId, plan.getId())
                .eq(UserGrowthDiagnosisReport::getDiagnosisDate, diagnosisDate)
                .last("LIMIT 1"));
        if (diagnosis == null) {
            diagnosis = new UserGrowthDiagnosisReport();
            diagnosis.setUserId(userId);
            diagnosis.setPlanId(plan.getId());
            diagnosis.setDiagnosisDate(diagnosisDate);
        }
        BigDecimal vitalityScore = scoringSnapshot.completionRate.multiply(BigDecimal.valueOf(0.40))
                .add(scoringSnapshot.masteryRate.multiply(BigDecimal.valueOf(0.35)))
                .add(scoringSnapshot.testPaperAverage.multiply(BigDecimal.valueOf(0.25)))
                .setScale(2, RoundingMode.HALF_UP);
        String weakPoints = joinKnowledgePoints(masteryList.stream()
                .sorted(Comparator.comparing(UserKnowledgeMastery::getPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UserKnowledgeMastery::getMasteryScore, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(3)
                .map(UserKnowledgeMastery::getKnowledgePoint)
                .collect(Collectors.toList()));

        diagnosis.setDiagnosisStage(resolveDiagnosisStage(vitalityScore));
        diagnosis.setVitalityScore(vitalityScore);
        diagnosis.setPlanCompletionRate(scoringSnapshot.completionRate);
        diagnosis.setKnowledgeMasteryRate(scoringSnapshot.masteryRate);
        diagnosis.setTestPaperAverageScore(scoringSnapshot.testPaperAverage);
        diagnosis.setPredictedPassRate(scoringSnapshot.predictedPassRate);
        diagnosis.setPredictionConfidence(scoringSnapshot.predictionConfidence);
        diagnosis.setFormulaVersion(scoringSnapshot.formulaVersion);
        diagnosis.setWeakKnowledgePoints(weakPoints);
        diagnosis.setRecommendationText(buildDiagnosisRecommendation(scoringSnapshot.completionRate, scoringSnapshot.masteryRate, scoringSnapshot.testPaperAverage, weakPoints));
        if (diagnosis.getId() == null) {
            userGrowthDiagnosisReportMapper.insert(diagnosis);
        } else {
            userGrowthDiagnosisReportMapper.updateById(diagnosis);
        }
        return diagnosis;
    }

    private Map<String, Object> toDiagnosisMap(UserGrowthDiagnosisReport diagnosis) {
        if (diagnosis == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", diagnosis.getId());
        map.put("planId", diagnosis.getPlanId());
        map.put("diagnosisDate", diagnosis.getDiagnosisDate());
        map.put("diagnosisStage", diagnosis.getDiagnosisStage());
        map.put("diagnosisStageLabel", resolveDiagnosisStageLabel(diagnosis.getDiagnosisStage()));
        map.put("vitalityScore", diagnosis.getVitalityScore());
        map.put("planCompletionRate", diagnosis.getPlanCompletionRate());
        map.put("knowledgeMasteryRate", diagnosis.getKnowledgeMasteryRate());
        map.put("testPaperAverageScore", diagnosis.getTestPaperAverageScore());
        map.put("predictedPassRate", diagnosis.getPredictedPassRate());
        map.put("predictionConfidence", diagnosis.getPredictionConfidence());
        map.put("formulaVersion", diagnosis.getFormulaVersion());
        map.put("weakKnowledgePoints", splitKnowledgePoints(diagnosis.getWeakKnowledgePoints()));
        map.put("recommendationText", diagnosis.getRecommendationText());
        return map;
    }

    private Map<String, Object> toDiagnosisTrendMap(UserGrowthDiagnosisReport diagnosis) {
        Map<String, Object> map = new HashMap<>();
        map.put("diagnosisDate", diagnosis.getDiagnosisDate());
        map.put("vitalityScore", diagnosis.getVitalityScore());
        map.put("planCompletionRate", diagnosis.getPlanCompletionRate());
        map.put("knowledgeMasteryRate", diagnosis.getKnowledgeMasteryRate());
        map.put("testPaperAverageScore", diagnosis.getTestPaperAverageScore());
        map.put("predictedPassRate", diagnosis.getPredictedPassRate());
        map.put("predictionConfidence", diagnosis.getPredictionConfidence());
        return map;
    }

    private Map<String, Object> buildDiagnosisComparison(UserGrowthDiagnosisReport latestDiagnosis,
                                                         List<UserGrowthDiagnosisReport> diagnosisHistory) {
        if (latestDiagnosis == null) {
            return buildEmptyDiagnosisComparison();
        }
        UserGrowthDiagnosisReport previous = diagnosisHistory.stream()
                .filter(item -> latestDiagnosis.getId() == null || !latestDiagnosis.getId().equals(item.getId()))
                .max(Comparator.comparing(UserGrowthDiagnosisReport::getDiagnosisDate)
                        .thenComparing(UserGrowthDiagnosisReport::getId))
                .orElse(null);
        Map<String, Object> map = new HashMap<>();
        map.put("hasBaseline", previous != null);
        map.put("currentDate", latestDiagnosis.getDiagnosisDate());
        map.put("baselineDate", previous == null ? null : previous.getDiagnosisDate());
        map.put("currentStageLabel", resolveDiagnosisStageLabel(latestDiagnosis.getDiagnosisStage()));
        map.put("baselineStageLabel", previous == null ? null : resolveDiagnosisStageLabel(previous.getDiagnosisStage()));
        map.put("vitalityDelta", compareDiagnosisMetric(latestDiagnosis.getVitalityScore(), previous == null ? null : previous.getVitalityScore()));
        map.put("completionDelta", compareDiagnosisMetric(latestDiagnosis.getPlanCompletionRate(), previous == null ? null : previous.getPlanCompletionRate()));
        map.put("masteryDelta", compareDiagnosisMetric(latestDiagnosis.getKnowledgeMasteryRate(), previous == null ? null : previous.getKnowledgeMasteryRate()));
        map.put("testPaperDelta", compareDiagnosisMetric(latestDiagnosis.getTestPaperAverageScore(), previous == null ? null : previous.getTestPaperAverageScore()));
        map.put("overallDirection", resolveComparisonDirection(latestDiagnosis, previous));
        map.put("overallDirectionLabel", resolveComparisonDirectionLabel(resolveComparisonDirection(latestDiagnosis, previous)));
        return map;
    }

    private Map<String, Object> buildEmptyDiagnosisComparison() {
        Map<String, Object> map = new HashMap<>();
        map.put("hasBaseline", false);
        map.put("currentDate", null);
        map.put("baselineDate", null);
        map.put("currentStageLabel", null);
        map.put("baselineStageLabel", null);
        map.put("vitalityDelta", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        map.put("completionDelta", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        map.put("masteryDelta", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        map.put("testPaperDelta", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        map.put("overallDirection", "FLAT");
        map.put("overallDirectionLabel", "需继续积累周期数据");
        return map;
    }

    private BigDecimal compareDiagnosisMetric(BigDecimal current, BigDecimal baseline) {
        BigDecimal safeCurrent = current == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : current.setScale(2, RoundingMode.HALF_UP);
        BigDecimal safeBaseline = baseline == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : baseline.setScale(2, RoundingMode.HALF_UP);
        return safeCurrent.subtract(safeBaseline).setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveComparisonDirection(UserGrowthDiagnosisReport latestDiagnosis,
                                              UserGrowthDiagnosisReport previous) {
        if (latestDiagnosis == null || previous == null) {
            return "FLAT";
        }
        BigDecimal totalDelta = compareDiagnosisMetric(latestDiagnosis.getVitalityScore(), previous.getVitalityScore())
                .add(compareDiagnosisMetric(latestDiagnosis.getPlanCompletionRate(), previous.getPlanCompletionRate()))
                .add(compareDiagnosisMetric(latestDiagnosis.getKnowledgeMasteryRate(), previous.getKnowledgeMasteryRate()))
                .add(compareDiagnosisMetric(latestDiagnosis.getTestPaperAverageScore(), previous.getTestPaperAverageScore()));
        if (totalDelta.compareTo(BigDecimal.valueOf(10)) >= 0) {
            return "UP";
        }
        if (totalDelta.compareTo(BigDecimal.valueOf(-10)) <= 0) {
            return "DOWN";
        }
        return "FLAT";
    }

    private String resolveComparisonDirectionLabel(String direction) {
        if ("UP".equals(direction)) {
            return "相较上一个周期整体上升";
        }
        if ("DOWN".equals(direction)) {
            return "相较上一个周期需要重点补强";
        }
        return "与上一个周期基本持平";
    }

    private Map<String, Object> buildDiagnosisStageSummary(List<UserDailyTask> tasks,
                                                           List<UserKnowledgeMastery> masteryList,
                                                           List<Map<String, Object>> completedReports) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("completedTaskCount", tasks.stream().filter(item -> "COMPLETED".equals(item.getStatus())).count());
        summary.put("totalTaskCount", tasks.size());
        summary.put("weakKnowledgeCount", masteryList.stream().filter(item -> "WEAK".equals(item.getMasteryLevel())).count());
        summary.put("masteredKnowledgeCount", masteryList.stream().filter(item -> "MASTERED".equals(item.getMasteryLevel())).count());
        summary.put("completedTestPaperCount", completedReports.size());
        summary.put("bestTestPaperAccuracy", completedReports.stream()
                .map(item -> safeDecimal(item.get("latestAccuracyRate")))
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)));
        return summary;
    }

    private Map<String, Object> buildEmptyDiagnosisStageSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("completedTaskCount", 0);
        summary.put("totalTaskCount", 0);
        summary.put("weakKnowledgeCount", 0);
        summary.put("masteredKnowledgeCount", 0);
        summary.put("completedTestPaperCount", 0);
        summary.put("bestTestPaperAccuracy", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        return summary;
    }

    private List<Map<String, Object>> buildDiagnosisActions(UserStudyPlan plan,
                                                            List<UserDailyTask> tasks,
                                                            List<UserKnowledgeMastery> masteryList,
                                                            List<Map<String, Object>> completedReports,
                                                            UserGrowthDiagnosisReport diagnosis,
                                                            List<Map<String, Object>> assessmentHistory) {
        List<Map<String, Object>> actions = new ArrayList<>();
        long todayPendingCount = tasks.stream()
                .filter(item -> LocalDate.now().equals(item.getTaskDate()))
                .filter(item -> !"COMPLETED".equals(item.getStatus()))
                .count();
        if (todayPendingCount > 0) {
            actions.add(buildDiagnosisAction("先补今日任务", "当前仍有 " + todayPendingCount + " 个今日任务未达成，优先把日计划闭环做完。", "/user/task", "查看任务"));
        }
        masteryList.stream()
                .sorted(Comparator.comparing(UserKnowledgeMastery::getPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UserKnowledgeMastery::getMasteryScore, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(2)
                .forEach(item -> actions.add(buildDiagnosisAction(
                        "补强 " + item.getKnowledgePoint(),
                        buildKnowledgeDiagnosis(item),
                        "/user/test-paper?knowledge=" + item.getKnowledgePoint() + "&subject=" + item.getSubjectName(),
                        "生成测卷")));
        if (completedReports.isEmpty()) {
            actions.add(buildDiagnosisAction("补一次阶段测卷", "当前还没有已完成的个性化测卷，建议先生成一套检验计划执行效果。", "/user/test-paper", "立即生成"));
        } else {
            Map<String, Object> latestReport = completedReports.get(0);
            actions.add(buildDiagnosisAction(
                    "回看最近测卷",
                    "最近一次测卷准确率 " + safeDecimal(latestReport.get("latestAccuracyRate")) + "%，建议回看报告并复盘错题。",
                    String.valueOf(latestReport.get("reportPath")),
                    "查看报告"));
        }
        if (diagnosis != null && StringUtils.hasText(diagnosis.getRecommendationText())) {
            actions.add(buildDiagnosisAction("阶段建议", diagnosis.getRecommendationText(), "/user/archive", "查看档案"));
        }
        if (CollectionUtils.isEmpty(assessmentHistory)) {
            actions.add(buildDiagnosisAction("开始独立评测", "用一套 " + ENTRY_ASSESSMENT_QUESTION_COUNT + " 题评测校准当前通过率预测，并为后续训练定锚。", "/user/assessment", "立即评测"));
        } else {
            Map<String, Object> latestAssessment = assessmentHistory.get(0);
            actions.add(buildDiagnosisAction(
                    "回看入学评测",
                    "最近一次评测准确率 " + safeDecimal(latestAssessment.get("latestAccuracyRate")) + "%，建议结合预测结果继续补强。",
                    String.valueOf(latestAssessment.get("reportPath")),
                    "查看报告"));
        }
        return actions.stream().limit(6).collect(Collectors.toList());
    }

    private Map<String, Object> buildDiagnosisAction(String title, String content, String path, String actionText) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("content", content);
        map.put("path", path);
        map.put("actionText", actionText);
        return map;
    }

    private Map<String, Object> buildAssessmentEntry(UserStudyPlan plan, List<Map<String, Object>> assessmentHistory) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("questionCount", ENTRY_ASSESSMENT_QUESTION_COUNT);
        entry.put("title", "独立入学评测");
        entry.put("description", ENTRY_ASSESSMENT_QUESTION_COUNT + " 题独立评测入口，先校准当前通过率预测，再决定下一步是补弱还是冲刺。");
        entry.put("actionPath", "/user/assessment");
        entry.put("actionText", CollectionUtils.isEmpty(assessmentHistory) ? "开始评测" : "继续查看评测");
        entry.put("latestAssessmentPath", CollectionUtils.isEmpty(assessmentHistory) ? null : assessmentHistory.get(0).get("reportPath"));
        entry.put("subjectName", plan == null ? null : plan.getSubjectName());
        return entry;
    }

    private Map<String, Object> buildAssessmentSummary(List<Map<String, Object>> assessmentHistory) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("sessionCount", assessmentHistory.size());
        summary.put("completedCount", assessmentHistory.stream()
                .filter(item -> "COMPLETED".equals(item.get("assessmentStatus")))
                .count());
        summary.put("pendingCount", assessmentHistory.stream()
                .filter(item -> !"COMPLETED".equals(item.get("assessmentStatus")))
                .count());
        summary.put("averagePredictedPassRate", assessmentHistory.stream()
                .map(item -> safeDecimal(item.get("predictedPassRate")))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(assessmentHistory.size(), 1)), 2, RoundingMode.HALF_UP));
        summary.put("averageActualAccuracy", assessmentHistory.stream()
                .map(item -> safeDecimal(item.get("latestAccuracyRate")))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(assessmentHistory.size(), 1)), 2, RoundingMode.HALF_UP));
        summary.put("latestSubmittedAt", assessmentHistory.stream()
                .map(item -> (LocalDateTime) item.get("submittedAt"))
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        summary.put("latestAssessmentTitle", assessmentHistory.isEmpty() ? null : assessmentHistory.get(0).get("title"));
        summary.put("latestAssessmentQuestionCount", assessmentHistory.isEmpty() ? ENTRY_ASSESSMENT_QUESTION_COUNT : assessmentHistory.get(0).get("questionCount"));
        if (assessmentHistory.isEmpty()) {
            summary.put("latestAssessmentAccuracy", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        } else {
            Map<String, Object> latest = assessmentHistory.get(0);
            BigDecimal latestAccuracy = safeDecimal(latest.get("latestAccuracyRate"));
            if ("COMPLETED".equals(latest.get("assessmentStatus"))) {
                summary.put("latestAssessmentAccuracy", latestAccuracy);
            } else {
                summary.put("latestAssessmentAccuracy", safeDecimal(latest.get("predictedPassRate")));
            }
        }
        return summary;
    }

    private GrowthScoringSnapshot buildEmptyGrowthScoringSnapshot() {
        return new GrowthScoringSnapshot();
    }

    private GrowthScoringSnapshot buildGrowthScoringSnapshot(Long userId,
                                                             UserStudyPlan plan,
                                                             List<UserDailyTask> tasks,
                                                             List<UserKnowledgeMastery> masteryList,
                                                             List<Map<String, Object>> completedReports,
                                                             List<Map<String, Object>> assessmentHistory) {
        GrowthScoringSnapshot snapshot = new GrowthScoringSnapshot();
        snapshot.completionRate = calculatePercent(
                tasks.stream().filter(item -> "COMPLETED".equals(item.getStatus())).count(),
                tasks.size());
        snapshot.masteryRate = masteryList.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : masteryList.stream()
                .map(UserKnowledgeMastery::getMasteryScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(masteryList.size()), 2, RoundingMode.HALF_UP);
        snapshot.testPaperAverage = completedReports.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : completedReports.stream()
                .map(item -> safeDecimal(item.get("latestAccuracyRate")))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(completedReports.size()), 2, RoundingMode.HALF_UP);

        Map<String, Object> latestCompletedAssessment = assessmentHistory.stream()
                .filter(item -> "COMPLETED".equals(item.get("assessmentStatus")))
                .findFirst()
                .orElse(null);
        snapshot.assessmentFallback = latestCompletedAssessment == null;
        snapshot.assessmentReference = latestCompletedAssessment == null
                ? snapshot.testPaperAverage
                : safeDecimal(latestCompletedAssessment.get("latestAccuracyRate"));
        snapshot.basePredictedPassRate = snapshot.completionRate.multiply(WEIGHT_PLAN_COMPLETION)
                .add(snapshot.masteryRate.multiply(WEIGHT_KNOWLEDGE_MASTERY))
                .add(snapshot.testPaperAverage.multiply(WEIGHT_TEST_PAPER))
                .add(snapshot.assessmentReference.multiply(WEIGHT_ENTRY_ASSESSMENT))
                .setScale(2, RoundingMode.HALF_UP);
        applyAbilityCalibration(userId, snapshot);
        snapshot.predictedPassRate = clampScoreDecimal(snapshot.basePredictedPassRate.add(snapshot.abilityCalibrationDelta));
        snapshot.predictionConfidence = calculatePredictionConfidence(plan, masteryList, completedReports, assessmentHistory, snapshot);
        return snapshot;
    }

    private void applyAbilityCalibration(Long userId, GrowthScoringSnapshot snapshot) {
        if (userId == null) {
            return;
        }
        UserAbilityProfile profile = userAbilityProfileMapper.selectOne(new LambdaQueryWrapper<UserAbilityProfile>()
                .eq(UserAbilityProfile::getUserId, userId)
                .last("limit 1"));
        if (profile == null) {
            return;
        }
        BigDecimal theta = profile.getPreferredSubjectTheta() == null ? profile.getOverallTheta() : profile.getPreferredSubjectTheta();
        snapshot.abilityConfidence = profile.getAbilityConfidence() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : profile.getAbilityConfidence().setScale(2, RoundingMode.HALF_UP);
        snapshot.abilityScore = theta == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : clampScoreDecimal(BigDecimal.valueOf(50D + theta.doubleValue() * 12.5D));
        snapshot.abilityCalibratedAt = profile.getCalibratedAt();
        if (snapshot.abilityConfidence.compareTo(ABILITY_CONFIDENCE_THRESHOLD) < 0) {
            return;
        }
        BigDecimal rawDelta = snapshot.abilityScore.subtract(snapshot.basePredictedPassRate)
                .multiply(BigDecimal.valueOf(0.10D))
                .setScale(2, RoundingMode.HALF_UP);
        if (rawDelta.compareTo(MAX_ABILITY_CALIBRATION_DELTA) > 0) {
            rawDelta = MAX_ABILITY_CALIBRATION_DELTA;
        }
        if (rawDelta.compareTo(MAX_ABILITY_CALIBRATION_DELTA.negate()) < 0) {
            rawDelta = MAX_ABILITY_CALIBRATION_DELTA.negate();
        }
        snapshot.abilityCalibrationDelta = rawDelta.setScale(2, RoundingMode.HALF_UP);
        snapshot.abilityCalibrationApplied = true;
    }

    private BigDecimal calculatePredictionConfidence(UserStudyPlan plan,
                                                     List<UserKnowledgeMastery> masteryList,
                                                     List<Map<String, Object>> completedReports,
                                                     List<Map<String, Object>> assessmentHistory,
                                                     GrowthScoringSnapshot snapshot) {
        long completedAssessmentCount = assessmentHistory.stream()
                .filter(item -> "COMPLETED".equals(item.get("assessmentStatus")))
                .count();
        BigDecimal confidence = BigDecimal.valueOf(48)
                .add(BigDecimal.valueOf(Math.min(completedAssessmentCount, 3) * 7L))
                .add(BigDecimal.valueOf(Math.min(completedReports.size(), 4) * 4L))
                .add(BigDecimal.valueOf(Math.min(masteryList.size(), 6) * 2L))
                .add(BigDecimal.valueOf(plan == null ? 0 : Math.min(splitKnowledgePoints(plan.getFocusKnowledgePoints()).size(), 4) * 2L));
        if (snapshot.assessmentFallback) {
            confidence = confidence.subtract(BigDecimal.valueOf(10));
        }
        if (snapshot.abilityCalibrationApplied) {
            confidence = confidence.add(BigDecimal.valueOf(8));
        }
        if (confidence.compareTo(BigDecimal.valueOf(30)) < 0) {
            confidence = BigDecimal.valueOf(30);
        }
        if (confidence.compareTo(BigDecimal.valueOf(95)) > 0) {
            confidence = BigDecimal.valueOf(95);
        }
        return confidence.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> buildAssessmentPrediction(GrowthScoringSnapshot snapshot,
                                                          List<Map<String, Object>> assessmentHistory) {
        Map<String, Object> latestAssessment = CollectionUtils.isEmpty(assessmentHistory) ? null : assessmentHistory.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("predictedPassRate", snapshot.predictedPassRate);
        result.put("basePredictedPassRate", snapshot.basePredictedPassRate);
        result.put("predictionConfidence", snapshot.predictionConfidence);
        result.put("predictionStage", resolveAssessmentPredictionStage(snapshot.predictedPassRate));
        result.put("predictionStageLabel", resolveAssessmentPredictionStageLabel(resolveAssessmentPredictionStage(snapshot.predictedPassRate)));
        result.put("recommendationText", buildAssessmentRecommendation(snapshot));
        result.put("latestAssessmentAccuracy", snapshot.assessmentReference);
        result.put("latestAssessmentTitle", latestAssessment == null ? null : latestAssessment.get("title"));
        result.put("latestAssessmentSubmittedAt", latestAssessment == null ? null : latestAssessment.get("submittedAt"));
        result.put("assessmentCount", assessmentHistory.size());
        result.put("formulaVersion", snapshot.formulaVersion);
        result.put("weights", buildPredictionWeights());
        result.put("abilityCalibration", buildAbilityCalibrationMap(snapshot));
        List<Map<String, Object>> factors = new ArrayList<>();
        factors.add(buildAssessmentFactor("计划完成率", snapshot.completionRate, WEIGHT_PLAN_COMPLETION, "日常任务完成情况"));
        factors.add(buildAssessmentFactor("知识掌握度", snapshot.masteryRate, WEIGHT_KNOWLEDGE_MASTERY, "知识点掌握基础"));
        factors.add(buildAssessmentFactor("测卷平均分", snapshot.testPaperAverage, WEIGHT_TEST_PAPER, "最近测卷表现"));
        factors.add(buildAssessmentFactor("入学评测分", snapshot.assessmentReference, WEIGHT_ENTRY_ASSESSMENT,
                snapshot.assessmentFallback ? "暂无已完成入学评测，暂用测卷均分作为基准" : "最近一次已完成入学评测"));
        result.put("factorBreakdown", factors);
        result.put("weightedContributions", factors);
        return result;
    }

    private Map<String, Object> buildPredictionWeights() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("planCompletionRate", toWeightPercent(WEIGHT_PLAN_COMPLETION));
        map.put("knowledgeMasteryRate", toWeightPercent(WEIGHT_KNOWLEDGE_MASTERY));
        map.put("testPaperAverageScore", toWeightPercent(WEIGHT_TEST_PAPER));
        map.put("entryAssessmentScore", toWeightPercent(WEIGHT_ENTRY_ASSESSMENT));
        return map;
    }

    private Map<String, Object> buildAbilityCalibrationMap(GrowthScoringSnapshot snapshot) {
        Map<String, Object> map = new HashMap<>();
        map.put("applied", snapshot.abilityCalibrationApplied);
        map.put("delta", snapshot.abilityCalibrationDelta);
        map.put("abilityScore", snapshot.abilityScore);
        map.put("abilityConfidence", snapshot.abilityConfidence);
        map.put("confidenceThreshold", ABILITY_CONFIDENCE_THRESHOLD);
        map.put("calibratedAt", snapshot.abilityCalibratedAt);
        map.put("hint", snapshot.abilityCalibrationApplied
                ? "Rasch 能力画像已纳入通过率校准，校准幅度限制在 ±5 分。"
                : "Rasch 能力样本或置信度不足，当前仅展示不参与通过率校准。");
        return map;
    }

    private Map<String, Object> buildAssessmentFactor(String label, BigDecimal value, BigDecimal weight, String note) {
        BigDecimal normalizedValue = value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> map = new HashMap<>();
        map.put("label", label);
        map.put("value", normalizedValue);
        map.put("weight", toWeightPercent(weight));
        map.put("contribution", normalizedValue.multiply(weight).setScale(2, RoundingMode.HALF_UP));
        map.put("note", note);
        return map;
    }

    private BigDecimal toWeightPercent(BigDecimal weight) {
        return weight.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal clampScoreDecimal(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP);
        if (scaled.compareTo(ONE_HUNDRED) > 0) {
            return ONE_HUNDRED;
        }
        if (scaled.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return scaled;
    }

    private String buildAssessmentRecommendation(GrowthScoringSnapshot snapshot) {
        if (snapshot.predictedPassRate.compareTo(BigDecimal.valueOf(85)) >= 0) {
            return snapshot.abilityCalibrationApplied
                    ? "当前通过率预测较高，Rasch 能力画像也支持该判断，建议切换到模考和错题强化。"
                    : "当前通过率预测较高，建议完成入学评测后直接切换到模考和错题强化。";
        }
        if (snapshot.predictedPassRate.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "当前状态接近达标，建议把评测结果与最近诊断一起复盘，优先处理薄弱项。";
        }
        if (snapshot.predictedPassRate.compareTo(BigDecimal.valueOf(55)) >= 0) {
            return "当前仍处于爬坡期，先完成 " + ENTRY_ASSESSMENT_QUESTION_COUNT + " 题评测，再根据结果回到计划页收口基础任务。";
        }
        return "当前通过率预测偏低，建议先补基础任务和薄弱知识点，再进入阶段测卷或模考。";
    }

    private List<Question> resolveAssessmentQuestions(String subjectName,
                                                      UserStudyPlan plan,
                                                      List<UserKnowledgeMastery> masteryList) {
        List<String> focusKnowledgePoints = resolveAssessmentFocusKnowledgePoints(plan, masteryList);
        List<Question> primaryCandidates = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true)
                .ne(Question::getQuestionType, "ESSAY")
                .eq(StringUtils.hasText(subjectName), Question::getSubjectName, subjectName)
                .orderByDesc(Question::getCreatedAt));
        List<Question> allCandidates = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getEnabled, true)
                .ne(Question::getQuestionType, "ESSAY")
                .orderByDesc(Question::getCreatedAt));
        List<Question> pool = primaryCandidates.size() >= ENTRY_ASSESSMENT_QUESTION_COUNT ? primaryCandidates : allCandidates;
        List<Question> prioritized = pool.stream()
                .filter(item -> matchesKnowledge(item, focusKnowledgePoints))
                .collect(Collectors.toList());
        List<Question> fallback = pool.stream()
                .filter(item -> !prioritized.contains(item))
                .collect(Collectors.toList());
        List<Question> selected = new ArrayList<>();
        Collections.shuffle(prioritized);
        Collections.shuffle(fallback);
        selected.addAll(prioritized);
        selected.addAll(fallback);
        return selected.stream()
                .distinct()
                .limit(ENTRY_ASSESSMENT_QUESTION_COUNT)
                .collect(Collectors.toList());
    }

    private String resolveSubject(List<Question> questions) {
        if (CollectionUtils.isEmpty(questions)) {
            return null;
        }
        Map<String, Long> counts = questions.stream()
                .map(Question::getSubjectName)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(subject -> subject, LinkedHashMap::new, Collectors.counting()));
        if (counts.isEmpty()) {
            return questions.stream()
                    .map(Question::getSubjectName)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(null);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private List<String> resolveAssessmentFocusKnowledgePoints(UserStudyPlan plan, List<UserKnowledgeMastery> masteryList) {
        List<String> focusKnowledgePoints = new ArrayList<>();
        if (plan != null) {
            focusKnowledgePoints.addAll(splitKnowledgePoints(plan.getFocusKnowledgePoints()));
        }
        if (CollectionUtils.isEmpty(focusKnowledgePoints)) {
            focusKnowledgePoints.addAll(masteryList.stream()
                    .sorted(Comparator.comparing(UserKnowledgeMastery::getPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(UserKnowledgeMastery::getMasteryScore, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(UserKnowledgeMastery::getKnowledgePoint)
                    .filter(StringUtils::hasText)
                    .limit(5)
                    .collect(Collectors.toList()));
        }
        return focusKnowledgePoints.stream().distinct().collect(Collectors.toList());
    }

    private String resolveAssessmentPredictionStage(BigDecimal predictedPassRate) {
        if (predictedPassRate == null) {
            return "TRACKING";
        }
        if (predictedPassRate.compareTo(BigDecimal.valueOf(85)) >= 0) {
            return "STABLE";
        }
        if (predictedPassRate.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "PROMISING";
        }
        if (predictedPassRate.compareTo(BigDecimal.valueOf(55)) >= 0) {
            return "TRACKING";
        }
        return "RISK";
    }

    private String resolveAssessmentPredictionStageLabel(String predictionStage) {
        if ("STABLE".equals(predictionStage)) {
            return "通过把握较高";
        }
        if ("PROMISING".equals(predictionStage)) {
            return "通过前景较好";
        }
        if ("TRACKING".equals(predictionStage)) {
            return "仍需持续补强";
        }
        return "当前需要重点关注";
    }

    private String resolveAssessmentStatusLabel(String assessmentStatus) {
        if ("COMPLETED".equals(assessmentStatus)) {
            return "已完成";
        }
        if ("IN_PROGRESS".equals(assessmentStatus)) {
            return "进行中";
        }
        return "待开始";
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

    private Map<String, Object> toAssessmentMap(UserGrowthAssessmentSession session, PracticeRecord practiceRecord) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", session.getId());
        map.put("planId", session.getPlanId());
        map.put("practiceRecordId", session.getPracticeRecordId());
        map.put("assessmentType", session.getAssessmentType());
        map.put("assessmentTypeLabel", resolveAssessmentTypeLabel(session.getAssessmentType()));
        map.put("questionCount", session.getQuestionCount());
        map.put("predictedPassRate", session.getPredictedPassRate());
        map.put("predictedConfidence", session.getPredictedConfidence());
        map.put("predictionStage", session.getPredictionStage());
        map.put("predictionStageLabel", resolveAssessmentPredictionStageLabel(session.getPredictionStage()));
        map.put("weakKnowledgePoints", splitKnowledgePoints(session.getWeakKnowledgePoints()));
        map.put("recommendationText", session.getRecommendationText());
        map.put("assessmentStatus", session.getAssessmentStatus());
        map.put("assessmentStatusLabel", resolveAssessmentStatusLabel(session.getAssessmentStatus()));
        map.put("createdAt", session.getCreatedAt());
        map.put("updatedAt", session.getUpdatedAt());
        map.put("title", practiceRecord == null ? "独立入学评测" : practiceRecord.getTitle());
        map.put("subjectName", practiceRecord == null ? null : practiceRecord.getSubjectName());
        map.put("reportPath", practiceRecord == null ? null : "/practice/report/" + practiceRecord.getId());
        map.put("practicePath", practiceRecord == null ? null : "/practice/" + practiceRecord.getId());
        map.put("submittedAt", practiceRecord == null ? null : practiceRecord.getSubmittedAt());
        map.put("latestAccuracyRate", practiceRecord == null ? null : practiceRecord.getAccuracyRate());
        map.put("latestScore", practiceRecord == null ? null : practiceRecord.getScore());
        return map;
    }

    private List<Map<String, Object>> loadCompletedTestPaperReports(Long userId, String subjectName) {
        List<UserTestPaper> papers = userTestPaperMapper.selectList(new LambdaQueryWrapper<UserTestPaper>()
                .eq(UserTestPaper::getUserId, userId)
                .eq(StringUtils.hasText(subjectName), UserTestPaper::getSubjectName, subjectName)
                .orderByDesc(UserTestPaper::getCreatedAt)
                .orderByDesc(UserTestPaper::getId));
        List<PracticeRecord> testPaperPractices = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getPracticeType, "TEST_PAPER")
                .eq(PracticeRecord::getStatus, "SUBMITTED")
                .orderByDesc(PracticeRecord::getSubmittedAt));
        Map<Long, List<PracticeRecord>> practiceMap = testPaperPractices.stream()
                .filter(item -> parseTestPaperId(item.getSourceSnapshot()) != null)
                .collect(Collectors.groupingBy(item -> parseTestPaperId(item.getSourceSnapshot())));
        return papers.stream()
                .map(item -> toTestPaperMap(item, practiceMap.getOrDefault(item.getId(), Collections.emptyList())))
                .filter(item -> "COMPLETED".equals(item.get("status")))
                .sorted((left, right) -> {
                    LocalDateTime leftTime = (LocalDateTime) left.get("latestSubmittedAt");
                    LocalDateTime rightTime = (LocalDateTime) right.get("latestSubmittedAt");
                    if (leftTime == null && rightTime == null) {
                        return 0;
                    }
                    if (leftTime == null) {
                        return 1;
                    }
                    if (rightTime == null) {
                        return -1;
                    }
                    return rightTime.compareTo(leftTime);
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> loadAssessmentSessions(Long userId,
                                                             Long planId,
                                                             LocalDate startDate,
                                                             LocalDate endDate) {
        List<UserGrowthAssessmentSession> sessions = userGrowthAssessmentSessionMapper.selectList(new LambdaQueryWrapper<UserGrowthAssessmentSession>()
                .eq(UserGrowthAssessmentSession::getUserId, userId)
                .eq(planId != null, UserGrowthAssessmentSession::getPlanId, planId)
                .orderByDesc(UserGrowthAssessmentSession::getCreatedAt)
                .orderByDesc(UserGrowthAssessmentSession::getId));
        if (CollectionUtils.isEmpty(sessions)) {
            return new ArrayList<>();
        }
        List<Long> practiceRecordIds = sessions.stream()
                .map(UserGrowthAssessmentSession::getPracticeRecordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, PracticeRecord> practiceRecordMap = practiceRecordMapper.selectBatchIds(practiceRecordIds)
                .stream()
                .collect(Collectors.toMap(PracticeRecord::getId, Function.identity(), (left, right) -> left));
        return sessions.stream()
                .map(item -> toAssessmentMap(item, practiceRecordMap.get(item.getPracticeRecordId())))
                .filter(item -> withinDateRange(toLocalDate(item.get("createdAt")), startDate, endDate)
                        || withinDateRange(toLocalDate(item.get("submittedAt")), startDate, endDate))
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> loadTeacherFollowUps(Long userId, Long planId) {
        List<TeacherGrowthFocusFollow> follows = teacherGrowthFocusFollowMapper.selectList(new LambdaQueryWrapper<TeacherGrowthFocusFollow>()
                .eq(TeacherGrowthFocusFollow::getUserId, userId)
                .eq(planId != null, TeacherGrowthFocusFollow::getPlanId, planId)
                .orderByDesc(TeacherGrowthFocusFollow::getUpdatedAt)
                .orderByDesc(TeacherGrowthFocusFollow::getId));
        if (CollectionUtils.isEmpty(follows)) {
            return new ArrayList<>();
        }
        Map<Long, Classroom> classroomMap = classroomMapper.selectBatchIds(follows.stream()
                        .map(TeacherGrowthFocusFollow::getClassroomId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Classroom::getId, Function.identity(), (left, right) -> left));
        Map<Long, SysUser> teacherMap = sysUserMapper.selectBatchIds(classroomMap.values().stream()
                        .map(Classroom::getTeacherId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        return follows.stream()
                .limit(5)
                .map(item -> {
                    Classroom classroom = classroomMap.get(item.getClassroomId());
                    SysUser teacher = classroom == null ? null : teacherMap.get(classroom.getTeacherId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("classroomId", item.getClassroomId());
                    map.put("classroomName", classroom == null ? "班级协同" : classroom.getName());
                    map.put("teacherName", teacher == null ? (classroom == null ? "教师" : classroom.getTeacherName()) : teacher.getNickname());
                    map.put("followStatus", item.getFollowStatus());
                    map.put("followStatusLabel", resolveFollowStatusLabel(item.getFollowStatus()));
                    map.put("priorityLevel", item.getPriorityLevel());
                    map.put("priorityLevelLabel", resolvePriorityLabel(item.getPriorityLevel()));
                    map.put("remark", item.getRemark());
                    map.put("nextFollowUpAt", item.getNextFollowUpAt());
                    map.put("updatedAt", item.getUpdatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> loadAdminFollowUp(Long userId, Long planId) {
        LambdaQueryWrapper<AdminGrowthFocusFollow> wrapper = new LambdaQueryWrapper<AdminGrowthFocusFollow>()
                .eq(AdminGrowthFocusFollow::getUserId, userId)
                .orderByDesc(AdminGrowthFocusFollow::getUpdatedAt)
                .orderByDesc(AdminGrowthFocusFollow::getId)
                .last("LIMIT 1");
        if (planId == null) {
            wrapper.isNull(AdminGrowthFocusFollow::getPlanId);
        } else {
            wrapper.eq(AdminGrowthFocusFollow::getPlanId, planId);
        }
        AdminGrowthFocusFollow follow = adminGrowthFocusFollowMapper.selectOne(wrapper);
        if (follow == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("followStatus", follow.getFollowStatus());
        map.put("followStatusLabel", resolveFollowStatusLabel(follow.getFollowStatus()));
        map.put("priorityLevel", follow.getPriorityLevel());
        map.put("priorityLevelLabel", resolvePriorityLabel(follow.getPriorityLevel()));
        map.put("remark", follow.getRemark());
        map.put("nextFollowUpAt", follow.getNextFollowUpAt());
        map.put("updatedAt", follow.getUpdatedAt());
        return map;
    }

    private List<Map<String, Object>> loadTeacherFollowTimeline(Long userId, Long planId) {
        List<TeacherGrowthFocusFollowHistory> histories = teacherGrowthFocusFollowHistoryMapper.selectList(new LambdaQueryWrapper<TeacherGrowthFocusFollowHistory>()
                .eq(TeacherGrowthFocusFollowHistory::getUserId, userId)
                .eq(planId != null, TeacherGrowthFocusFollowHistory::getPlanId, planId)
                .orderByDesc(TeacherGrowthFocusFollowHistory::getCreatedAt)
                .orderByDesc(TeacherGrowthFocusFollowHistory::getId)
                .last("LIMIT 10"));
        if (CollectionUtils.isEmpty(histories)) {
            return Collections.emptyList();
        }
        Map<Long, Classroom> classroomMap = classroomMapper.selectBatchIds(histories.stream()
                        .map(TeacherGrowthFocusFollowHistory::getClassroomId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Classroom::getId, Function.identity(), (left, right) -> left));
        Map<Long, SysUser> operatorMap = buildUserMap(histories.stream()
                .map(TeacherGrowthFocusFollowHistory::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return histories.stream()
                .map(item -> {
                    Classroom classroom = classroomMap.get(item.getClassroomId());
                    SysUser operator = operatorMap.get(item.getOperatorId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("sourceType", "TEACHER");
                    map.put("sourceTypeLabel", "教师跟进");
                    map.put("classroomId", item.getClassroomId());
                    map.put("classroomName", classroom == null ? "班级协同" : classroom.getName());
                    map.put("followStatus", item.getFollowStatus());
                    map.put("followStatusLabel", resolveFollowStatusLabel(item.getFollowStatus()));
                    map.put("priorityLevel", item.getPriorityLevel());
                    map.put("priorityLevelLabel", resolvePriorityLabel(item.getPriorityLevel()));
                    map.put("remark", item.getRemark());
                    map.put("actionType", item.getActionType());
                    map.put("actionTypeLabel", resolveFollowActionLabel(item.getActionType()));
                    map.put("nextFollowUpAt", item.getNextFollowUpAt());
                    map.put("operatorName", operator == null ? "系统" : operator.getNickname());
                    map.put("createdAt", item.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> loadAdminFollowTimeline(Long userId, Long planId) {
        List<AdminGrowthFocusFollowHistory> histories = adminGrowthFocusFollowHistoryMapper.selectList(new LambdaQueryWrapper<AdminGrowthFocusFollowHistory>()
                .eq(AdminGrowthFocusFollowHistory::getUserId, userId)
                .eq(planId != null, AdminGrowthFocusFollowHistory::getPlanId, planId)
                .orderByDesc(AdminGrowthFocusFollowHistory::getCreatedAt)
                .orderByDesc(AdminGrowthFocusFollowHistory::getId)
                .last("LIMIT 10"));
        if (CollectionUtils.isEmpty(histories)) {
            return Collections.emptyList();
        }
        Map<Long, SysUser> operatorMap = buildUserMap(histories.stream()
                .map(AdminGrowthFocusFollowHistory::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return histories.stream()
                .map(item -> {
                    SysUser operator = operatorMap.get(item.getOperatorId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("sourceType", "ADMIN");
                    map.put("sourceTypeLabel", "后台运营");
                    map.put("followStatus", item.getFollowStatus());
                    map.put("followStatusLabel", resolveFollowStatusLabel(item.getFollowStatus()));
                    map.put("priorityLevel", item.getPriorityLevel());
                    map.put("priorityLevelLabel", resolvePriorityLabel(item.getPriorityLevel()));
                    map.put("remark", item.getRemark());
                    map.put("actionType", item.getActionType());
                    map.put("actionTypeLabel", resolveFollowActionLabel(item.getActionType()));
                    map.put("nextFollowUpAt", item.getNextFollowUpAt());
                    map.put("operatorName", operator == null ? "系统" : operator.getNickname());
                    map.put("createdAt", item.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildCollaborationTimeline(List<Map<String, Object>> teacherTimeline,
                                                                 List<Map<String, Object>> adminTimeline) {
        List<Map<String, Object>> merged = new ArrayList<>();
        merged.addAll(teacherTimeline);
        merged.addAll(adminTimeline);
        return merged.stream()
                .sorted((left, right) -> compareDateTime((LocalDateTime) right.get("createdAt"), (LocalDateTime) left.get("createdAt")))
                .limit(12)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildCollaborationSummary(List<Map<String, Object>> teacherFollowUps,
                                                          Map<String, Object> adminFollowUp,
                                                          List<Map<String, Object>> teacherTimeline,
                                                          List<Map<String, Object>> adminTimeline) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("teacherFollowCount", teacherTimeline.size());
        summary.put("adminFollowCount", adminTimeline.size());
        summary.put("teacherClassroomCount", teacherFollowUps.stream()
                .map(item -> item.get("classroomId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size());
        summary.put("hasAdminFollow", adminFollowUp != null);
        summary.put("latestFollowAt", Stream.concat(teacherTimeline.stream(), adminTimeline.stream())
                .map(item -> (LocalDateTime) item.get("createdAt"))
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        summary.put("nextFollowUpAt", Stream.concat(
                        teacherFollowUps.stream().map(item -> (LocalDateTime) item.get("nextFollowUpAt")),
                        Stream.of(adminFollowUp == null ? null : (LocalDateTime) adminFollowUp.get("nextFollowUpAt")))
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null));
        summary.put("openFollowCount", Stream.concat(teacherTimeline.stream(), adminTimeline.stream())
                .map(item -> String.valueOf(item.get("followStatus")))
                .filter(status -> !"DONE".equals(status))
                .count());
        return summary;
    }

    private Map<String, Object> buildEmptyCollaborationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("teacherFollowCount", 0);
        summary.put("adminFollowCount", 0);
        summary.put("teacherClassroomCount", 0);
        summary.put("hasAdminFollow", false);
        summary.put("latestFollowAt", null);
        summary.put("nextFollowUpAt", null);
        summary.put("openFollowCount", 0);
        return summary;
    }

    private Map<String, Object> buildDiagnosisFilterState(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("startDate", startDate);
        filters.put("endDate", endDate);
        return filters;
    }

    private boolean withinDateRange(LocalDate value, LocalDate startDate, LocalDate endDate) {
        if (value == null) {
            return false;
        }
        if (startDate != null && value.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && value.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalDate();
        }
        if (value != null) {
            try {
                return LocalDate.parse(String.valueOf(value).substring(0, 10));
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    private String resolveLatestFollowRemark(List<Map<String, Object>> collaborationTimeline, String sourceType) {
        return collaborationTimeline.stream()
                .filter(item -> sourceType.equals(item.get("sourceType")))
                .map(item -> {
                    String status = String.valueOf(item.get("followStatusLabel"));
                    String remark = item.get("remark") == null ? "" : String.valueOf(item.get("remark"));
                    return StringUtils.hasText(remark) ? status + "：" + remark : status;
                })
                .findFirst()
                .orElse("");
    }

    private Map<Long, SysUser> buildUserMap(Set<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
    }

    private int compareDateTime(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private Map<String, Object> buildTestPaperReportSummary(List<Map<String, Object>> reports) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("completedCount", reports.size());
        BigDecimal averageAccuracy = reports.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : reports.stream()
                .map(item -> safeDecimal(item.get("latestAccuracyRate")))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(reports.size()), 2, RoundingMode.HALF_UP);
        summary.put("averageAccuracyRate", averageAccuracy);
        summary.put("bestAccuracyRate", reports.stream()
                .map(item -> safeDecimal(item.get("latestAccuracyRate")))
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)));
        summary.put("latestSubmittedAt", reports.stream()
                .map(item -> (LocalDateTime) item.get("latestSubmittedAt"))
                .filter(item -> item != null)
                .max(LocalDateTime::compareTo)
                .orElse(null));
    summary.put("highScoreCount", reports.stream()
                .filter(item -> safeDecimal(item.get("latestAccuracyRate")).compareTo(BigDecimal.valueOf(80)) >= 0)
                .count());
        return summary;
    }

    private Map<String, Object> buildKnowledgeSummary(List<UserKnowledgeMastery> allRecords) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("knowledgeCount", allRecords.size());
        summary.put("weakKnowledgeCount", allRecords.stream().filter(item -> "WEAK".equals(item.getMasteryLevel())).count());
        summary.put("masteredCount", allRecords.stream().filter(item -> "MASTERED".equals(item.getMasteryLevel())).count());
        summary.put("pendingWrongCount", allRecords.stream().mapToInt(item -> item.getPendingWrongCount() == null ? 0 : item.getPendingWrongCount()).sum());
        BigDecimal averageMastery = allRecords.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : allRecords.stream()
                .map(UserKnowledgeMastery::getMasteryScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(allRecords.size()), 2, RoundingMode.HALF_UP);
        summary.put("averageMasteryScore", averageMastery);
        return summary;
    }

    private Map<String, Object> toKnowledgeMap(UserKnowledgeMastery item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("subjectName", item.getSubjectName());
        map.put("knowledgePoint", item.getKnowledgePoint());
        map.put("attemptCount", item.getAttemptCount());
        map.put("correctCount", item.getCorrectCount());
        map.put("wrongCount", item.getWrongCount());
        map.put("pendingWrongCount", item.getPendingWrongCount());
        map.put("accuracyRate", item.getAccuracyRate());
        map.put("masteryScore", item.getMasteryScore());
        map.put("masteryConfidence", item.getMasteryConfidence());
        map.put("priorityScore", item.getPriorityScore());
        map.put("masteryLevel", item.getMasteryLevel());
        map.put("masteryLevelLabel", resolveMasteryLevelLabel(item.getMasteryLevel()));
        map.put("knowledgeStatus", item.getKnowledgeStatus());
        map.put("knowledgeStatusLabel", resolveKnowledgeStatusLabel(item.getKnowledgeStatus()));
        map.put("lastPracticedAt", item.getLastPracticedAt());
        map.put("diagnosis", buildKnowledgeDiagnosis(item));
        map.put("suggestedAction", resolveSuggestedAction(item));
        map.put("practicePath", "/question-bank");
        map.put("testPaperPath", "/user/test-paper");
        return map;
    }

    private String buildKnowledgeDiagnosis(UserKnowledgeMastery item) {
        if ("TO_DIAGNOSE".equals(item.getKnowledgeStatus())) {
            return "当前样本量还不足，建议先完成诊断或专项练习，再判断是否为真实薄弱项。";
        }
        if ("MASTERED".equals(item.getMasteryLevel())) {
            return "当前掌握度较高，建议用测卷维持手感并扩大题量。";
        }
        if ("IMPROVING".equals(item.getMasteryLevel())) {
            return "当前处于提升期，建议继续做专项题并定期用测卷检验。";
        }
        if ("FOUNDATION".equals(item.getMasteryLevel())) {
            return "基础正在建立，建议先完成专项练习，再进入个性化测卷。";
        }
        return "当前仍是明显薄弱项，建议优先刷题并尽快做一套针对性强化测卷。";
    }

    private String resolveKnowledgeStatusLabel(String knowledgeStatus) {
        if ("TO_DIAGNOSE".equals(knowledgeStatus)) {
            return "待诊断";
        }
        return resolveMasteryLevelLabel(knowledgeStatus);
    }

    private String resolveSuggestedAction(UserKnowledgeMastery item) {
        if ("TO_DIAGNOSE".equals(item.getKnowledgeStatus())) {
            return "TEST_PAPER";
        }
        if ("MASTERED".equals(item.getMasteryLevel())) {
            return "REVIEW";
        }
        if ("FOUNDATION".equals(item.getMasteryLevel())) {
            return "PRACTICE";
        }
        return "TEST_PAPER";
    }

    private void createTaskAchievement(UserDailyTask task) {
        Long existed = userGrowthAchievementMapper.selectCount(new LambdaQueryWrapper<UserGrowthAchievement>()
                .eq(UserGrowthAchievement::getTaskId, task.getId())
                .eq(UserGrowthAchievement::getAchievementType, "TASK_COMPLETED"));
        if (existed != null && existed > 0) {
            return;
        }
        UserGrowthAchievement achievement = new UserGrowthAchievement();
        achievement.setUserId(task.getUserId());
        achievement.setPlanId(task.getPlanId());
        achievement.setTaskId(task.getId());
        achievement.setAchievementType("TASK_COMPLETED");
        achievement.setTitle("完成" + resolveTaskTypeLabel(task.getTaskType()) + "任务");
        achievement.setDescriptionText(task.getTitle() + " 已达成，完成值 " + task.getCompletedValue() + " / " + task.getTargetValue() + " " + resolveTaskUnit(task.getTaskType()) + "。");
        achievement.setAchievementValue(task.getCompletedValue());
        userGrowthAchievementMapper.insert(achievement);
    }

    private void createPlanArchivedAchievement(UserStudyPlan plan) {
        Long existed = userGrowthAchievementMapper.selectCount(new LambdaQueryWrapper<UserGrowthAchievement>()
                .eq(UserGrowthAchievement::getPlanId, plan.getId())
                .eq(UserGrowthAchievement::getAchievementType, "PLAN_ARCHIVED"));
        if (existed != null && existed > 0) {
            return;
        }
        UserGrowthAchievement achievement = new UserGrowthAchievement();
        achievement.setUserId(plan.getUserId());
        achievement.setPlanId(plan.getId());
        achievement.setAchievementType("PLAN_ARCHIVED");
        achievement.setTitle("成长计划归档");
        achievement.setDescriptionText(plan.getPlanName() + " 已归档，完成率 " + (plan.getCompletionRate() == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : plan.getCompletionRate()) + "%。");
        achievement.setAchievementValue(plan.getCompletionRate() == null ? 0 : plan.getCompletionRate().intValue());
        userGrowthAchievementMapper.insert(achievement);
    }

    private Map<String, Object> buildTestPaperSummary(List<Map<String, Object>> records) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("paperCount", records.size());
        summary.put("completedCount", records.stream().filter(item -> "COMPLETED".equals(item.get("status"))).count());
        List<BigDecimal> scores = records.stream()
                .map(item -> item.get("latestScore"))
                .filter(item -> item instanceof Integer)
                .map(item -> BigDecimal.valueOf((Integer) item))
                .collect(Collectors.toList());
        BigDecimal averageScore = scores.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        summary.put("averageScore", averageScore);
        summary.put("latestGeneratedAt", records.stream()
                .map(item -> (LocalDateTime) item.get("createdAt"))
                .filter(item -> item != null)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        return summary;
    }

    private Map<String, Object> toTestPaperMap(UserTestPaper paper, List<PracticeRecord> practices) {
        PracticeRecord latestPractice = practices.stream()
                .max(Comparator.comparing(PracticeRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        PracticeRecord latestSubmitted = practices.stream()
                .filter(item -> "SUBMITTED".equals(item.getStatus()))
                .max(Comparator.comparing(PracticeRecord::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        Map<String, Object> map = new HashMap<>();
        map.put("id", paper.getId());
        map.put("title", paper.getTitle());
        map.put("subjectName", paper.getSubjectName());
        map.put("categoryName", paper.getCategoryName());
        map.put("difficultyLevel", paper.getDifficultyLevel());
        map.put("questionCount", paper.getQuestionCount());
        map.put("generateMode", paper.getGenerateMode());
        map.put("generateModeLabel", resolvePaperModeLabel(paper.getGenerateMode()));
        map.put("paperType", paper.getPaperType());
        map.put("paperTypeLabel", resolvePaperTypeLabel(paper.getPaperType()));
        map.put("knowledgePoints", splitKnowledgePoints(paper.getKnowledgePoints()));
        map.put("targetKnowledgePoints", splitKnowledgePoints(paper.getTargetKnowledgePoints()));
        map.put("recommendationReason", paper.getRecommendationReason());
        map.put("generationSnapshot", paper.getGenerationSnapshot());
        map.put("difficultyDistribution", paper.getDifficultyDistribution());
        map.put("sourceStrategy", paper.getSourceStrategy());
        map.put("createdAt", paper.getCreatedAt());
        map.put("attemptCount", practices.size());
        map.put("latestPracticeId", latestPractice == null ? null : latestPractice.getId());
        map.put("latestScore", latestSubmitted == null ? null : latestSubmitted.getScore());
        map.put("latestAccuracyRate", latestSubmitted == null ? null : latestSubmitted.getAccuracyRate());
        map.put("latestSubmittedAt", latestSubmitted == null ? null : latestSubmitted.getSubmittedAt());
        map.put("status", resolvePaperStatus(latestPractice, latestSubmitted));
        map.put("reportPath", latestSubmitted == null ? null : "/practice/report/" + latestSubmitted.getId());
        return map;
    }

    private String resolvePaperStatus(PracticeRecord latestPractice, PracticeRecord latestSubmitted) {
        if (latestSubmitted != null) {
            return "COMPLETED";
        }
        if (latestPractice != null) {
            return "IN_PROGRESS";
        }
        return "GENERATED";
    }

    private String resolveTaskStatus(int completedValue, Integer targetValue) {
        int target = targetValue == null ? 0 : targetValue;
        if (target <= 0) {
            return completedValue > 0 ? "COMPLETED" : "PENDING";
        }
        if (completedValue >= target) {
            return "COMPLETED";
        }
        if (completedValue > 0) {
            return "IN_PROGRESS";
        }
        return "PENDING";
    }

    private String resolveTaskTypeLabel(String taskType) {
        if ("CHECK_IN".equals(taskType)) {
            return "学习打卡";
        }
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

    private String resolveTaskStatusLabel(String status) {
        if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("IN_PROGRESS".equals(status)) {
            return "进行中";
        }
        return "待完成";
    }

    private String resolveTaskActionText(String taskType) {
        if ("CHECK_IN".equals(taskType)) {
            return "去打卡";
        }
        if ("LEARN".equals(taskType)) {
            return "去学习";
        }
        if ("PRACTICE".equals(taskType)) {
            return "去刷题";
        }
        if ("EXAM".equals(taskType)) {
            return "去模考";
        }
        return "去生成";
    }

    private String resolveTaskUnit(String taskType) {
        if ("CHECK_IN".equals(taskType)) {
            return "次";
        }
        if ("LEARN".equals(taskType)) {
            return "分钟";
        }
        if ("PRACTICE".equals(taskType)) {
            return "题";
        }
        if ("EXAM".equals(taskType)) {
            return "次";
        }
        return "套";
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

    private String resolveFollowActionLabel(String actionType) {
        if ("CREATE".equals(actionType)) {
            return "首次建档";
        }
        if ("BATCH_UPDATE".equals(actionType)) {
            return "批量跟进";
        }
        if ("UPDATE".equals(actionType)) {
            return "更新跟进";
        }
        return "跟进记录";
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

    private String resolvePaperModeLabel(String generateMode) {
        if ("PLAN".equals(generateMode)) {
            return "计划驱动";
        }
        if ("CUSTOM".equals(generateMode)) {
            return "自定义筛选";
        }
        if ("WRONG_BOOK".equals(generateMode)) {
            return "错题强化";
        }
        return "薄弱项推荐";
    }

    private String resolvePaperTypeLabel(String paperType) {
        if ("DIAGNOSIS".equals(paperType)) {
            return "诊断卷";
        }
        if ("CONSOLIDATION".equals(paperType)) {
            return "巩固卷";
        }
        if ("MIXED".equals(paperType)) {
            return "混合卷";
        }
        return "补弱卷";
    }

    private String resolveDiagnosisStage(BigDecimal vitalityScore) {
        BigDecimal score = vitalityScore == null ? BigDecimal.ZERO : vitalityScore;
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "STABLE";
        }
        if (score.compareTo(BigDecimal.valueOf(55)) >= 0) {
            return "RISING";
        }
        if (score.compareTo(BigDecimal.valueOf(35)) >= 0) {
            return "TRACKING";
        }
        return "AT_RISK";
    }

    private String resolveDiagnosisStageLabel(String diagnosisStage) {
        if ("STABLE".equals(diagnosisStage)) {
            return "稳定冲刺";
        }
        if ("RISING".equals(diagnosisStage)) {
            return "持续上升";
        }
        if ("TRACKING".equals(diagnosisStage)) {
            return "跟踪补强";
        }
        return "重点预警";
    }

    private String buildDiagnosisRecommendation(BigDecimal completionRate,
                                               BigDecimal masteryRate,
                                               BigDecimal testPaperAverage,
                                               String weakPoints) {
        if (completionRate.compareTo(BigDecimal.valueOf(40)) < 0) {
            return "计划完成率偏低，建议先收口每日任务，再推进新的练习和模考。";
        }
        if (masteryRate.compareTo(BigDecimal.valueOf(40)) < 0) {
            return "知识点掌握度偏弱，优先围绕「" + defaultWeakPoints(weakPoints) + "」做专项补强。";
        }
        if (testPaperAverage.compareTo(BigDecimal.valueOf(60)) < 0) {
            return "测卷稳定性仍不足，建议每 2-3 天完成一套强化测卷并复盘报告。";
        }
        return "当前节奏较稳，继续保持计划执行，并逐步把薄弱项转成稳定得分项。";
    }

    private String defaultWeakPoints(String weakPoints) {
        return StringUtils.hasText(weakPoints) ? weakPoints.replace(",", " / ") : "当前重点知识点";
    }

    private BigDecimal calculatePercent(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal percent = BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        return percent.compareTo(ONE_HUNDRED) > 0 ? ONE_HUNDRED : percent;
    }

    private boolean matchesKnowledge(Question question, List<String> focusKnowledgePoints) {
        if (CollectionUtils.isEmpty(focusKnowledgePoints)) {
            return true;
        }
        String knowledge = question.getKnowledgePoints();
        if (!StringUtils.hasText(knowledge)) {
            return false;
        }
        for (String point : focusKnowledgePoints) {
            if (knowledge.contains(point)) {
                return true;
            }
        }
        return false;
    }

    private String resolvePaperType(TestPaperGenerateRequest request, List<UserKnowledgeMastery> masteryList) {
        if (StringUtils.hasText(request.getPaperType())) {
            return request.getPaperType().trim().toUpperCase();
        }
        if (Boolean.TRUE.equals(request.getDiagnosisMode())) {
            return "DIAGNOSIS";
        }
        boolean lowConfidenceOnly = masteryList.isEmpty() || masteryList.stream()
                .noneMatch(item -> item.getMasteryConfidence() != null && item.getMasteryConfidence().compareTo(BigDecimal.valueOf(40)) >= 0);
        if (lowConfidenceOnly) {
            return "DIAGNOSIS";
        }
        return "WEAKNESS";
    }

    private int resolveQuestionCount(Integer requestedQuestionCount, String paperType) {
        if (requestedQuestionCount != null) {
            return requestedQuestionCount;
        }
        return "DIAGNOSIS".equals(paperType) ? 10 : 5;
    }

    private List<String> resolveTargetKnowledgePoints(TestPaperGenerateRequest request,
                                                      UserStudyPlan plan,
                                                      List<UserKnowledgeMastery> masteryList,
                                                      List<Question> baseQuestions,
                                                      String paperType) {
        if (!CollectionUtils.isEmpty(request.getTargetKnowledgePoints())) {
            return request.getTargetKnowledgePoints().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (StringUtils.hasText(request.getKnowledgePoint())) {
            return splitKnowledgePoints(request.getKnowledgePoint());
        }
        if ("PLAN".equals(request.getGenerateMode()) && plan != null) {
            return splitKnowledgePoints(plan.getFocusKnowledgePoints());
        }
        if ("CUSTOM".equals(request.getGenerateMode())) {
            return new ArrayList<>();
        }
        List<String> prioritized = masteryList.stream()
                .sorted(Comparator.comparing(UserKnowledgeMastery::getPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UserKnowledgeMastery::getMasteryScore, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(UserKnowledgeMastery::getKnowledgePoint)
                .filter(StringUtils::hasText)
                .distinct()
                .limit("DIAGNOSIS".equals(paperType) ? 5 : 4)
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(prioritized)) {
            List<String> planFocused = plan == null ? new ArrayList<>() : splitKnowledgePoints(plan.getFocusKnowledgePoints());
            if (!CollectionUtils.isEmpty(planFocused) && prioritized.stream().noneMatch(planFocused::contains)) {
                prioritized.add(0, planFocused.get(0));
            }
            return prioritized.stream().distinct().limit("DIAGNOSIS".equals(paperType) ? 5 : 4).collect(Collectors.toList());
        }
        if (plan != null && StringUtils.hasText(plan.getFocusKnowledgePoints())) {
            return splitKnowledgePoints(plan.getFocusKnowledgePoints());
        }
        return baseQuestions.stream()
                .flatMap(question -> splitKnowledgePoints(question.getKnowledgePoints()).stream())
                .filter(StringUtils::hasText)
                .distinct()
                .limit("DIAGNOSIS".equals(paperType) ? 5 : 3)
                .collect(Collectors.toList());
    }

    private BigDecimal resolveSubjectMasteryAverage(List<UserKnowledgeMastery> masteryList,
                                                    String subjectName,
                                                    List<String> focusKnowledgePoints) {
        Stream<UserKnowledgeMastery> stream = masteryList.stream()
                .filter(item -> !StringUtils.hasText(subjectName) || Objects.equals(subjectName, item.getSubjectName()));
        if (!CollectionUtils.isEmpty(focusKnowledgePoints)) {
            List<String> focusSet = focusKnowledgePoints;
            stream = stream.filter(item -> focusSet.contains(item.getKnowledgePoint()));
        }
        List<BigDecimal> scores = stream.map(UserKnowledgeMastery::getMasteryScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (scores.isEmpty()) {
            return BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
        }
        return scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private String resolveDifficultyStrategy(String requestedStrategy, BigDecimal subjectMasteryAverage) {
        if (StringUtils.hasText(requestedStrategy)) {
            return requestedStrategy.trim().toUpperCase();
        }
        BigDecimal average = subjectMasteryAverage == null ? BigDecimal.valueOf(50) : subjectMasteryAverage;
        if (average.compareTo(BigDecimal.valueOf(45)) < 0) {
            return "BOOST";
        }
        if (average.compareTo(BigDecimal.valueOf(70)) <= 0) {
            return "BALANCED";
        }
        return "CHALLENGE";
    }

    private Set<Long> loadRecentQuestionIds(Long userId, int excludeRecentDays) {
        if (excludeRecentDays <= 0) {
            return new LinkedHashSet<>();
        }
        LocalDateTime threshold = LocalDateTime.now().minusDays(excludeRecentDays);
        Set<Long> practiceRecordIds = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .ge(PracticeRecord::getCreatedAt, threshold))
                .stream()
                .map(PracticeRecord::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> examRecordIds = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .ge(ExamRecord::getCreatedAt, threshold))
                .stream()
                .map(ExamRecord::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> recentQuestionIds = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(practiceRecordIds)) {
            recentQuestionIds.addAll(practiceAnswerMapper.selectList(new LambdaQueryWrapper<PracticeAnswer>()
                            .in(PracticeAnswer::getPracticeRecordId, practiceRecordIds))
                    .stream()
                    .map(PracticeAnswer::getQuestionId)
                    .collect(Collectors.toSet()));
        }
        if (!CollectionUtils.isEmpty(examRecordIds)) {
            recentQuestionIds.addAll(examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                            .in(ExamAnswer::getExamRecordId, examRecordIds))
                    .stream()
                    .map(ExamAnswer::getQuestionId)
                    .collect(Collectors.toSet()));
        }
        return recentQuestionIds;
    }

    private List<Question> selectQuestionsForPaper(List<Question> baseQuestions,
                                                   List<String> targetKnowledgePoints,
                                                   List<UserKnowledgeMastery> masteryList,
                                                   Set<Long> wrongQuestionIds,
                                                   Set<Long> recentQuestionIds,
                                                   int questionCount,
                                                   boolean onlyWrong,
                                                   String paperType,
                                                   String difficultyStrategy) {
        if (CollectionUtils.isEmpty(baseQuestions)) {
            return new ArrayList<>();
        }
        List<Question> filtered = baseQuestions.stream()
                .filter(item -> !onlyWrong || wrongQuestionIds.contains(item.getId()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(filtered)) {
            return new ArrayList<>();
        }
        List<Question> targetScoped = CollectionUtils.isEmpty(targetKnowledgePoints)
                ? filtered
                : filtered.stream().filter(item -> matchesKnowledge(item, targetKnowledgePoints)).collect(Collectors.toList());
        List<Question> preferred = targetScoped.stream()
                .filter(item -> !recentQuestionIds.contains(item.getId()))
                .collect(Collectors.toList());
        List<Question> candidatePool = !CollectionUtils.isEmpty(preferred) ? preferred : targetScoped;
        if (CollectionUtils.isEmpty(candidatePool)) {
            candidatePool = filtered.stream()
                    .filter(item -> !recentQuestionIds.contains(item.getId()))
                    .collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(candidatePool)) {
            candidatePool = filtered;
        }
        if ("DIAGNOSIS".equals(paperType)) {
            return selectDiagnosisQuestions(candidatePool, targetKnowledgePoints, questionCount, wrongQuestionIds, recentQuestionIds);
        }
        return selectStrengtheningQuestions(candidatePool, targetKnowledgePoints, masteryList, wrongQuestionIds, recentQuestionIds, questionCount, difficultyStrategy, paperType);
    }

    private List<Question> selectDiagnosisQuestions(List<Question> candidates,
                                                    List<String> targetKnowledgePoints,
                                                    int questionCount,
                                                    Set<Long> wrongQuestionIds,
                                                    Set<Long> recentQuestionIds) {
        Map<String, List<Question>> byKnowledge = new LinkedHashMap<>();
        for (String point : targetKnowledgePoints) {
            byKnowledge.put(point, candidates.stream()
                    .filter(question -> matchesKnowledge(question, Collections.singletonList(point)))
                    .sorted(Comparator.comparingDouble((Question question) -> scoreQuestionCandidate(question, point, wrongQuestionIds, recentQuestionIds, "BALANCED", 0D)).reversed())
                    .collect(Collectors.toList()));
        }
        List<Question> selected = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        boolean progress = true;
        while (selected.size() < questionCount && progress) {
            progress = false;
            for (String point : byKnowledge.keySet()) {
                List<Question> questions = byKnowledge.get(point);
                while (!CollectionUtils.isEmpty(questions)) {
                    Question question = questions.remove(0);
                    if (seen.add(question.getId())) {
                        selected.add(question);
                        progress = true;
                        break;
                    }
                }
                if (selected.size() >= questionCount) {
                    break;
                }
            }
        }
        return selected;
    }

    private List<Question> selectStrengtheningQuestions(List<Question> candidates,
                                                        List<String> targetKnowledgePoints,
                                                        List<UserKnowledgeMastery> masteryList,
                                                        Set<Long> wrongQuestionIds,
                                                        Set<Long> recentQuestionIds,
                                                        int questionCount,
                                                        String difficultyStrategy,
                                                        String paperType) {
        Map<String, UserKnowledgeMastery> masteryMap = masteryList.stream()
                .collect(Collectors.toMap(item -> item.getSubjectName() + "::" + item.getKnowledgePoint(), Function.identity(), (left, right) -> left));
        List<String> primaryPoints = targetKnowledgePoints.stream().limit(1).collect(Collectors.toList());
        List<String> secondaryPoints = targetKnowledgePoints.stream().skip(1).limit(2).collect(Collectors.toList());
        List<String> tertiaryPoints = targetKnowledgePoints.stream().skip(3).collect(Collectors.toList());
        int primaryQuota = Math.max(2, (int) Math.round(questionCount * 0.5D));
        int secondaryQuota = Math.max(1, (int) Math.round(questionCount * 0.3D));
        int tertiaryQuota = Math.max(0, questionCount - primaryQuota - secondaryQuota);
        if ("CONSOLIDATION".equals(paperType)) {
            primaryQuota = Math.max(1, (int) Math.round(questionCount * 0.4D));
            secondaryQuota = Math.max(1, (int) Math.round(questionCount * 0.4D));
            tertiaryQuota = Math.max(0, questionCount - primaryQuota - secondaryQuota);
        }
        List<Question> selected = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        selectByKnowledgeQuota(selected, seen, candidates, primaryPoints, primaryQuota, wrongQuestionIds, recentQuestionIds, difficultyStrategy, masteryMap, 0D);
        selectByKnowledgeQuota(selected, seen, candidates, secondaryPoints, secondaryQuota, wrongQuestionIds, recentQuestionIds, difficultyStrategy, masteryMap, 8D);
        selectByKnowledgeQuota(selected, seen, candidates, tertiaryPoints, tertiaryQuota, wrongQuestionIds, recentQuestionIds, difficultyStrategy, masteryMap, 15D);
        if (selected.size() < questionCount) {
            List<Question> fallback = candidates.stream()
                    .filter(question -> !seen.contains(question.getId()))
                    .sorted(Comparator.comparingDouble((Question question) ->
                            scoreQuestionCandidate(question,
                                    resolvePrimaryKnowledgePoint(question, targetKnowledgePoints),
                                    wrongQuestionIds,
                                    recentQuestionIds,
                                    difficultyStrategy,
                                    resolveDifficultyBoost(question, masteryMap, targetKnowledgePoints))).reversed())
                    .collect(Collectors.toList());
            for (Question question : fallback) {
                if (seen.add(question.getId())) {
                    selected.add(question);
                }
                if (selected.size() >= questionCount) {
                    break;
                }
            }
        }
        return selected;
    }

    private void selectByKnowledgeQuota(List<Question> selected,
                                        Set<Long> seen,
                                        List<Question> candidates,
                                        List<String> targetPoints,
                                        int quota,
                                        Set<Long> wrongQuestionIds,
                                        Set<Long> recentQuestionIds,
                                        String difficultyStrategy,
                                        Map<String, UserKnowledgeMastery> masteryMap,
                                        double difficultyBias) {
        if (quota <= 0 || CollectionUtils.isEmpty(targetPoints)) {
            return;
        }
        List<Question> ranked = candidates.stream()
                .filter(question -> !seen.contains(question.getId()))
                .filter(question -> matchesKnowledge(question, targetPoints))
                .sorted(Comparator.comparingDouble((Question question) ->
                        scoreQuestionCandidate(question,
                                resolvePrimaryKnowledgePoint(question, targetPoints),
                                wrongQuestionIds,
                                recentQuestionIds,
                                difficultyStrategy,
                                resolveDifficultyBoost(question, masteryMap, targetPoints) + difficultyBias)).reversed())
                .collect(Collectors.toList());
        int added = 0;
        for (Question question : ranked) {
            if (seen.add(question.getId())) {
                selected.add(question);
                added = added + 1;
            }
            if (added >= quota) {
                break;
            }
        }
    }

    private double scoreQuestionCandidate(Question question,
                                          String primaryKnowledgePoint,
                                          Set<Long> wrongQuestionIds,
                                          Set<Long> recentQuestionIds,
                                          String difficultyStrategy,
                                          double difficultyBias) {
        double knowledgeMatch = StringUtils.hasText(primaryKnowledgePoint) && matchesKnowledge(question, Collections.singletonList(primaryKnowledgePoint)) ? 100D : 65D;
        double difficultyMatch = Math.max(0D, Math.min(100D, resolveDifficultyMatch(question.getDifficultyLevel(), difficultyStrategy) + difficultyBias));
        double discriminationProxy = question.getScore() == null ? 60D : Math.min(100D, 55D + question.getScore() * 15D);
        if (StringUtils.hasText(question.getAnalysis())) {
            discriminationProxy = Math.min(100D, discriminationProxy + 10D);
        }
        double wrongBookBoost = wrongQuestionIds.contains(question.getId()) ? 100D : 35D;
        double noveltyScore = recentQuestionIds.contains(question.getId()) ? 20D : 100D;
        return knowledgeMatch * 0.45D
                + difficultyMatch * 0.20D
                + discriminationProxy * 0.15D
                + wrongBookBoost * 0.10D
                + noveltyScore * 0.10D;
    }

    private double resolveDifficultyBoost(Question question,
                                          Map<String, UserKnowledgeMastery> masteryMap,
                                          List<String> targetPoints) {
        String point = resolvePrimaryKnowledgePoint(question, targetPoints);
        if (!StringUtils.hasText(point)) {
            return 0D;
        }
        UserKnowledgeMastery mastery = masteryMap.get((question.getSubjectName() == null ? "未分类" : question.getSubjectName()) + "::" + point);
        if (mastery == null || mastery.getMasteryScore() == null) {
            return 0D;
        }
        if (mastery.getMasteryScore().compareTo(BigDecimal.valueOf(45)) < 0) {
            return isEasyDifficulty(question.getDifficultyLevel()) ? 12D : -6D;
        }
        if (mastery.getMasteryScore().compareTo(BigDecimal.valueOf(70)) > 0) {
            return isHardDifficulty(question.getDifficultyLevel()) ? 12D : 0D;
        }
        return isMediumDifficulty(question.getDifficultyLevel()) ? 10D : 0D;
    }

    private String resolvePrimaryKnowledgePoint(Question question, List<String> targetPoints) {
        for (String point : targetPoints) {
            if (matchesKnowledge(question, Collections.singletonList(point))) {
                return point;
            }
        }
        List<String> points = splitKnowledgePoints(question.getKnowledgePoints());
        return points.isEmpty() ? null : points.get(0);
    }

    private double resolveDifficultyMatch(String difficultyLevel, String difficultyStrategy) {
        if ("BOOST".equals(difficultyStrategy)) {
            if (isEasyDifficulty(difficultyLevel)) {
                return 100D;
            }
            if (isMediumDifficulty(difficultyLevel)) {
                return 80D;
            }
            return 35D;
        }
        if ("CHALLENGE".equals(difficultyStrategy)) {
            if (isHardDifficulty(difficultyLevel)) {
                return 100D;
            }
            if (isMediumDifficulty(difficultyLevel)) {
                return 78D;
            }
            return 45D;
        }
        if (isMediumDifficulty(difficultyLevel)) {
            return 100D;
        }
        if (isEasyDifficulty(difficultyLevel) || isHardDifficulty(difficultyLevel)) {
            return 72D;
        }
        return 65D;
    }

    private boolean isEasyDifficulty(String difficultyLevel) {
        String normalized = normalizeDifficultyLevel(difficultyLevel);
        return "EASY".equals(normalized);
    }

    private boolean isMediumDifficulty(String difficultyLevel) {
        String normalized = normalizeDifficultyLevel(difficultyLevel);
        return "MEDIUM".equals(normalized);
    }

    private boolean isHardDifficulty(String difficultyLevel) {
        String normalized = normalizeDifficultyLevel(difficultyLevel);
        return "HARD".equals(normalized);
    }

    private String normalizeDifficultyLevel(String difficultyLevel) {
        String value = StringUtils.hasText(difficultyLevel) ? difficultyLevel.trim().toUpperCase() : "";
        if ("简单".equals(difficultyLevel) || value.contains("EASY") || value.contains("LOW")) {
            return "EASY";
        }
        if ("困难".equals(difficultyLevel) || value.contains("HARD") || value.contains("HIGH")) {
            return "HARD";
        }
        if ("中等".equals(difficultyLevel) || value.contains("MEDIUM") || value.contains("MID")) {
            return "MEDIUM";
        }
        return "MEDIUM";
    }

    private List<String> resolveSelectedKnowledgePoints(List<Question> selected, List<String> focusKnowledgePoints) {
        List<String> resolved = new ArrayList<>();
        for (Question question : selected) {
            List<String> points = splitKnowledgePoints(question.getKnowledgePoints());
            for (String focus : focusKnowledgePoints) {
                if (points.contains(focus)) {
                    resolved.add(focus);
                }
            }
            if (resolved.size() >= 5) {
                break;
            }
        }
        if (!CollectionUtils.isEmpty(resolved)) {
            return resolved.stream().distinct().collect(Collectors.toList());
        }
        return selected.stream()
                .flatMap(question -> splitKnowledgePoints(question.getKnowledgePoints()).stream())
                .filter(StringUtils::hasText)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }

    private String buildDifficultyDistribution(List<Question> selected) {
        long easy = selected.stream().filter(question -> isEasyDifficulty(question.getDifficultyLevel())).count();
        long medium = selected.stream().filter(question -> isMediumDifficulty(question.getDifficultyLevel())).count();
        long hard = selected.stream().filter(question -> isHardDifficulty(question.getDifficultyLevel())).count();
        return "EASY:" + easy + ",MEDIUM:" + medium + ",HARD:" + hard;
    }

    private String buildGenerationSnapshot(String paperType,
                                           String difficultyStrategy,
                                           int excludeRecentDays,
                                           List<String> targetKnowledgePoints,
                                           List<Question> selected,
                                           Set<Long> recentQuestionIds) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("paperType", paperType);
        snapshot.put("difficultyStrategy", difficultyStrategy);
        snapshot.put("excludeRecentDays", excludeRecentDays);
        snapshot.put("targetKnowledgePoints", targetKnowledgePoints);
        snapshot.put("selectedQuestionIds", selected.stream().map(Question::getId).collect(Collectors.toList()));
        snapshot.put("selectedKnowledgeCoverage", selected.stream().collect(Collectors.toMap(Question::getId, question -> splitKnowledgePoints(question.getKnowledgePoints()), (left, right) -> left, LinkedHashMap::new)));
        snapshot.put("recentAvoidanceEnabled", !CollectionUtils.isEmpty(recentQuestionIds));
        snapshot.put("sourceStrategy", PAPER_SOURCE_STRATEGY);
        try {
            return JSON_MAPPER.writeValueAsString(snapshot);
        } catch (Exception ex) {
            return "{\"paperType\":\"" + paperType + "\",\"sourceStrategy\":\"" + PAPER_SOURCE_STRATEGY + "\"}";
        }
    }

    private String buildPaperReason(String generateMode,
                                    String paperType,
                                    boolean onlyWrong,
                                    List<String> knowledgePoints,
                                    UserStudyPlan plan) {
        if (onlyWrong) {
            return "根据你的错题本与未消化错题生成强化测卷。";
        }
        if ("DIAGNOSIS".equals(paperType)) {
            return "当前样本量不足，先生成一套诊断卷来校准知识点掌握度和置信度。";
        }
        if ("PLAN".equals(generateMode) && plan != null) {
            return "根据当前学习计划的主攻科目和重点知识点生成测卷。";
        }
        if (!CollectionUtils.isEmpty(knowledgePoints)) {
            return "根据你的薄弱知识点「" + String.join(" / ", knowledgePoints) + "」生成强化测卷。";
        }
        return "根据当前筛选条件生成的个性化测卷。";
    }

    private String buildPlanName(String targetExamType, String subjectName) {
        return targetExamType + subjectName + "成长计划";
    }

    private String buildTestPaperTitle(String subjectName, int questionCount, String generateMode, String paperType) {
        String prefix = StringUtils.hasText(subjectName) ? subjectName : "综合";
        if ("DIAGNOSIS".equals(paperType)) {
            return prefix + "知识点诊断卷（" + questionCount + "题）";
        }
        if ("CONSOLIDATION".equals(paperType)) {
            return prefix + "巩固测卷（" + questionCount + "题）";
        }
        if ("MIXED".equals(paperType)) {
            return prefix + "混合补强测卷（" + questionCount + "题）";
        }
        if ("WRONG_BOOK".equals(generateMode)) {
            return prefix + "错题强化测卷（" + questionCount + "题）";
        }
        if ("PLAN".equals(generateMode)) {
            return prefix + "计划强化测卷（" + questionCount + "题）";
        }
        if ("CUSTOM".equals(generateMode)) {
            return prefix + "自定义测卷（" + questionCount + "题）";
        }
        return prefix + "薄弱项测卷（" + questionCount + "题）";
    }

    private String resolveProvince(List<Question> questions) {
        return questions.isEmpty() ? null : questions.get(0).getProvince();
    }

    private Long parseTestPaperId(String sourceSnapshot) {
        if (!StringUtils.hasText(sourceSnapshot) || !sourceSnapshot.startsWith("test-paper:")) {
            return null;
        }
        try {
            return Long.parseLong(sourceSnapshot.substring("test-paper:".length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeCsv(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        return joinKnowledgePoints(splitKnowledgePoints(rawValue));
    }

    private BigDecimal toScore(double value) {
        return BigDecimal.valueOf(clampScore(value)).setScale(2, RoundingMode.HALF_UP);
    }

    private double clampScore(double value) {
        if (value < 0D) {
            return 0D;
        }
        if (value > 100D) {
            return 100D;
        }
        return value;
    }

    private String joinKnowledgePoints(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        return values.stream().distinct().collect(Collectors.joining(","));
    }

    private List<String> splitKnowledgePoints(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>();
        for (String item : rawValue.split("[,，]")) {
            String normalized = normalizeKnowledgeValue(item);
            if (StringUtils.hasText(normalized)) {
                values.add(normalized);
            }
        }
        return values.stream().distinct().collect(Collectors.toList());
    }

    private String normalizeKnowledgeSubject(String value) {
        String normalized = normalizeKnowledgeValue(value);
        return StringUtils.hasText(normalized) ? normalized : "未分类";
    }

    private String normalizeKnowledgeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private LocalDateTime maxTime(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private BigDecimal safeDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Integer) {
            return BigDecimal.valueOf((Integer) value).setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Long) {
            return BigDecimal.valueOf((Long) value).setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value).setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Float) {
            return BigDecimal.valueOf((Float) value).setScale(2, RoundingMode.HALF_UP);
        }
        if (value != null) {
            try {
                return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException ex) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private PageResult<Map<String, Object>> manualPage(List<Map<String, Object>> records, Long pageNo, Long pageSize) {
        long safePageNo = pageNo == null || pageNo <= 0 ? 1 : pageNo;
        long safePageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        int fromIndex = (int) Math.min((safePageNo - 1) * safePageSize, records.size());
        int toIndex = (int) Math.min(fromIndex + safePageSize, records.size());
        return new PageResult<>((long) records.size(), safePageNo, safePageSize, records.subList(fromIndex, toIndex));
    }
}
