package com.jobsity.birthdayscheduler.cli;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.repositories.FriendRepository;
import com.jobsity.birthdayscheduler.services.BirthdaySchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
public class BirthdaySchedulerCliRunner implements ApplicationRunner {

    private final BirthdaySchedulerService schedulerService;
    private final FriendRepository friendRepository;
    private final Clock clock;
    private final PrintStream printStream;
    private final ConfigurableApplicationContext applicationContext;

    @Autowired
    public BirthdaySchedulerCliRunner(BirthdaySchedulerService schedulerService,
                                      FriendRepository friendRepository,
                                      Clock clock,
                                      ConfigurableApplicationContext applicationContext) {
        this(schedulerService, friendRepository, clock, System.out, applicationContext);
    }

    public BirthdaySchedulerCliRunner(BirthdaySchedulerService schedulerService,
                                      FriendRepository friendRepository,
                                      Clock clock,
                                      PrintStream printStream,
                                      ConfigurableApplicationContext applicationContext) {
        this.schedulerService = schedulerService;
        this.friendRepository = friendRepository;
        this.clock = clock;
        this.printStream = printStream;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("list")) {
            handleListFriends();
            if (applicationContext != null) {
                applicationContext.close();
            }
            return;
        }

        handleStartupCheck();
        printStream.println("Birthday scheduler is running. Press Ctrl+C to exit.");
    }

    private void handleListFriends() {
        List<Friend> friends = friendRepository.findAll();
        if (friends.isEmpty()) {
            printStream.println("No friends found.");
            return;
        }
        printStream.printf("Friends (%d):%n", friends.size());
        for (Friend friend : friends) {
            printStream.printf("  [%d] %s | DOB: %s | Email: %s | Phone: %s%n",
                    friend.id(),
                    friend.firstName() + " " + friend.lastName(),
                    friend.dateOfBirth(),
                    friend.email(),
                    friend.phoneNumber() != null ? friend.phoneNumber() : "-");
        }
    }

    private void handleStartupCheck() {
        LocalDate targetDate = LocalDate.now(clock);
        boolean executed = schedulerService.processBirthdays();
        if (executed) {
            printStream.printf("Birthday notifications processed for %s.%n", targetDate);
        } else {
            printStream.printf("Birthday notifications already processed for %s; skipping.%n", targetDate);
        }
    }
}
