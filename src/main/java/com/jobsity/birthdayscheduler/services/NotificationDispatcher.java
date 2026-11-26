package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final MessageTemplateService messageTemplateService;
    private final List<NotificationChannel> channels;

    public NotificationDispatcher(MessageTemplateService messageTemplateService,
                                  List<NotificationChannel> channels) {
        this.messageTemplateService = messageTemplateService;
        this.channels = List.copyOf(channels);
        if (this.channels.isEmpty()) {
            log.warn("No notification channels are enabled (email and SMS are both disabled).");
        }
    }

    public void sendTemplatedNotification(Friend recipient,
                                          String subject,
                                          String templateName,
                                          Map<String, Object> model) {
        String body = messageTemplateService.render(templateName, model);
        for (NotificationChannel channel : channels) {
            channel.send(recipient, subject, body);
        }
    }
}
