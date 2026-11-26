package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;
import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationChannel smsChannel;

    private MessageTemplateService templateService;
    private Friend alice;

    @BeforeEach
    void setUp() {
        Configuration freemarkerConfiguration = new Configuration(Configuration.VERSION_2_3_34);
        freemarkerConfiguration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/templates");
        freemarkerConfiguration.setDefaultEncoding("UTF-8");
        templateService = new MessageTemplateService(freemarkerConfiguration);
        alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com", "+15550000001");
    }

    @Test
    void dispatchesRenderedMessageToAllEnabledChannels() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(templateService, List.of(emailChannel, smsChannel));

        dispatcher.sendTemplatedNotification(alice, "Happy birthday!", "birthday-email.ftl", Map.of("firstName", "Alice"));

        verify(emailChannel).send(alice, "Happy birthday!", "Happy birthday, dear Alice!");
        verify(smsChannel).send(alice, "Happy birthday!", "Happy birthday, dear Alice!");
    }

    @Test
    void succeedsGracefullyWhenNoChannelsAreEnabled() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(templateService, List.of());

        assertThatCode(() -> dispatcher.sendTemplatedNotification(
                alice,
                "Happy birthday!",
                "birthday-email.ftl",
                Map.of("firstName", "Alice")
        )).doesNotThrowAnyException();
    }

    @Test
    void failsFastWhenTemplateRenderingFailsWithoutInvokingChannels() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(templateService, List.of(emailChannel, smsChannel));

        assertThatThrownBy(() -> dispatcher.sendTemplatedNotification(
                alice,
                "Happy birthday!",
                "missing.ftl",
                Map.of("firstName", "Alice")
        )).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(emailChannel, smsChannel);
    }
}
