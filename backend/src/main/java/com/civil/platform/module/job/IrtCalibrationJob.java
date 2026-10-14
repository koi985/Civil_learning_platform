package com.civil.platform.module.job;

import com.civil.platform.common.util.SpringContextHolder;
import com.civil.platform.module.service.IrtCalibrationService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class IrtCalibrationJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SpringContextHolder.getBean(IrtCalibrationService.class).refreshAll();
    }
}
