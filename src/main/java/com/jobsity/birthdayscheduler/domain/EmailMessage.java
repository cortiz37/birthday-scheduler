package com.jobsity.birthdayscheduler.domain;

public record EmailMessage(String recipient,
                           String subject,
                           String body) {
}
