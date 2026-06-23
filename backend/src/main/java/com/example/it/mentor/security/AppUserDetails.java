package com.example.it.mentor.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Расширение стандартного {@link User}, содержащее идентификатор пользователя из БД.
 */
public class AppUserDetails extends User {

    private final Long userId;
    private final Long tokenVersion;

    public AppUserDetails(Long userId,
                          String username,
                          String password,
                          boolean enabled,
                          Long tokenVersion,
                          Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.userId = userId;
        this.tokenVersion = tokenVersion;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTokenVersion() {
        return tokenVersion;
    }
}
