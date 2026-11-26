package com.jobsity.birthdayscheduler.repositories;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration
public class FriendRepositoryConfiguration {

    @Bean
    public FriendRepository friendRepository(ObjectProvider<JdbcClient> jdbcClientProvider,
                                             ResourceLoader resourceLoader,
                                             @Value("${app.friends.source:sqlite}") String source,
                                             @Value("${app.friends.file-location:classpath:friends.txt}") String fileLocation) {

        return switch (source) {
            case "sqlite" -> {
                JdbcClient jdbcClient = jdbcClientProvider.getObject();
                ensurePhoneNumberColumn(jdbcClient);
                yield new SqliteFriendRepository(jdbcClient);
            }
            case "file" -> throw new UnsupportedOperationException("Unsupported file source");
            default -> throw new IllegalArgumentException("Unsupported source '" + source);
        };
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
