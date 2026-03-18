package com.example.it.mentor.controller;

import com.example.it.mentor.dto.ProfileSummaryResponse;
import com.example.it.mentor.service.ProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Сводка профиля")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profile/me")
    public ProfileSummaryResponse me() {
        return profileService.getProfileSummary();
    }
}
