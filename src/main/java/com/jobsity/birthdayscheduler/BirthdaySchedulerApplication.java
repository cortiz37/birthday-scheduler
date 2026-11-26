package com.jobsity.birthdayscheduler;

import com.jobsity.birthdayscheduler.config.TwilioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TwilioProperties.class)
public class BirthdaySchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BirthdaySchedulerApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
