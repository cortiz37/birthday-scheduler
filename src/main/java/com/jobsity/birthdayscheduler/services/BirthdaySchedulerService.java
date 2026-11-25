package com.jobsity.birthdayscheduler.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BirthdaySchedulerService {

    @Scheduled(cron = "${app.birthday-scheduler.schedule.cron:-}")
    public void processBirthdays() {
        // TODO: Add birthday scheduling logic
    }
}
