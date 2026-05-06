package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.mentor.MentorCardResponse;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentor.MentorSearchFilter;
import com.example.it.mentor.entity.enums.MentoringChannel;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.service.MentorProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST-контроллер для управления профилями менторов.
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Mentor Profile", description = "Профиль ментора")
public class MentorProfileController {

    private static final Set<String> SORT_WHITELIST = Set.of("createdAt", "firstName", "lastName");

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

    /**
     * Поиск менторов с фильтрацией и пагинацией.
     *
     * @param q текстовый поиск по профилю
     * @param skillIds список идентификаторов навыков
     * @param cityId идентификатор города
     * @param recruitmentStatus статус набора ментора
     * @param mentoringType тип менторства
     * @param mentoringChannel канал взаимодействия
     * @param page номер страницы, начиная с {@code 0}
     * @param size размер страницы
     * @param sort поле и направление сортировки в формате {@code field,direction}
     * @return страница менторов, соответствующих фильтру
     */
    @GetMapping("/profiles/mentors")
    public PagedResponse<MentorCardResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @Size(max = 20) List<Long> skillIds,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) RecruitmentStatus recruitmentStatus,
            @RequestParam(required = false) MentoringType mentoringType,
            @RequestParam(required = false) MentoringChannel mentoringChannel,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        MentorSearchFilter filter = new MentorSearchFilter(q, skillIds, cityId, recruitmentStatus, mentoringType, mentoringChannel);
        return mentorProfileService.searchMentors(filter, buildPageable(page, size, sort));
    }

    /**
     * Собирает объект пагинации с белым списком полей сортировки.
     *
     * @param page номер страницы
     * @param size размер страницы
     * @param sort строка сортировки в формате {@code field,direction}
     * @return безопасный {@link Pageable} для поиска
     */
    private Pageable buildPageable(int page, int size, String sort) {
        String[] parts = sort.split(",", 2);
        String field = SORT_WHITELIST.contains(parts[0]) ? parts[0] : "createdAt";
        Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, field));
    }
}
