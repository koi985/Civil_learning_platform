package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.module.service.BroadcastService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/broadcasts")
public class BroadcastController {

    private final BroadcastService broadcastService;

    public BroadcastController(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @GetMapping("/recent")
    public ApiResponse<List<Map<String, Object>>> recent() {
        return ApiResponse.success(broadcastService.recentBroadcasts(10));
    }
}
