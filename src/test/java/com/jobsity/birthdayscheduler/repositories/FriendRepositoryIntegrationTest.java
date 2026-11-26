package com.jobsity.birthdayscheduler.repositories;

import com.jobsity.birthdayscheduler.domain.Friend;
import com.jobsity.birthdayscheduler.services.BirthdayNotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class FriendRepositoryIntegrationTest {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private BirthdayNotificationService notificationService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    void loadsSeededFriendsIncludingPhoneNumbers() {
        List<Friend> friends = friendRepository.findAll();

        assertThat(friends)
                .hasSize(4)
                .extracting(Friend::firstName, Friend::phoneNumber)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Emily", "+154557600001"),
                        org.assertj.core.groups.Tuple.tuple("Alice", "+15550000001"),
                        org.assertj.core.groups.Tuple.tuple("Bob", "+15550000002"),
                        org.assertj.core.groups.Tuple.tuple("Clara", "+15550000003")
                );
    }

    @Test
    void addsPhoneNumberColumnIdempotentlyWhenMissingFromLegacySchema() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        try {
            JdbcClient jdbcClient = JdbcClient.create(dataSource);
            jdbcClient.sql("""
                    CREATE TABLE friends (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        first_name TEXT NOT NULL,
                        last_name TEXT NOT NULL,
                        date_of_birth TEXT NOT NULL,
                        email TEXT NOT NULL UNIQUE
                    )
                    """).update();
            jdbcClient.sql("""
                    INSERT INTO friends (first_name, last_name, date_of_birth, email)
                    VALUES ('Legacy', 'Friend', '1991-05-01', 'legacy@example.com')
                    """).update();

            FriendRepositoryConfiguration.ensurePhoneNumberColumn(jdbcClient);
            FriendRepositoryConfiguration.ensurePhoneNumberColumn(jdbcClient);

            SqliteFriendRepository legacyRepository = new SqliteFriendRepository(jdbcClient);
            List<Friend> friends = legacyRepository.findAll();
            assertThat(friends).containsExactly(
                    new Friend(1L, "Legacy", "Friend", LocalDate.of(1991, 5, 1), "legacy@example.com", null)
            );
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void sendsBirthdayEmailForFeb29FriendOnFeb28EndToEnd() {
        notificationService.sendBirthdayNotifications(LocalDate.of(2026, 2, 28));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("emily.pier@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Happy birthday!");
        assertThat(captor.getValue().getText()).isEqualTo("Happy birthday, dear Emily!");
    }
}
