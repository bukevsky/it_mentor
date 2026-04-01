package com.example.it.mentor.controller;

import com.example.it.mentor.dto.ProfileSummaryResponse;
import com.example.it.mentor.service.ProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер для получения сводной информации о профиле текущего пользователя.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Сводка профиля")
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Возвращает сводку профиля текущего пользователя с учётом его роли.
     *
     * <p>Для студента включает данные студенческого профиля,
     * для ментора — профиль ментора.</p>
     *
     * @return ролевая сводка профиля
     */
    @GetMapping("/profile/me")
    public ProfileSummaryResponse me() {
        return profileService.getProfileSummary();
    }
}
