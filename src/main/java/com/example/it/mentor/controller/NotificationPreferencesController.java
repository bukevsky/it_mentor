package com.example.it.mentor.controller;

import com.example.it.mentor.dto.notification.NotificationPreferencesResponse;
import com.example.it.mentor.dto.notification.UpdateNotificationPreferencesRequest;
import com.example.it.mentor.service.NotificationPreferencesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile/me/notifications")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferencesService preferencesService;

    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> get() {
        return ResponseEntity.ok(preferencesService.getForCurrentUser());
    }

    @PutMapping
    public ResponseEntity<NotificationPreferencesResponse> update(
            @RequestBody UpdateNotificationPreferencesRequest request) {
        return ResponseEntity.ok(preferencesService.update(request));
    }
}
