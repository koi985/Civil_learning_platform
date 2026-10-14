package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.module.dto.ExamRegionSaveRequest;
import com.civil.platform.module.dto.RecommendFeedbackRequest;
import com.civil.platform.module.service.QuestionService;
import com.civil.platform.module.service.RecommendEngineService;
import com.civil.platform.module.service.recommend.UserCfRebuildService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendEngineService recommendEngineService;
    private final QuestionService questionService;
    private final UserCfRebuildService userCfRebuildService;

    public RecommendController(RecommendEngineService recommendEngineService,
                               QuestionService questionService,
                               UserCfRebuildService userCfRebuildService) {
        this.recommendEngineService = recommendEngineService;
        this.questionService = questionService;
        this.userCfRebuildService = userCfRebuildService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@RequestHeader(value = "X-Exam-Region", required = false) String examRegion) {
        return ApiResponse.success(recommendEngineService.overview(decodeExamRegion(examRegion)));
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(@RequestHeader(value = "X-Exam-Region", required = false) String examRegion) {
        return ApiResponse.success(recommendEngineService.profile(decodeExamRegion(examRegion)));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(@RequestHeader(value = "X-Exam-Region", required = false) String examRegion) {
        return ApiResponse.success(recommendEngineService.refresh(decodeExamRegion(examRegion)));
    }

    @PostMapping("/exam-region")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "推荐系统", name = "保存报考地区")
    public ApiResponse<Map<String, Object>> saveExamRegion(@RequestBody @Valid ExamRegionSaveRequest request) {
        return ApiResponse.success("报考地区已保存", recommendEngineService.saveExamRegion(request));
    }

    @PostMapping("/feedback")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> feedback(@RequestBody @Valid RecommendFeedbackRequest request) {
        recommendEngineService.feedback(request);
        return ApiResponse.success("反馈成功", null);
    }

    @PostMapping("/practice-bundles/{bundleId}/start")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "推荐系统", name = "开始推荐练习包")
    public ApiResponse<Map<String, Object>> startPracticeBundle(@PathVariable Long bundleId) {
        return ApiResponse.success(questionService.startRecommendPracticeBundle(bundleId));
    }

    @GetMapping("/admin-feedback-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> adminFeedbackSummary() {
        return ApiResponse.success(recommendEngineService.adminFeedbackSummary());
    }

    @GetMapping("/admin-feedbacks")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResult<Map<String, Object>>> adminFeedbacks(@RequestParam(defaultValue = "1") Long pageNo,
                                                                       @RequestParam(defaultValue = "10") Long pageSize,
                                                                       @RequestParam(required = false) String resourceType) {
        return ApiResponse.success(recommendEngineService.adminFeedbackPage(pageNo, pageSize, resourceType));
    }

    @PostMapping("/admin-refresh-hot-pool")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "管理后台", name = "刷新热门推荐池")
    public ApiResponse<Void> adminRefreshHotPool() {
        recommendEngineService.refreshHotPool();
        return ApiResponse.success("热门池刷新成功", null);
    }

    @PostMapping("/admin-refresh-user-pool")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "管理后台", name = "刷新用户推荐池")
    public ApiResponse<Void> adminRefreshUserPool() {
        recommendEngineService.refreshActiveUserRecommendPool();
        return ApiResponse.success("用户池刷新成功", null);
    }

    @GetMapping("/admin-cf-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, String>> adminCfStatus() {
        return ApiResponse.success(userCfRebuildService.readStatus());
    }

    @PostMapping("/admin-cf-rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "管理后台", name = "UserCF 全量重建")
    public ApiResponse<Map<String, Object>> adminCfRebuild() {
        return ApiResponse.success("重建任务已完成", userCfRebuildService.fullRebuild());
    }

    private String decodeExamRegion(String examRegion) {
        if (examRegion == null) {
            return null;
        }
        try {
            return URLDecoder.decode(examRegion, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException ignored) {
            return examRegion;
        }
    }
}
