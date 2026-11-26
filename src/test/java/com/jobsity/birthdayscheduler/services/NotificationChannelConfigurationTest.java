package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.config.TwilioProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NotificationChannelConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FreeMarkerAutoConfiguration.class))
            .withUserConfiguration(
                    TestConfig.class,
                    MessageTemplateService.class,
                    EmailNotificationChannel.class,
                    TwilioSmsNotificationChannel.class,
                    TwilioSdkSmsGateway.class,
                    NotificationDispatcher.class
            )
            .withPropertyValues("app.mail.from=sender@example.com");

    @Test
    void enablesOnlyEmailChannelByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(EmailNotificationChannel.class);
            assertThat(context).doesNotHaveBean(TwilioSmsNotificationChannel.class);
            assertThat(context).doesNotHaveBean(TwilioSdkSmsGateway.class);
            assertThat(context).hasSingleBean(NotificationDispatcher.class);
        });
    }

    @Test
    void enablesOnlySmsChannelWhenEmailDisabledAndSmsEnabled() {
        contextRunner
                .withPropertyValues(
                        "app.notifications.email.enabled=false",
                        "app.notifications.sms.enabled=true",
                        "app.twilio.account-sid=AC123",
                        "app.twilio.auth-token=secret",
                        "app.twilio.from-number=+15551234567"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EmailNotificationChannel.class);
                    assertThat(context).hasSingleBean(TwilioSmsNotificationChannel.class);
                    assertThat(context).hasSingleBean(TwilioSdkSmsGateway.class);
                });
    }

    @Test
    void enablesBothEmailAndSmsChannelsWhenBothEnabled() {
        contextRunner
                .withPropertyValues(
                        "app.notifications.email.enabled=true",
                        "app.notifications.sms.enabled=true",
                        "app.twilio.account-sid=AC123",
                        "app.twilio.auth-token=secret",
                        "app.twilio.from-number=+15551234567"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailNotificationChannel.class);
                    assertThat(context).hasSingleBean(TwilioSmsNotificationChannel.class);
                });
    }

    @Test
    void disablesBothChannelsWhenBothFlagsAreFalse() {
        contextRunner
                .withPropertyValues(
                        "app.notifications.email.enabled=false",
                        "app.notifications.sms.enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EmailNotificationChannel.class);
                    assertThat(context).doesNotHaveBean(TwilioSmsNotificationChannel.class);
                    assertThat(context).hasSingleBean(NotificationDispatcher.class);
                });
    }

    @Test
    void failsStartupWhenSmsIsEnabledWithoutTwilioCredentials() {
        contextRunner
                .withPropertyValues(
                        "app.notifications.sms.enabled=true"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(TwilioProperties.class)
    static class TestConfig {
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }
}
