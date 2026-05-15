package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.notification.OutboxEntryResponse;
import com.example.it.mentor.entity.enums.NotificationOutboxStatus;
import com.example.it.mentor.service.notification.NotificationOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationOutboxService outboxService;

    @GetMapping("/outbox")
    public ResponseEntity<PagedResponse<OutboxEntryResponse>> getOutbox(
            @RequestParam(required = false) NotificationOutboxStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(outboxService.getOutbox(status, pageable));
    }
}
