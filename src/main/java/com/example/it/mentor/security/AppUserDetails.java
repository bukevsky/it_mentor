package com.example.it.mentor.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Расширение стандартного {@link User}, содержащее идентификатор пользователя из БД.
 */
public class AppUserDetails extends User {

    private final Long userId;

    /**
     * Создаёт security-представление пользователя.
     *
     * @param userId идентификатор пользователя в БД
     * @param username username пользователя
     * @param password захешированный пароль
     * @param enabled признак доступности учётной записи
     * @param authorities список ролей и прав
     */
    public AppUserDetails(Long userId,
                          String username,
                          String password,
                          boolean enabled,
                          Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.userId = userId;
    }

    /**
     * Возвращает идентификатор пользователя в базе данных.
     *
     * @return идентификатор пользователя
     */
    public Long getUserId() {
        return userId;
    }
}
