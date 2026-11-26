package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.config.TwilioProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.notifications.sms", name = "enabled", havingValue = "true")
public class TwilioSdkSmsGateway implements TwilioSmsGateway {

    private final String fromNumber;

    public TwilioSdkSmsGateway(TwilioProperties properties) {
        if (isBlank(properties.accountSid()) || isBlank(properties.authToken()) || isBlank(properties.fromNumber())) {
            throw new IllegalStateException(
                    "Twilio SMS is enabled, but app.twilio.account-sid, auth-token, and from-number must all be configured");
        }
        this.fromNumber = properties.fromNumber().trim();
        Twilio.init(properties.accountSid().trim(), properties.authToken().trim());
    }

    @Override
    public void sendSms(String toPhoneNumber, String body) {
        Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(fromNumber),
                body
        ).create();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
