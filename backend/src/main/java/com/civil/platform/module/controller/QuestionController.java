package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.module.dto.PracticeCreateRequest;
import com.civil.platform.module.dto.PracticeSubmitRequest;
import com.civil.platform.module.service.QuestionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/questions")
    public ApiResponse<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNo,
                                                             @RequestParam(defaultValue = "10") Long pageSize,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String province,
                                                             @RequestParam(required = false) String subjectName,
                                                             @RequestParam(required = false) String categoryName,
                                                             @RequestParam(required = false) String questionType,
                                                             @RequestParam(required = false) String difficultyLevel,
                                                             @RequestParam(required = false) String sourceName,
                                                             @RequestParam(required = false) String sortBy) {
        return ApiResponse.success(questionService.pageQuestions(pageNo, pageSize, keyword, province, subjectName, categoryName, questionType, difficultyLevel, sourceName, sortBy));
    }

    @GetMapping("/shenlun-groups")
    public ApiResponse<PageResult<Map<String, Object>>> pageShenlunGroups(@RequestParam(defaultValue = "1") Long pageNo,
                                                                          @RequestParam(defaultValue = "10") Long pageSize,
                                                                          @RequestParam(required = false) String keyword,
                                                                          @RequestParam(required = false) String province,
                                                                          @RequestParam(required = false) String sourceName,
                                                                          @RequestParam(required = false) Integer year) {
        return ApiResponse.success(questionService.pageShenlunGroups(pageNo, pageSize, keyword, province, sourceName, year));
    }

    @GetMapping("/shenlun-groups/{groupId}")
    public ApiResponse<Map<String, Object>> shenlunGroupDetail(@PathVariable Long groupId) {
        return ApiResponse.success(questionService.shenlunGroupDetail(groupId));
    }

    @PostMapping("/shenlun-groups/{groupId}/start-practice")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "题库中心", name = "开始申论材料组练习")
    public ApiResponse<Map<String, Object>> startShenlunGroupPractice(@PathVariable Long groupId) {
        return ApiResponse.success(questionService.startShenlunGroupPractice(groupId));
    }

    @GetMapping("/questions/random")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> randomQuestions(@RequestParam(defaultValue = "120") Integer count) {
        return ApiResponse.success(questionService.randomQuestions(count));
    }

    @GetMapping("/questions/search-dashboard")
    public ApiResponse<Map<String, Object>> searchDashboard() {
        return ApiResponse.success(questionService.searchDashboard());
    }

    @GetMapping("/question-bank/check-in")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> checkInStatus() {
        return ApiResponse.success(questionService.dailyCheckInStatus());
    }

    @PostMapping("/question-bank/check-in")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "题库中心", name = "每日打卡")
    public ApiResponse<Map<String, Object>> checkInToday() {
        return ApiResponse.success(questionService.checkInToday());
    }

    @GetMapping("/question-papers")
    public ApiResponse<PageResult<Map<String, Object>>> questionPapers(@RequestParam(defaultValue = "1") Long pageNo,
                                                                       @RequestParam(defaultValue = "12") Long pageSize,
                                                                       @RequestParam(required = false) String province) {
        return ApiResponse.success(questionService.pageQuestionPapers(pageNo, pageSize, province));
    }

    @GetMapping("/questions/papers")
    public ApiResponse<PageResult<Map<String, Object>>> questionPapersCompat(@RequestParam(defaultValue = "1") Long pageNo,
                                                                             @RequestParam(defaultValue = "12") Long pageSize,
                                                                             @RequestParam(required = false) String province) {
        return ApiResponse.success(questionService.pageQuestionPapers(pageNo, pageSize, province));
    }

    @PostMapping("/question-papers/{paperId}/start")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "题库中心", name = "开始试卷练习")
    public ApiResponse<Map<String, Object>> startQuestionPaper(@PathVariable Long paperId) {
        return ApiResponse.success(questionService.startQuestionPaper(paperId));
    }

    @PostMapping("/questions/papers/{paperId}/start")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "题库中心", name = "开始试卷练习")
    public ApiResponse<Map<String, Object>> startQuestionPaperCompat(@PathVariable Long paperId) {
        return ApiResponse.success(questionService.startQuestionPaper(paperId));
    }

    @PostMapping("/practices")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "题库中心", name = "创建练习")
    public ApiResponse<Map<String, Object>> createPractice(@RequestBody @Valid PracticeCreateRequest request) {
        return ApiResponse.success(questionService.createPractice(request));
    }

    @PostMapping("/wrong-questions/practice")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "题库中心", name = "错题再练")
    public ApiResponse<Map<String, Object>> createWrongPractice() {
        return ApiResponse.success(questionService.createWrongBookPractice());
    }

    @GetMapping("/practices/{practiceId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> practiceDetail(@PathVariable Long practiceId) {
        return ApiResponse.success(questionService.practiceDetail(practiceId));
    }

    @PostMapping("/practices/{practiceId}/submit")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "题库中心", name = "提交练习")
    public ApiResponse<Map<String, Object>> submitPractice(@PathVariable Long practiceId,
                                                           @RequestBody @Valid PracticeSubmitRequest request) {
        return ApiResponse.success(questionService.submitPractice(practiceId, request));
    }

    @GetMapping("/practices/{practiceId}/report")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> practiceReport(@PathVariable Long practiceId) {
        return ApiResponse.success(questionService.practiceReport(practiceId));
    }

    @GetMapping("/wrong-questions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<Map<String, Object>>> wrongQuestions(@RequestParam(defaultValue = "1") Long pageNo,
                                                                       @RequestParam(defaultValue = "10") Long pageSize,
                                                                       @RequestParam(required = false) String subjectName,
                                                                       @RequestParam(required = false) String categoryName) {
        return ApiResponse.success(questionService.wrongQuestions(pageNo, pageSize, subjectName, categoryName));
    }

    @GetMapping("/question-favorites")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<Map<String, Object>>> favoriteQuestions(@RequestParam(defaultValue = "1") Long pageNo,
                                                                          @RequestParam(defaultValue = "10") Long pageSize,
                                                                          @RequestParam(required = false) String subjectName,
                                                                          @RequestParam(required = false) String questionType) {
        return ApiResponse.success(questionService.favoriteQuestions(pageNo, pageSize, subjectName, questionType));
    }

    @PostMapping("/question-favorites/{questionId}")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "题库中心", name = "收藏题目")
    public ApiResponse<Map<String, Object>> addFavorite(@PathVariable Long questionId) {
        return ApiResponse.success(questionService.addFavorite(questionId));
    }

    @DeleteMapping("/question-favorites/{questionId}")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "题库中心", name = "取消收藏题目")
    public ApiResponse<Map<String, Object>> removeFavorite(@PathVariable Long questionId) {
        return ApiResponse.success(questionService.removeFavorite(questionId));
    }

    @DeleteMapping("/wrong-questions/{questionId}")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "题库中心", name = "移除错题")
    public ApiResponse<Void> removeWrongQuestion(@PathVariable Long questionId) {
        questionService.removeWrongQuestion(questionId);
        return ApiResponse.success("操作成功", null);
    }
}
