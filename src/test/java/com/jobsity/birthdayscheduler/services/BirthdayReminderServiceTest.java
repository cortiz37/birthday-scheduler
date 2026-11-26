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
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BirthdayReminderServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private JavaMailSender mailSender;

    private Configuration freemarkerConfiguration;
    private BirthdayReminderService service;

    @BeforeEach
    void setUp() {
        freemarkerConfiguration = new Configuration(Configuration.VERSION_2_3_34);
        freemarkerConfiguration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/templates");
        freemarkerConfiguration.setDefaultEncoding("UTF-8");

        service = new BirthdayReminderService(
                friendRepository,
                mailSender,
                freemarkerConfiguration,
                "sender@example.com",
                "Birthday Reminder",
                "birthday-reminder-email.ftl"
        );
    }

    @Test
    void sendsSixTotalEmailsInMultiBirthdayScenarioAndFourRemindersExcludingSelf() {
        LocalDate oct8 = LocalDate.of(2026, 10, 8);
        Friend john = new Friend(1L, "John", "Doe", LocalDate.of(1990, 10, 8), "john.doe@example.com");
        Friend jane = new Friend(2L, "Jane", "Smith", LocalDate.of(1992, 10, 8), "jane.smith@example.com");
        Friend mary = new Friend(3L, "Mary", "Ann", LocalDate.of(1995, 9, 11), "mary.ann@example.com");
        when(friendRepository.findAll()).thenReturn(List.of(john, jane, mary));

        BirthdayNotificationService notificationService = new BirthdayNotificationService(
                friendRepository,
                mailSender,
                freemarkerConfiguration,
                "sender@example.com",
                "Happy birthday!",
                "birthday-email.ftl"
        );

        notificationService.sendBirthdayEmails(oct8);
        service.sendBirthdayReminders(oct8);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(6)).send(captor.capture());

        assertThat(captor.getAllValues()).extracting(
                msg -> msg.getTo()[0],
                SimpleMailMessage::getSubject,
                SimpleMailMessage::getText
        ).containsExactlyInAnyOrder(
                tuple("john.doe@example.com", "Happy birthday!", "Happy birthday, dear John!"),
                tuple("john.doe@example.com", "Birthday Reminder", "Dear John,\nToday is Jane Smith's birthday.\nDon't forget to send him a message !"),
                tuple("jane.smith@example.com", "Happy birthday!", "Happy birthday, dear Jane!"),
                tuple("jane.smith@example.com", "Birthday Reminder", "Dear Jane,\nToday is John Doe's birthday.\nDon't forget to send him a message !"),
                tuple("mary.ann@example.com", "Birthday Reminder", "Dear Mary,\nToday is John Doe's birthday.\nDon't forget to send him a message !"),
                tuple("mary.ann@example.com", "Birthday Reminder", "Dear Mary,\nToday is Jane Smith's birthday.\nDon't forget to send him a message !")
        );
    }

    @Test
    void sendsNoRemindersWhenRepositoryIsEmpty() {
        when(friendRepository.findAll()).thenReturn(List.of());

        service.sendBirthdayReminders(LocalDate.of(2026, 10, 8));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendsNoRemindersWhenNoOneIsCelebrating() {
        LocalDate targetDate = LocalDate.of(2026, 1, 1);
        Friend john = new Friend(1L, "John", "Doe", LocalDate.of(1990, 10, 8), "john.doe@example.com");
        when(friendRepository.findAll()).thenReturn(List.of(john));

        service.sendBirthdayReminders(targetDate);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendsNoRemindersWhenOnlyOneFriendExistsAndIsCelebrating() {
        LocalDate targetDate = LocalDate.of(2026, 10, 8);
        Friend john = new Friend(1L, "John", "Doe", LocalDate.of(1990, 10, 8), "john.doe@example.com");
        when(friendRepository.findAll()).thenReturn(List.of(john));

        service.sendBirthdayReminders(targetDate);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void excludesSelfAndRendersApostrophesWithoutHtmlEscapingWhenFriendIdsAreNull() {
        LocalDate oct8 = LocalDate.of(2026, 10, 8);
        Friend dArtagnan = new Friend(null, "D'Artagnan", "O'Connor", LocalDate.of(1990, 10, 8), "dartagnan@example.com");
        Friend rene = new Friend(null, "Rene", "L'Amour", LocalDate.of(1992, 5, 1), "rene@example.com");
        when(friendRepository.findAll()).thenReturn(List.of(dArtagnan, rene));

        service.sendBirthdayReminders(oct8);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("sender@example.com");
        assertThat(sent.getTo()).containsExactly("rene@example.com");
        assertThat(sent.getSubject()).isEqualTo("Birthday Reminder");
        assertThat(sent.getText()).isEqualTo("Dear Rene,\nToday is D'Artagnan O'Connor's birthday.\nDon't forget to send him a message !");
    }

    @Test
    void sendsRemindersOnlyToOtherCelebratingFriendWhenEveryoneHasBirthday() {
        LocalDate oct8 = LocalDate.of(2026, 10, 8);
        Friend john = new Friend(1L, "John", "Doe", LocalDate.of(1990, 10, 8), "john.doe@example.com");
        Friend jane = new Friend(2L, "Jane", "Smith", LocalDate.of(1992, 10, 8), "jane.smith@example.com");
        when(friendRepository.findAll()).thenReturn(List.of(john, jane));

        service.sendBirthdayReminders(oct8);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());

        assertThat(captor.getAllValues()).extracting(
                msg -> msg.getTo()[0],
                SimpleMailMessage::getText
        ).containsExactlyInAnyOrder(
                tuple("john.doe@example.com", "Dear John,\nToday is Jane Smith's birthday.\nDon't forget to send him a message !"),
                tuple("jane.smith@example.com", "Dear Jane,\nToday is John Doe's birthday.\nDon't forget to send him a message !")
        );
    }

    @Test
    void throwsWhenTargetDateIsNull() {
        assertThatThrownBy(() -> service.sendBirthdayReminders(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("targetDate must not be null");
    }

    @Test
    void throwsWhenTemplateDoesNotExist() {
        BirthdayReminderService badTemplateService = new BirthdayReminderService(
                friendRepository,
                mailSender,
                freemarkerConfiguration,
                "sender@example.com",
                "Birthday Reminder",
                "non-existent.ftl"
        );
        LocalDate targetDate = LocalDate.of(2026, 10, 8);
        Friend john = new Friend(1L, "John", "Doe", LocalDate.of(1990, 10, 8), "john.doe@example.com");
        Friend mary = new Friend(2L, "Mary", "Ann", LocalDate.of(1995, 9, 11), "mary.ann@example.com");
        when(friendRepository.findAll()).thenReturn(List.of(john, mary));

        assertThatThrownBy(() -> badTemplateService.sendBirthdayReminders(targetDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to render email template 'non-existent.ftl'");
    }

    @Test
    void continuesSendingRemainingRemindersWhenOneSendFails() {
        LocalDate targetDate = LocalDate.of(2026, 10, 8);
        Friend john = new Friend(1L, "John", "Doe", LocalDate.of(1990, 10, 8), "john.doe@example.com");
        Friend jane = new Friend(2L, "Jane", "Smith", LocalDate.of(1992, 9, 11), "jane.smith@example.com");
        Friend mary = new Friend(3L, "Mary", "Ann", LocalDate.of(1995, 9, 11), "mary.ann@example.com");
        when(friendRepository.findAll()).thenReturn(List.of(john, jane, mary));

        doThrow(new MailSendException("SMTP error"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        service.sendBirthdayReminders(targetDate);

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }
}
