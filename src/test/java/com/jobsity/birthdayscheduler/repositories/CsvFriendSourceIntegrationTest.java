package com.jobsity.birthdayscheduler.repositories;

import com.jobsity.birthdayscheduler.services.BirthdaySchedulerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "app.friends.source=file",
        "app.friends.file-location=classpath:friends.txt"
})
@ActiveProfiles("test")
class CsvFriendSourceIntegrationTest {

    @TestConfiguration
    static class Feb28FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            // In friends.txt, Alice is born on 1992-02-29 (celebrated on Feb 28 in non-leap 2026)
            return Clock.fixed(Instant.parse("2026-02-28T09:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private BirthdayJobRunRepository jobRunRepository;

    @Autowired
    private BirthdaySchedulerService schedulerService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    void readsFriendsFromCsvWhileKeepingJobExecutionHistoryInSqlite() {
        assertThat(friendRepository).isInstanceOf(CsvFriendRepository.class);
        assertThat(jobRunRepository).isInstanceOf(SqliteBirthdayJobRunRepository.class);
        assertThat(jobRunRepository.hasRunOn(LocalDate.of(2026, 2, 28))).isTrue();

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(3)).send(captor.capture());

        assertThat(captor.getAllValues()).extracting(
                msg -> msg.getTo()[0],
                SimpleMailMessage::getSubject,
                SimpleMailMessage::getText
        ).containsExactlyInAnyOrder(
                tuple("alice.johnson@example.com", "Happy birthday!", "Happy birthday, dear Alice!"),
                tuple("bob.smith@example.com", "Birthday Reminder", "Dear Bob,\nToday is Alice Johnson's birthday.\nDon't forget to send him a message !"),
                tuple("charlie.brown@example.com", "Birthday Reminder", "Dear Charlie,\nToday is Alice Johnson's birthday.\nDon't forget to send him a message !")
        );

        // Subsequent scheduled trigger on the same day must be skipped via SQLite job tracking
        assertThat(schedulerService.processBirthdays()).isFalse();
        verify(javaMailSender, times(3)).send(any(SimpleMailMessage.class));
    }
}
