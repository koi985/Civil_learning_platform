package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.module.service.NoticeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNo,
                                                             @RequestParam(defaultValue = "10") Long pageSize,
                                                             @RequestParam(required = false) String keyword) {
        return ApiResponse.success(noticeService.pagePublicNotices(pageNo, pageSize, keyword));
    }

    @GetMapping("/{noticeId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long noticeId) {
        return ApiResponse.success(noticeService.detail(noticeId));
    }
}
