package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.module.dto.TaskSubmitRequest;
import com.civil.platform.module.service.ClassroomTaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/classroom-tasks")
@PreAuthorize("isAuthenticated()")
public class ClassroomTaskController {

    private final ClassroomTaskService classroomTaskService;

    public ClassroomTaskController(ClassroomTaskService classroomTaskService) {
        this.classroomTaskService = classroomTaskService;
    }

    @PostMapping("/homeworks/{homeworkId}/start")
    @OperationLog(module = "班级中心", name = "开始班级作业")
    public ApiResponse<Map<String, Object>> startHomework(@PathVariable Long homeworkId) {
        return ApiResponse.success(classroomTaskService.startHomework(homeworkId));
    }

    @GetMapping("/homework-submissions/{submissionId}")
    public ApiResponse<Map<String, Object>> homeworkSession(@PathVariable Long submissionId) {
        return ApiResponse.success(classroomTaskService.homeworkSession(submissionId));
    }

    @PostMapping("/homework-submissions/{submissionId}/submit")
    @OperationLog(module = "班级中心", name = "提交班级作业")
    public ApiResponse<Map<String, Object>> submitHomework(@PathVariable Long submissionId,
                                                           @RequestBody @Valid TaskSubmitRequest request) {
        return ApiResponse.success(classroomTaskService.submitHomework(submissionId, request));
    }

    @GetMapping("/homework-submissions/{submissionId}/report")
    public ApiResponse<Map<String, Object>> homeworkReport(@PathVariable Long submissionId) {
        return ApiResponse.success(classroomTaskService.homeworkReport(submissionId));
    }

    @PostMapping("/classroom-exams/{classroomExamId}/start")
    @OperationLog(module = "班级中心", name = "开始班级考试")
    public ApiResponse<Map<String, Object>> startClassroomExam(@PathVariable Long classroomExamId) {
        return ApiResponse.success(classroomTaskService.startClassroomExam(classroomExamId));
    }

    @GetMapping("/classroom-exam-submissions/{submissionId}")
    public ApiResponse<Map<String, Object>> classroomExamSession(@PathVariable Long submissionId) {
        return ApiResponse.success(classroomTaskService.classroomExamSession(submissionId));
    }

    @PostMapping("/classroom-exam-submissions/{submissionId}/submit")
    @OperationLog(module = "班级中心", name = "提交班级考试")
    public ApiResponse<Map<String, Object>> submitClassroomExam(@PathVariable Long submissionId,
                                                                @RequestBody @Valid TaskSubmitRequest request) {
        return ApiResponse.success(classroomTaskService.submitClassroomExam(submissionId, request));
    }

    @GetMapping("/classroom-exam-submissions/{submissionId}/report")
    public ApiResponse<Map<String, Object>> classroomExamReport(@PathVariable Long submissionId) {
        return ApiResponse.success(classroomTaskService.classroomExamReport(submissionId));
    }
}
