package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.repositories.FriendRepository;
import freemarker.template.Configuration;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BirthdayNotificationServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TwilioSmsGateway smsGateway;

    private MessageTemplateService messageTemplateService;
    private BirthdayNotificationService service;

    @BeforeEach
    void setUp() {
        Configuration freemarkerConfiguration = new Configuration(Configuration.VERSION_2_3_34);
        freemarkerConfiguration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/templates");
        freemarkerConfiguration.setDefaultEncoding("UTF-8");

        messageTemplateService = new MessageTemplateService(freemarkerConfiguration);
        service = new BirthdayNotificationService(
                friendRepository,
                messageTemplateService,
                List.of(new EmailNotificationChannel(mailSender, "sender@example.com")),
                "Happy birthday!",
                "birthday-email.ftl"
        );
    }

    @Test
    void sendsRenderedBirthdayEmailToCelebratingFriendsWithoutHtmlEscaping() {
        LocalDate targetDate = LocalDate.of(2026, 3, 15);
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com");
        Friend bob = new Friend(2L, "Bob", "Smith", LocalDate.of(1990, 7, 22), "bob@example.com");
        Friend oConnor = new Friend(3L, "D'Artagnan", "O'Connor", LocalDate.of(1995, 3, 15), "dartagnan@example.com");
        when(friendRepository.findAll()).thenReturn(List.of(alice, bob, oConnor));

        service.sendBirthdayNotifications(targetDate);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());

        List<SimpleMailMessage> sentMails = captor.getAllValues();
        assertThat(sentMails.get(0).getFrom()).isEqualTo("sender@example.com");
        assertThat(sentMails.get(0).getTo()).containsExactly("alice@example.com");
        assertThat(sentMails.get(0).getSubject()).isEqualTo("Happy birthday!");
        assertThat(sentMails.get(0).getText()).isEqualTo("Happy birthday, dear Alice!");

        assertThat(sentMails.get(1).getTo()).containsExactly("dartagnan@example.com");
        assertThat(sentMails.get(1).getText()).isEqualTo("Happy birthday, dear D'Artagnan!");
    }

    @Test
    void sendsBothEmailAndSmsWhenBothChannelsEnabledAndSkipsMissingPhoneNumbers() {
        BirthdayNotificationService multiChannelService = new BirthdayNotificationService(
                friendRepository,
                messageTemplateService,
                List.of(
                        new EmailNotificationChannel(mailSender, "sender@example.com"),
                        new TwilioSmsNotificationChannel(smsGateway)
                ),
                "Happy birthday!",
                "birthday-email.ftl"
        );
        LocalDate targetDate = LocalDate.of(2026, 3, 15);
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com", "+15550000001");
        Friend noPhone = new Friend(2L, "Clara", "Martinez", LocalDate.of(1995, 3, 15), "clara@example.com", "  ");
        when(friendRepository.findAll()).thenReturn(List.of(alice, noPhone));

        multiChannelService.sendBirthdayNotifications(targetDate);

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        verify(smsGateway, times(1)).sendSms("+15550000001", "Happy birthday, dear Alice!");
    }

    @Test
    void throwsWhenTargetDateIsNull() {
        assertThatThrownBy(() -> service.sendBirthdayNotifications(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("targetDate must not be null");
    }

    @Test
    void throwsWhenTemplateDoesNotExist() {
        BirthdayNotificationService badTemplateService = new BirthdayNotificationService(
                friendRepository,
                messageTemplateService,
                List.of(new EmailNotificationChannel(mailSender, "sender@example.com")),
                "Happy birthday!",
                "non-existent.ftl"
        );
        LocalDate targetDate = LocalDate.of(2026, 3, 15);
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com");
        when(friendRepository.findAll()).thenReturn(List.of(alice));

        assertThatThrownBy(() -> badTemplateService.sendBirthdayNotifications(targetDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to render message template 'non-existent.ftl'");
    }

    @Test
    void continuesSendingRemainingEmailsWhenOneSendFails() {
        LocalDate targetDate = LocalDate.of(2026, 3, 15);
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com");
        Friend clara = new Friend(2L, "Clara", "Martinez", LocalDate.of(1995, 3, 15), "clara@example.com");
        when(friendRepository.findAll()).thenReturn(List.of(alice, clara));

        doThrow(new MailSendException("SMTP error"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        service.sendBirthdayNotifications(targetDate);

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendsNoNotificationsWhenNoFriendsCelebrateOnTargetDate() {
        LocalDate targetDate = LocalDate.of(2026, 1, 1);
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com");
        when(friendRepository.findAll()).thenReturn(List.of(alice));

        service.sendBirthdayNotifications(targetDate);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendsNoNotificationsWhenRepositoryIsEmpty() {
        when(friendRepository.findAll()).thenReturn(List.of());

        service.sendBirthdayNotifications(LocalDate.of(2026, 3, 15));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
