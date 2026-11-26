package com.jobsity.birthdayscheduler.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FriendTest {

    @Test
    void returnsTrueWhenMonthAndDayMatchInDifferentYear() {
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com");

        assertThat(alice.isBirthdayOn(LocalDate.of(2026, 3, 15))).isTrue();
    }

    @Test
    void returnsFalseWhenMonthOrDayDoNotMatch() {
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com");

        assertThat(alice.isBirthdayOn(LocalDate.of(2026, 3, 16))).isFalse();
        assertThat(alice.isBirthdayOn(LocalDate.of(2026, 4, 15))).isFalse();
    }

    @Test
    void greetsFeb29FriendOnFeb28InNonLeapYears() {
        Friend leapFriend = new Friend(1L, "Leap", "Person", LocalDate.of(1992, 2, 29), "leap@example.com");

        assertThat(leapFriend.isBirthdayOn(LocalDate.of(2026, 2, 28))).isTrue();
        assertThat(leapFriend.isBirthdayOn(LocalDate.of(2026, 2, 27))).isFalse();
        assertThat(leapFriend.isBirthdayOn(LocalDate.of(2026, 3, 1))).isFalse();
    }

    @Test
    void greetsFeb29FriendOnFeb29OnlyInLeapYears() {
        Friend leapFriend = new Friend(1L, "Leap", "Person", LocalDate.of(1992, 2, 29), "leap@example.com");

        assertThat(leapFriend.isBirthdayOn(LocalDate.of(2024, 2, 29))).isTrue();
        assertThat(leapFriend.isBirthdayOn(LocalDate.of(2024, 2, 28))).isFalse();
    }

    @Test
    void throwsWhenTargetDateIsNull() {
        Friend alice = new Friend(1L, "Alice", "Johnson", LocalDate.of(1992, 3, 15), "alice@example.com");

        assertThatThrownBy(() -> alice.isBirthdayOn(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("targetDate must not be null");
    }
}
