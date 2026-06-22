package com.example.it.mentor.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationService")
class JwtAuthenticationServiceTest {

    @InjectMocks private JwtAuthenticationService service;

    @Mock private JwtProvider jwtProvider;
    @Mock private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("валидный JWT создаёт Authentication с AppUserDetails")
    void validToken_shouldReturnAuthentication() {
        AppUserDetails details = details(true, 3L);
        when(jwtProvider.validateToken("token")).thenReturn(true);
        when(jwtProvider.extractUsername("token")).thenReturn("user@test.com");
        when(jwtProvider.extractTokenVersion("token")).thenReturn(3L);
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(details);

        Authentication authentication = service.authenticate("token");

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isSameAs(details);
    }

    @Test
    @DisplayName("несовпадающий tokenVersion отклоняется")
    void tokenVersionMismatch_shouldThrowBadCredentials() {
        when(jwtProvider.validateToken("token")).thenReturn(true);
        when(jwtProvider.extractUsername("token")).thenReturn("user@test.com");
        when(jwtProvider.extractTokenVersion("token")).thenReturn(2L);
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(details(true, 3L));

        assertThatThrownBy(() -> service.authenticate("token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("инвалидирован");
    }

    @Test
    @DisplayName("заблокированный пользователь отклоняется")
    void disabledUser_shouldThrowBadCredentials() {
        when(jwtProvider.validateToken("token")).thenReturn(true);
        when(jwtProvider.extractUsername("token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(details(false, 0L));

        assertThatThrownBy(() -> service.authenticate("token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("заблокирован");
    }

    private AppUserDetails details(boolean enabled, Long tokenVersion) {
        return new AppUserDetails(
                15L, "user@test.com", "password", enabled, tokenVersion, List.of());
    }
}
