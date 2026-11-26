package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.Friend;

public interface NotificationChannel {

    String channelName();

    void send(Friend recipient, String subject, String body);
}
