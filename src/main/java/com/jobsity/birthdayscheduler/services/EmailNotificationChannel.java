package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.notifications.email", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmailNotificationChannel extends BaseNotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailNotificationChannel(JavaMailSender mailSender,
                                    @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public String channelName() {
        return "email";
    }

    @Override
    protected String extractRecipient(Friend recipient) {
        return recipient.email();
    }

    @Override
    protected void deliver(Message message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(message.recipient());
            mail.setSubject(message.subject());
            mail.setText(message.body());
            mailSender.send(mail);
        } catch (MailException ex) {
            log.error("Failed to send email to {}, subject ({}), exeption: {}", message.recipient(), message.subject(), ex.getMessage());
        }
    }
}
