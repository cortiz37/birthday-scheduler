package com.jobsity.birthdayscheduler.services;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageTemplateServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MessageTemplateService service;

    @BeforeEach
    void setUp() {
        Configuration freemarkerConfiguration = new Configuration(Configuration.VERSION_2_3_34);
        freemarkerConfiguration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/templates");
        freemarkerConfiguration.setDefaultEncoding("UTF-8");

        service = new MessageTemplateService(mailSender, freemarkerConfiguration, "sender@example.com");
    }

    @Test
    void rendersTemplateAndSendsPlainTextEmailWithoutHtmlEscaping() {
        service.sendTemplatedEmail(
                "dartagnan@example.com",
                "Happy birthday!",
                "birthday-email.ftl",
                Map.of("firstName", "D'Artagnan")
        );

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("sender@example.com");
        assertThat(sent.getTo()).containsExactly("dartagnan@example.com");
        assertThat(sent.getSubject()).isEqualTo("Happy birthday!");
        assertThat(sent.getText()).isEqualTo("Happy birthday, dear D'Artagnan!");
    }

    @Test
    void throwsIllegalStateExceptionWhenTemplateIsMissing() {
        assertThatThrownBy(() -> service.sendTemplatedEmail(
                "alice@example.com",
                "Subject",
                "missing-template.ftl",
                Map.of("firstName", "Alice")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to render email template 'missing-template.ftl'");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void throwsIllegalStateExceptionWhenRequiredModelVariableIsMissing() {
        assertThatThrownBy(() -> service.sendTemplatedEmail(
                "alice@example.com",
                "Happy birthday!",
                "birthday-email.ftl",
                Map.of()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to render email template 'birthday-email.ftl'");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void logsAndSwallowsMailExceptionWhenSendingFails() {
        doThrow(new MailSendException("SMTP down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> service.sendTemplatedEmail(
                "alice@example.com",
                "Happy birthday!",
                "birthday-email.ftl",
                Map.of("firstName", "Alice")
        )).doesNotThrowAnyException();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
