package com.example.it.mentor.security;

import com.example.it.mentor.entity.UserStatus;
import com.example.it.mentor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Реализация {@link UserDetailsService} для загрузки пользователей из базы данных.
 *
 * <p>Используется Spring Security при аутентификации. Ищет активных (не удалённых)
 * пользователей по email и формирует список прав доступа на основе ролей.
 * Результат кешируется на 60 секунд для снижения нагрузки на БД при highload.</p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Загружает пользователя по email для последующей аутентификации.
     * Заблокированные пользователи получают disabled=true, что предотвращает аутентификацию.
     *
     * @param email адрес электронной почты (используется как username)
     * @return объект {@link UserDetails} с ролями в формате {@code ROLE_<CODE>}
     * @throws UsernameNotFoundException если пользователь не найден или удалён
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userDetails", key = "#email")
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.example.it.mentor.entity.User user = userRepository
                .findWithRolesByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + email));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode().name()))
                .toList();

        boolean enabled = user.getStatus() != UserStatus.BLOCKED
                && user.getStatus() != UserStatus.DELETED;

        return new AppUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                enabled,
                user.getTokenVersion(),
                authorities
        );
    }

    /**
     * Удаляет пользователя из кэша security-деталей.
     *
     * @param email email пользователя
     */
    @CacheEvict(value = "userDetails", key = "#email")
    public void evictUserCache(String email) {
        // Вызывается при изменении пользователя (смена пароля, блокировка, смена ролей)
    }
}
