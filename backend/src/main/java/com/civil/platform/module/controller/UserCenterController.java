package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.module.dto.StudyPlanSaveRequest;
import com.civil.platform.module.dto.TestPaperGenerateRequest;
import com.civil.platform.module.dto.CourseLearningProgressRequest;
import com.civil.platform.module.dto.PasswordChangeRequest;
import com.civil.platform.module.dto.UserSettingSaveRequest;
import com.civil.platform.common.util.ExcelUtils;
import com.civil.platform.module.service.UserCourseLearningService;
import com.civil.platform.module.service.UserGrowthService;
import com.civil.platform.module.service.UserCenterService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/user-center")
public class UserCenterController {

    private final UserCenterService userCenterService;
    private final UserCourseLearningService userCourseLearningService;
    private final UserGrowthService userGrowthService;

    public UserCenterController(UserCenterService userCenterService,
                                UserCourseLearningService userCourseLearningService,
                                UserGrowthService userGrowthService) {
        this.userCenterService = userCenterService;
        this.userCourseLearningService = userCourseLearningService;
        this.userGrowthService = userGrowthService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(userCenterService.overview());
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        return ApiResponse.success(userCenterService.statistics());
    }

    @GetMapping("/courses")
    public ApiResponse<List<Map<String, Object>>> courses() {
        return ApiResponse.success(userCourseLearningService.myCourses());
    }

    @GetMapping("/classrooms")
    public ApiResponse<Map<String, Object>> myClassrooms() {
        return ApiResponse.success(userCenterService.myClassrooms());
    }

    @PostMapping("/classroom-invitations/{requestId}/confirm")
    public ApiResponse<Map<String, Object>> confirmClassroomInvitation(@PathVariable Long requestId,
                                                                       @RequestParam String action) {
        return ApiResponse.success(userCenterService.confirmClassroomInvitation(requestId, action));
    }

    @GetMapping("/exports")
    public ApiResponse<Map<String, Object>> exportCenter() {
        return ApiResponse.success(userCenterService.exportCenter());
    }

    @GetMapping("/exports/wrong-questions")
    public ResponseEntity<byte[]> exportWrongQuestions() {
        return ExcelUtils.buildResponse("wrong-questions.xlsx", userCenterService.exportWrongQuestions());
    }

    @GetMapping("/exports/scores")
    public ResponseEntity<byte[]> exportScores() {
        return ExcelUtils.buildResponse("learning-scores.xlsx", userCenterService.exportScores());
    }

    @GetMapping("/settings")
    public ApiResponse<Map<String, Object>> settings() {
        return ApiResponse.success(userCenterService.settings());
    }

    @PostMapping("/settings")
    public ApiResponse<Map<String, Object>> saveSettings(@RequestBody @Valid UserSettingSaveRequest request) {
        return ApiResponse.success(userCenterService.saveSettings(request));
    }

    @PostMapping("/settings/password")
    public ApiResponse<Void> changePassword(@RequestBody @Valid PasswordChangeRequest request) {
        userCenterService.changePassword(request);
        return ApiResponse.success("密码修改成功", null);
    }

    @GetMapping("/courses/{courseId}/learn")
    public ApiResponse<Map<String, Object>> courseLearning(@PathVariable Long courseId) {
        return ApiResponse.success(userCourseLearningService.courseLearning(courseId));
    }

    @PostMapping("/courses/{courseId}/claim-free")
    public ApiResponse<Map<String, Object>> claimFreeCourse(@PathVariable Long courseId) {
        return ApiResponse.success(userCourseLearningService.claimFreeCourse(courseId));
    }

    @PostMapping("/courses/{courseId}/learn-progress")
    public ApiResponse<Map<String, Object>> updateCourseLearningProgress(@PathVariable Long courseId,
                                                                         @RequestBody CourseLearningProgressRequest request) {
        return ApiResponse.success(userCourseLearningService.updateCourseLearningProgress(courseId, request));
    }

    @GetMapping("/plans/current")
    public ApiResponse<Map<String, Object>> currentPlan() {
        return ApiResponse.success(userGrowthService.currentPlan());
    }

    @GetMapping("/growth-archive")
    public ApiResponse<Map<String, Object>> growthArchive() {
        return ApiResponse.success(userGrowthService.growthArchive());
    }

    @GetMapping("/growth-diagnosis")
    public ApiResponse<Map<String, Object>> growthDiagnosis(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return ApiResponse.success(userGrowthService.growthDiagnosis(startDate, endDate));
    }

    @GetMapping("/growth-assessment")
    public ApiResponse<Map<String, Object>> growthAssessment(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return ApiResponse.success(userGrowthService.growthAssessment(startDate, endDate));
    }

    @PostMapping("/growth-assessment/start")
    public ApiResponse<Map<String, Object>> startGrowthAssessment() {
        return ApiResponse.success(userGrowthService.startAssessment());
    }

    @GetMapping("/growth-diagnosis/export")
    public ResponseEntity<byte[]> exportGrowthDiagnosis(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return ExcelUtils.buildResponse("growth-diagnosis.xlsx", userGrowthService.exportGrowthDiagnosis(startDate, endDate));
    }

    @PostMapping("/plans")
    public ApiResponse<Map<String, Object>> savePlan(@RequestBody @Valid StudyPlanSaveRequest request) {
        return ApiResponse.success(userGrowthService.savePlan(request));
    }

    @GetMapping("/tasks")
    public ApiResponse<Map<String, Object>> tasks(@RequestParam(defaultValue = "1") Long pageNo,
                                                  @RequestParam(defaultValue = "10") Long pageSize,
                                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate taskDate,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String taskType) {
        return ApiResponse.success(userGrowthService.tasks(pageNo, pageSize, taskDate, status, taskType));
    }

    @GetMapping("/knowledge-mastery")
    public ApiResponse<Map<String, Object>> knowledgeMastery(@RequestParam(defaultValue = "1") Long pageNo,
                                                             @RequestParam(defaultValue = "10") Long pageSize,
                                                             @RequestParam(required = false) String subjectName,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String masteryLevel) {
        return ApiResponse.success(userGrowthService.knowledgeMastery(pageNo, pageSize, subjectName, keyword, masteryLevel));
    }

    @GetMapping("/test-papers")
    public ApiResponse<Map<String, Object>> testPapers(@RequestParam(defaultValue = "1") Long pageNo,
                                                       @RequestParam(defaultValue = "10") Long pageSize,
                                                       @RequestParam(required = false) String subjectName,
                                                       @RequestParam(required = false) String status) {
        return ApiResponse.success(userGrowthService.testPapers(pageNo, pageSize, subjectName, status));
    }

    @GetMapping("/test-paper-reports")
    public ApiResponse<Map<String, Object>> testPaperReports(@RequestParam(defaultValue = "1") Long pageNo,
                                                             @RequestParam(defaultValue = "10") Long pageSize,
                                                             @RequestParam(required = false) String subjectName) {
        return ApiResponse.success(userGrowthService.testPaperReports(pageNo, pageSize, subjectName));
    }

    @PostMapping("/test-papers/generate")
    public ApiResponse<Map<String, Object>> generateTestPaper(@RequestBody @Valid TestPaperGenerateRequest request) {
        return ApiResponse.success(userGrowthService.generateTestPaper(request));
    }

    @PostMapping("/test-papers/{paperId}/start")
    public ApiResponse<Map<String, Object>> startTestPaper(@PathVariable Long paperId) {
        return ApiResponse.success(userGrowthService.startTestPaper(paperId));
    }
}
