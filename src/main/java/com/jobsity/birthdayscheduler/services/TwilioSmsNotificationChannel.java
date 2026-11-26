package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.domain.Message;
import com.twilio.exception.TwilioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.notifications.sms", name = "enabled", havingValue = "true")
public class TwilioSmsNotificationChannel extends BaseNotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsNotificationChannel.class);

    private final TwilioSmsGateway smsGateway;

    public TwilioSmsNotificationChannel(TwilioSmsGateway smsGateway) {
        this.smsGateway = smsGateway;
    }

    @Override
    public String channelName() {
        return "sms";
    }

    @Override
    protected String extractRecipient(Friend recipient) {
        return recipient.phoneNumber();
    }

    @Override
    protected Message buildMessage(String target, String subject, String body) {
        return new Message(target, body);
    }

    @Override
    protected void deliver(Message message) {
        try {
            smsGateway.sendSms(message.recipient(), message.body());
        } catch (TwilioException ex) {
            log.error("Failed to send SMS to {}", message.recipient(), ex);
        }
    }
}
