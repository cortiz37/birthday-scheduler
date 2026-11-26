package com.jobsity.birthdayscheduler.cli;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.repositories.FriendRepository;
import com.jobsity.birthdayscheduler.services.BirthdaySchedulerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BirthdaySchedulerCliRunnerTest {

    @Mock
    private BirthdaySchedulerService schedulerService;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private ConfigurableApplicationContext applicationContext;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void runsStartupBirthdayCheckAndKeepsSchedulerRunningWhenNoListOption() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        when(schedulerService.processBirthdays()).thenReturn(true);
        BirthdaySchedulerCliRunner runner = new BirthdaySchedulerCliRunner(
                schedulerService,
                friendRepository,
                fixedClock,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                applicationContext
        );

        runner.run(new DefaultApplicationArguments());

        verify(schedulerService).processBirthdays();
        verify(applicationContext, never()).close();
        assertThat(output.toString(StandardCharsets.UTF_8))
                .contains("Birthday notifications processed for 2026-11-25.")
                .contains("Birthday scheduler is running. Press Ctrl+C to exit.");
    }

    @Test
    void printsSkipMessageWhenDayWasAlreadyProcessed() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        when(schedulerService.processBirthdays()).thenReturn(false);
        BirthdaySchedulerCliRunner runner = new BirthdaySchedulerCliRunner(
                schedulerService,
                friendRepository,
                fixedClock,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                applicationContext
        );

        runner.run(new DefaultApplicationArguments());

        assertThat(output.toString(StandardCharsets.UTF_8))
                .contains("Birthday notifications already processed for 2026-11-25; skipping.");
    }

    @Test
    void listsFriendsAndClosesContextWhenListOptionIsProvided() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        when(friendRepository.findAll()).thenReturn(List.of(
                new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com", "+15550000001")
        ));
        BirthdaySchedulerCliRunner runner = new BirthdaySchedulerCliRunner(
                schedulerService,
                friendRepository,
                fixedClock,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                applicationContext
        );

        runner.run(new DefaultApplicationArguments("--list"));

        verify(schedulerService, never()).processBirthdays();
        verify(applicationContext).close();
        assertThat(output.toString(StandardCharsets.UTF_8))
                .contains("Friends (1):")
                .contains("Alice Johnson")
                .contains("Phone: +15550000001");
    }
}
