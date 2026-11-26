package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.repositories.BirthdayJobRunRepository;
import org.junit.jupiter.api.Test;
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
        // BirthdaySchedulerCliRunner already ran on startup for 2026-03-15
        assertThat(jobRunRepository.hasRunOn(LocalDate.of(2026, 3, 15))).isTrue();
        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));

        // Scheduled trigger later on the same day must skip sending again
        schedulerService.processScheduledBirthdays();

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
