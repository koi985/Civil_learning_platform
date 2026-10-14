package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.module.dto.ExamSubmitRequest;
import com.civil.platform.module.entity.Exam;
import com.civil.platform.module.service.ExamCenterService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api")
public class ExamCenterController {

    private final ExamCenterService examCenterService;

    public ExamCenterController(ExamCenterService examCenterService) {
        this.examCenterService = examCenterService;
    }

    @GetMapping("/exams")
    public ApiResponse<PageResult<Map<String, Object>>> exams(@RequestParam(defaultValue = "1") Long pageNo,
                                                              @RequestParam(defaultValue = "10") Long pageSize,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) String province,
                                                              @RequestParam(required = false) String examType,
                                                              @RequestParam(required = false) String seasonName,
                                                              @RequestParam(required = false) String subjectName,
                                                              @RequestParam(required = false) String status) {
        return ApiResponse.success(examCenterService.pageExams(pageNo, pageSize, keyword, province, examType, seasonName, subjectName, status));
    }

    @GetMapping("/exams/{examId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long examId) {
        return ApiResponse.success(examCenterService.detail(examId));
    }

    @PostMapping("/exams/{examId}/start")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "模考中心", name = "开始模考")
    public ApiResponse<Map<String, Object>> start(@PathVariable Long examId) {
        return ApiResponse.success(examCenterService.startExam(examId));
    }

    @GetMapping("/exam-records/{recordId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> session(@PathVariable Long recordId) {
        return ApiResponse.success(examCenterService.examSession(recordId));
    }

    @PostMapping("/exam-records/{recordId}/submit")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "模考中心", name = "提交模考")
    public ApiResponse<Map<String, Object>> submit(@PathVariable Long recordId,
                                                   @RequestBody @Valid ExamSubmitRequest request) {
        return ApiResponse.success(examCenterService.submitExam(recordId, request));
    }

    @GetMapping("/exam-records/{recordId}/report")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> report(@PathVariable Long recordId) {
        return ApiResponse.success(examCenterService.examReport(recordId));
    }

    @GetMapping("/exams/{examId}/ranking")
    public ApiResponse<Map<String, Object>> ranking(@PathVariable Long examId,
                                                    @RequestParam(defaultValue = "GLOBAL") String scope) {
        return ApiResponse.success(examCenterService.ranking(examId, scope));
    }
}
