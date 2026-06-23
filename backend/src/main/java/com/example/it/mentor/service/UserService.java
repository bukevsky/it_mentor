package com.example.it.mentor.service;

import com.example.it.mentor.dto.UserInfoResponse;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.AuthMapper;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис работы с пользователями и текущим контекстом аутентификации.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final FileStorage fileStorage;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Ищет пользователя по email вместе с ролями.
     *
     * @param email email пользователя
     * @return найденный пользователь
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findWithRolesByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    /**
     * Проверяет наличие активного пользователя с указанным email.
     *
     * @param email email пользователя
     * @return {@code true}, если пользователь существует
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndDeletedFalse(email);
    }

    /**
     * Ищет пользователя по email без выброса исключения.
     *
     * @param email email пользователя
     * @return optional с найденным пользователем
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmailOptional(String email) {
        return userRepository.findByEmailAndDeletedFalse(email);
    }

    /**
     * Сохраняет пользователя и сбрасывает кэш security-деталей.
     *
     * @param user пользователь для сохранения
     * @return сохранённая сущность
     */
    @Transactional
    public User save(User user) {
        User saved = userRepository.save(user);
        userDetailsService.evictUserCache(saved.getEmail());
        log.debug("Пользователь сохранён: userId={}, status={}, step={}",
                saved.getId(), saved.getStatus(), "user_saved");
        return saved;
    }

    /**
     * Привязывает аватар к пользователю после проверки владения файлом.
     *
     * @param userId идентификатор пользователя
     * @param fileId идентификатор файла аватара
     */
    @Transactional
    public void linkAvatar(Long userId, Long fileId) {
        fileStorage.requireOwned(fileId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        user.setAvatarFileId(fileId);
        save(user);
        log.info("Аватар привязан к пользователю: userId={}, fileId={}, step={}",
                userId, fileId, "avatar_linked");
    }

    /**
     * Возвращает информацию о текущем аутентифицированном пользователе.
     *
     * @return DTO текущего пользователя
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser() {
        String email = currentEmail();
        User user = findByEmail(email);
        log.debug("Текущий пользователь загружен: userId={}, step={}",
                user.getId(), "current_user_loaded");
        return authMapper.toUserInfoResponse(user);
    }

    /**
     * Возвращает сущность текущего аутентифицированного пользователя.
     * Централизованный метод для устранения дублирования SecurityContextHolder во всех сервисах.
     */
    @Transactional(readOnly = true)
    public User getCurrentUserEntity() {
        return findByEmail(currentEmail());
    }

    /**
     * Извлекает email текущего пользователя из {@link SecurityContextHolder}.
     *
     * @return email текущего пользователя
     */
    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
