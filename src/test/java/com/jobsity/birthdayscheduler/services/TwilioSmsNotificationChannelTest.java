package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.domain.Message;
import com.twilio.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TwilioSmsNotificationChannelTest {

    @Mock
    private TwilioSmsGateway smsGateway;

    private TwilioSmsNotificationChannel channel;

    @BeforeEach
    void setUp() {
        channel = new TwilioSmsNotificationChannel(smsGateway);
    }

    @Test
    void buildsMessageWithoutSubjectAndSendsSmsToFriendPhoneNumber() {
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com", " +15550000001 ");

        Message built = channel.buildMessage("+15550000001", "Ignored Subject", "Happy birthday, dear Alice!");
        assertThat(built.recipient()).isEqualTo("+15550000001");
        assertThat(built.subject()).isNull();
        assertThat(built.body()).isEqualTo("Happy birthday, dear Alice!");

        channel.send(alice, "Happy birthday!", "Happy birthday, dear Alice!");

        verify(smsGateway).sendSms("+15550000001", "Happy birthday, dear Alice!");
    }

    @Test
    void skipsSendingWhenPhoneNumberIsNullOrBlank() {
        Friend nullPhone = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com", null);
        Friend blankPhone = new Friend(2L, "Bob", "Smith", LocalDate.of(1990, 7, 22), "bob@example.com", "   ");

        channel.send(nullPhone, "Happy birthday!", "Body");
        channel.send(blankPhone, "Happy birthday!", "Body");

        verify(smsGateway, never()).sendSms(anyString(), anyString());
    }

    @Test
    void logsAndSwallowsTwilioException() {
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com", "+15550000001");
        doThrow(new ApiException("Twilio unavailable"))
                .when(smsGateway).sendSms("+15550000001", "Happy birthday, dear Alice!");

        assertThatCode(() -> channel.send(alice, "Happy birthday!", "Happy birthday, dear Alice!"))
                .doesNotThrowAnyException();

        verify(smsGateway).sendSms("+15550000001", "Happy birthday, dear Alice!");
    }

    @Test
    void throwsWhenRecipientIsNull() {
        assertThatThrownBy(() -> channel.send(null, "Subject", "Body"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("recipient must not be null");
    }
}
