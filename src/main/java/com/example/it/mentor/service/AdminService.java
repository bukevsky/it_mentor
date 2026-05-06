package com.example.it.mentor.service;

import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис административного управления прикладными ролями пользователей.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Переключает прикладную роль пользователя между {@code STUDENT} и {@code MENTOR}.
     *
     * <p>После смены роли метод гарантирует наличие соответствующего профиля и
     * инвалидирует security-кэш пользователя.</p>
     *
     * @param userId идентификатор пользователя
     * @param targetRole целевая роль
     */
    @Transactional
    public void assignRole(Long userId, RoleCode targetRole) {
        log.info("Назначение роли пользователю: userId={}, targetRole={}", userId, targetRole);
        validateTargetRole(targetRole);
        User user = loadUser(userId);
        Role roleToAdd = loadRole(targetRole);
        RoleCode roleToRemove = targetRole == RoleCode.MENTOR ? RoleCode.STUDENT : RoleCode.MENTOR;
        replaceRole(user, roleToAdd, targetRole, roleToRemove);
        userRepository.save(user);
        log.info("Роль назначена: userId={}, newRole={}, removedRole={}", userId, targetRole, roleToRemove);
        ensureProfileExists(user, userId, targetRole);
        userDetailsService.evictUserCache(user.getEmail());
    }

    /**
     * Проверяет, что целевая роль может быть назначена через административный endpoint.
     *
     * @param targetRole целевая роль
     */
    private void validateTargetRole(RoleCode targetRole) {
        if (targetRole == RoleCode.ADMIN) {
            throw new BusinessRuleViolationException("Нельзя назначить роль ADMIN через этот endpoint");
        }
    }

    /**
     * Загружает пользователя вместе с ролями.
     *
     * @param userId идентификатор пользователя
     * @return найденный пользователь
     */
    private User loadUser(Long userId) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
    }

    /**
     * Загружает сущность роли по коду.
     *
     * @param targetRole код роли
     * @return найденная роль
     */
    private Role loadRole(RoleCode targetRole) {
        return roleRepository.findByCode(targetRole)
                .orElseThrow(() -> new NotFoundException("Роль не найдена: " + targetRole));
    }

    /**
     * Обновляет набор ролей пользователя, оставляя только целевую прикладную роль.
     *
     * @param user пользователь
     * @param roleToAdd сущность добавляемой роли
     * @param targetRole код добавляемой роли
     * @param roleToRemove код роли, которую нужно удалить
     */
    private void replaceRole(User user, Role roleToAdd, RoleCode targetRole, RoleCode roleToRemove) {
        user.getRoles().removeIf(role -> role.getCode() == roleToRemove || role.getCode() == targetRole);
        user.getRoles().add(roleToAdd);
    }

    /**
     * Гарантирует наличие профиля, соответствующего новой роли пользователя.
     *
     * @param user пользователь
     * @param userId идентификатор пользователя
     * @param targetRole целевая роль
     */
    private void ensureProfileExists(User user, Long userId, RoleCode targetRole) {
        if (targetRole == RoleCode.MENTOR) {
            ensureMentorProfileExists(user, userId);
            return;
        }
        ensureStudentProfileExists(user, userId);
    }

    /**
     * Создаёт профиль ментора, если после смены роли он отсутствует.
     *
     * @param user пользователь
     * @param userId идентификатор пользователя
     */
    private void ensureMentorProfileExists(User user, Long userId) {
        if (mentorProfileRepository.findByUserId(userId).isPresent()) {
            return;
        }

        ProfileNames names = studentProfileRepository.findByUserId(userId)
                .map(this::toProfileNames)
                .orElse(ProfileNames.empty());

        mentorProfileRepository.save(MentorProfile.builder()
                .user(user)
                .firstName(names.firstName())
                .lastName(names.lastName())
                .build());
        log.debug("Создан профиль ментора при смене роли: userId={}", userId);
    }

    /**
     * Создаёт профиль студента, если после смены роли он отсутствует.
     *
     * @param user пользователь
     * @param userId идентификатор пользователя
     */
    private void ensureStudentProfileExists(User user, Long userId) {
        if (studentProfileRepository.findByUserId(userId).isPresent()) {
            return;
        }

        ProfileNames names = mentorProfileRepository.findByUserId(userId)
                .map(this::toProfileNames)
                .orElse(ProfileNames.empty());

        studentProfileRepository.save(StudentProfile.builder()
                .user(user)
                .firstName(names.firstName())
                .lastName(names.lastName())
                .build());
        log.debug("Создан профиль студента при смене роли: userId={}", userId);
    }

    /**
     * Извлекает имя и фамилию из профиля студента для копирования в новый профиль.
     *
     * @param profile профиль студента
     * @return переносимые имя и фамилия
     */
    private ProfileNames toProfileNames(StudentProfile profile) {
        return new ProfileNames(profile.getFirstName(), profile.getLastName());
    }

    /**
     * Извлекает имя и фамилию из профиля ментора для копирования в новый профиль.
     *
     * @param profile профиль ментора
     * @return переносимые имя и фамилия
     */
    private ProfileNames toProfileNames(MentorProfile profile) {
        return new ProfileNames(profile.getFirstName(), profile.getLastName());
    }

    /**
     * Упрощённое представление имени и фамилии при переносе между профилями.
     *
     * @param firstName имя
     * @param lastName фамилия
     */
    private record ProfileNames(String firstName, String lastName) {

        /**
         * Возвращает пустой набор имени и фамилии.
         *
         * @return пустые значения имён
         */
        private static ProfileNames empty() {
            return new ProfileNames("", "");
        }
    }
}
