package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.module.dto.ClassroomChatSendRequest;
import com.civil.platform.module.entity.Classroom;
import com.civil.platform.module.service.ClassroomChatService;
import com.civil.platform.module.service.ClassroomService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

    private final ClassroomService classroomService;
    private final ClassroomChatService classroomChatService;

    public ClassroomController(ClassroomService classroomService,
                               ClassroomChatService classroomChatService) {
        this.classroomService = classroomService;
        this.classroomChatService = classroomChatService;
    }

    @GetMapping
    public ApiResponse<PageResult<Classroom>> page(@RequestParam(defaultValue = "1") Long pageNo,
                                                   @RequestParam(defaultValue = "8") Long pageSize,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String subjectName) {
        return ApiResponse.success(classroomService.page(pageNo, pageSize, keyword, subjectName));
    }

    @GetMapping("/{classroomId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long classroomId) {
        return ApiResponse.success(classroomService.detail(classroomId));
    }

    @PostMapping("/{classroomId}/apply")
    @PreAuthorize("hasRole('USER') or hasRole('TEACHER')")
    @OperationLog(module = "班级中心", name = "申请加入班级")
    public ApiResponse<Void> apply(@PathVariable Long classroomId,
                                   @RequestParam(required = false) String remark) {
        classroomService.applyJoin(classroomId, remark);
        return ApiResponse.success("申请已提交", null);
    }

    @GetMapping("/{classroomId}/chat/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<Map<String, Object>>> chatMessages(@PathVariable Long classroomId,
                                                                     @RequestParam(defaultValue = "1") Long pageNo,
                                                                     @RequestParam(defaultValue = "30") Long pageSize) {
        return ApiResponse.success(classroomChatService.pageMessages(classroomId, pageNo, pageSize));
    }

    @PostMapping("/{classroomId}/chat/messages")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "班级中心", name = "发送班群消息")
    public ApiResponse<Map<String, Object>> sendChatMessage(@PathVariable Long classroomId,
                                                            @RequestBody @Valid ClassroomChatSendRequest request) {
        return ApiResponse.success(classroomChatService.sendMessage(classroomId, request));
    }
}
