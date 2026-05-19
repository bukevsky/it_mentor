package com.example.it.mentor.security;

import com.example.it.mentor.dto.AdminUserStatusRequest;
import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.UserStatus;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("JWT tokenVersion IT")
class JwtTokenVersionIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private String adminToken;
    private Long targetUserId;
    private String targetEmail;

    @BeforeEach
    void setUp() {
        String adminEmail = "admin_" + uid() + "@test.com";
        targetEmail = "student_" + uid() + "@test.com";

        register(adminEmail);
        grantAdminRole(adminEmail);
        adminToken = login(adminEmail);

        register(targetEmail);
        targetUserId = userRepository.findByEmailAndDeletedFalse(targetEmail).orElseThrow().getId();
    }

    @Test
    @DisplayName("login_thenBlock_oldJwtImmediatelyInvalid")
    void login_thenBlock_oldJwtImmediatelyInvalid() {
        String oldToken = login(targetEmail);
        assertThat(getMe(oldToken).getStatusCode()).isEqualTo(HttpStatus.OK);

        blockUser(targetUserId);

        assertThat(getMe(oldToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("login_thenUnblock_oldJwtRemainsInvalid_newLoginWorks")
    void login_thenUnblock_oldJwtRemainsInvalid_newLoginWorks() {
        String tokenBeforeBlock = login(targetEmail);

        blockUser(targetUserId);
        unblockUser(targetUserId);

        // старый токен по-прежнему инвалиден (tokenVersion не возвращается назад)
        assertThat(getMe(tokenBeforeBlock).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // новый логин работает
        String newToken = login(targetEmail);
        assertThat(getMe(newToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("legacyToken_userNotBumped_remainsValid")
    void legacyToken_userNotBumped_remainsValid() {
        // Генерируем legacy токен без claim 'tv' вручную
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        String legacyToken = Jwts.builder()
                .subject(targetEmail)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000L))
                .signWith(key)
                .compact();

        // tokenVersion у пользователя = 0 (не изменялось), legacy токен должен работать
        assertThat(getMe(legacyToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void blockUser(Long userId) {
        restTemplate.exchange("/admin/users/" + userId + "/status", HttpMethod.PUT,
                bearerRequest(new AdminUserStatusRequest(UserStatus.BLOCKED), adminToken), Void.class);
    }

    private void unblockUser(Long userId) {
        restTemplate.exchange("/admin/users/" + userId + "/status", HttpMethod.PUT,
                bearerRequest(new AdminUserStatusRequest(UserStatus.ACTIVE), adminToken), Void.class);
    }

    private ResponseEntity<Object> getMe(String token) {
        return restTemplate.exchange("/auth/me", HttpMethod.GET, bearerRequest(null, token), Object.class);
    }

    private void register(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123!", "Тест", "Тестов"), Object.class);
    }

    private String login(String email) {
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, "Password123!"), LoginResponse.class);
        return resp.getBody().accessToken();
    }

    private void grantAdminRole(String email) {
        User user = userRepository.findWithRolesByEmailAndDeletedFalse(email).orElseThrow();
        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN).orElseThrow();
        user.getRoles().clear();
        user.getRoles().add(adminRole);
        userRepository.save(user);
        userDetailsService.evictUserCache(email);
    }

    private <T> HttpEntity<T> bearerRequest(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
    }

    private String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
