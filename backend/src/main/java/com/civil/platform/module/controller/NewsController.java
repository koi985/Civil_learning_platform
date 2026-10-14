package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.module.dto.NewsCommentSaveRequest;
import com.civil.platform.module.service.NewsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNo,
                                                             @RequestParam(defaultValue = "8") Long pageSize,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) Long categoryId,
                                                             @RequestParam(required = false) String infoCategory,
                                                             @RequestParam(required = false) String province) {
        return ApiResponse.success(newsService.pageNews(pageNo, pageSize, keyword, categoryId, infoCategory, province));
    }

    @GetMapping("/categories")
    public ApiResponse<java.util.List<Map<String, Object>>> categories() {
        return ApiResponse.success(newsService.categories());
    }

    @GetMapping("/filter-options")
    public ApiResponse<Map<String, Object>> filterOptions() {
        return ApiResponse.success(newsService.filterOptions());
    }

    @PostMapping("/categories/{categoryId}/subscribe")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> subscribeCategory(@PathVariable Long categoryId) {
        return ApiResponse.success("订阅成功", newsService.subscribeCategory(categoryId));
    }

    @DeleteMapping("/categories/{categoryId}/subscribe")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> unsubscribeCategory(@PathVariable Long categoryId) {
        return ApiResponse.success("已取消订阅", newsService.unsubscribeCategory(categoryId));
    }

    @GetMapping("/{newsId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long newsId) {
        return ApiResponse.success(newsService.detail(newsId));
    }

    @GetMapping("/hot")
    public ApiResponse<List<Map<String, Object>>> hotList() {
        return ApiResponse.success(newsService.hotNews());
    }

    @PostMapping("/{newsId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> saveComment(@PathVariable Long newsId, @RequestBody @Valid NewsCommentSaveRequest request) {
        newsService.saveComment(newsId, request);
        return ApiResponse.success("评论成功", null);
    }

    @PostMapping("/{newsId}/share")
    public ApiResponse<Void> share(@PathVariable Long newsId) {
        newsService.share(newsId);
        return ApiResponse.success("分享成功", null);
    }
}
