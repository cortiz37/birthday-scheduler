package com.jobsity.birthdayscheduler.repositories;

import java.time.Instant;
import java.time.LocalDate;

public interface BirthdayJobRunRepository {

    boolean hasRunOn(LocalDate date);

    boolean markAsRun(LocalDate date, Instant executedAt);
}
