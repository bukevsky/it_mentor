package com.example.it.mentor.controller;

import com.example.it.mentor.dto.presence.PresenceResponse;
import com.example.it.mentor.service.PresenceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/presence")
@RequiredArgsConstructor
@Tag(name = "Presence", description = "Присутствие пользователей")
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/{userId}")
    public PresenceResponse getPresence(@PathVariable Long userId) {
        return presenceService.getPresence(userId);
    }
}
