package com.jobsity.birthdayscheduler.services;

public interface TwilioSmsGateway {

    void sendSms(String toPhoneNumber, String body);
}
