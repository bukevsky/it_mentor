package com.example.it.mentor.controller;

import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.service.StudentProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST-контроллер для управления профилями студентов.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Student Profile", description = "Профиль студента")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    /**
     * Создаёт или обновляет профиль текущего студента (upsert).
     *
     * @param request данные профиля
     * @return актуальный профиль студента после сохранения
     */
    @PutMapping("/profile/student")
    public StudentProfileResponse upsert(@Valid @RequestBody StudentProfileRequest request) {
        return studentProfileService.upsertProfile(request);
    }

    /**
     * Возвращает профиль текущего аутентифицированного студента.
     *
     * @return профиль студента
     */
    @GetMapping("/profile/student/me")
    public StudentProfileResponse myProfile() {
        return studentProfileService.getMyProfile();
    }

    /**
     * Возвращает публичный профиль студента по идентификатору.
     *
     * @param id идентификатор профиля студента
     * @return профиль студента
     */
    @GetMapping("/profiles/students/{id}")
    public StudentProfileResponse byId(@PathVariable Long id) {
        return studentProfileService.getProfileById(id);
    }
}
