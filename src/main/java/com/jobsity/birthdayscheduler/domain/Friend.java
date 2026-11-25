package com.jobsity.birthdayscheduler.domain;

import java.time.LocalDate;
import java.time.MonthDay;

public record Friend(Long id,
                     String firstName,
                     String lastName,
                     LocalDate dateOfBirth,
                     String email) {

    public boolean isBirthdayOn(LocalDate targetDate) {
        MonthDay birthDay = MonthDay.from(dateOfBirth);
        return birthDay.equals(MonthDay.from(targetDate));
    }
}
