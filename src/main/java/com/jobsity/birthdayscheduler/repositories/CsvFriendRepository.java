package com.jobsity.birthdayscheduler.repositories;

import com.jobsity.birthdayscheduler.domain.Friend;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvFriendRepository implements FriendRepository {

    private static final String HEADER_FIRST_NAME = "first_name";
    private static final String HEADER_LAST_NAME = "last_name";
    private static final String HEADER_DATE_OF_BIRTH = "date_of_birth";
    private static final String HEADER_EMAIL = "email";
    private static final String HEADER_PHONE_NUMBER = "phone_number";

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setIgnoreSurroundingSpaces(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get();

    private final Resource resource;

    public CsvFriendRepository(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        if (!resource.exists()) {
            throw new IllegalStateException("Friends file does not exist: " + resource.getDescription());
        }
        this.resource = resource;
    }

    @Override
    public List<Friend> findAll() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)); CSVParser parser = CSV_FORMAT.parse(reader)) {
            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (headerMap == null || headerMap.isEmpty()) {
                return List.of();
            }

            List<Friend> friends = new ArrayList<>();
            for (CSVRecord record : parser) {
                friends.add(mapRecord(record));
            }
            return List.copyOf(friends);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read friends from " + resource.getDescription(), ex);
        }
    }

    private Friend mapRecord(CSVRecord record) {
        String firstName = requireValue(record, HEADER_FIRST_NAME);
        String lastName = requireValue(record, HEADER_LAST_NAME);
        LocalDate dateOfBirth = parseDate(record, requireValue(record, HEADER_DATE_OF_BIRTH));
        String email = requireValue(record, HEADER_EMAIL);
        String phoneNumber = optionalValue(record, HEADER_PHONE_NUMBER);
        return new Friend(0L, firstName, lastName, dateOfBirth, email, phoneNumber);
    }

    private String requireValue(CSVRecord record, String column) {
        if (!record.isSet(column)) {
            throw new IllegalStateException("Missing value for '" + column + "' at record " + record.getRecordNumber());
        }
        String value = record.get(column);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Blank value for '" + column + "' at record " + record.getRecordNumber());
        }
        return value;
    }

    private String optionalValue(CSVRecord record, String column) {
        if (!record.isSet(column)) {
            return null;
        }
        String value = record.get(column);
        return (value == null || value.isBlank()) ? null : value;
    }

    private LocalDate parseDate(CSVRecord record, String rawDate) {
        try {
            return LocalDate.parse(rawDate);
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("Invalid date_of_birth '" + rawDate + "' at record " + record.getRecordNumber(), ex);
        }
    }
}
