package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(BaseNotificationChannel.class);

    @Override
    public final void send(Friend recipient, String subject, String body) {
        if (recipient == null) {
            throw new IllegalArgumentException("recipient must not be null");
        }
        String target = extractRecipient(recipient);
        if (target == null || target.isBlank()) {
            log.warn("Skipping {} notification for {} {}: missing contact info",
                    channelName(), recipient.firstName(), recipient.lastName());
            return;
        }
        deliver(buildMessage(target.trim(), subject, body));
    }

    protected abstract String extractRecipient(Friend recipient);

    protected Message buildMessage(String target, String subject, String body) {
        return new Message(target, subject, body);
    }

    protected abstract void deliver(Message message);
}
