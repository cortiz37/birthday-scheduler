package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.repositories.FriendRepository;
import freemarker.template.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BirthdayReminderService {

    private final FriendRepository friendRepository;
    private final MessageTemplateService messageTemplateService;
    private final String subject;
    private final String templateName;

    @Autowired
    public BirthdayReminderService(FriendRepository friendRepository,
                                   MessageTemplateService messageTemplateService,
                                   @Value("${app.mail.template-birthday-reminder-subject}") String subject,
                                   @Value("${app.mail.template-birthday-reminder}") String templateName) {
        this.friendRepository = friendRepository;
        this.messageTemplateService = messageTemplateService;
        this.subject = subject;
        this.templateName = templateName;
    }

    public BirthdayReminderService(FriendRepository friendRepository,
                                   JavaMailSender mailSender,
                                   Configuration freemarkerConfiguration,
                                   String fromAddress,
                                   String subject,
                                   String templateName) {
        this(friendRepository, new MessageTemplateService(mailSender, freemarkerConfiguration, fromAddress), subject, templateName);
    }

    public void sendBirthdayReminders(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("targetDate must not be null");
        }

        List<Friend> allFriends = friendRepository.findAll();
        List<Friend> celebratingFriends = allFriends.stream()
                .filter(friend -> friend.isBirthdayOn(targetDate))
                .toList();

        if (celebratingFriends.isEmpty()) {
            return;
        }

        for (Friend recipient : allFriends) {
            for (Friend celebratingFriend : celebratingFriends) {
                if (!isSameFriend(recipient, celebratingFriend)) {
                    sendReminder(recipient, celebratingFriend);
                }
            }
        }
    }

    private void sendReminder(Friend recipient, Friend celebratingFriend) {
        Map<String, Object> model = Map.of(
                "recipientFirstName", recipient.firstName(),
                "celebratingFirstName", celebratingFriend.firstName(),
                "celebratingLastName", celebratingFriend.lastName()
        );
        messageTemplateService.sendTemplatedEmail(recipient.email(), subject, templateName, model);
    }

    private boolean isSameFriend(Friend first, Friend second) {
        if (first.id() != null && second.id() != null) {
            return Objects.equals(first.id(), second.id());
        }
        return first.equals(second);
    }
}
