package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.module.dto.EssayAiReviewRequest;
import com.civil.platform.module.service.EssayAiReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EssayAiReviewController {

    private final EssayAiReviewService essayAiReviewService;

    public EssayAiReviewController(EssayAiReviewService essayAiReviewService) {
        this.essayAiReviewService = essayAiReviewService;
    }

    @PostMapping("/practices/{practiceId}/questions/{questionId}/essay-ai-review")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "题库中心", name = "申论AI批改")
    public ApiResponse<Map<String, Object>> review(@PathVariable Long practiceId,
                                                   @PathVariable Long questionId,
                                                   @RequestBody @Valid EssayAiReviewRequest request) {
        return ApiResponse.success(essayAiReviewService.review(practiceId, questionId, request));
    }
}
