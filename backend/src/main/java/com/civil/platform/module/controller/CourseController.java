package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.module.dto.CourseCommentSaveRequest;
import com.civil.platform.module.dto.CourseListItemResponse;
import com.civil.platform.module.service.CourseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ApiResponse<PageResult<CourseListItemResponse>> page(@RequestParam(defaultValue = "1") Long pageNo,
                                                                @RequestParam(defaultValue = "8") Long pageSize,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String province,
                                                                @RequestParam(required = false) String subjectName,
                                                                @RequestParam(required = false) String examType,
                                                                @RequestParam(required = false) String courseType,
                                                                @RequestParam(required = false) java.math.BigDecimal minPrice,
                                                                @RequestParam(required = false) java.math.BigDecimal maxPrice,
                                                                @RequestParam(required = false) String sortBy,
                                                                @RequestHeader(value = "X-Exam-Region", required = false) String examRegion) {
        return ApiResponse.success(courseService.pageCourses(pageNo, pageSize, keyword, province, subjectName, examType, courseType, minPrice, maxPrice, sortBy, decodeExamRegion(examRegion)));
    }

    @GetMapping("/{courseId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long courseId) {
        return ApiResponse.success(courseService.detail(courseId));
    }

    @PostMapping("/{courseId}/comments")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "课程中心", name = "提交课程评价")
    public ApiResponse<Map<String, Object>> submitComment(@PathVariable Long courseId,
                                                          @RequestBody @Valid CourseCommentSaveRequest request) {
        return ApiResponse.success(courseService.submitComment(courseId, request));
    }

    private String decodeExamRegion(String examRegion) {
        if (examRegion == null || examRegion.trim().isEmpty()) {
            return null;
        }
        try {
            return URLDecoder.decode(examRegion, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return examRegion;
        }
    }
}
