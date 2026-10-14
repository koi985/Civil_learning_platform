package com.civil.platform.config;

import com.civil.platform.module.job.RecommendHotPoolRefreshJob;
import com.civil.platform.module.job.RecommendUserPoolRefreshJob;
import org.quartz.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class RecommendQuartzBootstrap {

    private final Scheduler scheduler;

    public RecommendQuartzBootstrap(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void init() throws SchedulerException {
        scheduleHotPoolJob();
        scheduleUserPoolJob();
    }

    private void scheduleHotPoolJob() throws SchedulerException {
        JobKey jobKey = JobKey.jobKey("recommendHotPoolRefreshJob");
        if (!scheduler.checkExists(jobKey)) {
            JobDetail detail = JobBuilder.newJob(RecommendHotPoolRefreshJob.class)
                    .withIdentity(jobKey)
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("recommendHotPoolRefreshTrigger")
                    .forJob(detail)
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * * * ?"))
                    .build();
            scheduler.scheduleJob(detail, trigger);
        }
    }

    private void scheduleUserPoolJob() throws SchedulerException {
        JobKey jobKey = JobKey.jobKey("recommendUserPoolRefreshJob");
        if (!scheduler.checkExists(jobKey)) {
            JobDetail detail = JobBuilder.newJob(RecommendUserPoolRefreshJob.class)
                    .withIdentity(jobKey)
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("recommendUserPoolRefreshTrigger")
                    .forJob(detail)
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0/15 * * * ?"))
                    .build();
            scheduler.scheduleJob(detail, trigger);
        }
    }
}
