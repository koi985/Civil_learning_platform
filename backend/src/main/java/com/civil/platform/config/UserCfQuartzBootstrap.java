package com.civil.platform.config;

import com.civil.platform.module.job.UserCfFullRebuildJob;
import com.civil.platform.module.job.UserCfIncrementalJob;
import org.quartz.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class UserCfQuartzBootstrap {

    private final Scheduler scheduler;

    public UserCfQuartzBootstrap(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void init() throws SchedulerException {
        scheduleFullRebuildJob();
        scheduleIncrementalJob();
    }

    private void scheduleFullRebuildJob() throws SchedulerException {
        JobKey jobKey = JobKey.jobKey("userCfFullRebuildJob");
        if (!scheduler.checkExists(jobKey)) {
            JobDetail detail = JobBuilder.newJob(UserCfFullRebuildJob.class)
                    .withIdentity(jobKey)
                    .build();
            // 每日 02:30
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("userCfFullRebuildTrigger")
                    .forJob(detail)
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 30 2 * * ?"))
                    .build();
            scheduler.scheduleJob(detail, trigger);
        }
    }

    private void scheduleIncrementalJob() throws SchedulerException {
        JobKey jobKey = JobKey.jobKey("userCfIncrementalJob");
        if (!scheduler.checkExists(jobKey)) {
            JobDetail detail = JobBuilder.newJob(UserCfIncrementalJob.class)
                    .withIdentity(jobKey)
                    .build();
            // 每 30 分钟
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("userCfIncrementalTrigger")
                    .forJob(detail)
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0/30 * * * ?"))
                    .build();
            scheduler.scheduleJob(detail, trigger);
        }
    }
}
