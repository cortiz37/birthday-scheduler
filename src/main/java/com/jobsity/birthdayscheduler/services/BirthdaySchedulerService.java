package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.repositories.BirthdayJobRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Component
public class BirthdaySchedulerService {

    private static final Logger log = LoggerFactory.getLogger(BirthdaySchedulerService.class);

    private final BirthdayNotificationService notificationService;
    private final BirthdayReminderService reminderService;
    private final BirthdayJobRunRepository jobRunRepository;
    private final Clock clock;

    public BirthdaySchedulerService(BirthdayNotificationService notificationService,
                                    BirthdayReminderService reminderService,
                                    BirthdayJobRunRepository jobRunRepository,
                                    Clock clock) {
        this.notificationService = notificationService;
        this.reminderService = reminderService;
        this.jobRunRepository = jobRunRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.birthday-scheduler.schedule.cron:0 0 0 * * *}")
    public void processScheduledBirthdays() {
        processBirthdays();
    }

    public synchronized boolean processBirthdays() {
        LocalDate targetDate = LocalDate.now(clock);
        if (jobRunRepository.hasRunOn(targetDate)) {
            log.info("Birthday job already executed for {}; skipping.", targetDate);
            return false;
        }

        notificationService.sendBirthdayNotifications(targetDate);
        reminderService.sendBirthdayReminders(targetDate);
        jobRunRepository.markAsRun(targetDate, Instant.now(clock));
        log.info("Birthday job completed for {}.", targetDate);
        return true;
    }
}
