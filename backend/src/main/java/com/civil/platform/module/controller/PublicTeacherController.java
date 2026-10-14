package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.module.service.TeacherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
public class PublicTeacherController {

    private final TeacherService teacherService;

    public PublicTeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/{teacherProfileId}/public-detail")
    public ApiResponse<Map<String, Object>> publicDetail(@PathVariable Long teacherProfileId) {
        return ApiResponse.success(teacherService.publicDetail(teacherProfileId));
    }
}
