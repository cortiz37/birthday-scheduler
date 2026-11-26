package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.repositories.BirthdayJobRunRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class BirthdaySchedulerServiceIntegrationTest {

    @TestConfiguration
    static class FixedBirthdayClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            // Alice's birthday in data.sql is 1992-03-15
            return Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private BirthdaySchedulerService schedulerService;

    @Autowired
    private BirthdayJobRunRepository jobRunRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    void startupRunnerMarksDayProcessedSoSubsequentScheduledTriggerDoesNotResend() {
        // BirthdaySchedulerCliRunner already ran on startup for 2026-03-15 (1 notification + 3 reminders)
        assertThat(jobRunRepository.hasRunOn(LocalDate.of(2026, 3, 15))).isTrue();
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(4)).send(captor.capture());

        assertThat(captor.getAllValues()).extracting(
                msg -> msg.getTo()[0],
                SimpleMailMessage::getSubject
        ).containsExactlyInAnyOrder(
                tuple("alice.johnson@example.com", "Happy birthday!"),
                tuple("emily.pier@example.com", "Birthday Reminder"),
                tuple("bob.smith@example.com", "Birthday Reminder"),
                tuple("clara.martinez@example.com", "Birthday Reminder")
        );

        // Scheduled trigger later on the same day must skip sending again
        schedulerService.processScheduledBirthdays();

        verify(javaMailSender, times(4)).send(any(SimpleMailMessage.class));
    }
}
