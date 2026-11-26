package com.jobsity.birthdayscheduler.repositories;

import com.jobsity.birthdayscheduler.domain.Friend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FriendRepositoryConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FriendRepositoryConfiguration.class);

    @Test
    void usesSqliteFriendRepositoryByDefault() {
        contextRunner
                .withUserConfiguration(InMemorySqliteConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(SqliteFriendRepository.class);
                    assertThat(context).doesNotHaveBean(CsvFriendRepository.class);
                });
    }

    @Test
    void usesCsvFriendRepositoryFromDefaultClasspathFileWhenSourceIsFile() {
        contextRunner
                .withPropertyValues("app.friends.source=file")
                .run(context -> {
                    assertThat(context).hasSingleBean(CsvFriendRepository.class);
                    assertThat(context).doesNotHaveBean(SqliteFriendRepository.class);
                    FriendRepository repository = context.getBean(FriendRepository.class);
                    assertThat(repository.findAll()).hasSize(3);
                });
    }

    @Test
    void loadsCsvFromCustomFileUriAndPlainFilesystemPath(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("custom-friends.csv");
        Files.writeString(csvFile, """
                first_name, last_name, date_of_birth, email, phone_number
                Dana, Scully, 1964-02-23, dana.scully@example.com, +15559990000
                """);

        contextRunner
                .withPropertyValues(
                        "app.friends.source=file",
                        "app.friends.file-location=" + csvFile.toUri()
                )
                .run(context -> {
                    FriendRepository repository = context.getBean(FriendRepository.class);
                    assertThat(repository.findAll()).containsExactly(
                            new Friend(1L, "Dana", "Scully", LocalDate.of(1964, 2, 23), "dana.scully@example.com", "+15559990000")
                    );
                });

        contextRunner
                .withPropertyValues(
                        "app.friends.source=file",
                        "app.friends.file-location=" + csvFile.toAbsolutePath()
                )
                .run(context -> {
                    FriendRepository repository = context.getBean(FriendRepository.class);
                    assertThat(repository.findAll()).containsExactly(
                            new Friend(1L, "Dana", "Scully", LocalDate.of(1964, 2, 23), "dana.scully@example.com", "+15559990000")
                    );
                });
    }

    @Test
    void normalizesSourceNameCaseAndWhitespace() {
        contextRunner
                .withPropertyValues("app.friends.source= FILE ")
                .run(context -> {
                    assertThat(context).hasSingleBean(CsvFriendRepository.class);
                    assertThat(context).doesNotHaveBean(SqliteFriendRepository.class);
                });
    }

    @Test
    void failsContextStartupForUnsupportedSourceBlankLocationOrMissingFile() {
        contextRunner
                .withPropertyValues("app.friends.source=unknown")
                .run(context -> assertThat(context).hasFailed());

        contextRunner
                .withPropertyValues(
                        "app.friends.source=file",
                        "app.friends.file-location=   "
                )
                .run(context -> assertThat(context).hasFailed());

        contextRunner
                .withPropertyValues(
                        "app.friends.source=file",
                        "app.friends.file-location=classpath:missing-friends.csv"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    static class InMemorySqliteConfig {
        @Bean(destroyMethod = "destroy")
        DataSource dataSource() {
            SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
            JdbcClient.create(dataSource).sql("""
                    CREATE TABLE friends (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        first_name TEXT NOT NULL,
                        last_name TEXT NOT NULL,
                        date_of_birth TEXT NOT NULL,
                        email TEXT NOT NULL UNIQUE
                    )
                    """).update();
            return dataSource;
        }

        @Bean
        JdbcClient jdbcClient(DataSource dataSource) {
            return JdbcClient.create(dataSource);
        }
    }
}
