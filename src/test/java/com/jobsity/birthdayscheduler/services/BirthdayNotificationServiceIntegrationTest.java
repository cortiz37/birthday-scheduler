package com.jobsity.birthdayscheduler.services;

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
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class BirthdayNotificationServiceIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private BirthdayNotificationService notificationService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    void rendersFreeMarkerTemplateEndToEnd() {
        notificationService.sendBirthdayNotifications(LocalDate.of(2026, 3, 15));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("myaccount@server.com");
        assertThat(sent.getTo()).containsExactly("alice.johnson@example.com");
        assertThat(sent.getSubject()).isEqualTo("Happy birthday!");
        assertThat(sent.getText()).isEqualTo("Happy birthday, dear Alice!");
    }
}
