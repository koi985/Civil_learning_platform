package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.common.util.QuestionAnswerUtils;
import com.civil.platform.common.util.SecurityUtils;
import com.civil.platform.module.dto.TaskSubmitRequest;
import com.civil.platform.module.entity.*;
import com.civil.platform.module.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClassroomTaskService {

    private final ClassroomMapper classroomMapper;
    private final ClassroomMemberMapper classroomMemberMapper;
    private final ClassroomHomeworkMapper classroomHomeworkMapper;
    private final ClassroomHomeworkQuestionMapper classroomHomeworkQuestionMapper;
    private final ClassroomHomeworkSubmissionMapper classroomHomeworkSubmissionMapper;
    private final ClassroomHomeworkAnswerMapper classroomHomeworkAnswerMapper;
    private final ClassroomExamTaskMapper classroomExamTaskMapper;
    private final ClassroomExamQuestionMapper classroomExamQuestionMapper;
    private final ClassroomExamSubmissionMapper classroomExamSubmissionMapper;
    private final ClassroomExamAnswerMapper classroomExamAnswerMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final UserWrongQuestionMapper userWrongQuestionMapper;

    public ClassroomTaskService(ClassroomMapper classroomMapper,
                                ClassroomMemberMapper classroomMemberMapper,
                                ClassroomHomeworkMapper classroomHomeworkMapper,
                                ClassroomHomeworkQuestionMapper classroomHomeworkQuestionMapper,
                                ClassroomHomeworkSubmissionMapper classroomHomeworkSubmissionMapper,
                                ClassroomHomeworkAnswerMapper classroomHomeworkAnswerMapper,
                                ClassroomExamTaskMapper classroomExamTaskMapper,
                                ClassroomExamQuestionMapper classroomExamQuestionMapper,
                                ClassroomExamSubmissionMapper classroomExamSubmissionMapper,
                                ClassroomExamAnswerMapper classroomExamAnswerMapper,
                                QuestionMapper questionMapper,
                                QuestionOptionMapper questionOptionMapper,
                                UserWrongQuestionMapper userWrongQuestionMapper) {
        this.classroomMapper = classroomMapper;
        this.classroomMemberMapper = classroomMemberMapper;
        this.classroomHomeworkMapper = classroomHomeworkMapper;
        this.classroomHomeworkQuestionMapper = classroomHomeworkQuestionMapper;
        this.classroomHomeworkSubmissionMapper = classroomHomeworkSubmissionMapper;
        this.classroomHomeworkAnswerMapper = classroomHomeworkAnswerMapper;
        this.classroomExamTaskMapper = classroomExamTaskMapper;
        this.classroomExamQuestionMapper = classroomExamQuestionMapper;
        this.classroomExamSubmissionMapper = classroomExamSubmissionMapper;
        this.classroomExamAnswerMapper = classroomExamAnswerMapper;
        this.questionMapper = questionMapper;
        this.questionOptionMapper = questionOptionMapper;
        this.userWrongQuestionMapper = userWrongQuestionMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startHomework(Long homeworkId) {
        Long userId = SecurityUtils.getUserId();
        ClassroomHomework homework = getHomework(homeworkId);
        ensureClassroomActive(homework.getClassroomId());
        ensureClassroomMember(homework.getClassroomId(), userId);
        ClassroomHomeworkSubmission existing = classroomHomeworkSubmissionMapper.selectOne(new LambdaQueryWrapper<ClassroomHomeworkSubmission>()
                .eq(ClassroomHomeworkSubmission::getHomeworkId, homeworkId)
                .eq(ClassroomHomeworkSubmission::getUserId, userId)
                .orderByDesc(ClassroomHomeworkSubmission::getId)
                .last("limit 1"));
        if (existing == null && homework.getDeadlineAt() != null && LocalDateTime.now().isAfter(homework.getDeadlineAt())) {
            throw new BizException("作业已超过截止时间，不能再开始作答");
        }
        if (existing != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("submissionId", existing.getId());
            result.put("submitted", "SUBMITTED".equals(existing.getStatus()));
            return result;
        }
        ClassroomHomeworkSubmission submission = new ClassroomHomeworkSubmission();
        submission.setHomeworkId(homeworkId);
        submission.setClassroomId(homework.getClassroomId());
        submission.setUserId(userId);
        submission.setScore(0);
        submission.setObjectiveScore(0);
        submission.setQuestionCount(homework.getQuestionCount());
        submission.setAnsweredCount(0);
        submission.setCorrectCount(0);
        submission.setDurationSeconds(0);
        submission.setStatus("IN_PROGRESS");
        submission.setReviewStatus("PENDING");
        submission.setStartedAt(LocalDateTime.now());
        classroomHomeworkSubmissionMapper.insert(submission);

        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", submission.getId());
        result.put("submitted", false);
        return result;
    }

    public Map<String, Object> homeworkSession(Long submissionId) {
        ClassroomHomeworkSubmission submission = getHomeworkSubmissionAccessible(submissionId);
        ClassroomHomework homework = getHomework(submission.getHomeworkId());
        return buildHomeworkPayload(homework, submission, "SUBMITTED".equals(submission.getStatus()));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitHomework(Long submissionId, TaskSubmitRequest request) {
        ClassroomHomeworkSubmission submission = getHomeworkSubmissionOwned(submissionId);
        if ("SUBMITTED".equals(submission.getStatus())) {
            throw new BizException("作业已提交");
        }
        ClassroomHomework homework = getHomework(submission.getHomeworkId());
        ensureClassroomActive(homework.getClassroomId());
        List<ClassroomHomeworkQuestion> relations = classroomHomeworkQuestionMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkQuestion>()
                .eq(ClassroomHomeworkQuestion::getHomeworkId, homework.getId())
                .orderByAsc(ClassroomHomeworkQuestion::getSortNo));
        Map<Long, ClassroomHomeworkQuestion> relationMap = relations.stream()
                .collect(Collectors.toMap(ClassroomHomeworkQuestion::getQuestionId, Function.identity()));
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(relationMap.keySet()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        classroomHomeworkAnswerMapper.delete(new LambdaQueryWrapper<ClassroomHomeworkAnswer>()
                .eq(ClassroomHomeworkAnswer::getSubmissionId, submissionId));

        int answeredCount = 0;
        int correctCount = 0;
        int totalScore = 0;
        for (TaskSubmitRequest.AnswerItem item : request.getAnswers()) {
            ClassroomHomeworkQuestion relation = relationMap.get(item.getQuestionId());
            Question question = questionMap.get(item.getQuestionId());
            if (relation == null || question == null) {
                throw new BizException("作业题目异常");
            }
            boolean correct = normalizeAnswer(question.getAnswerValue()).equals(normalizeAnswer(item.getUserAnswer()));
            int score = correct ? relation.getScore() : 0;
            if (StringUtils.hasText(item.getUserAnswer())) {
                answeredCount++;
            }
            if (correct) {
                correctCount++;
            }
            totalScore += score;
            ClassroomHomeworkAnswer answer = new ClassroomHomeworkAnswer();
            answer.setSubmissionId(submissionId);
            answer.setHomeworkId(homework.getId());
            answer.setQuestionId(question.getId());
            answer.setUserAnswer(item.getUserAnswer());
            answer.setCorrectAnswer(question.getAnswerValue());
            answer.setIsCorrect(correct);
            answer.setScore(score);
            answer.setTeacherComment(null);
            classroomHomeworkAnswerMapper.insert(answer);
            syncWrongQuestion(submission.getUserId(), question.getId(), submission.getId(), item.getUserAnswer(), correct, "CLASSROOM_HOMEWORK");
        }

        submission.setObjectiveScore(totalScore);
        submission.setScore(totalScore);
        submission.setAnsweredCount(answeredCount);
        submission.setCorrectCount(correctCount);
        submission.setDurationSeconds(request.getDurationSeconds());
        LocalDateTime now = LocalDateTime.now();
        boolean late = homework.getDeadlineAt() != null && now.isAfter(homework.getDeadlineAt());
        submission.setStatus("SUBMITTED");
        submission.setReviewStatus(late ? "AUTO_GRADED_LATE" : "AUTO_GRADED");
        submission.setSubmittedAt(now);
        classroomHomeworkSubmissionMapper.updateById(submission);

        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", submission.getId());
        result.put("score", submission.getScore());
        result.put("late", late);
        return result;
    }

    public Map<String, Object> homeworkReport(Long submissionId) {
        ClassroomHomeworkSubmission submission = getHomeworkSubmissionAccessible(submissionId);
        if (!"SUBMITTED".equals(submission.getStatus())) {
            throw new BizException("作业尚未提交");
        }
        ClassroomHomework homework = getHomework(submission.getHomeworkId());
        return buildHomeworkPayload(homework, submission, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startClassroomExam(Long classroomExamId) {
        Long userId = SecurityUtils.getUserId();
        ClassroomExamTask task = getClassroomExam(classroomExamId);
        ensureClassroomActive(task.getClassroomId());
        ensureClassroomMember(task.getClassroomId(), userId);
        validateTaskTime(task.getStartTime(), task.getEndTime());
        ClassroomExamSubmission existing = classroomExamSubmissionMapper.selectOne(new LambdaQueryWrapper<ClassroomExamSubmission>()
                .eq(ClassroomExamSubmission::getClassroomExamId, classroomExamId)
                .eq(ClassroomExamSubmission::getUserId, userId)
                .orderByDesc(ClassroomExamSubmission::getId)
                .last("limit 1"));
        if (existing != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("submissionId", existing.getId());
            result.put("submitted", "SUBMITTED".equals(existing.getStatus()));
            return result;
        }
        ClassroomExamSubmission submission = new ClassroomExamSubmission();
        submission.setClassroomExamId(classroomExamId);
        submission.setClassroomId(task.getClassroomId());
        submission.setUserId(userId);
        submission.setScore(0);
        submission.setQuestionCount(task.getQuestionCount());
        submission.setAnsweredCount(0);
        submission.setCorrectCount(0);
        submission.setDurationSeconds(0);
        submission.setStatus("IN_PROGRESS");
        submission.setReviewStatus("PENDING");
        submission.setStartedAt(LocalDateTime.now());
        classroomExamSubmissionMapper.insert(submission);

        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", submission.getId());
        result.put("submitted", false);
        return result;
    }

    public Map<String, Object> classroomExamSession(Long submissionId) {
        ClassroomExamSubmission submission = getClassroomExamSubmissionAccessible(submissionId);
        ClassroomExamTask task = getClassroomExam(submission.getClassroomExamId());
        return buildClassroomExamPayload(task, submission, "SUBMITTED".equals(submission.getStatus()));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitClassroomExam(Long submissionId, TaskSubmitRequest request) {
        ClassroomExamSubmission submission = getClassroomExamSubmissionOwned(submissionId);
        if ("SUBMITTED".equals(submission.getStatus())) {
            throw new BizException("考试已提交");
        }
        ClassroomExamTask task = getClassroomExam(submission.getClassroomExamId());
        ensureClassroomActive(task.getClassroomId());
        validateTaskTime(task.getStartTime(), task.getEndTime());
        List<ClassroomExamQuestion> relations = classroomExamQuestionMapper.selectList(new LambdaQueryWrapper<ClassroomExamQuestion>()
                .eq(ClassroomExamQuestion::getClassroomExamId, task.getId())
                .orderByAsc(ClassroomExamQuestion::getSortNo));
        Map<Long, ClassroomExamQuestion> relationMap = relations.stream()
                .collect(Collectors.toMap(ClassroomExamQuestion::getQuestionId, Function.identity()));
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(relationMap.keySet()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        classroomExamAnswerMapper.delete(new LambdaQueryWrapper<ClassroomExamAnswer>()
                .eq(ClassroomExamAnswer::getSubmissionId, submissionId));

        int answeredCount = 0;
        int correctCount = 0;
        int totalScore = 0;
        for (TaskSubmitRequest.AnswerItem item : request.getAnswers()) {
            ClassroomExamQuestion relation = relationMap.get(item.getQuestionId());
            Question question = questionMap.get(item.getQuestionId());
            if (relation == null || question == null) {
                throw new BizException("班级考试题目异常");
            }
            boolean correct = normalizeAnswer(question.getAnswerValue()).equals(normalizeAnswer(item.getUserAnswer()));
            int score = correct ? relation.getScore() : 0;
            if (StringUtils.hasText(item.getUserAnswer())) {
                answeredCount++;
            }
            if (correct) {
                correctCount++;
            }
            totalScore += score;
            ClassroomExamAnswer answer = new ClassroomExamAnswer();
            answer.setSubmissionId(submissionId);
            answer.setClassroomExamId(task.getId());
            answer.setQuestionId(question.getId());
            answer.setUserAnswer(item.getUserAnswer());
            answer.setCorrectAnswer(question.getAnswerValue());
            answer.setIsCorrect(correct);
            answer.setScore(score);
            answer.setTeacherComment(null);
            classroomExamAnswerMapper.insert(answer);
            syncWrongQuestion(submission.getUserId(), question.getId(), submission.getId(), item.getUserAnswer(), correct, "CLASSROOM_EXAM");
        }

        submission.setScore(totalScore);
        submission.setAnsweredCount(answeredCount);
        submission.setCorrectCount(correctCount);
        submission.setDurationSeconds(request.getDurationSeconds());
        submission.setStatus("SUBMITTED");
        submission.setReviewStatus("AUTO_GRADED");
        submission.setSubmittedAt(LocalDateTime.now());
        classroomExamSubmissionMapper.updateById(submission);

        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", submission.getId());
        result.put("score", submission.getScore());
        return result;
    }

    public Map<String, Object> classroomExamReport(Long submissionId) {
        ClassroomExamSubmission submission = getClassroomExamSubmissionAccessible(submissionId);
        if (!"SUBMITTED".equals(submission.getStatus())) {
            throw new BizException("考试尚未提交");
        }
        ClassroomExamTask task = getClassroomExam(submission.getClassroomExamId());
        Map<String, Object> result = buildClassroomExamPayload(task, submission, true);
        result.put("rank", calculateClassroomExamRank(submission));
        return result;
    }

    private void ensureClassroomMember(Long classroomId, Long userId) {
        Long count = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .eq(ClassroomMember::getUserId, userId));
        if (count == 0) {
            throw new BizException(403, "请先加入班级");
        }
    }

    private void ensureClassroomActive(Long classroomId) {
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null || "DISMISSED".equals(classroom.getStatus())) {
            throw new BizException(404, "班级已解散");
        }
    }

    private ClassroomHomework getHomework(Long homeworkId) {
        ClassroomHomework homework = classroomHomeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new BizException(404, "班级作业不存在");
        }
        return homework;
    }

    private ClassroomExamTask getClassroomExam(Long classroomExamId) {
        ClassroomExamTask task = classroomExamTaskMapper.selectById(classroomExamId);
        if (task == null) {
            throw new BizException(404, "班级考试不存在");
        }
        return task;
    }

    private ClassroomHomeworkSubmission getHomeworkSubmissionOwned(Long submissionId) {
        ClassroomHomeworkSubmission submission = classroomHomeworkSubmissionMapper.selectById(submissionId);
        if (submission == null || !submission.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BizException(403, "无权操作该作业");
        }
        return submission;
    }

    private ClassroomHomeworkSubmission getHomeworkSubmissionAccessible(Long submissionId) {
        ClassroomHomeworkSubmission submission = classroomHomeworkSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BizException(404, "作业记录不存在");
        }
        if (submission.getUserId().equals(SecurityUtils.getUserId())) {
            return submission;
        }
        assertTeacherOfClassroom(submission.getClassroomId());
        return submission;
    }

    private ClassroomExamSubmission getClassroomExamSubmissionOwned(Long submissionId) {
        ClassroomExamSubmission submission = classroomExamSubmissionMapper.selectById(submissionId);
        if (submission == null || !submission.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BizException(403, "无权操作该考试");
        }
        return submission;
    }

    private ClassroomExamSubmission getClassroomExamSubmissionAccessible(Long submissionId) {
        ClassroomExamSubmission submission = classroomExamSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BizException(404, "考试记录不存在");
        }
        if (submission.getUserId().equals(SecurityUtils.getUserId())) {
            return submission;
        }
        assertTeacherOfClassroom(submission.getClassroomId());
        return submission;
    }

    private void assertTeacherOfClassroom(Long classroomId) {
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null || !SecurityUtils.getUserId().equals(classroom.getTeacherId())) {
            throw new BizException(403, "无权查看该班级任务");
        }
    }

    private Map<String, Object> buildHomeworkPayload(ClassroomHomework homework, ClassroomHomeworkSubmission submission, boolean withAnalysis) {
        List<ClassroomHomeworkQuestion> relations = classroomHomeworkQuestionMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkQuestion>()
                .eq(ClassroomHomeworkQuestion::getHomeworkId, homework.getId())
                .orderByAsc(ClassroomHomeworkQuestion::getSortNo));
        List<Long> questionIds = relations.stream().map(ClassroomHomeworkQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, List<QuestionOption>> optionMap = loadQuestionOptions(questionIds);
        Map<Long, ClassroomHomeworkAnswer> answerMap = classroomHomeworkAnswerMapper.selectList(new LambdaQueryWrapper<ClassroomHomeworkAnswer>()
                        .eq(ClassroomHomeworkAnswer::getSubmissionId, submission.getId()))
                .stream()
                .collect(Collectors.toMap(ClassroomHomeworkAnswer::getQuestionId, Function.identity()));
        List<Map<String, Object>> questions = new ArrayList<>();
        for (ClassroomHomeworkQuestion relation : relations) {
            Question question = questionMap.get(relation.getQuestionId());
            if (question == null) {
                continue;
            }
            ClassroomHomeworkAnswer answer = answerMap.get(question.getId());
            questions.add(toTaskQuestion(question, optionMap.get(question.getId()), relation.getScore(), answer, withAnalysis));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("homework", homework);
        result.put("submission", submission);
        result.put("questions", questions);
        return result;
    }

    private Map<String, Object> buildClassroomExamPayload(ClassroomExamTask task, ClassroomExamSubmission submission, boolean withAnalysis) {
        List<ClassroomExamQuestion> relations = classroomExamQuestionMapper.selectList(new LambdaQueryWrapper<ClassroomExamQuestion>()
                .eq(ClassroomExamQuestion::getClassroomExamId, task.getId())
                .orderByAsc(ClassroomExamQuestion::getSortNo));
        List<Long> questionIds = relations.stream().map(ClassroomExamQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, List<QuestionOption>> optionMap = loadQuestionOptions(questionIds);
        Map<Long, ClassroomExamAnswer> answerMap = classroomExamAnswerMapper.selectList(new LambdaQueryWrapper<ClassroomExamAnswer>()
                        .eq(ClassroomExamAnswer::getSubmissionId, submission.getId()))
                .stream()
                .collect(Collectors.toMap(ClassroomExamAnswer::getQuestionId, Function.identity()));
        List<Map<String, Object>> questions = new ArrayList<>();
        for (ClassroomExamQuestion relation : relations) {
            Question question = questionMap.get(relation.getQuestionId());
            if (question == null) {
                continue;
            }
            ClassroomExamAnswer answer = answerMap.get(question.getId());
            questions.add(toTaskQuestion(question, optionMap.get(question.getId()), relation.getScore(), answer, withAnalysis));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("classroomExam", task);
        result.put("submission", submission);
        result.put("questions", questions);
        return result;
    }

    private Map<String, Object> toTaskQuestion(Question question, List<QuestionOption> options, Integer score, ClassroomHomeworkAnswer answer, boolean withAnalysis) {
        return toTaskQuestion(question, options, score,
                answer == null ? null : answer.getUserAnswer(),
                answer == null ? null : answer.getIsCorrect(),
                answer == null ? null : answer.getScore(),
                answer == null ? null : answer.getTeacherComment(),
                withAnalysis);
    }

    private Map<String, Object> toTaskQuestion(Question question, List<QuestionOption> options, Integer score, ClassroomExamAnswer answer, boolean withAnalysis) {
        return toTaskQuestion(question, options, score,
                answer == null ? null : answer.getUserAnswer(),
                answer == null ? null : answer.getIsCorrect(),
                answer == null ? null : answer.getScore(),
                answer == null ? null : answer.getTeacherComment(),
                withAnalysis);
    }

    private Map<String, Object> toTaskQuestion(Question question, List<QuestionOption> options, Integer score,
                                               String userAnswer, Boolean isCorrect, Integer answerScore,
                                               String teacherComment, boolean withAnalysis) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", question.getId());
        item.put("material", question.getMaterial());
        item.put("stem", question.getStem());
        item.put("subjectName", question.getSubjectName());
        item.put("categoryName", question.getCategoryName());
        item.put("questionType", question.getQuestionType());
        item.put("subjective", isSubjectiveQuestion(question.getQuestionType()));
        item.put("difficultyLevel", question.getDifficultyLevel());
        item.put("knowledgePoints", question.getKnowledgePoints());
        item.put("score", score);
        item.put("options", options == null ? Collections.emptyList() : options);
        item.put("userAnswer", userAnswer);
        item.put("isCorrect", isCorrect);
        item.put("answerScore", answerScore);
        item.put("teacherComment", teacherComment);
        if (withAnalysis) {
            item.put("correctAnswer", question.getAnswerValue());
            item.put("analysis", question.getAnalysis());
        }
        return item;
    }

    private boolean isSubjectiveQuestion(String questionType) {
        return !Arrays.asList("SINGLE", "MULTI", "JUDGE").contains(questionType);
    }

    private Map<Long, List<QuestionOption>> loadQuestionOptions(List<Long> questionIds) {
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

    private int calculateClassroomExamRank(ClassroomExamSubmission submission) {
        List<ClassroomExamSubmission> submissions = classroomExamSubmissionMapper.selectList(new LambdaQueryWrapper<ClassroomExamSubmission>()
                .eq(ClassroomExamSubmission::getClassroomExamId, submission.getClassroomExamId())
                .eq(ClassroomExamSubmission::getStatus, "SUBMITTED")
                .orderByDesc(ClassroomExamSubmission::getScore)
                .orderByAsc(ClassroomExamSubmission::getDurationSeconds)
                .orderByAsc(ClassroomExamSubmission::getSubmittedAt));
        int rank = 1;
        for (ClassroomExamSubmission item : submissions) {
            if (item.getId().equals(submission.getId())) {
                return rank;
            }
            rank++;
        }
        return rank;
    }

    private void validateTaskTime(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (startTime != null && now.isBefore(startTime)) {
            throw new BizException("当前任务尚未开始");
        }
        if (endTime != null && now.isAfter(endTime)) {
            throw new BizException("当前任务已结束");
        }
    }

    private void syncWrongQuestion(Long userId, Long questionId, Long recordId, String userAnswer, boolean correct, String sourceType) {
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

    private String normalizeAnswer(String answer) {
        return QuestionAnswerUtils.normalizeAnswer(answer);
    }
}
