package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.repositories.BirthdayJobRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BirthdaySchedulerServiceTest {

    @Mock
    private BirthdayNotificationService notificationService;

    @Mock
    private BirthdayReminderService reminderService;

    private Clock mutableClock;
    private Instant currentInstant;
    private Set<LocalDate> recordedDates;
    private BirthdayJobRunRepository jobRunRepository;
    private BirthdaySchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        currentInstant = Instant.parse("2026-11-25T10:00:00Z");
        recordedDates = new HashSet<>();
        mutableClock = new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return currentInstant;
            }
        };

        jobRunRepository = new BirthdayJobRunRepository() {
            @Override
            public boolean hasRunOn(LocalDate date) {
                return recordedDates.contains(date);
            }

            @Override
            public boolean markAsRun(LocalDate date, Instant executedAt) {
                return recordedDates.add(date);
            }
        };

        schedulerService = new BirthdaySchedulerService(notificationService, reminderService, jobRunRepository, mutableClock);
    }

    @Test
    void runsOncePerDayAcrossStartupAndScheduledTriggerThenRunsAgainNextDay() {
        // 1st entry point: startup on 2026-11-25
        boolean firstRun = schedulerService.processBirthdays();
        assertThat(firstRun).isTrue();
        verify(notificationService, times(1)).sendBirthdayNotifications(LocalDate.of(2026, 11, 25));
        verify(reminderService, times(1)).sendBirthdayReminders(LocalDate.of(2026, 11, 25));

        // 2nd entry point: scheduler triggers later the same day
        currentInstant = Instant.parse("2026-11-25T23:59:00Z");
        schedulerService.processScheduledBirthdays();
        verify(notificationService, times(1)).sendBirthdayNotifications(LocalDate.of(2026, 11, 25));
        verify(reminderService, times(1)).sendBirthdayReminders(LocalDate.of(2026, 11, 25));

        // Next day: scheduler triggers on 2026-11-26
        currentInstant = Instant.parse("2026-11-26T00:00:00Z");
        schedulerService.processScheduledBirthdays();
        verify(notificationService, times(1)).sendBirthdayNotifications(LocalDate.of(2026, 11, 26));
        verify(reminderService, times(1)).sendBirthdayReminders(LocalDate.of(2026, 11, 26));
    }

    @Test
    void doesNotMarkDayAsRunWhenNotificationServiceFails() {
        BirthdayJobRunRepository mockRepo = org.mockito.Mockito.mock(BirthdayJobRunRepository.class);
        when(mockRepo.hasRunOn(LocalDate.of(2026, 11, 25))).thenReturn(false);
        doThrow(new IllegalStateException("template failure"))
                .when(notificationService).sendBirthdayNotifications(LocalDate.of(2026, 11, 25));

        BirthdaySchedulerService service = new BirthdaySchedulerService(notificationService, reminderService, mockRepo, mutableClock);

        assertThatThrownBy(service::processBirthdays)
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(reminderService);
        verify(mockRepo, never()).markAsRun(any(), any());
    }

    @Test
    void doesNotMarkDayAsRunWhenReminderServiceFails() {
        BirthdayJobRunRepository mockRepo = org.mockito.Mockito.mock(BirthdayJobRunRepository.class);
        when(mockRepo.hasRunOn(LocalDate.of(2026, 11, 25))).thenReturn(false);
        doThrow(new IllegalStateException("reminder template failure"))
                .when(reminderService).sendBirthdayReminders(LocalDate.of(2026, 11, 25));

        BirthdaySchedulerService service = new BirthdaySchedulerService(notificationService, reminderService, mockRepo, mutableClock);

        assertThatThrownBy(service::processBirthdays)
                .isInstanceOf(IllegalStateException.class);
        verify(mockRepo, never()).markAsRun(any(), any());
    }

    @Test
    void skipsNotificationsAndRemindersAndReturnsFalseWhenDayAlreadyProcessed() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        recordedDates.add(today);

        boolean executed = schedulerService.processBirthdays();

        assertThat(executed).isFalse();
        verifyNoInteractions(notificationService, reminderService);
    }

    @Test
    void sendsNotificationsBeforeRemindersAndMarksRunWithCurrentTimestamp() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        BirthdayJobRunRepository mockRepo = org.mockito.Mockito.mock(BirthdayJobRunRepository.class);
        when(mockRepo.hasRunOn(today)).thenReturn(false);

        BirthdaySchedulerService service = new BirthdaySchedulerService(notificationService, reminderService, mockRepo, mutableClock);

        boolean executed = service.processBirthdays();

        assertThat(executed).isTrue();
        InOrder order = inOrder(mockRepo, notificationService, reminderService);
        order.verify(mockRepo).hasRunOn(today);
        order.verify(notificationService).sendBirthdayNotifications(today);
        order.verify(reminderService).sendBirthdayReminders(today);
        order.verify(mockRepo).markAsRun(today, currentInstant);
    }
}
