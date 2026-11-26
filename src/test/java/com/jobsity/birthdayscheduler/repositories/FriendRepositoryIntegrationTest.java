package com.jobsity.birthdayscheduler.repositories;

import com.jobsity.birthdayscheduler.services.BirthdayNotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class FriendRepositoryIntegrationTest {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private BirthdayNotificationService notificationService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    //@Test
    void sendsBirthdayEmailForFeb29FriendOnFeb28EndToEnd() {
        notificationService.sendBirthdayEmails(LocalDate.of(2026, 2, 28));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("alice.johnson@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Happy birthday!");
        assertThat(captor.getValue().getText()).isEqualTo("Happy birthday, dear Alice!");
    }
}
