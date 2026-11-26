package com.jobsity.birthdayscheduler.domain;

public record Message(String recipient,
                      String subject,
                      String body) {

    public Message(String recipient, String body) {
        this(recipient, null, body);
    }
}
