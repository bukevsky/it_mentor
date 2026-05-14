package com.example.it.mentor.repository;

import com.example.it.mentor.entity.UserNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNotificationPreferencesRepository extends JpaRepository<UserNotificationPreferences, Long> {

    Optional<UserNotificationPreferences> findByUserId(Long userId);
}
