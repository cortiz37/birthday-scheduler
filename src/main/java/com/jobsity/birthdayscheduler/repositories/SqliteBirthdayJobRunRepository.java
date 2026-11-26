package com.jobsity.birthdayscheduler.repositories;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;

@Repository
public class SqliteBirthdayJobRunRepository implements BirthdayJobRunRepository {

    private static final String EXISTS_SQL = """
            SELECT EXISTS(SELECT 1 FROM birthday_job_runs WHERE run_date = :runDate)
            """;

    private static final String INSERT_SQL = """
            INSERT OR IGNORE INTO birthday_job_runs (run_date, executed_at)
            VALUES (:runDate, :executedAt)
            """;

    private final JdbcClient jdbcClient;

    public SqliteBirthdayJobRunRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean hasRunOn(LocalDate date) {
        Integer exists = jdbcClient.sql(EXISTS_SQL)
                .param("runDate", date.toString())
                .query(Integer.class)
                .single();
        return exists != null && exists == 1;
    }

    @Override
    public boolean markAsRun(LocalDate date, Instant executedAt) {
        int rowsAffected = jdbcClient.sql(INSERT_SQL)
                .param("runDate", date.toString())
                .param("executedAt", executedAt.toString())
                .update();
        return rowsAffected > 0;
    }
}
