package com.jobsity.birthdayscheduler.repositories;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.ResourceUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Configuration
public class FriendRepositoryConfiguration {

    @Bean
    public FriendRepository friendRepository(ObjectProvider<JdbcClient> jdbcClientProvider,
                                             ResourceLoader resourceLoader,
                                             @Value("${app.friends.source:sqlite}") String source,
                                             @Value("${app.friends.file-location:classpath:friends.txt}") String fileLocation) {

        String normalizedSource = source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedSource) {
            case "sqlite" -> {
                JdbcClient jdbcClient = jdbcClientProvider.getObject();
                ensurePhoneNumberColumn(jdbcClient);
                yield new SqliteFriendRepository(jdbcClient);
            }
            case "file" -> new CsvFriendRepository(resolveResource(resourceLoader, fileLocation));
            default -> throw new IllegalArgumentException("Unsupported source '" + source + "'");
        };
    }

    static Resource resolveResource(ResourceLoader resourceLoader, String fileLocation) {
        if (fileLocation == null || fileLocation.isBlank()) {
            throw new IllegalArgumentException("app.friends.file-location must not be blank");
        }
        String trimmedLocation = fileLocation.trim();
        Resource resource = resourceLoader.getResource(trimmedLocation);
        if (!resource.exists() && !ResourceUtils.isUrl(trimmedLocation)) {
            Path filePath = Path.of(trimmedLocation);
            if (Files.isRegularFile(filePath)) {
                return new FileSystemResource(filePath);
            }
        }
        return resource;
    }

    static void ensurePhoneNumberColumn(JdbcClient jdbcClient) {
        boolean hasPhoneNumber = jdbcClient.sql("PRAGMA table_info(friends)")
                .query((rs, rowNum) -> rs.getString("name"))
                .list()
                .contains("phone_number");
        if (!hasPhoneNumber) {
            jdbcClient.sql("ALTER TABLE friends ADD COLUMN phone_number TEXT").update();
        }
    }
}
