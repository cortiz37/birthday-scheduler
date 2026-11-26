package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.repositories.FriendRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class BirthdayNotificationService {

    private final FriendRepository friendRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final String subject;
    private final String templateName;

    @Autowired
    public BirthdayNotificationService(FriendRepository friendRepository,
                                       NotificationDispatcher notificationDispatcher,
                                       @Value("${app.mail.template-birthday-subject}") String subject,
                                       @Value("${app.mail.template-birthday}") String templateName) {
        this.friendRepository = friendRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.subject = subject;
        this.templateName = templateName;
    }

    public BirthdayNotificationService(FriendRepository friendRepository,
                                       MessageTemplateService messageTemplateService,
                                       List<NotificationChannel> channels,
                                       String subject,
                                       String templateName) {
        this(friendRepository, new NotificationDispatcher(messageTemplateService, channels), subject, templateName);
    }

    public void sendBirthdayNotifications(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("targetDate must not be null");
        }

        List<Friend> celebratingFriends = friendRepository.findAll().stream()
                .filter(friend -> friend.isBirthdayOn(targetDate))
                .toList();

        for (Friend friend : celebratingFriends) {
            Map<String, Object> model = Map.of("firstName", friend.firstName());
            notificationDispatcher.sendTemplatedNotification(friend, subject, templateName, model);
        }
    }
}
