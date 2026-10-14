package com.civil.platform.module.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class RecommendRefreshAsyncService {

    private final RecommendEngineService recommendEngineService;
    private final IrtCalibrationService irtCalibrationService;

    public RecommendRefreshAsyncService(RecommendEngineService recommendEngineService,
                                        IrtCalibrationService irtCalibrationService) {
        this.recommendEngineService = recommendEngineService;
        this.irtCalibrationService = irtCalibrationService;
    }

    @Async("recommendRefreshExecutor")
    public void refreshUserAsync(Long userId, String source) {
        irtCalibrationService.refreshUser(userId);
        recommendEngineService.rebuildUserCache(userId, source, true);
    }
}
