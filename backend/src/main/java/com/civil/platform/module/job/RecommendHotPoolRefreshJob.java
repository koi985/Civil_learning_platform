package com.civil.platform.module.job;

import com.civil.platform.common.util.SpringContextHolder;
import com.civil.platform.module.service.RecommendEngineService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class RecommendHotPoolRefreshJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SpringContextHolder.getBean(RecommendEngineService.class).refreshHotPool();
    }
}
