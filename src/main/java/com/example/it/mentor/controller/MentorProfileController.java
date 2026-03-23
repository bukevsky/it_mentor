package com.example.it.mentor.controller;

import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.service.MentorProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST-контроллер для управления профилями менторов.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Mentor Profile", description = "Профиль ментора")
public class MentorProfileController {

    private final MentorProfileService mentorProfileService;

    /**
     * Создаёт или обновляет профиль текущего ментора (upsert).
     *
     * @param request данные профиля
     * @return актуальный профиль ментора после сохранения
     */
    @PutMapping("/profile/mentor")
    public MentorProfileResponse upsert(@Valid @RequestBody MentorProfileRequest request) {
        return mentorProfileService.upsertProfile(request);
    }

    /**
     * Возвращает профиль текущего аутентифицированного ментора.
     *
     * @return профиль ментора
     */
    @GetMapping("/profile/mentor/me")
    public MentorProfileResponse myProfile() {
        return mentorProfileService.getMyProfile();
    }

    /**
     * Возвращает публичный профиль ментора по идентификатору.
     *
     * @param id идентификатор профиля ментора
     * @return профиль ментора
     */
    @GetMapping("/profiles/mentors/{id}")
    public MentorProfileResponse byId(@PathVariable Long id) {
        return mentorProfileService.getProfileById(id);
    }
}
