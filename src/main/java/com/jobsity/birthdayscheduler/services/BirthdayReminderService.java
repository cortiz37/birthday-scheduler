package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.repositories.FriendRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BirthdayReminderService {

    private final FriendRepository friendRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final String subject;
    private final String templateName;

    @Autowired
    public BirthdayReminderService(FriendRepository friendRepository,
                                   NotificationDispatcher notificationDispatcher,
                                   @Value("${app.mail.template-birthday-reminder-subject}") String subject,
                                   @Value("${app.mail.template-birthday-reminder}") String templateName) {
        this.friendRepository = friendRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.subject = subject;
        this.templateName = templateName;
    }

    public BirthdayReminderService(FriendRepository friendRepository,
                                   MessageTemplateService messageTemplateService,
                                   List<NotificationChannel> channels,
                                   String subject,
                                   String templateName) {
        this(friendRepository, new NotificationDispatcher(messageTemplateService, channels), subject, templateName);
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
        notificationDispatcher.sendTemplatedNotification(recipient, subject, templateName, model);
    }

    private boolean isSameFriend(Friend first, Friend second) {
        if (first.id() != null && second.id() != null) {
            return Objects.equals(first.id(), second.id());
        }
        return first.equals(second);
    }
}
