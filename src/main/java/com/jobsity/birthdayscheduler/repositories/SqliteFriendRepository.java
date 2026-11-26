package com.jobsity.birthdayscheduler.repositories;

import com.jobsity.birthdayscheduler.domain.Friend;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SqliteFriendRepository implements FriendRepository {

    private static final String SELECT_ALL_SQL = """
            SELECT id, first_name, last_name, date_of_birth, email, phone_number
            FROM friends
            ORDER BY id
            """;

    private final JdbcClient jdbcClient;

    public SqliteFriendRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Friend> findAll() {
        return jdbcClient.sql(SELECT_ALL_SQL)
                .query(this::mapRow)
                .list();
    }

    private Friend mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Friend(
                rs.getLong("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                LocalDate.parse(rs.getString("date_of_birth")),
                rs.getString("email"),
                rs.getString("phone_number")
        );
    }
}
