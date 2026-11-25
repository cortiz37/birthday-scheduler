package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.EmailMessage;
import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.repositories.FriendRepository;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class BirthdayNotificationService {

    private static final Logger log = LoggerFactory.getLogger(BirthdayNotificationService.class);

    private final FriendRepository friendRepository;
    private final JavaMailSender mailSender;
    private final Configuration freemarkerConfiguration;
    private final String fromAddress;
    private final String subject;
    private final String templateName;

    public BirthdayNotificationService(FriendRepository friendRepository,
                                       JavaMailSender mailSender,
                                       Configuration freemarkerConfiguration,
                                       @Value("${app.mail.from}") String fromAddress,
                                       @Value("${app.mail.template-birthday-subject}") String subject,
                                       @Value("${app.mail.template-birthday}") String templateName) {
        this.friendRepository = friendRepository;
        this.mailSender = mailSender;
        this.freemarkerConfiguration = freemarkerConfiguration;
        this.fromAddress = fromAddress;
        this.subject = subject;
        this.templateName = templateName;
    }

    public void sendBirthdayEmails(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("targetDate must not be null");
        }

        List<Friend> celebratingFriends = filterCelebratingFriends(friendRepository.findAll(), targetDate);

        for (Friend friend : celebratingFriends) {
            EmailMessage message = buildEmail(friend);
            sendEmail(message);
        }
    }

    private List<Friend> filterCelebratingFriends(List<Friend> friends, LocalDate targetDate) {
        return friends.stream()
                .filter(friend -> friend.isBirthdayOn(targetDate))
                .toList();
    }

    private EmailMessage buildEmail(Friend friend) {
        Map<String, Object> model = Map.of("firstName", friend.firstName());
        String body = renderTemplate(model);
        return new EmailMessage(friend.email(), subject, body);
    }

    private String renderTemplate(Map<String, Object> model) {
        try {
            Template template = freemarkerConfiguration.getTemplate(templateName);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (IOException | TemplateException ex) {
            throw new IllegalStateException("Failed to render email template '" + templateName + "'", ex);
        }
    }

    private void sendEmail(EmailMessage message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(message.recipient());
            mail.setSubject(message.subject());
            mail.setText(message.body());
            mailSender.send(mail);
        } catch (MailException ex) {
            log.error("Failed to send birthday email to {}", message.recipient(), ex);
        }
    }
}
