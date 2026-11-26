package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationChannelTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmailNotificationChannel(mailSender, "sender@example.com");
    }

    @Test
    void sendsSimpleMailMessageToFriendEmail() {
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), " alice@example.com ", "+15550000001");

        channel.send(alice, "Happy birthday!", "Happy birthday, dear Alice!");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("sender@example.com");
        assertThat(sent.getTo()).containsExactly("alice@example.com");
        assertThat(sent.getSubject()).isEqualTo("Happy birthday!");
        assertThat(sent.getText()).isEqualTo("Happy birthday, dear Alice!");
    }

    @Test
    void skipsSendingWhenEmailIsNullOrBlank() {
        Friend nullEmail = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), null, "+15550000001");
        Friend blankEmail = new Friend(2L, "Bob", "Smith", LocalDate.of(1990, 7, 22), "   ", "+15550000002");

        channel.send(nullEmail, "Happy birthday!", "Body");
        channel.send(blankEmail, "Happy birthday!", "Body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void logsAndSwallowsMailException() {
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com", "+15550000001");
        doThrow(new MailSendException("SMTP down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> channel.send(alice, "Happy birthday!", "Happy birthday, dear Alice!"))
                .doesNotThrowAnyException();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void throwsWhenRecipientIsNull() {
        assertThatThrownBy(() -> channel.send(null, "Subject", "Body"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("recipient must not be null");
    }
}
