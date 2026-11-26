package com.jobsity.birthdayscheduler.repositories;

import com.jobsity.birthdayscheduler.domain.Friend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvFriendRepositoryTest {

    @Test
    void loadsClasspathFriendsTxtIncludingOptionalMissingPhoneNumber() {
        CsvFriendRepository repository = new CsvFriendRepository(new ClassPathResource("friends.txt"));

        List<Friend> friends = repository.findAll();

        assertThat(friends).containsExactly(
                new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 2, 29), "alice.johnson@example.com", "+234928374923"),
                new Friend(2L, "Bob", "Smith", LocalDate.of(1988, 11, 25), "bob.smith@example.com", null),
                new Friend(3L, "Charlie", "Brown", LocalDate.of(1995, 7, 14), "charlie.brown@example.com", "+19034820393")
        );
    }

    @Test
    void handlesBomQuotedFieldsCaseInsensitiveHeadersAndBlankPhoneNumbers() {
        String csv = "\uFEFFFIRST_NAME, LAST_NAME, Date_Of_Birth, EMAIL, PHONE_NUMBER\n"
                + "\"Doe, Jr.\", Jane, 1990-05-12, jane.doe@example.com,   \n"
                + "John, O'Connor, 1985-01-03, john@example.com, +15551112222\n";
        CsvFriendRepository repository = new CsvFriendRepository(
                new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8))
        );

        List<Friend> friends = repository.findAll();

        assertThat(friends).containsExactly(
                new Friend(1L, "Doe, Jr.", "Jane", LocalDate.of(1990, 5, 12), "jane.doe@example.com", null),
                new Friend(2L, "John", "O'Connor", LocalDate.of(1985, 1, 3), "john@example.com", "+15551112222")
        );
    }

    @Test
    void loadsCsvWithReorderedColumnsAndWithoutPhoneNumberHeader() {
        String csv = """
                email, date_of_birth, last_name, first_name
                alice@example.com, 1992-02-29, Johnson, Alice
                """;
        CsvFriendRepository repository = new CsvFriendRepository(
                new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8))
        );

        assertThat(repository.findAll()).containsExactly(
                new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 2, 29), "alice@example.com", null)
        );
    }

    @Test
    void returnsEmptyListForEmptyOrHeaderOnlyCsv() {
        CsvFriendRepository emptyRepo = new CsvFriendRepository(new ByteArrayResource(new byte[0]));
        CsvFriendRepository headerOnlyRepo = new CsvFriendRepository(
                new ByteArrayResource("first_name, last_name, date_of_birth, email\n".getBytes(StandardCharsets.UTF_8))
        );

        assertThat(emptyRepo.findAll()).isEmpty();
        assertThat(headerOnlyRepo.findAll()).isEmpty();
    }

    @Test
    void readsLatestChangesFromFileSystemResource(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("friends.csv");
        Files.writeString(csvFile, """
                first_name, last_name, date_of_birth, email
                Alice, Johnson, 1992-02-29, alice@example.com
                """);
        CsvFriendRepository repository = new CsvFriendRepository(new FileSystemResource(csvFile));

        assertThat(repository.findAll()).hasSize(1);

        Files.writeString(csvFile, """
                first_name, last_name, date_of_birth, email
                Alice, Johnson, 1992-02-29, alice@example.com
                Bob, Smith, 1988-11-25, bob@example.com
                """);

        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void throwsOnInvalidConstructorArgumentsOrMissingRequiredHeaders() {
        assertThatThrownBy(() -> new CsvFriendRepository(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("resource must not be null");

        assertThatThrownBy(() -> new CsvFriendRepository(new ClassPathResource("does-not-exist.csv")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not exist");

        CsvFriendRepository missingHeaderRepo = new CsvFriendRepository(
                new ByteArrayResource("first_name, last_name, email\nAlice, Johnson, alice@example.com\n"
                        .getBytes(StandardCharsets.UTF_8))
        );
        assertThatThrownBy(missingHeaderRepo::findAll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing required CSV header 'date_of_birth'");
    }

    @Test
    void throwsOnMissingBlankOrInvalidFieldValues() {
        CsvFriendRepository missingColumnInRowRepo = new CsvFriendRepository(
                new ByteArrayResource("first_name, last_name, date_of_birth, email\nAlice, Johnson, 1992-02-29\n"
                        .getBytes(StandardCharsets.UTF_8))
        );
        assertThatThrownBy(missingColumnInRowRepo::findAll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing value for 'email' at record 1");

        CsvFriendRepository blankEmailRepo = new CsvFriendRepository(
                new ByteArrayResource("first_name, last_name, date_of_birth, email\nAlice, Johnson, 1992-02-29,   \n"
                        .getBytes(StandardCharsets.UTF_8))
        );
        assertThatThrownBy(blankEmailRepo::findAll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Blank value for 'email' at record 1");

        CsvFriendRepository badDateRepo = new CsvFriendRepository(
                new ByteArrayResource("first_name, last_name, date_of_birth, email\nAlice, Johnson, not-a-date, alice@example.com\n"
                        .getBytes(StandardCharsets.UTF_8))
        );
        assertThatThrownBy(badDateRepo::findAll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid date_of_birth 'not-a-date' at record 1");
    }

    @Test
    void wrapsIOExceptionInUncheckedIOExceptionWhenReadingFails() {
        AbstractResource failingResource = new AbstractResource() {
            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public String getDescription() {
                return "failing-resource";
            }

            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("disk read error");
            }
        };

        CsvFriendRepository repository = new CsvFriendRepository(failingResource);

        assertThatThrownBy(repository::findAll)
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Failed to read friends from failing-resource")
                .hasCauseInstanceOf(IOException.class);
    }
}
