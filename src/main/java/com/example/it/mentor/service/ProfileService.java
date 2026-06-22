package com.example.it.mentor.service;

import com.example.it.mentor.dto.ProfileSummaryResponse;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис получения краткой сводки профиля текущего пользователя.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserService userService;
    private final StudentProfileRepository studentProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;

    /**
     * Возвращает сводную информацию о профиле в зависимости от основной роли пользователя.
     *
     * @return сводка по профилю и факту его наличия
     */
    @Transactional(readOnly = true)
    public ProfileSummaryResponse getProfileSummary() {
        User user = userService.getCurrentUserEntity();
        RoleCode role = user.primaryRole();

        ProfileSummaryResponse response = switch (role) {
            case STUDENT -> studentProfileRepository.findByUserId(user.getId())
                    .map(profile -> new ProfileSummaryResponse(role.name(), profile.getId(), true))
                    .orElse(new ProfileSummaryResponse(role.name(), null, false));
            case MENTOR -> mentorProfileRepository.findByUserId(user.getId())
                    .map(profile -> new ProfileSummaryResponse(role.name(), profile.getId(), true))
                    .orElse(new ProfileSummaryResponse(role.name(), null, false));
            case ADMIN -> new ProfileSummaryResponse(role.name(), null, false);
        };
        log.debug("Сводка профиля загружена: userId={}, role={}, profileId={}, profileExists={}, step={}",
                user.getId(), role, response.profileId(), response.profileExists(), "profile_summary_loaded");
        return response;
    }
}
