package com.civil.platform.module.job;

import com.civil.platform.common.util.SpringContextHolder;
import com.civil.platform.module.service.recommend.UserCfRebuildService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class UserCfIncrementalJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SpringContextHolder.getBean(UserCfRebuildService.class).incrementalRefresh();
    }
}
