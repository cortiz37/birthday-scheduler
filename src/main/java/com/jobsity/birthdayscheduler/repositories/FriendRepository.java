package com.jobsity.birthdayscheduler.repositories;

import com.jobsity.birthdayscheduler.domain.Friend;

import java.util.List;

public interface FriendRepository {

    List<Friend> findAll();
}
