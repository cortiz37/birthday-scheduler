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
            case "sqlite" -> new SqliteFriendRepository(jdbcClientProvider.getObject());
            case "file" -> throw new UnsupportedOperationException("Unsupported file source");
            default -> throw new IllegalArgumentException("Unsupported source '" + source);
        };
    }
}
