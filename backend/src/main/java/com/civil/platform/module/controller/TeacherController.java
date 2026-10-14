package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.common.util.ExcelUtils;
import com.civil.platform.module.dto.*;
import com.civil.platform.module.service.TeachingManageService;
import com.civil.platform.module.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherService teacherService;
    private final TeachingManageService teachingManageService;

    public TeacherController(TeacherService teacherService, TeachingManageService teachingManageService) {
        this.teacherService = teacherService;
        this.teachingManageService = teachingManageService;
    }

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "教师中心", name = "提交教师申请")
    public ApiResponse<Void> apply(@RequestBody @Valid TeacherApplyRequest request) {
        teacherService.apply(request);
        return ApiResponse.success("申请已提交", null);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(teachingManageService.dashboard());
    }

    @GetMapping("/student-recommendations")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResult<Map<String, Object>>> studentRecommendations(@RequestParam(required = false) Long classroomId,
                                                                               @RequestParam(defaultValue = "1") Long pageNo,
                                                                               @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(teachingManageService.studentRecommendations(classroomId, pageNo, pageSize));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Map<String, Object>> profile() {
        return ApiResponse.success(teacherService.profile());
    }

    @PostMapping("/profile")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "更新教师资料")
    public ApiResponse<Map<String, Object>> saveProfile(@RequestBody @Valid TeacherProfileSaveRequest request) {
        return ApiResponse.success(teacherService.saveProfile(request));
    }

    @GetMapping("/classrooms")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResult<Map<String, Object>>> classrooms(@RequestParam(defaultValue = "1") Long pageNo,
                                                                   @RequestParam(defaultValue = "10") Long pageSize,
                                                                   @RequestParam(required = false) String keyword) {
        return ApiResponse.success(teachingManageService.teacherClassrooms(pageNo, pageSize, keyword));
    }

    @GetMapping("/classrooms/{classroomId}/detail")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Map<String, Object>> classroomDetail(@PathVariable Long classroomId) {
        return ApiResponse.success(teachingManageService.teacherClassroomDetail(classroomId));
    }

    @PostMapping("/classrooms")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "保存班级")
    public ApiResponse<Void> saveClassroom(@RequestBody @Valid ClassroomSaveRequest request) {
        teachingManageService.saveClassroom(request);
        return ApiResponse.success("保存成功", null);
    }

    @PostMapping("/classrooms/{classroomId}/dismiss")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "解散班级")
    public ApiResponse<Void> dismissClassroom(@PathVariable Long classroomId) {
        teachingManageService.dismissClassroom(classroomId);
        return ApiResponse.success("班级已解散", null);
    }

    @PostMapping("/classrooms/{classroomId}/invite-students")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "邀请学生入班")
    public ApiResponse<Map<String, Object>> inviteStudents(@PathVariable Long classroomId,
                                                           @RequestBody @Valid TeacherInviteStudentsRequest request) {
        return ApiResponse.success(teachingManageService.inviteStudents(classroomId, request));
    }

    @PostMapping("/classrooms/materials")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "保存班级材料")
    public ApiResponse<Void> saveClassroomMaterial(@RequestBody @Valid TeacherClassroomMaterialSaveRequest request) {
        teachingManageService.saveClassroomMaterial(request);
        return ApiResponse.success("保存成功", null);
    }

    @DeleteMapping("/classrooms/materials/{materialId}")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "删除班级材料")
    public ApiResponse<Void> deleteClassroomMaterial(@PathVariable Long materialId) {
        teachingManageService.deleteClassroomMaterial(materialId);
        return ApiResponse.success("删除成功", null);
    }

    @DeleteMapping("/classrooms/{classroomId}/members/{userId}")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "移除班级成员")
    public ApiResponse<Void> removeMember(@PathVariable Long classroomId, @PathVariable Long userId) {
        teachingManageService.removeMember(classroomId, userId);
        return ApiResponse.success("移除成功", null);
    }

    @GetMapping("/classrooms/{classroomId}/join-requests")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResult<Map<String, Object>>> joinRequests(@PathVariable Long classroomId,
                                                                     @RequestParam(defaultValue = "1") Long pageNo,
                                                                     @RequestParam(defaultValue = "10") Long pageSize,
                                                                     @RequestParam(required = false) String status) {
        return ApiResponse.success(teachingManageService.joinRequests(classroomId, pageNo, pageSize, status));
    }

    @PostMapping("/classrooms/join-requests/review")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "审核入班申请")
    public ApiResponse<Void> reviewJoinRequest(@RequestBody @Valid ClassroomJoinReviewRequest request) {
        teachingManageService.reviewJoinRequest(request);
        return ApiResponse.success("审核完成", null);
    }

    @GetMapping("/homeworks")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResult<Map<String, Object>>> homeworks(@RequestParam(defaultValue = "1") Long pageNo,
                                                                  @RequestParam(defaultValue = "10") Long pageSize,
                                                                  @RequestParam(required = false) Long classroomId) {
        return ApiResponse.success(teachingManageService.pageHomeworks(pageNo, pageSize, classroomId));
    }

    @PostMapping("/homeworks")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "保存班级作业")
    public ApiResponse<Void> saveHomework(@RequestBody @Valid TeacherHomeworkSaveRequest request) {
        teachingManageService.saveHomework(request);
        return ApiResponse.success("保存成功", null);
    }

    @GetMapping("/classroom-exams")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResult<Map<String, Object>>> classroomExams(@RequestParam(defaultValue = "1") Long pageNo,
                                                                       @RequestParam(defaultValue = "10") Long pageSize,
                                                                       @RequestParam(required = false) Long classroomId) {
        return ApiResponse.success(teachingManageService.pageClassroomExams(pageNo, pageSize, classroomId));
    }

    @PostMapping("/classroom-exams")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "保存班级考试")
    public ApiResponse<Void> saveClassroomExam(@RequestBody @Valid TeacherClassroomExamSaveRequest request) {
        teachingManageService.saveClassroomExam(request);
        return ApiResponse.success("保存成功", null);
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResult<Map<String, Object>>> reviews(@RequestParam(defaultValue = "1") Long pageNo,
                                                                @RequestParam(defaultValue = "10") Long pageSize,
                                                                @RequestParam(required = false) String taskType,
                                                                @RequestParam(required = false) Long classroomId,
                                                                @RequestParam(required = false) String reviewStatus) {
        return ApiResponse.success(teachingManageService.pageReviews(pageNo, pageSize, taskType, classroomId, reviewStatus));
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "批改作业考试")
    public ApiResponse<Void> reviewSubmission(@RequestBody @Valid TeacherSubmissionReviewRequest request) {
        teachingManageService.reviewSubmission(request);
        return ApiResponse.success("批改成功", null);
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Map<String, Object>> reports(@RequestParam(required = false) Long classroomId,
                                                    @RequestParam(required = false) String timeRange,
                                                    @RequestParam(required = false) String subjectName,
                                                    @RequestParam(required = false) String taskType) {
        return ApiResponse.success(teachingManageService.reportOverview(classroomId, timeRange, subjectName, taskType));
    }

    @GetMapping("/reports/export")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<byte[]> exportReports(@RequestParam(required = false) Long classroomId,
                                                @RequestParam(required = false) String timeRange,
                                                @RequestParam(required = false) String subjectName,
                                                @RequestParam(required = false) String taskType) {
        return ExcelUtils.buildResponse("teacher-report.xlsx", teachingManageService.exportReportOverview(classroomId, timeRange, subjectName, taskType));
    }

    @GetMapping("/growth-dashboard")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Map<String, Object>> growthDashboard(@RequestParam(required = false) Long classroomId,
                                                            @RequestParam(required = false) String segment,
                                                            @RequestParam(required = false) String followStatus,
                                                            @RequestParam(required = false) String timeRange,
                                                            @RequestParam(required = false) String subjectName,
                                                            @RequestParam(required = false) String taskType) {
        return ApiResponse.success(teachingManageService.growthDashboard(classroomId, segment, followStatus, timeRange, subjectName, taskType));
    }

    @PostMapping("/growth-focus-follows")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "保存学员成长跟进")
    public ApiResponse<Void> saveGrowthFocusFollow(@RequestBody @Valid TeacherGrowthFocusFollowRequest request) {
        teachingManageService.saveGrowthFocusFollow(request);
        return ApiResponse.success("保存成功", null);
    }

    @PostMapping("/growth-focus-follows/batch")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "批量保存学员成长跟进")
    public ApiResponse<Void> batchSaveGrowthFocusFollow(@RequestBody @Valid TeacherGrowthBatchFollowRequest request) {
        teachingManageService.batchSaveGrowthFocusFollow(request);
        return ApiResponse.success("批量保存成功", null);
    }

    @PostMapping("/growth-dashboard/filter-preference")
    @PreAuthorize("hasRole('TEACHER')")
    @OperationLog(module = "教师中心", name = "保存成长筛选偏好")
    public ApiResponse<Void> saveGrowthDashboardPreference(@RequestBody TeacherGrowthDashboardPreferenceRequest request) {
        teachingManageService.saveGrowthDashboardPreference(request);
        return ApiResponse.success("保存成功", null);
    }

    @GetMapping("/growth-dashboard/export")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<byte[]> exportGrowthDashboard(@RequestParam(required = false) Long classroomId,
                                                        @RequestParam(required = false) String segment,
                                                        @RequestParam(required = false) String followStatus,
                                                        @RequestParam(required = false) String timeRange,
                                                        @RequestParam(required = false) String subjectName,
                                                        @RequestParam(required = false) String taskType) {
        return ExcelUtils.buildResponse("teacher-growth-dashboard.xlsx",
                teachingManageService.exportGrowthDashboard(classroomId, segment, followStatus, timeRange, subjectName, taskType));
    }
}
