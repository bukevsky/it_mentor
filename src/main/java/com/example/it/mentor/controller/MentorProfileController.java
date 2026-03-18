package com.example.it.mentor.controller;

import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.service.MentorProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mentor Profile", description = "Профиль ментора")
public class MentorProfileController {

    private final MentorProfileService mentorProfileService;

    @PutMapping("/profile/mentor")
    public MentorProfileResponse upsert(@Valid @RequestBody MentorProfileRequest request) {
        return mentorProfileService.upsertProfile(request);
    }

    @GetMapping("/profile/mentor/me")
    public MentorProfileResponse myProfile() {
        return mentorProfileService.getMyProfile();
    }

    @GetMapping("/profiles/mentors/{id}")
    public MentorProfileResponse byId(@PathVariable Long id) {
        return mentorProfileService.getProfileById(id);
    }
}
