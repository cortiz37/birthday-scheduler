package com.jobsity.birthdayscheduler.cli;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.repositories.FriendRepository;
import com.jobsity.birthdayscheduler.services.BirthdayNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
public class BirthdaySchedulerCliRunner implements ApplicationRunner {

    private final BirthdayNotificationService notificationService;
    private final FriendRepository friendRepository;
    private final Clock clock;
    private final PrintStream printStream;

    @Autowired
    public BirthdaySchedulerCliRunner(BirthdayNotificationService notificationService,
                                      FriendRepository friendRepository,
                                      Clock clock) {
        this(notificationService, friendRepository, clock, System.out);
    }

    public BirthdaySchedulerCliRunner(BirthdayNotificationService notificationService,
                                      FriendRepository friendRepository,
                                      Clock clock,
                                      PrintStream printStream) {
        this.notificationService = notificationService;
        this.friendRepository = friendRepository;
        this.clock = clock;
        this.printStream = printStream;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("list")) {
            handleListFriends();
            return;
        }

        handleSend(LocalDate.now(clock));
    }

    private void handleListFriends() {
        List<Friend> friends = friendRepository.findAll();
        if (friends.isEmpty()) {
            printStream.println("No friends found.");
            return;
        }
        printStream.printf("Friends (%d):%n", friends.size());
        for (Friend friend : friends) {
            printStream.printf("  [%d] %s | DOB: %s | Email: %s%n", friend.id(), friend.firstName() + " " + friend.lastName(), friend.dateOfBirth(), friend.email());
        }
    }

    private void handleSend(LocalDate targetDate) {
        notificationService.sendBirthdayEmails(targetDate);
        printStream.printf("Birthday emails processed for %s.%n", targetDate);
    }
}
