package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.ExcelUtils;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.PasswordChangeRequest;
import com.civil.platform.module.dto.UserSettingSaveRequest;
import com.civil.platform.module.entity.Classroom;
import com.civil.platform.module.entity.ClassroomExamSubmission;
import com.civil.platform.module.entity.ClassroomExamTask;
import com.civil.platform.module.entity.ClassroomHomework;
import com.civil.platform.module.entity.ClassroomHomeworkSubmission;
import com.civil.platform.module.entity.ClassroomJoinRequest;
import com.civil.platform.module.entity.ClassroomMember;
import com.civil.platform.module.entity.Course;
import com.civil.platform.module.entity.CourseChapter;
import com.civil.platform.module.entity.CourseMaterial;
import com.civil.platform.module.entity.CourseOrder;
import com.civil.platform.module.entity.Exam;
import com.civil.platform.module.entity.ExamRecord;
import com.civil.platform.module.entity.PracticeRecord;
import com.civil.platform.module.entity.SysFileRecord;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.entity.UserCourse;
import com.civil.platform.module.entity.UserCourseChapterProgress;
import com.civil.platform.module.entity.UserCourseLearningRecord;
import com.civil.platform.module.entity.UserCourseStudyProgress;
import com.civil.platform.module.entity.UserWrongQuestion;
import com.civil.platform.module.mapper.CourseChapterMapper;
import com.civil.platform.module.mapper.CourseMapper;
import com.civil.platform.module.mapper.CourseMaterialMapper;
import com.civil.platform.module.mapper.CourseOrderMapper;
import com.civil.platform.module.mapper.ExamMapper;
import com.civil.platform.module.mapper.ExamRecordMapper;
import com.civil.platform.module.mapper.PracticeRecordMapper;
import com.civil.platform.module.mapper.QuestionMapper;
import com.civil.platform.module.mapper.ClassroomJoinRequestMapper;
import com.civil.platform.module.mapper.ClassroomMapper;
import com.civil.platform.module.mapper.ClassroomExamSubmissionMapper;
import com.civil.platform.module.mapper.ClassroomExamTaskMapper;
import com.civil.platform.module.mapper.ClassroomHomeworkMapper;
import com.civil.platform.module.mapper.ClassroomHomeworkSubmissionMapper;
import com.civil.platform.module.mapper.ClassroomMemberMapper;
import com.civil.platform.module.mapper.SysFileRecordMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.UserCourseChapterProgressMapper;
import com.civil.platform.module.mapper.UserCourseLearningRecordMapper;
import com.civil.platform.module.mapper.UserCourseMapper;
import com.civil.platform.module.mapper.UserCourseStudyProgressMapper;
import com.civil.platform.module.mapper.UserWrongQuestionMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class UserCenterService {

    private final SysUserMapper sysUserMapper;
    private final UserCourseMapper userCourseMapper;
    private final CourseOrderMapper courseOrderMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final UserWrongQuestionMapper userWrongQuestionMapper;
    private final OrderService orderService;
    private final CourseMapper courseMapper;
    private final CourseChapterMapper courseChapterMapper;
    private final CourseMaterialMapper courseMaterialMapper;
    private final SysFileRecordMapper sysFileRecordMapper;
    private final UserCourseStudyProgressMapper userCourseStudyProgressMapper;
    private final UserCourseChapterProgressMapper userCourseChapterProgressMapper;
    private final UserCourseLearningRecordMapper userCourseLearningRecordMapper;
    private final ClassroomMemberMapper classroomMemberMapper;
    private final ClassroomJoinRequestMapper classroomJoinRequestMapper;
    private final ClassroomMapper classroomMapper;
    private final ClassroomHomeworkMapper classroomHomeworkMapper;
    private final ClassroomHomeworkSubmissionMapper classroomHomeworkSubmissionMapper;
    private final ClassroomExamTaskMapper classroomExamTaskMapper;
    private final ClassroomExamSubmissionMapper classroomExamSubmissionMapper;
    private final PasswordEncoder passwordEncoder;

    public UserCenterService(SysUserMapper sysUserMapper,
                             UserCourseMapper userCourseMapper,
                             CourseOrderMapper courseOrderMapper,
                             PracticeRecordMapper practiceRecordMapper,
                             ExamRecordMapper examRecordMapper,
                             ExamMapper examMapper,
                             QuestionMapper questionMapper,
                             UserWrongQuestionMapper userWrongQuestionMapper,
                             OrderService orderService,
                             CourseMapper courseMapper,
                             CourseChapterMapper courseChapterMapper,
                             CourseMaterialMapper courseMaterialMapper,
                             SysFileRecordMapper sysFileRecordMapper,
                             UserCourseStudyProgressMapper userCourseStudyProgressMapper,
                             UserCourseChapterProgressMapper userCourseChapterProgressMapper,
                             UserCourseLearningRecordMapper userCourseLearningRecordMapper,
                             ClassroomMemberMapper classroomMemberMapper,
                             ClassroomJoinRequestMapper classroomJoinRequestMapper,
                             ClassroomMapper classroomMapper,
                             ClassroomHomeworkMapper classroomHomeworkMapper,
                             ClassroomHomeworkSubmissionMapper classroomHomeworkSubmissionMapper,
                             ClassroomExamTaskMapper classroomExamTaskMapper,
                             ClassroomExamSubmissionMapper classroomExamSubmissionMapper,
                             PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.userCourseMapper = userCourseMapper;
        this.courseOrderMapper = courseOrderMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.examRecordMapper = examRecordMapper;
        this.examMapper = examMapper;
        this.questionMapper = questionMapper;
        this.userWrongQuestionMapper = userWrongQuestionMapper;
        this.orderService = orderService;
        this.courseMapper = courseMapper;
        this.courseChapterMapper = courseChapterMapper;
        this.courseMaterialMapper = courseMaterialMapper;
        this.sysFileRecordMapper = sysFileRecordMapper;
        this.userCourseStudyProgressMapper = userCourseStudyProgressMapper;
        this.userCourseChapterProgressMapper = userCourseChapterProgressMapper;
        this.userCourseLearningRecordMapper = userCourseLearningRecordMapper;
        this.classroomMemberMapper = classroomMemberMapper;
        this.classroomJoinRequestMapper = classroomJoinRequestMapper;
        this.classroomMapper = classroomMapper;
        this.classroomHomeworkMapper = classroomHomeworkMapper;
        this.classroomHomeworkSubmissionMapper = classroomHomeworkSubmissionMapper;
        this.classroomExamTaskMapper = classroomExamTaskMapper;
        this.classroomExamSubmissionMapper = classroomExamSubmissionMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> overview() {
        Long userId = SecurityUtils.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        List<PracticeRecord> practices = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .orderByDesc(PracticeRecord::getCreatedAt));
        List<ExamRecord> exams = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .orderByDesc(ExamRecord::getCreatedAt));
        List<CourseOrder> orders = courseOrderMapper.selectList(new LambdaQueryWrapper<CourseOrder>()
                .eq(CourseOrder::getUserId, userId)
                .orderByDesc(CourseOrder::getCreatedAt));

        Map<String, Object> result = new HashMap<>();
        result.put("profile", user);
        result.put("courseCount", userCourseMapper.selectCount(new LambdaQueryWrapper<com.civil.platform.module.entity.UserCourse>()
                .eq(com.civil.platform.module.entity.UserCourse::getUserId, userId)));
        result.put("paidOrderCount", orders.stream().filter(item -> "PAID".equals(item.getOrderStatus())).count());
        result.put("practiceCount", practices.size());
        result.put("examCount", exams.size());
        result.put("wrongQuestionCount", userWrongQuestionMapper.selectCount(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getRemoved, false)));
        result.put("averagePracticeAccuracy", averageAccuracy(practices));
        result.put("bestExamScore", exams.stream().map(ExamRecord::getScore).max(Integer::compareTo).orElse(0));
        result.put("recentOrders", orders.stream().limit(5).map(item -> {
            Map<String, Object> order = new HashMap<>();
            order.put("id", item.getId());
            order.put("orderNo", item.getOrderNo());
            order.put("orderStatus", item.getOrderStatus());
            order.put("totalAmount", item.getTotalAmount());
            order.put("createdAt", item.getCreatedAt());
            return order;
        }).collect(Collectors.toList()));
        result.put("recentPractices", practices.stream().limit(5).map(item -> {
            Map<String, Object> practice = new HashMap<>();
            practice.put("id", item.getId());
            practice.put("title", item.getTitle());
            practice.put("score", item.getScore());
            practice.put("accuracyRate", item.getAccuracyRate());
            practice.put("createdAt", item.getCreatedAt());
            return practice;
        }).collect(Collectors.toList()));
        result.put("recentExams", exams.stream().limit(5).map(item -> {
            Map<String, Object> exam = new HashMap<>();
            exam.put("id", item.getId());
            exam.put("title", item.getTitle());
            exam.put("score", item.getScore());
            exam.put("createdAt", item.getCreatedAt());
            return exam;
        }).collect(Collectors.toList()));
        return result;
    }

    public Map<String, Object> statistics() {
        Long userId = SecurityUtils.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        List<PracticeRecord> practices = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getStatus, "SUBMITTED"));
        List<ExamRecord> exams = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getStatus, "SUBMITTED"));
        List<CourseOrder> paidOrders = courseOrderMapper.selectList(new LambdaQueryWrapper<CourseOrder>()
                .eq(CourseOrder::getUserId, userId)
                .eq(CourseOrder::getOrderStatus, "PAID"));
        List<UserWrongQuestion> wrongQuestions = userWrongQuestionMapper.selectList(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getRemoved, false));
        List<UserCourseStudyProgress> studyProgressList = userCourseStudyProgressMapper.selectList(new LambdaQueryWrapper<UserCourseStudyProgress>()
                .eq(UserCourseStudyProgress::getUserId, userId)
                .orderByDesc(UserCourseStudyProgress::getLastStudyAt));
        List<UserCourseChapterProgress> chapterProgressList = userCourseChapterProgressMapper.selectList(new LambdaQueryWrapper<UserCourseChapterProgress>()
                .eq(UserCourseChapterProgress::getUserId, userId)
                .orderByDesc(UserCourseChapterProgress::getLastStudyAt));
        List<UserCourseLearningRecord> learningRecords = userCourseLearningRecordMapper.selectList(new LambdaQueryWrapper<UserCourseLearningRecord>()
                .eq(UserCourseLearningRecord::getUserId, userId)
                .orderByDesc(UserCourseLearningRecord::getCreatedAt));

        Map<String, Object> result = new HashMap<>();
        result.put("last7Days", buildLast7Days(practices, exams, paidOrders, learningRecords));
        result.put("subjectSummary", buildSubjectSummary(practices, exams, wrongQuestions, studyProgressList));
        result.put("totals", buildTotals(practices, exams, paidOrders, wrongQuestions, studyProgressList, chapterProgressList));
        result.put("recentLearningTracks", buildRecentLearningTracks(chapterProgressList));
        result.put("continueLearningCourses", buildContinueLearningCourses(studyProgressList));
        result.put("learningDiagnosis", buildLearningDiagnosis(user, practices, exams, wrongQuestions, studyProgressList));
        return result;
    }

    public List<Map<String, Object>> myCourses() {
        return orderService.myCourses();
    }

    public Map<String, Object> myClassrooms() {
        Long userId = SecurityUtils.getUserId();
        List<ClassroomMember> classroomMembers = classroomMemberMapper.selectList(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getUserId, userId)
                .orderByDesc(ClassroomMember::getJoinedAt));
        List<ClassroomJoinRequest> joinRequests = classroomJoinRequestMapper.selectList(new LambdaQueryWrapper<ClassroomJoinRequest>()
                .eq(ClassroomJoinRequest::getUserId, userId)
                .orderByDesc(ClassroomJoinRequest::getCreatedAt));

        Map<Long, Classroom> classroomMap = classroomMapper.selectBatchIds(classroomMembers.stream()
                        .map(ClassroomMember::getClassroomId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Classroom::getId, item -> item, (left, right) -> left));
        Map<Long, Classroom> requestClassroomMap = classroomMapper.selectBatchIds(joinRequests.stream()
                        .map(ClassroomJoinRequest::getClassroomId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Classroom::getId, item -> item, (left, right) -> left));

        Map<Long, List<ClassroomHomework>> homeworkMap = classroomHomeworkMapper.selectList(new LambdaQueryWrapper<ClassroomHomework>()
                        .in(!classroomMap.isEmpty(), ClassroomHomework::getClassroomId, classroomMap.keySet())
                        .orderByDesc(ClassroomHomework::getDeadlineAt)
                        .orderByDesc(ClassroomHomework::getCreatedAt))
                .stream()
                .collect(Collectors.groupingBy(ClassroomHomework::getClassroomId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<ClassroomExamTask>> examMap = classroomExamTaskMapper.selectList(new LambdaQueryWrapper<ClassroomExamTask>()
                        .in(!classroomMap.isEmpty(), ClassroomExamTask::getClassroomId, classroomMap.keySet())
                        .orderByDesc(ClassroomExamTask::getStartTime)
                        .orderByDesc(ClassroomExamTask::getCreatedAt))
                .stream()
                .collect(Collectors.groupingBy(ClassroomExamTask::getClassroomId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<ClassroomHomeworkSubmission>> homeworkSubmissionMap = classroomHomeworkSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkSubmission>()
                        .eq(ClassroomHomeworkSubmission::getUserId, userId)
                        .in(!classroomMap.isEmpty(), ClassroomHomeworkSubmission::getClassroomId, classroomMap.keySet()))
                .stream()
                .collect(Collectors.groupingBy(ClassroomHomeworkSubmission::getClassroomId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<ClassroomExamSubmission>> examSubmissionMap = classroomExamSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomExamSubmission>()
                        .eq(ClassroomExamSubmission::getUserId, userId)
                        .in(!classroomMap.isEmpty(), ClassroomExamSubmission::getClassroomId, classroomMap.keySet()))
                .stream()
                .collect(Collectors.groupingBy(ClassroomExamSubmission::getClassroomId, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> joinedClassrooms = classroomMembers.stream().map(item -> {
            Classroom classroom = classroomMap.get(item.getClassroomId());
            List<ClassroomHomework> homeworks = homeworkMap.getOrDefault(item.getClassroomId(), new ArrayList<>());
            List<ClassroomExamTask> exams = examMap.getOrDefault(item.getClassroomId(), new ArrayList<>());
            List<ClassroomHomeworkSubmission> homeworkSubmissions = homeworkSubmissionMap.getOrDefault(item.getClassroomId(), new ArrayList<>());
            List<ClassroomExamSubmission> examSubmissions = examSubmissionMap.getOrDefault(item.getClassroomId(), new ArrayList<>());
            long pendingHomeworkCount = homeworks.stream()
                    .filter(task -> homeworkSubmissions.stream().noneMatch(submission -> task.getId().equals(submission.getHomeworkId()) && "SUBMITTED".equals(submission.getStatus())))
                    .count();
            long pendingExamCount = exams.stream()
                    .filter(task -> examSubmissions.stream().noneMatch(submission -> task.getId().equals(submission.getClassroomExamId()) && "SUBMITTED".equals(submission.getStatus())))
                    .count();
            BigDecimal averageScore = averageClassroomScore(homeworkSubmissions, examSubmissions);
            Map<String, Object> latestReminder = buildLatestClassroomReminder(homeworks, exams, homeworkSubmissions, examSubmissions);
            Map<String, Object> map = new HashMap<>();
            map.put("classroomId", item.getClassroomId());
            map.put("name", classroom == null ? "班级已删除" : classroom.getName());
            map.put("subjectName", classroom == null ? null : classroom.getSubjectName());
            map.put("teacherName", classroom == null ? null : classroom.getTeacherName());
            map.put("coverUrl", classroom == null ? null : classroom.getCoverUrl());
            map.put("joinedAt", item.getJoinedAt());
            map.put("roleCode", item.getRoleCode());
            map.put("detailPath", "/classroom/detail/" + item.getClassroomId());
            map.put("pendingHomeworkCount", pendingHomeworkCount);
            map.put("pendingExamCount", pendingExamCount);
            map.put("taskCount", homeworks.size() + exams.size());
            map.put("averageScore", averageScore);
            map.put("latestReminder", latestReminder);
            return map;
        }).collect(Collectors.toList());

        List<Map<String, Object>> applicationRecords = joinRequests.stream()
                .map(item -> {
                    Classroom classroom = requestClassroomMap.get(item.getClassroomId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", item.getId());
                    map.put("classroomId", item.getClassroomId());
                    map.put("name", classroom == null ? "班级已删除" : classroom.getName());
                    map.put("subjectName", classroom == null ? null : classroom.getSubjectName());
                    map.put("teacherName", classroom == null ? null : classroom.getTeacherName());
                    map.put("createdAt", item.getCreatedAt());
                    map.put("remark", item.getRemark());
                    map.put("status", item.getStatus());
                    map.put("statusLabel", resolveJoinStatusLabel(item.getStatus()));
                    map.put("invitation", "INVITED".equals(item.getStatus()));
                    return map;
                }).collect(Collectors.toList());

        List<Map<String, Object>> recentReminders = joinedClassrooms.stream()
                .map(item -> {
                    Map<String, Object> reminder = (Map<String, Object>) item.get("latestReminder");
                    if (reminder == null) {
                        return null;
                    }
                    reminder.put("classroomName", item.get("name"));
                    reminder.put("detailPath", item.get("detailPath"));
                    return reminder;
                })
                .filter(item -> item != null)
                .sorted((left, right) -> {
                    LocalDateTime leftTime = (LocalDateTime) left.get("remindAt");
                    LocalDateTime rightTime = (LocalDateTime) right.get("remindAt");
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
                .limit(8)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("joinedClassrooms", joinedClassrooms);
        result.put("pendingClassrooms", applicationRecords.stream()
                .filter(item -> "PENDING".equals(item.get("status")))
                .collect(Collectors.toList()));
        result.put("invitationRecords", applicationRecords.stream()
                .filter(item -> "INVITED".equals(item.get("status")))
                .collect(Collectors.toList()));
        result.put("applicationRecords", applicationRecords);
        result.put("recentReminders", recentReminders);
        result.put("summary", buildClassroomSummary(joinedClassrooms, applicationRecords));
        result.put("quickEntries", joinedClassrooms.stream().limit(6).map(item -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("title", item.get("name"));
            entry.put("subjectName", item.get("subjectName"));
            entry.put("path", item.get("detailPath"));
            return entry;
        }).collect(Collectors.toList()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmClassroomInvitation(Long requestId, String action) {
        Long userId = SecurityUtils.getUserId();
        ClassroomJoinRequest invitation = classroomJoinRequestMapper.selectById(requestId);
        if (invitation == null || !userId.equals(invitation.getUserId()) || !"INVITED".equals(invitation.getStatus())) {
            throw new BizException("邀请不存在或已处理");
        }
        Classroom classroom = classroomMapper.selectById(invitation.getClassroomId());
        if (classroom == null || "DISMISSED".equals(classroom.getStatus())) {
            throw new BizException("班级不存在或已解散");
        }
        String normalizedAction = StringUtils.hasText(action) ? action.trim().toUpperCase() : "";
        if ("REJECT".equals(normalizedAction)) {
            invitation.setStatus("REJECTED");
            invitation.setReviewedAt(LocalDateTime.now());
            classroomJoinRequestMapper.updateById(invitation);
            Map<String, Object> result = new HashMap<>();
            result.put("status", invitation.getStatus());
            return result;
        }
        if (!"ACCEPT".equals(normalizedAction)) {
            throw new BizException("不支持的邀请处理操作");
        }
        long studentCount = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroom.getId())
                .eq(ClassroomMember::getRoleCode, "STUDENT"));
        if (studentCount >= classroom.getCapacity()) {
            throw new BizException("班级招生人数已满");
        }
        ClassroomMember existingMember = classroomMemberMapper.selectOne(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroom.getId())
                .eq(ClassroomMember::getUserId, userId)
                .last("limit 1"));
        if (existingMember == null) {
            ClassroomMember member = new ClassroomMember();
            member.setClassroomId(classroom.getId());
            member.setUserId(userId);
            member.setRoleCode("STUDENT");
            member.setJoinedAt(LocalDateTime.now());
            classroomMemberMapper.insert(member);
            classroom.setMemberCount((classroom.getMemberCount() == null ? 0 : classroom.getMemberCount()) + 1);
            classroomMapper.updateById(classroom);
        }
        invitation.setStatus("APPROVED");
        invitation.setReviewedAt(LocalDateTime.now());
        classroomJoinRequestMapper.updateById(invitation);
        Map<String, Object> result = new HashMap<>();
        result.put("status", invitation.getStatus());
        result.put("classroomId", classroom.getId());
        result.put("detailPath", "/classroom/detail/" + classroom.getId());
        return result;
    }

    public Map<String, Object> exportCenter() {
        Long userId = SecurityUtils.getUserId();
        long wrongQuestionCount = userWrongQuestionMapper.selectCount(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getRemoved, false));
        long practiceCount = practiceRecordMapper.selectCount(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getStatus, "SUBMITTED"));
        long examCount = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getStatus, "SUBMITTED"));
        Map<String, Object> result = new HashMap<>();
        result.put("entries", java.util.Arrays.asList(
                buildExportEntry("错题导出", "导出当前错题本和知识点回顾数据", "/api/user-center/exports/wrong-questions", wrongQuestionCount),
                buildExportEntry("成绩导出", "导出练习、模考成绩和作答表现", "/api/user-center/exports/scores", practiceCount + examCount),
                buildExportEntry("成长报告导出", "导出阶段诊断、历史对比和协同跟进摘要", "/api/user-center/growth-diagnosis/export", 1)
        ));
        result.put("wrongQuestionCount", wrongQuestionCount);
        result.put("scoreRecordCount", practiceCount + examCount);
        result.put("tips", java.util.Arrays.asList(
                "错题导出包含题干与加入时间，适合集中复盘。",
                "成绩导出会汇总题库练习与模考成绩，可用于阶段回看。",
                "成长报告导出适合和班级老师、后台运营一起复盘阶段诊断。"
        ));
        return result;
    }

    public byte[] exportWrongQuestions() {
        Long userId = SecurityUtils.getUserId();
        List<UserWrongQuestion> wrongQuestions = userWrongQuestionMapper.selectList(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getRemoved, false)
                .orderByDesc(UserWrongQuestion::getCreatedAt));
        Map<Long, String> stemMap = questionMapper.selectBatchIds(wrongQuestions.stream()
                        .map(UserWrongQuestion::getQuestionId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(question -> question.getId(), question -> question.getStem(), (left, right) -> left));
        List<List<Object>> rows = wrongQuestions.stream()
                .map(item -> {
                    List<Object> row = new ArrayList<>();
                    row.add(item.getQuestionId());
                    row.add(stemMap.getOrDefault(item.getQuestionId(), "题目已删除"));
                    row.add(item.getCreatedAt());
                    return row;
                })
                .collect(Collectors.toList());
        return ExcelUtils.buildWorkbook("错题本", java.util.Arrays.asList("题目ID", "题干", "加入时间"), rows);
    }

    public byte[] exportScores() {
        Long userId = SecurityUtils.getUserId();
        List<List<Object>> rows = new ArrayList<>();
        practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .eq(PracticeRecord::getStatus, "SUBMITTED")
                        .orderByDesc(PracticeRecord::getSubmittedAt))
                .forEach(item -> {
                    List<Object> row = new ArrayList<>();
                    row.add("练习");
                    row.add(item.getTitle());
                    row.add(item.getScore());
                    row.add(item.getAccuracyRate());
                    row.add(item.getSubmittedAt());
                    rows.add(row);
                });
        examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .eq(ExamRecord::getStatus, "SUBMITTED")
                        .orderByDesc(ExamRecord::getSubmittedAt))
                .forEach(item -> {
                    List<Object> row = new ArrayList<>();
                    row.add("模考");
                    row.add(item.getTitle());
                    row.add(item.getScore());
                    row.add(null);
                    row.add(item.getSubmittedAt());
                    rows.add(row);
                });
        return ExcelUtils.buildWorkbook("成绩汇总", java.util.Arrays.asList("类型", "标题", "分数", "正确率", "提交时间"), rows);
    }

    public Map<String, Object> settings() {
        Map<String, Object> result = new HashMap<>();
        result.put("profile", sysUserMapper.selectById(SecurityUtils.getUserId()));
        result.put("preferenceOptions", buildPreferenceOptions());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveSettings(UserSettingSaveRequest request) {
        SysUser user = sysUserMapper.selectById(SecurityUtils.getUserId());
        user.setNickname(request.getNickname());
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setProvince(request.getProvince());
        user.setExamType(request.getExamType());
        user.setPreferredSubject(request.getPreferredSubject());
        sysUserMapper.updateById(user);
        return settings();
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(PasswordChangeRequest request) {
        SysUser user = sysUserMapper.selectById(SecurityUtils.getUserId());
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    public Map<String, Object> courseLearning(Long courseId) {
        Long userId = SecurityUtils.getUserId();
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new com.civil.platform.common.exception.BizException(404, "课程不存在");
        }
        Long owned = userCourseMapper.selectCount(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId)
                .eq(UserCourse::getCourseId, courseId));
        if (owned == 0 && (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) > 0)) {
            throw new com.civil.platform.common.exception.BizException(403, "请先购买课程");
        }
        List<CourseChapter> chapters = courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, courseId)
                .orderByAsc(CourseChapter::getSortNo));
        List<Map<String, Object>> materials = courseMaterialMapper.selectList(new LambdaQueryWrapper<CourseMaterial>()
                        .eq(CourseMaterial::getCourseId, courseId)
                        .orderByAsc(CourseMaterial::getSortNo))
                .stream().map(item -> {
                    SysFileRecord file = sysFileRecordMapper.selectById(item.getFileId());
                    Map<String, Object> material = new HashMap<>();
                    material.put("id", item.getId());
                    material.put("title", item.getTitle());
                    material.put("materialType", item.getMaterialType());
                    material.put("fileUrl", file == null ? null : "/api/files/view/" + file.getId());
                    return material;
                }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("course", course);
        result.put("chapters", chapters);
        result.put("materials", materials);
        return result;
    }

    private BigDecimal averageAccuracy(List<PracticeRecord> practices) {
        if (practices.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = practices.stream()
                .map(item -> item.getAccuracyRate() == null ? BigDecimal.ZERO : item.getAccuracyRate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(practices.size()), 2, RoundingMode.HALF_UP);
    }

    private List<Map<String, Object>> buildLast7Days(List<PracticeRecord> practices,
                                                     List<ExamRecord> exams,
                                                     List<CourseOrder> paidOrders,
                                                     List<UserCourseLearningRecord> learningRecords) {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(formatter));
            item.put("practiceCount", practices.stream().filter(record -> between(record.getSubmittedAt(), start, end)).count());
            item.put("examCount", exams.stream().filter(record -> between(record.getSubmittedAt(), start, end)).count());
            item.put("studySeconds", learningRecords.stream()
                    .filter(record -> between(record.getCreatedAt(), start, end))
                    .mapToInt(record -> record.getWatchSeconds() == null ? 0 : record.getWatchSeconds())
                    .sum());
            item.put("learningSessionCount", learningRecords.stream()
                    .filter(record -> between(record.getCreatedAt(), start, end))
                    .count());
            item.put("paymentAmount", paidOrders.stream()
                    .filter(order -> between(order.getPaidAt(), start, end))
                    .map(CourseOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildSubjectSummary(List<PracticeRecord> practices,
                                                          List<ExamRecord> exams,
                                                          List<UserWrongQuestion> wrongQuestions,
                                                          List<UserCourseStudyProgress> studyProgressList) {
        Map<String, Map<String, Object>> summary = new HashMap<>();
        for (PracticeRecord practice : practices) {
            String subjectName = StringUtils.hasText(practice.getSubjectName()) ? practice.getSubjectName() : "专项练习";
            Map<String, Object> item = initSubjectSummary(summary, subjectName);
            item.put("practiceCount", (Long) item.get("practiceCount") + 1);
            item.put("practiceAccuracyTotal", ((BigDecimal) item.get("practiceAccuracyTotal"))
                    .add(practice.getAccuracyRate() == null ? BigDecimal.ZERO : practice.getAccuracyRate()));
        }
        List<Long> examIds = exams.stream()
                .map(ExamRecord::getExamId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Exam> examMap = examIds.isEmpty()
                ? Collections.emptyMap()
                : examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(Exam::getId, exam -> exam));
        for (ExamRecord examRecord : exams) {
            Exam exam = examMap.get(examRecord.getExamId());
            String subjectName = exam == null ? "模考" : exam.getSubjectName();
            Map<String, Object> item = initSubjectSummary(summary, subjectName);
            item.put("examCount", (Long) item.get("examCount") + 1);
            item.put("bestScore", Math.max((Integer) item.get("bestScore"), examRecord.getScore() == null ? 0 : examRecord.getScore()));
        }
        List<Long> courseIds = studyProgressList.stream()
                .map(UserCourseStudyProgress::getCourseId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Course> courseMap = courseIds.isEmpty()
                ? Collections.emptyMap()
                : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, course -> course));
        for (UserCourseStudyProgress progress : studyProgressList) {
            Course course = courseMap.get(progress.getCourseId());
            String subjectName = course == null ? "课程学习" : course.getSubjectName();
            Map<String, Object> item = initSubjectSummary(summary, subjectName);
            item.put("courseCount", (Long) item.get("courseCount") + 1);
            item.put("studySeconds", (Integer) item.get("studySeconds") + (progress.getTotalStudySeconds() == null ? 0 : progress.getTotalStudySeconds()));
        }
        List<Long> wrongQuestionIds = wrongQuestions.stream()
                .map(UserWrongQuestion::getQuestionId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> questionSubjectMap = wrongQuestionIds.isEmpty()
                ? Collections.emptyMap()
                : questionMapper.selectBatchIds(wrongQuestionIds).stream()
                .collect(Collectors.toMap(question -> question.getId(), question -> question.getSubjectName(), (left, right) -> left));
        for (UserWrongQuestion wrongQuestion : wrongQuestions) {
            String subjectName = questionSubjectMap.getOrDefault(wrongQuestion.getQuestionId(), "专项练习");
            Map<String, Object> item = initSubjectSummary(summary, subjectName);
            item.put("wrongQuestionCount", (Long) item.get("wrongQuestionCount") + 1);
        }
        return summary.values().stream()
                .map(item -> {
                    long practiceCount = (Long) item.get("practiceCount");
                    BigDecimal practiceAccuracy = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    if (practiceCount > 0) {
                        practiceAccuracy = ((BigDecimal) item.get("practiceAccuracyTotal"))
                                .divide(BigDecimal.valueOf(practiceCount), 2, RoundingMode.HALF_UP);
                    }
                    item.put("practiceAccuracy", practiceAccuracy);
                    BigDecimal masteryScore = practiceAccuracy
                            .add(BigDecimal.valueOf((Integer) item.get("bestScore")).multiply(BigDecimal.valueOf(0.2)))
                            .add(BigDecimal.valueOf((Long) item.get("courseCount")).multiply(BigDecimal.valueOf(6)))
                            .subtract(BigDecimal.valueOf((Long) item.get("wrongQuestionCount")).multiply(BigDecimal.valueOf(8)));
                    item.put("masteryScore", masteryScore.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                    item.remove("practiceAccuracyTotal");
                    return item;
                })
                .sorted((left, right) -> ((BigDecimal) right.get("masteryScore")).compareTo((BigDecimal) left.get("masteryScore")))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildTotals(List<PracticeRecord> practices,
                                            List<ExamRecord> exams,
                                            List<CourseOrder> paidOrders,
                                            List<UserWrongQuestion> wrongQuestions,
                                            List<UserCourseStudyProgress> studyProgressList,
                                            List<UserCourseChapterProgress> chapterProgressList) {
        Map<String, Object> totals = new HashMap<>();
        totals.put("practiceQuestions", practices.stream().mapToInt(item -> item.getQuestionCount() == null ? 0 : item.getQuestionCount()).sum());
        totals.put("practiceCorrect", practices.stream().mapToInt(item -> item.getCorrectCount() == null ? 0 : item.getCorrectCount()).sum());
        totals.put("examBestScore", exams.stream().map(ExamRecord::getScore).max(Integer::compareTo).orElse(0));
        totals.put("paymentAmount", paidOrders.stream().map(CourseOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        totals.put("totalStudySeconds", studyProgressList.stream().mapToInt(item -> item.getTotalStudySeconds() == null ? 0 : item.getTotalStudySeconds()).sum());
        totals.put("activeCourseCount", studyProgressList.size());
        totals.put("completedChapterCount", chapterProgressList.stream().filter(item -> Boolean.TRUE.equals(item.getCompleted())).count());
        totals.put("wrongQuestionCount", wrongQuestions.size());
        return totals;
    }

    private List<Map<String, Object>> buildRecentLearningTracks(List<UserCourseChapterProgress> chapterProgressList) {
        if (chapterProgressList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, CourseChapter> chapterMap = courseChapterMapper.selectBatchIds(chapterProgressList.stream()
                        .map(UserCourseChapterProgress::getChapterId)
                        .distinct()
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(CourseChapter::getId, chapter -> chapter));
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(chapterProgressList.stream()
                        .map(UserCourseChapterProgress::getCourseId)
                        .distinct()
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Course::getId, course -> course));
        return chapterProgressList.stream()
                .filter(item -> item.getLastStudyAt() != null)
                .sorted(Comparator.comparing(UserCourseChapterProgress::getLastStudyAt).reversed())
                .limit(6)
                .map(item -> {
                    Course course = courseMap.get(item.getCourseId());
                    CourseChapter chapter = chapterMap.get(item.getChapterId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("courseId", item.getCourseId());
                    map.put("courseTitle", course == null ? "课程已删除" : course.getTitle());
                    map.put("courseCoverUrl", course == null ? null : course.getCoverUrl());
                    map.put("chapterId", item.getChapterId());
                    map.put("chapterTitle", chapter == null ? "章节已删除" : chapter.getTitle());
                    map.put("studySeconds", item.getStudySeconds() == null ? 0 : item.getStudySeconds());
                    map.put("progressPercent", item.getProgressPercent() == null ? BigDecimal.ZERO : item.getProgressPercent());
                    map.put("completed", Boolean.TRUE.equals(item.getCompleted()));
                    map.put("lastStudyAt", item.getLastStudyAt());
                    map.put("path", "/course/play/" + item.getCourseId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildContinueLearningCourses(List<UserCourseStudyProgress> studyProgressList) {
        if (studyProgressList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(studyProgressList.stream()
                        .map(UserCourseStudyProgress::getCourseId)
                        .distinct()
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Course::getId, course -> course));
        return studyProgressList.stream()
                .filter(item -> {
                    BigDecimal percent = item.getProgressPercent() == null ? BigDecimal.ZERO : item.getProgressPercent();
                    return percent.compareTo(BigDecimal.ZERO) > 0 && percent.compareTo(BigDecimal.valueOf(100)) < 0;
                })
                .sorted(Comparator.comparing(UserCourseStudyProgress::getLastStudyAt, Comparator.nullsLast(Comparator.reverseOrder())))
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

    private List<Map<String, Object>> buildLearningDiagnosis(SysUser user,
                                                             List<PracticeRecord> practices,
                                                             List<ExamRecord> exams,
                                                             List<UserWrongQuestion> wrongQuestions,
                                                             List<UserCourseStudyProgress> studyProgressList) {
        List<Map<String, Object>> diagnosis = new ArrayList<>();
        BigDecimal avgAccuracy = averageAccuracy(practices);
        diagnosis.add(buildDiagnosisCard("学习节奏",
                studyProgressList.stream().anyMatch(item -> item.getLastStudyAt() != null && item.getLastStudyAt().isAfter(LocalDateTime.now().minusDays(3)))
                        ? "最近 3 天保持了课程学习，节奏正在形成。"
                        : "最近 3 天课程学习偏少，建议先恢复每天固定 30 分钟学习。",
                studyProgressList.isEmpty() ? "去选课" : "继续学习",
                studyProgressList.isEmpty() ? "/course" : "/my-course"));
        diagnosis.add(buildDiagnosisCard("刷题表现",
                avgAccuracy.compareTo(BigDecimal.valueOf(70)) >= 0
                        ? "近期刷题正确率稳定在 70% 以上，可以逐步增加模考检验。"
                        : "近期刷题正确率仍有提升空间，建议优先回到薄弱科目做专项练习。",
                "去刷题",
                "/question-bank"));
        diagnosis.add(buildDiagnosisCard("模考检验",
                exams.isEmpty()
                        ? "你还没有完成模考记录，建议做一套模考看看阶段分数。"
                        : "已有模考成绩，可以结合课程和错题进一步补强短板。",
                "查看模考",
                "/exam"));
        diagnosis.add(buildDiagnosisCard("错题治理",
                wrongQuestions.isEmpty()
                        ? "当前没有待巩固错题，保持复盘习惯即可。"
                        : "目前仍有 " + wrongQuestions.size() + " 道错题待强化，建议回到错题本集中处理。",
                "错题再练",
                "/wrong-book"));
        if (StringUtils.hasText(user.getPreferredSubject())) {
            diagnosis.add(buildDiagnosisCard("备考主线",
                    "你的主攻科目是 " + user.getPreferredSubject() + "，当前推荐会优先向该方向和薄弱项补强倾斜。",
                    "查看推荐",
                    "/recommend"));
        }
        return diagnosis;
    }

    private Map<String, Object> initSubjectSummary(Map<String, Map<String, Object>> summary, String subjectName) {
        return summary.computeIfAbsent(subjectName, key -> {
            Map<String, Object> map = new HashMap<>();
            map.put("subjectName", key);
            map.put("practiceCount", 0L);
            map.put("examCount", 0L);
            map.put("bestScore", 0);
            map.put("practiceAccuracyTotal", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            map.put("wrongQuestionCount", 0L);
            map.put("courseCount", 0L);
            map.put("studySeconds", 0);
            map.put("masteryScore", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return map;
        });
    }

    private Map<String, Object> buildDiagnosisCard(String title, String content, String actionText, String path) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("content", content);
        map.put("actionText", actionText);
        map.put("path", path);
        return map;
    }

    private Map<String, Object> buildExportEntry(String title, String description, String path, long count) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("title", title);
        entry.put("description", description);
        entry.put("path", path);
        entry.put("count", count);
        return entry;
    }

    private Map<String, Object> buildClassroomSummary(List<Map<String, Object>> joinedClassrooms,
                                                      List<Map<String, Object>> applicationRecords) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("joinedCount", joinedClassrooms.size());
        summary.put("pendingApplyCount", applicationRecords.stream().filter(item -> "PENDING".equals(item.get("status"))).count());
        summary.put("pendingInvitationCount", applicationRecords.stream().filter(item -> "INVITED".equals(item.get("status"))).count());
        summary.put("pendingTaskCount", joinedClassrooms.stream().mapToLong(item ->
                ((Number) item.getOrDefault("pendingHomeworkCount", 0)).longValue()
                        + ((Number) item.getOrDefault("pendingExamCount", 0)).longValue()).sum());
        summary.put("averageScore", joinedClassrooms.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : joinedClassrooms.stream()
                .map(item -> (BigDecimal) item.getOrDefault("averageScore", BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(joinedClassrooms.size()), 2, RoundingMode.HALF_UP));
        return summary;
    }

    private BigDecimal averageClassroomScore(List<ClassroomHomeworkSubmission> homeworkSubmissions,
                                             List<ClassroomExamSubmission> examSubmissions) {
        List<Integer> scores = new ArrayList<>();
        homeworkSubmissions.stream()
                .map(ClassroomHomeworkSubmission::getScore)
                .filter(score -> score != null)
                .forEach(scores::add);
        examSubmissions.stream()
                .map(ClassroomExamSubmission::getScore)
                .filter(score -> score != null)
                .forEach(scores::add);
        if (scores.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(scores.stream().mapToInt(Integer::intValue).average().orElse(0D))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> buildLatestClassroomReminder(List<ClassroomHomework> homeworks,
                                                             List<ClassroomExamTask> exams,
                                                             List<ClassroomHomeworkSubmission> homeworkSubmissions,
                                                             List<ClassroomExamSubmission> examSubmissions) {
        Map<String, Object> latest = new HashMap<>();
        homeworks.stream()
                .sorted(Comparator.comparing(ClassroomHomework::getDeadlineAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(task -> homeworkSubmissions.stream().noneMatch(submission -> task.getId().equals(submission.getHomeworkId()) && "SUBMITTED".equals(submission.getStatus())))
                .findFirst()
                .ifPresent(task -> {
                    latest.put("type", "HOMEWORK");
                    latest.put("title", task.getTitle());
                    latest.put("statusLabel", "待完成作业");
                    latest.put("remindAt", task.getDeadlineAt());
                });
        exams.stream()
                .sorted(Comparator.comparing(ClassroomExamTask::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(task -> examSubmissions.stream().noneMatch(submission -> task.getId().equals(submission.getClassroomExamId()) && "SUBMITTED".equals(submission.getStatus())))
                .findFirst()
                .ifPresent(task -> {
                    LocalDateTime remindAt = task.getStartTime() == null ? task.getEndTime() : task.getStartTime();
                    if (latest.isEmpty() || compareReminder(remindAt, (LocalDateTime) latest.get("remindAt")) > 0) {
                        latest.put("type", "EXAM");
                        latest.put("title", task.getTitle());
                        latest.put("statusLabel", "待参加考试");
                        latest.put("remindAt", remindAt);
                    }
                });
        return latest.isEmpty() ? null : latest;
    }

    private int compareReminder(LocalDateTime left, LocalDateTime right) {
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

    private String resolveJoinStatusLabel(String status) {
        if ("APPROVED".equals(status)) {
            return "已通过";
        }
        if ("REJECTED".equals(status)) {
            return "已拒绝";
        }
        if ("INVITED".equals(status)) {
            return "待确认邀请";
        }
        return "待审核";
    }

    private Map<String, Object> buildPreferenceOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("provinces", java.util.Arrays.asList(
                "全国", "北京", "天津", "河北", "山西", "内蒙古", "辽宁", "吉林", "黑龙江",
                "上海", "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北",
                "湖南", "广东", "广西", "海南", "重庆", "四川", "贵州", "云南", "西藏",
                "陕西", "甘肃", "青海", "宁夏", "新疆"
        ));
        options.put("examTypes", java.util.Arrays.asList("国考", "省考", "事业单位", "选调生", "面试"));
        options.put("subjects", java.util.Arrays.asList("行测", "申论", "面试", "综合能力"));
        return options;
    }

    private boolean between(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }
}
