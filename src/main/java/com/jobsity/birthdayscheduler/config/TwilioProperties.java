package com.jobsity.birthdayscheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.twilio")
public record TwilioProperties(String accountSid,
                               String authToken,
                               String fromNumber) {
}
