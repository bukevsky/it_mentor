package com.example.it.mentor.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {

    private final JwtProvider jwtProvider;
    private final UserDetailsServiceImpl userDetailsService;

    public Authentication authenticate(String token) {
        if (!jwtProvider.validateToken(token)) {
            throw new BadCredentialsException("Невалидный JWT");
        }

        String email = jwtProvider.extractUsername(token);
        AppUserDetails userDetails = (AppUserDetails) userDetailsService.loadUserByUsername(email);
        if (!userDetails.isEnabled()) {
            throw new BadCredentialsException("Пользователь заблокирован или удалён");
        }

        Long jwtTokenVersion = jwtProvider.extractTokenVersion(token);
        Long userTokenVersion = userDetails.getTokenVersion();
        boolean versionMatches = jwtTokenVersion == null
                ? userTokenVersion == 0L
                : jwtTokenVersion.equals(userTokenVersion);
        if (!versionMatches) {
            throw new BadCredentialsException("JWT инвалидирован изменением tokenVersion");
        }

        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }
}
