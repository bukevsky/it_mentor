package com.example.it.mentor.controller;

import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.service.StudentProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Student Profile", description = "Профиль студента")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @PutMapping("/profile/student")
    public StudentProfileResponse upsert(@Valid @RequestBody StudentProfileRequest request) {
        return studentProfileService.upsertProfile(request);
    }

    @GetMapping("/profile/student/me")
    public StudentProfileResponse myProfile() {
        return studentProfileService.getMyProfile();
    }

    @GetMapping("/profiles/students/{id}")
    public StudentProfileResponse byId(@PathVariable Long id) {
        return studentProfileService.getProfileById(id);
    }
}
