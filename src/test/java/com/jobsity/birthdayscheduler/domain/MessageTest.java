package com.jobsity.birthdayscheduler.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTest {

    @Test
    void createsEmailStyleMessageWithSubject() {
        Message message = new Message("alice@example.com", "Happy birthday!", "Happy birthday, dear Alice!");

        assertThat(message.recipient()).isEqualTo("alice@example.com");
        assertThat(message.subject()).isEqualTo("Happy birthday!");
        assertThat(message.body()).isEqualTo("Happy birthday, dear Alice!");
    }

    @Test
    void createsSmsStyleMessageWithoutSubject() {
        Message message = new Message("+15550000001", "Happy birthday, dear Alice!");

        assertThat(message.recipient()).isEqualTo("+15550000001");
        assertThat(message.subject()).isNull();
        assertThat(message.body()).isEqualTo("Happy birthday, dear Alice!");
    }
}
