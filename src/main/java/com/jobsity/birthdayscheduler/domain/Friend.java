package com.jobsity.birthdayscheduler.domain;

import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;

public record Friend(Long id,
                     String firstName,
                     String lastName,
                     LocalDate dateOfBirth,
                     String email) {

    private static final MonthDay FEB_28 = MonthDay.of(Month.FEBRUARY, 28);
    private static final MonthDay FEB_29 = MonthDay.of(Month.FEBRUARY, 29);

    public boolean isBirthdayOn(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("targetDate must not be null");
        }

        MonthDay birthDay = MonthDay.from(dateOfBirth);
        MonthDay targetMonthDay = MonthDay.from(targetDate);
        if (birthDay.equals(FEB_29) && !targetDate.isLeapYear()) {
            return targetMonthDay.equals(FEB_28);
        }
        return birthDay.equals(targetMonthDay);
    }
}
