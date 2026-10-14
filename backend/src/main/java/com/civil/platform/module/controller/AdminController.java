package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.common.util.ExcelUtils;
import com.civil.platform.module.dto.*;
import com.civil.platform.module.entity.Course;
import com.civil.platform.module.entity.Banner;
import com.civil.platform.module.entity.Exam;
import com.civil.platform.module.entity.News;
import com.civil.platform.module.entity.Notice;
import com.civil.platform.module.entity.QuestionPaper;
import com.civil.platform.module.entity.RecommendConfig;
import com.civil.platform.module.service.AdminService;
import com.civil.platform.module.service.BroadcastService;
import com.civil.platform.module.service.GovernanceService;
import com.civil.platform.module.service.OrderRefundService;
import com.civil.platform.module.service.TeacherService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final TeacherService teacherService;
    private final GovernanceService governanceService;
    private final BroadcastService broadcastService;
    private final OrderRefundService orderRefundService;

    public AdminController(AdminService adminService,
                           TeacherService teacherService,
                           GovernanceService governanceService,
                           BroadcastService broadcastService,
                           OrderRefundService orderRefundService) {
        this.adminService = adminService;
        this.teacherService = teacherService;
        this.governanceService = governanceService;
        this.broadcastService = broadcastService;
        this.orderRefundService = orderRefundService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(adminService.dashboard());
    }

    @GetMapping("/growth-dashboard")
    public ApiResponse<Map<String, Object>> growthDashboard(@RequestParam(required = false) String segment,
                                                            @RequestParam(required = false) java.math.BigDecimal minCompletionRate,
                                                            @RequestParam(required = false) String followStatus) {
        return ApiResponse.success(adminService.growthDashboard(segment, minCompletionRate, followStatus));
    }

    @GetMapping("/growth-dashboard/export")
    public ResponseEntity<byte[]> exportGrowthDashboard(@RequestParam(required = false) String segment,
                                                        @RequestParam(required = false) java.math.BigDecimal minCompletionRate,
                                                        @RequestParam(required = false) String followStatus) {
        return ExcelUtils.buildResponse("admin-growth-dashboard.xlsx",
                adminService.exportGrowthDashboard(segment, minCompletionRate, followStatus));
    }

    @PostMapping("/growth-focus-follows")
    @OperationLog(module = "管理后台", name = "保存成长跟进")
    public ApiResponse<Void> saveGrowthFocusFollow(@RequestBody @Valid AdminGrowthFocusFollowRequest request) {
        adminService.saveGrowthFocusFollow(request);
        return ApiResponse.success("保存成功", null);
    }

    @PostMapping("/growth-focus-follows/batch")
    @OperationLog(module = "管理后台", name = "批量保存成长跟进")
    public ApiResponse<Void> batchSaveGrowthFocusFollow(@RequestBody @Valid AdminGrowthBatchFollowRequest request) {
        adminService.batchSaveGrowthFocusFollow(request);
        return ApiResponse.success("批量保存成功", null);
    }

    @PostMapping("/growth-dashboard/filter-preference")
    @OperationLog(module = "管理后台", name = "保存成长筛选偏好")
    public ApiResponse<Void> saveGrowthDashboardPreference(@RequestBody AdminGrowthDashboardPreferenceRequest request) {
        adminService.saveGrowthDashboardPreference(request);
        return ApiResponse.success("保存成功", null);
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<Map<String, Object>>> users(@RequestParam(defaultValue = "1") Long pageNo,
                                                              @RequestParam(defaultValue = "10") Long pageSize,
                                                              @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.pageUsers(pageNo, pageSize, keyword));
    }

    @PutMapping("/users")
    @OperationLog(module = "管理后台", name = "更新用户")
    public ApiResponse<Void> updateUser(@RequestBody @Valid UserUpdateRequest request) {
        adminService.updateUser(request);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/users/{id}")
    @OperationLog(module = "管理后台", name = "删除用户")
    public ApiResponse<Void> removeUser(@PathVariable Long id) {
        adminService.removeUser(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/roles")
    public ApiResponse<List<Map<String, Object>>> roles() {
        return ApiResponse.success(governanceService.listRoles());
    }

    @PostMapping("/roles")
    @OperationLog(module = "管理后台", name = "保存角色")
    public ApiResponse<Void> saveRole(@RequestBody @Valid AdminRoleSaveRequest request) {
        governanceService.saveRole(request);
        return ApiResponse.success("保存成功", null);
    }

    @DeleteMapping("/roles/{id}")
    @OperationLog(module = "管理后台", name = "删除角色")
    public ApiResponse<Void> removeRole(@PathVariable Long id) {
        governanceService.removeRole(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/teacher-audits")
    public ApiResponse<PageResult<Map<String, Object>>> teacherAudits(@RequestParam(defaultValue = "1") Long pageNo,
                                                                      @RequestParam(defaultValue = "10") Long pageSize,
                                                                      @RequestParam(required = false) String auditStatus) {
        return ApiResponse.success(teacherService.auditPage(pageNo, pageSize, auditStatus));
    }

    @GetMapping("/teachers")
    public ApiResponse<PageResult<Map<String, Object>>> teachers(@RequestParam(defaultValue = "1") Long pageNo,
                                                                 @RequestParam(defaultValue = "10") Long pageSize,
                                                                 @RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String auditStatus) {
        return ApiResponse.success(adminService.pageTeachers(pageNo, pageSize, keyword, auditStatus));
    }

    @PutMapping("/teachers")
    @OperationLog(module = "管理后台", name = "更新教师资料")
    public ApiResponse<Void> updateTeacher(@RequestBody @Valid AdminTeacherUpdateRequest request) {
        adminService.updateTeacher(request);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/teachers/{id}")
    @OperationLog(module = "管理后台", name = "删除教师")
    public ApiResponse<Void> removeTeacher(@PathVariable Long id) {
        adminService.removeTeacher(id);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/teacher-audits")
    @OperationLog(module = "管理后台", name = "审核教师")
    public ApiResponse<Void> teacherAudit(@RequestBody @Valid TeacherAuditRequest request) {
        teacherService.audit(request);
        return ApiResponse.success("审核完成", null);
    }

    @GetMapping("/courses")
    public ApiResponse<PageResult<Course>> courses(@RequestParam(defaultValue = "1") Long pageNo,
                                                   @RequestParam(defaultValue = "10") Long pageSize,
                                                   @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.pageCourses(pageNo, pageSize, keyword));
    }

    @GetMapping("/courses/{id}")
    public ApiResponse<Map<String, Object>> courseDetail(@PathVariable Long id) {
        return ApiResponse.success(adminService.courseDetail(id));
    }

    @PostMapping("/courses")
    @OperationLog(module = "管理后台", name = "保存课程")
    public ApiResponse<Void> saveCourse(@RequestBody @Valid CourseSaveRequest request) {
        adminService.saveCourse(request);
        return ApiResponse.success("保存成功", null);
    }

    @PostMapping("/courses/sync-learn-count")
    @OperationLog(module = "管理后台", name = "同步课程学习人数")
    public ApiResponse<Void> syncCourseLearnCount() {
        adminService.syncCourseLearnCounts();
        return ApiResponse.success("学习人数已同步", null);
    }

    @DeleteMapping("/courses/{id}")
    @OperationLog(module = "管理后台", name = "删除课程")
    public ApiResponse<Void> removeCourse(@PathVariable Long id) {
        adminService.removeCourse(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/questions")
    public ApiResponse<PageResult<Map<String, Object>>> questions(@RequestParam(defaultValue = "1") Long pageNo,
                                                                  @RequestParam(defaultValue = "10") Long pageSize,
                                                                  @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.pageQuestions(pageNo, pageSize, keyword));
    }

    @GetMapping("/questions/random")
    public ApiResponse<List<Map<String, Object>>> randomQuestions(@RequestParam(defaultValue = "120") Integer count,
                                                                  @RequestParam(required = false) String province,
                                                                  @RequestParam(required = false) String subjectName,
                                                                  @RequestParam(required = false) String questionType,
                                                                  @RequestParam(required = false) String difficultyLevel) {
        return ApiResponse.success(adminService.randomQuestions(count, province, subjectName, questionType, difficultyLevel));
    }

    @PostMapping("/questions")
    @OperationLog(module = "管理后台", name = "保存题目")
    public ApiResponse<Void> saveQuestion(@RequestBody @Valid QuestionSaveRequest request) {
        adminService.saveQuestion(request);
        return ApiResponse.success("保存成功", null);
    }

    @DeleteMapping("/questions/{id}")
    @OperationLog(module = "管理后台", name = "删除题目")
    public ApiResponse<Void> removeQuestion(@PathVariable Long id) {
        adminService.removeQuestion(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/question-papers")
    public ApiResponse<PageResult<QuestionPaper>> questionPapers(@RequestParam(defaultValue = "1") Long pageNo,
                                                                 @RequestParam(defaultValue = "10") Long pageSize,
                                                                 @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.pageQuestionPapers(pageNo, pageSize, keyword));
    }

    @PostMapping("/question-papers")
    @OperationLog(module = "管理后台", name = "保存试卷")
    public ApiResponse<Void> saveQuestionPaper(@RequestBody @Valid QuestionPaperSaveRequest request) {
        adminService.saveQuestionPaper(request);
        return ApiResponse.success("保存成功", null);
    }

    @DeleteMapping("/question-papers/{id}")
    @OperationLog(module = "管理后台", name = "删除试卷")
    public ApiResponse<Void> removeQuestionPaper(@PathVariable Long id) {
        adminService.removeQuestionPaper(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/shenlun-groups")
    public ApiResponse<PageResult<Map<String, Object>>> shenlunGroups(@RequestParam(defaultValue = "1") Long pageNo,
                                                                      @RequestParam(defaultValue = "10") Long pageSize,
                                                                      @RequestParam(required = false) String keyword,
                                                                      @RequestParam(required = false) Integer year,
                                                                      @RequestParam(required = false) String region,
                                                                      @RequestParam(required = false) String sourceName,
                                                                      @RequestParam(required = false) String status) {
        return ApiResponse.success(adminService.pageAdminShenlunGroups(pageNo, pageSize, keyword, year, region, sourceName, status));
    }

    @GetMapping("/shenlun-groups/{id}")
    public ApiResponse<Map<String, Object>> shenlunGroupDetail(@PathVariable Long id) {
        return ApiResponse.success(adminService.adminShenlunGroupDetail(id));
    }

    @PostMapping("/shenlun-groups")
    @OperationLog(module = "管理后台", name = "保存申论材料组")
    public ApiResponse<Void> saveShenlunGroup(@RequestBody @Valid AdminShenlunGroupSaveRequest request) {
        adminService.saveShenlunGroup(request);
        return ApiResponse.success("保存成功", null);
    }

    @PostMapping("/shenlun-groups/{id}/publish")
    @OperationLog(module = "管理后台", name = "发布申论材料组")
    public ApiResponse<Void> publishShenlunGroup(@PathVariable Long id) {
        adminService.publishShenlunGroup(id);
        return ApiResponse.success("发布成功", null);
    }

    @DeleteMapping("/shenlun-groups/{id}")
    @OperationLog(module = "管理后台", name = "归档申论材料组")
    public ApiResponse<Void> removeShenlunGroup(@PathVariable Long id) {
        adminService.removeShenlunGroup(id);
        return ApiResponse.success("归档成功", null);
    }

    @PostMapping("/shenlun-groups/{id}/restore")
    @OperationLog(module = "管理后台", name = "恢复申论材料组")
    public ApiResponse<Void> restoreShenlunGroup(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "DRAFT") String status) {
        adminService.restoreShenlunGroup(id, status);
        return ApiResponse.success("恢复成功", null);
    }

    @GetMapping("/exams")
    public ApiResponse<PageResult<Exam>> exams(@RequestParam(defaultValue = "1") Long pageNo,
                                               @RequestParam(defaultValue = "10") Long pageSize,
                                               @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.pageExams(pageNo, pageSize, keyword));
    }

    @PostMapping("/exams")
    @OperationLog(module = "管理后台", name = "保存模考")
    public ApiResponse<Void> saveExam(@RequestBody @Valid ExamSaveRequest request) {
        adminService.saveExam(request);
        return ApiResponse.success("保存成功", null);
    }

    @DeleteMapping("/exams/{id}")
    @OperationLog(module = "管理后台", name = "删除模考")
    public ApiResponse<Void> removeExam(@PathVariable Long id) {
        adminService.removeExam(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/news")
    public ApiResponse<PageResult<News>> news(@RequestParam(defaultValue = "1") Long pageNo,
                                              @RequestParam(defaultValue = "10") Long pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) String infoCategory,
                                              @RequestParam(required = false) String province) {
        return ApiResponse.success(adminService.pageNews(pageNo, pageSize, keyword, categoryId, infoCategory, province));
    }

    @PostMapping("/news")
    @OperationLog(module = "管理后台", name = "保存资讯")
    public ApiResponse<Void> saveNews(@RequestBody @Valid NewsSaveRequest request) {
        adminService.saveNews(request);
        return ApiResponse.success("保存成功", null);
    }

    @DeleteMapping("/news/{id}")
    @OperationLog(module = "管理后台", name = "删除资讯")
    public ApiResponse<Void> removeNews(@PathVariable Long id) {
        adminService.removeNews(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/notices")
    public ApiResponse<PageResult<Notice>> notices(@RequestParam(defaultValue = "1") Long pageNo,
                                                   @RequestParam(defaultValue = "10") Long pageSize,
                                                   @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.pageNotices(pageNo, pageSize, keyword));
    }

    @PostMapping("/notices")
    @OperationLog(module = "管理后台", name = "保存公告")
    public ApiResponse<Void> saveNotice(@RequestBody @Valid NoticeSaveRequest request) {
        adminService.saveNotice(request);
        return ApiResponse.success("保存成功", null);
    }

    @DeleteMapping("/notices/{id}")
    @OperationLog(module = "管理后台", name = "删除公告")
    public ApiResponse<Void> removeNotice(@PathVariable Long id) {
        adminService.removeNotice(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/evaluations")
    public ApiResponse<PageResult<Map<String, Object>>> evaluations(@RequestParam(defaultValue = "1") Long pageNo,
                                                                     @RequestParam(defaultValue = "10") Long pageSize,
                                                                     @RequestParam(required = false) String source,
                                                                     @RequestParam(required = false) String keyword) {
        return ApiResponse.success(governanceService.pageEvaluations(pageNo, pageSize, source, keyword));
    }

    @DeleteMapping("/evaluations/{source}/{id}")
    @OperationLog(module = "管理后台", name = "删除评价")
    public ApiResponse<Void> removeEvaluation(@PathVariable String source, @PathVariable Long id) {
        governanceService.deleteEvaluation(source, id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/recommend-configs")
    public ApiResponse<List<RecommendConfig>> recommendConfigs() {
        return ApiResponse.success(adminService.listRecommendConfigs());
    }

    @GetMapping("/banners")
    public ApiResponse<List<Banner>> banners() {
        return ApiResponse.success(adminService.listBanners());
    }

    @PostMapping("/banners")
    @OperationLog(module = "管理后台", name = "保存首页Banner")
    public ApiResponse<Void> saveBanner(@RequestBody @Valid BannerSaveRequest request) {
        adminService.saveBanner(request);
        return ApiResponse.success("保存成功", null);
    }

    @DeleteMapping("/banners/{id}")
    @OperationLog(module = "管理后台", name = "删除首页Banner")
    public ApiResponse<Void> removeBanner(@PathVariable Long id) {
        adminService.removeBanner(id);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/recommend-configs")
    @OperationLog(module = "管理后台", name = "保存推荐配置")
    public ApiResponse<Void> saveRecommendConfig(@RequestBody @Valid RecommendConfigSaveRequest request) {
        adminService.saveRecommendConfig(request);
        return ApiResponse.success("保存成功", null);
    }

    @GetMapping("/recommend-pinned-resources")
    public ApiResponse<List<Map<String, Object>>> recommendPinnedResources() {
        return ApiResponse.success(adminService.listRecommendPinnedResources());
    }

    @PostMapping("/recommend-pinned-resources")
    @OperationLog(module = "管理后台", name = "保存推荐置顶资源")
    public ApiResponse<Void> saveRecommendPinnedResource(@RequestBody @Valid RecommendPinnedResourceSaveRequest request) {
        adminService.saveRecommendPinnedResource(request);
        return ApiResponse.success("保存成功", null);
    }

    @DeleteMapping("/recommend-pinned-resources/{id}")
    @OperationLog(module = "管理后台", name = "删除推荐置顶资源")
    public ApiResponse<Void> removeRecommendPinnedResource(@PathVariable Long id) {
        adminService.removeRecommendPinnedResource(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/refunds")
    public ApiResponse<PageResult<Map<String, Object>>> refunds(@RequestParam(defaultValue = "1") Long pageNo,
                                                                @RequestParam(defaultValue = "10") Long pageSize,
                                                                @RequestParam(required = false) String refundStatus,
                                                                @RequestParam(required = false) String keyword) {
        return ApiResponse.success(orderRefundService.pageRefunds(pageNo, pageSize, refundStatus, keyword));
    }

    @GetMapping("/refunds/{refundId}")
    public ApiResponse<Map<String, Object>> refundDetail(@PathVariable Long refundId) {
        return ApiResponse.success(orderRefundService.refundDetail(refundId));
    }

    @PostMapping("/refunds/{refundId}/approve")
    @OperationLog(module = "管理后台", name = "审核通过退款")
    public ApiResponse<Void> approveRefund(@PathVariable Long refundId, @RequestBody(required = false) AdminRefundReviewRequest request) {
        orderRefundService.approveRefund(refundId, request == null ? new AdminRefundReviewRequest() : request);
        return ApiResponse.success("退款已通过", null);
    }

    @PostMapping("/refunds/{refundId}/reject")
    @OperationLog(module = "管理后台", name = "驳回退款")
    public ApiResponse<Void> rejectRefund(@PathVariable Long refundId, @RequestBody(required = false) AdminRefundReviewRequest request) {
        orderRefundService.rejectRefund(refundId, request == null ? new AdminRefundReviewRequest() : request);
        return ApiResponse.success("退款已驳回", null);
    }

    @GetMapping("/broadcasts")
    public ApiResponse<Map<String, Object>> broadcasts(@RequestParam(defaultValue = "1") Long pageNo,
                                                       @RequestParam(defaultValue = "10") Long pageSize,
                                                       @RequestParam(required = false) String keyword) {
        return ApiResponse.success(broadcastService.pageBroadcasts(pageNo, pageSize, keyword));
    }

    @GetMapping("/broadcasts/summary")
    public ApiResponse<Map<String, Object>> broadcastSummary() {
        return ApiResponse.success(broadcastService.summary());
    }

    @PostMapping("/broadcasts")
    @OperationLog(module = "管理后台", name = "发送全局广播")
    public ApiResponse<Map<String, Object>> sendBroadcast(@RequestBody @Valid AdminBroadcastSendRequest request) {
        return ApiResponse.success(broadcastService.sendBroadcast(request));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<Map<String, Object>>> orders(@RequestParam(defaultValue = "1") Long pageNo,
                                                               @RequestParam(defaultValue = "10") Long pageSize,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) String orderStatus,
                                                               @RequestParam(required = false) String paymentStatus) {
        return ApiResponse.success(adminService.pageOrders(pageNo, pageSize, keyword, orderStatus, paymentStatus));
    }

    @PostMapping("/orders/status")
    @OperationLog(module = "管理后台", name = "更新订单状态")
    public ApiResponse<Void> updateOrder(@RequestBody @Valid AdminOrderUpdateRequest request) {
        adminService.updateOrder(request);
        return ApiResponse.success("更新成功", null);
    }

    @GetMapping("/payment-logs")
    public ApiResponse<PageResult<com.civil.platform.module.entity.PaymentLog>> paymentLogs(@RequestParam(defaultValue = "1") Long pageNo,
                                                                                             @RequestParam(defaultValue = "10") Long pageSize,
                                                                                             @RequestParam(required = false) String paymentChannel,
                                                                                             @RequestParam(required = false) String paymentStatus) {
        return ApiResponse.success(adminService.pagePaymentLogs(pageNo, pageSize, paymentChannel, paymentStatus));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(governanceService.statistics());
    }

    @GetMapping("/stats/export")
    public ResponseEntity<byte[]> exportStats() {
        return ExcelUtils.buildResponse("platform-stats.xlsx", governanceService.exportStatistics());
    }

    @GetMapping("/system-configs")
    public ApiResponse<Map<String, Object>> systemConfigs() {
        return ApiResponse.success(adminService.systemConfigs());
    }

    @PostMapping("/system-configs")
    @OperationLog(module = "管理后台", name = "保存系统设置")
    public ApiResponse<Void> saveSystemConfigs(@RequestBody @Valid SystemConfigSaveRequest request) {
        adminService.saveSystemConfigs(request);
        return ApiResponse.success("保存成功", null);
    }

    @GetMapping("/operation-logs")
    public ApiResponse<PageResult<Map<String, Object>>> operationLogs(@RequestParam(defaultValue = "1") Long pageNo,
                                                                      @RequestParam(defaultValue = "10") Long pageSize,
                                                                      @RequestParam(required = false) String moduleName,
                                                                      @RequestParam(required = false) String success,
                                                                      @RequestParam(required = false) String keyword,
                                                                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                                                      @RequestParam(required = false) Boolean sensitiveOnly) {
        return ApiResponse.success(adminService.pageOperationLogs(pageNo, pageSize, moduleName, success, keyword, startTime, endTime, sensitiveOnly));
    }

    @GetMapping("/operation-logs/summary")
    public ApiResponse<Map<String, Object>> operationLogSummary(@RequestParam(required = false) String moduleName,
                                                                @RequestParam(required = false) String success,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                                                @RequestParam(required = false) Boolean sensitiveOnly) {
        return ApiResponse.success(adminService.operationLogSummary(moduleName, success, keyword, startTime, endTime, sensitiveOnly));
    }

    @GetMapping("/operation-logs/export")
    public ResponseEntity<byte[]> exportOperationLogs(@RequestParam(required = false) String moduleName,
                                                      @RequestParam(required = false) String success,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                                      @RequestParam(required = false) Boolean sensitiveOnly) {
        return ExcelUtils.buildResponse("operation-logs.xlsx",
                adminService.exportOperationLogs(moduleName, success, keyword, startTime, endTime, sensitiveOnly));
    }
}
