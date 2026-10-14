package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.QuestionAnswerUtils;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.ExamSubmitRequest;
import com.civil.platform.module.entity.Course;
import com.civil.platform.module.entity.Exam;
import com.civil.platform.module.entity.ExamAnalysisCourse;
import com.civil.platform.module.entity.ExamAnswer;
import com.civil.platform.module.entity.ExamQuestion;
import com.civil.platform.module.entity.ExamRecord;
import com.civil.platform.module.entity.Question;
import com.civil.platform.module.entity.QuestionOption;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.entity.UserQuestionFavorite;
import com.civil.platform.module.entity.UserWrongQuestion;
import com.civil.platform.module.mapper.CourseMapper;
import com.civil.platform.module.mapper.ExamAnalysisCourseMapper;
import com.civil.platform.module.mapper.ExamAnswerMapper;
import com.civil.platform.module.mapper.ExamMapper;
import com.civil.platform.module.mapper.ExamQuestionMapper;
import com.civil.platform.module.mapper.ExamRecordMapper;
import com.civil.platform.module.mapper.QuestionMapper;
import com.civil.platform.module.mapper.QuestionOptionMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.UserQuestionFavoriteMapper;
import com.civil.platform.module.mapper.UserWrongQuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExamCenterService {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final SysUserMapper sysUserMapper;
    private final UserWrongQuestionMapper userWrongQuestionMapper;
    private final CourseMapper courseMapper;
    private final ExamAnalysisCourseMapper examAnalysisCourseMapper;
    private final UserQuestionFavoriteMapper userQuestionFavoriteMapper;
    private final QuestionService questionService;
    private final RecommendEngineService recommendEngineService;

    public ExamCenterService(ExamMapper examMapper,
                             ExamQuestionMapper examQuestionMapper,
                             QuestionMapper questionMapper,
                             QuestionOptionMapper questionOptionMapper,
                             ExamRecordMapper examRecordMapper,
                             ExamAnswerMapper examAnswerMapper,
                             SysUserMapper sysUserMapper,
                             UserWrongQuestionMapper userWrongQuestionMapper,
                             CourseMapper courseMapper,
                             ExamAnalysisCourseMapper examAnalysisCourseMapper,
                             UserQuestionFavoriteMapper userQuestionFavoriteMapper,
                             QuestionService questionService,
                             RecommendEngineService recommendEngineService) {
        this.examMapper = examMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.questionMapper = questionMapper;
        this.questionOptionMapper = questionOptionMapper;
        this.examRecordMapper = examRecordMapper;
        this.examAnswerMapper = examAnswerMapper;
        this.sysUserMapper = sysUserMapper;
        this.userWrongQuestionMapper = userWrongQuestionMapper;
        this.courseMapper = courseMapper;
        this.examAnalysisCourseMapper = examAnalysisCourseMapper;
        this.userQuestionFavoriteMapper = userQuestionFavoriteMapper;
        this.questionService = questionService;
        this.recommendEngineService = recommendEngineService;
    }

    public PageResult<Map<String, Object>> pageExams(Long pageNo,
                                                     Long pageSize,
                                                     String keyword,
                                                     String province,
                                                     String examType,
                                                     String seasonName,
                                                     String subjectName,
                                                     String status) {
        List<Map<String, Object>> allRecords = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .like(StringUtils.hasText(keyword), Exam::getTitle, keyword)
                .eq(StringUtils.hasText(province), Exam::getProvince, province)
                .eq(StringUtils.hasText(examType), Exam::getExamType, examType)
                .eq(StringUtils.hasText(seasonName), Exam::getSeasonName, seasonName)
                .eq(StringUtils.hasText(subjectName), Exam::getSubjectName, subjectName)
                .ne(Exam::getStatus, "CLOSED")
                .orderByDesc(Exam::getCreatedAt)).stream()
                .filter(exam -> matchesAvailabilityStatus(exam, status))
                .map(this::toExamSummary)
                .collect(Collectors.toList());
        int fromIndex = Math.max(0, (int) ((pageNo - 1) * pageSize));
        int toIndex = Math.min(allRecords.size(), fromIndex + pageSize.intValue());
        List<Map<String, Object>> pageRecords = fromIndex >= allRecords.size() ? new ArrayList<>() : allRecords.subList(fromIndex, toIndex);
        return new PageResult<>((long) allRecords.size(), pageNo, pageSize, pageRecords);
    }

    public Map<String, Object> detail(Long examId) {
        Exam exam = getExam(examId);
        List<ExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .orderByAsc(ExamQuestion::getSortNo));
        Map<String, Object> rankingPayload = ranking(examId, "GLOBAL");
        List<Map<String, Object>> ranking = rankingPayload.get("records") instanceof List
                ? (List<Map<String, Object>>) rankingPayload.get("records")
                : Collections.emptyList();
        Map<String, Object> result = new HashMap<>();
        result.put("exam", toExamSummary(exam));
        result.put("questionCount", relations.size());
        result.put("totalScore", relations.stream().mapToInt(ExamQuestion::getScore).sum());
        result.put("rankingPreview", ranking.stream().limit(5).collect(Collectors.toList()));
        result.put("analysisCourses", loadAnalysisCourses(exam));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startExam(Long examId) {
        Long userId = SecurityUtils.getUserId();
        Exam exam = getExam(examId);
        validateExamTime(exam);
        List<ExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .orderByAsc(ExamQuestion::getSortNo));
        if (CollectionUtils.isEmpty(relations)) {
            throw new BizException("当前模考暂无题目");
        }

        ExamRecord record = new ExamRecord();
        record.setExamId(examId);
        record.setUserId(userId);
        record.setTitle(exam.getTitle());
        record.setTotalScore(exam.getTotalScore());
        record.setQuestionCount(relations.size());
        record.setCorrectCount(0);
        record.setScore(0);
        record.setDurationSeconds(0);
        record.setStatus("IN_PROGRESS");
        record.setRankingEnabled(exam.getRankingEnabled());
        record.setStartedAt(LocalDateTime.now());
        examRecordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        return result;
    }

    public Map<String, Object> examSession(Long recordId) {
        ExamRecord record = getOwnedRecord(recordId);
        Exam exam = getExam(record.getExamId());
        List<ExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, exam.getId())
                .orderByAsc(ExamQuestion::getSortNo));
        return buildExamPayload(record, exam, relations, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitExam(Long recordId, ExamSubmitRequest request) {
        ExamRecord record = getOwnedRecord(recordId);
        if ("SUBMITTED".equals(record.getStatus())) {
            throw new BizException("该模考已交卷");
        }
        Exam exam = getExam(record.getExamId());
        List<ExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, exam.getId())
                .orderByAsc(ExamQuestion::getSortNo));
        Map<Long, ExamQuestion> relationMap = relations.stream().collect(Collectors.toMap(ExamQuestion::getQuestionId, Function.identity()));
        List<Question> questions = questionMapper.selectBatchIds(relationMap.keySet());
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));

        examAnswerMapper.delete(new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getExamRecordId, recordId));

        int correctCount = 0;
        int totalScore = 0;
        for (ExamSubmitRequest.AnswerItem item : request.getAnswers()) {
            ExamQuestion relation = relationMap.get(item.getQuestionId());
            Question question = questionMap.get(item.getQuestionId());
            if (relation == null || question == null) {
                throw new BizException("存在不属于本次模考的题目");
            }
            boolean correct = normalizeAnswer(question.getAnswerValue()).equals(normalizeAnswer(item.getUserAnswer()));
            int score = correct ? relation.getScore() : 0;
            if (correct) {
                correctCount++;
            }
            totalScore += score;

            ExamAnswer answer = new ExamAnswer();
            answer.setExamRecordId(recordId);
            answer.setExamId(exam.getId());
            answer.setQuestionId(question.getId());
            answer.setUserAnswer(item.getUserAnswer());
            answer.setCorrectAnswer(question.getAnswerValue());
            answer.setIsCorrect(correct);
            answer.setScore(score);
            examAnswerMapper.insert(answer);

            syncWrongQuestion(record.getUserId(), question.getId(), record.getId(), item.getUserAnswer(), correct);
        }

        record.setCorrectCount(correctCount);
        record.setScore(totalScore);
        record.setDurationSeconds(request.getDurationSeconds());
        record.setStatus("SUBMITTED");
        record.setSubmittedAt(LocalDateTime.now());
        examRecordMapper.updateById(record);
        questionService.ensureDailyCheckIn(record.getUserId(), "EXAM", record.getId());
        recommendEngineService.enqueueBehaviorRefresh(record.getUserId(), "EXAM_SUBMIT");

        Map<String, Object> result = new HashMap<>();
        result.put("reportId", record.getId());
        result.put("score", record.getScore());
        result.put("rank", calculateRank(record));
        return result;
    }

    public Map<String, Object> examReport(Long recordId) {
        ExamRecord record = getOwnedRecord(recordId);
        if (!"SUBMITTED".equals(record.getStatus())) {
            throw new BizException("模考尚未提交");
        }
        Exam exam = getExam(record.getExamId());
        List<ExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, exam.getId())
                .orderByAsc(ExamQuestion::getSortNo));
        Map<String, Object> result = buildExamPayload(record, exam, relations, true);
        result.put("rank", calculateRank(record));
        List<Map<String, Object>> wrongQuestions = ((List<Map<String, Object>>) result.get("questions")).stream()
                .filter(item -> Boolean.FALSE.equals(item.get("isCorrect")))
                .collect(Collectors.toList());
        result.put("wrongQuestions", wrongQuestions);
        result.put("wrongQuestionCount", wrongQuestions.size());
        result.put("analysisCourses", loadAnalysisCourses(exam));
        return result;
    }

    public Map<String, Object> ranking(Long examId, String scope) {
        Exam exam = getExam(examId);
        String resolvedScope = "PROVINCE".equalsIgnoreCase(scope) ? "PROVINCE" : "GLOBAL";
        String province = resolveRankingProvince(exam, resolvedScope);
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStatus, "SUBMITTED")
                .orderByDesc(ExamRecord::getScore)
                .orderByAsc(ExamRecord::getDurationSeconds)
                .orderByAsc(ExamRecord::getSubmittedAt));
        List<Map<String, Object>> ranking = new ArrayList<>();
        int rank = 1;
        for (ExamRecord record : records) {
            SysUser user = sysUserMapper.selectById(record.getUserId());
            if ("PROVINCE".equals(resolvedScope) && (user == null || !province.equals(user.getProvince()))) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("rank", rank++);
            item.put("userId", record.getUserId());
            item.put("nickname", user == null ? "匿名用户" : user.getNickname());
            item.put("province", user == null ? null : user.getProvince());
            item.put("score", record.getScore());
            item.put("correctCount", record.getCorrectCount());
            item.put("durationSeconds", record.getDurationSeconds());
            item.put("submittedAt", record.getSubmittedAt());
            ranking.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("records", ranking);
        result.put("total", ranking.size());
        result.put("scope", resolvedScope);
        result.put("province", province);
        result.put("scopeOptions", java.util.Arrays.asList("GLOBAL", "PROVINCE"));
        return result;
    }

    private Map<String, Object> buildExamPayload(ExamRecord record, Exam exam, List<ExamQuestion> relations, boolean withAnalysis) {
        List<Long> questionIds = relations.stream().map(ExamQuestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, List<QuestionOption>> optionMap = loadOptionMap(questionIds);
        Map<Long, ExamAnswer> answerMap = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                        .eq(ExamAnswer::getExamRecordId, record.getId()))
                .stream()
                .collect(Collectors.toMap(ExamAnswer::getQuestionId, Function.identity()));
        Map<Long, UserQuestionFavorite> favoriteMap = loadFavoriteMap(questionIds);

        List<Map<String, Object>> questionItems = new ArrayList<>();
        for (ExamQuestion relation : relations) {
            Question question = questionMap.get(relation.getQuestionId());
            if (question == null) {
                continue;
            }
            ExamAnswer answer = answerMap.get(question.getId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", question.getId());
            item.put("material", question.getMaterial());
            item.put("stem", question.getStem());
            item.put("subjectName", question.getSubjectName());
            item.put("categoryName", question.getCategoryName());
            item.put("questionType", question.getQuestionType());
            item.put("difficultyLevel", question.getDifficultyLevel());
            item.put("knowledgePoints", question.getKnowledgePoints());
            item.put("score", relation.getScore());
            item.put("options", optionMap.getOrDefault(question.getId(), Collections.emptyList()));
            item.put("userAnswer", answer == null ? null : answer.getUserAnswer());
            item.put("isCorrect", answer == null ? null : answer.getIsCorrect());
            item.put("favorite", favoriteMap.containsKey(question.getId()));
            item.put("videoExplainTitle", question.getVideoExplainTitle());
            item.put("videoExplainUrl", question.getVideoExplainUrl());
            if (withAnalysis) {
                item.put("correctAnswer", question.getAnswerValue());
                item.put("analysis", question.getAnalysis());
            }
            questionItems.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("exam", toExamSummary(exam));
        result.put("record", record);
        result.put("questions", questionItems);
        return result;
    }

    private Map<Long, List<QuestionOption>> loadOptionMap(List<Long> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return Collections.emptyMap();
        }
        return questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .in(QuestionOption::getQuestionId, questionIds)
                        .orderByAsc(QuestionOption::getQuestionId)
                        .orderByAsc(QuestionOption::getSortNo))
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId, LinkedHashMap::new, Collectors.toList()));
    }

    private Exam getExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BizException(404, "模考不存在");
        }
        return exam;
    }

    private ExamRecord getOwnedRecord(Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException(404, "模考记录不存在");
        }
        if (!record.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BizException(403, "无权查看该模考记录");
        }
        return record;
    }

    private void validateExamTime(Exam exam) {
        LocalDateTime now = LocalDateTime.now();
        if (!"OPEN".equals(exam.getStatus())) {
            throw new BizException("当前模考未开放");
        }
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BizException("模考尚未开始");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BizException("模考已结束");
        }
    }

    private int calculateRank(ExamRecord record) {
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, record.getExamId())
                .eq(ExamRecord::getStatus, "SUBMITTED")
                .orderByDesc(ExamRecord::getScore)
                .orderByAsc(ExamRecord::getDurationSeconds)
                .orderByAsc(ExamRecord::getSubmittedAt));
        int rank = 1;
        for (ExamRecord item : records) {
            if (item.getId().equals(record.getId())) {
                return rank;
            }
            rank++;
        }
        return rank;
    }

    private void syncWrongQuestion(Long userId, Long questionId, Long recordId, String userAnswer, boolean correct) {
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
        wrongQuestion.setLatestSourceType("EXAM");
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

    private String normalizeAnswer(String answer) {
        return QuestionAnswerUtils.normalizeAnswer(answer);
    }

    private Map<String, Object> toExamSummary(Exam exam) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", exam.getId());
        item.put("title", exam.getTitle());
        item.put("intro", exam.getIntro());
        item.put("province", exam.getProvince());
        item.put("examType", exam.getExamType());
        item.put("seasonName", exam.getSeasonName());
        item.put("subjectName", exam.getSubjectName());
        item.put("durationMinutes", exam.getDurationMinutes());
        item.put("totalScore", exam.getTotalScore());
        item.put("questionCount", exam.getQuestionCount());
        item.put("status", exam.getStatus());
        item.put("rankingEnabled", exam.getRankingEnabled());
        item.put("startTime", exam.getStartTime());
        item.put("endTime", exam.getEndTime());
        item.put("availabilityStatus", resolveAvailabilityStatus(exam));
        item.put("availabilityStatusLabel", resolveAvailabilityStatusLabel(exam));
        item.put("canStart", "ONGOING".equals(resolveAvailabilityStatus(exam)));
        return item;
    }

    private boolean matchesAvailabilityStatus(Exam exam, String status) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        return status.equalsIgnoreCase(resolveAvailabilityStatus(exam));
    }

    private String resolveAvailabilityStatus(Exam exam) {
        if (!"OPEN".equals(exam.getStatus())) {
            return "CLOSED";
        }
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            return "NOT_STARTED";
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            return "ENDED";
        }
        return "ONGOING";
    }

    private String resolveAvailabilityStatusLabel(Exam exam) {
        String status = resolveAvailabilityStatus(exam);
        if ("NOT_STARTED".equals(status)) {
            return "未开始";
        }
        if ("ENDED".equals(status)) {
            return "已结束";
        }
        if ("CLOSED".equals(status)) {
            return "已关闭";
        }
        return "可练习";
    }

    private String resolveRankingProvince(Exam exam, String scope) {
        if (!"PROVINCE".equals(scope)) {
            return null;
        }
        if (!SecurityUtils.getRoles().isEmpty()) {
            SysUser user = sysUserMapper.selectById(SecurityUtils.getUserId());
            if (user != null && StringUtils.hasText(user.getProvince())) {
                return user.getProvince();
            }
        }
        return StringUtils.hasText(exam.getProvince()) ? exam.getProvince() : "全国";
    }

    private Map<Long, UserQuestionFavorite> loadFavoriteMap(List<Long> questionIds) {
        if (CollectionUtils.isEmpty(questionIds) || SecurityUtils.getRoles().isEmpty()) {
            return Collections.emptyMap();
        }
        return userQuestionFavoriteMapper.selectList(new LambdaQueryWrapper<UserQuestionFavorite>()
                        .eq(UserQuestionFavorite::getUserId, SecurityUtils.getUserId())
                        .in(UserQuestionFavorite::getQuestionId, questionIds))
                .stream()
                .collect(Collectors.toMap(UserQuestionFavorite::getQuestionId, Function.identity(), (left, right) -> left));
    }

    private List<Map<String, Object>> loadAnalysisCourses(Exam exam) {
        List<ExamAnalysisCourse> relations = examAnalysisCourseMapper.selectList(new LambdaQueryWrapper<ExamAnalysisCourse>()
                .eq(ExamAnalysisCourse::getExamId, exam.getId())
                .orderByAsc(ExamAnalysisCourse::getSortNo)
                .orderByAsc(ExamAnalysisCourse::getId));
        Map<Long, String> reasonMap = relations.stream()
                .collect(Collectors.toMap(ExamAnalysisCourse::getCourseId, ExamAnalysisCourse::getRecommendReason, (left, right) -> left, LinkedHashMap::new));
        List<Course> courses = relations.isEmpty()
                ? courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, "PUBLISHED")
                .eq(StringUtils.hasText(exam.getSubjectName()), Course::getSubjectName, exam.getSubjectName())
                .eq(StringUtils.hasText(exam.getProvince()) && !"全国".equals(exam.getProvince()), Course::getProvince, exam.getProvince())
                .orderByDesc(Course::getLearnCount)
                .last("limit 3"))
                : courseMapper.selectBatchIds(relations.stream().map(ExamAnalysisCourse::getCourseId).collect(Collectors.toList()));
        return courses.stream()
                .map(course -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("courseId", course.getId());
                    item.put("title", course.getTitle());
                    item.put("coverUrl", course.getCoverUrl());
                    item.put("teacherName", course.getTeacherName());
                    item.put("subjectName", course.getSubjectName());
                    item.put("price", course.getPrice());
                    item.put("path", "/course/detail/" + course.getId());
                    item.put("recommendReason", reasonMap.getOrDefault(course.getId(),
                            "与本场模考的科目、题型和解析重点匹配，适合模考后继续系统补强。"));
                    return item;
                })
                .limit(3)
                .collect(Collectors.toList());
    }
}
