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
    private final BirthdayJobRunRepository jobRunRepository;
    private final Clock clock;

    public BirthdaySchedulerService(BirthdayNotificationService notificationService,
                                    BirthdayJobRunRepository jobRunRepository,
                                    Clock clock) {
        this.notificationService = notificationService;
        this.jobRunRepository = jobRunRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.birthday-scheduler.schedule.cron:0 0 0 * * *}")
    public void processScheduledBirthdays() {
        processBirthdays();
    }

    /**
     * Runs the birthday notification job for today's date if it has not already run today.
     *
     * @return {@code true} if the job executed for today, {@code false} if today was already processed
     */
    public synchronized boolean processBirthdays() {
        LocalDate targetDate = LocalDate.now(clock);
        if (jobRunRepository.hasRunOn(targetDate)) {
            log.info("Birthday job already executed for {}; skipping.", targetDate);
            return false;
        }

        notificationService.sendBirthdayEmails(targetDate);
        jobRunRepository.markAsRun(targetDate, Instant.now(clock));
        log.info("Birthday job completed for {}.", targetDate);
        return true;
    }
}
