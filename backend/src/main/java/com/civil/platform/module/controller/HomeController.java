package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.module.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@RequestHeader(value = "X-Exam-Region", required = false) String examRegion) {
        return ApiResponse.success(homeService.home(decodeExamRegion(examRegion)));
    }

    @GetMapping("/public-overview")
    public ApiResponse<Map<String, Object>> publicOverview(@RequestHeader(value = "X-Exam-Region", required = false) String examRegion) {
        return ApiResponse.success(homeService.publicOverview(decodeExamRegion(examRegion)));
    }

    @GetMapping("/personalized")
    public ApiResponse<Map<String, Object>> personalized(@RequestHeader(value = "X-Exam-Region", required = false) String examRegion) {
        return ApiResponse.success(homeService.personalizedOverview(decodeExamRegion(examRegion)));
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
