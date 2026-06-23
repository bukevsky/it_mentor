package com.example.it.mentor.service;

import com.example.it.mentor.dto.notification.NotificationPreferencesResponse;
import com.example.it.mentor.dto.notification.UpdateNotificationPreferencesRequest;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.UserNotificationPreferences;
import com.example.it.mentor.repository.UserNotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferencesService {

    private final UserNotificationPreferencesRepository prefsRepository;
    private final UserService userService;

    @Transactional
    public NotificationPreferencesResponse getForCurrentUser() {
        User user = userService.getCurrentUserEntity();
        UserNotificationPreferences prefs = getOrCreate(user);
        return toResponse(prefs);
    }

    @Transactional
    public NotificationPreferencesResponse update(UpdateNotificationPreferencesRequest dto) {
        User user = userService.getCurrentUserEntity();
        UserNotificationPreferences prefs = getOrCreate(user);

        if (dto.emailRequestEvents() != null) prefs.setEmailRequestEvents(dto.emailRequestEvents());
        if (dto.emailSessionEvents() != null) prefs.setEmailSessionEvents(dto.emailSessionEvents());
        if (dto.emailReviewEvents() != null) prefs.setEmailReviewEvents(dto.emailReviewEvents());

        prefsRepository.save(prefs);
        log.info("Настройки уведомлений обновлены: userId={}, emailRequestEvents={}, emailSessionEvents={}, " +
                        "emailReviewEvents={}, step={}",
                user.getId(), prefs.isEmailRequestEvents(), prefs.isEmailSessionEvents(), prefs.isEmailReviewEvents(),
                "notification_preferences_updated");
        return toResponse(prefs);
    }

    public boolean shouldNotify(Long userId, String category) {
        boolean shouldNotify = prefsRepository.findByUserId(userId)
                .map(prefs -> switch (category) {
                    case "request" -> prefs.isEmailRequestEvents();
                    case "session" -> prefs.isEmailSessionEvents();
                    case "review" -> prefs.isEmailReviewEvents();
                    default -> true;
                })
                .orElse(true);
        log.debug("Проверены настройки уведомлений: userId={}, category={}, shouldNotify={}, step={}",
                userId, category, shouldNotify, "notification_preference_checked");
        return shouldNotify;
    }

    private UserNotificationPreferences getOrCreate(User user) {
        return prefsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    try {
                        UserNotificationPreferences prefs = UserNotificationPreferences.builder()
                                .user(user)
                                .build();
                        return prefsRepository.save(prefs);
                    } catch (DataIntegrityViolationException ex) {
                        // concurrent insert — read what the other thread committed
                        return prefsRepository.findByUserId(user.getId())
                                .orElseThrow(() -> ex);
                    }
                });
    }

    private NotificationPreferencesResponse toResponse(UserNotificationPreferences prefs) {
        return new NotificationPreferencesResponse(
                prefs.isEmailRequestEvents(),
                prefs.isEmailSessionEvents(),
                prefs.isEmailReviewEvents()
        );
    }
}
