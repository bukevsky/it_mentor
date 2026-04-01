package com.example.it.mentor.service;

import com.example.it.mentor.dto.ProfileSummaryResponse;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService")
class ProfileServiceTest {

    @InjectMocks
    private ProfileService service;

    @Mock private UserService userService;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private MentorProfileRepository mentorProfileRepository;

    private static final String TEST_EMAIL = "user@example.com";

    @BeforeEach
    void setUpSecurityContext() {
        var auth = new UsernamePasswordAuthenticationToken(TEST_EMAIL, null, List.of());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getProfileSummary")
    class GetProfileSummary {

        @Test
        @DisplayName("студент с профилем — возвращает STUDENT, profileExists=true")
        void studentWithProfile_shouldReturnStudentSummary() {
            User user = userWithRole(RoleCode.STUDENT);
            StudentProfile profile = StudentProfile.builder().user(user).build();
            ReflectionTestUtils.setField(profile, "id", 10L);

            when(userService.getCurrentUserEntity()).thenReturn(user);
            when(studentProfileRepository.findByUserId(any())).thenReturn(Optional.of(profile));

            ProfileSummaryResponse result = service.getProfileSummary();

            assertThat(result.role()).isEqualTo("STUDENT");
            assertThat(result.profileId()).isEqualTo(10L);
            assertThat(result.profileExists()).isTrue();
            verify(mentorProfileRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("студент без профиля — возвращает STUDENT, profileExists=false")
        void studentWithoutProfile_shouldReturnNoProfile() {
            User user = userWithRole(RoleCode.STUDENT);

            when(userService.getCurrentUserEntity()).thenReturn(user);
            when(studentProfileRepository.findByUserId(any())).thenReturn(Optional.empty());

            ProfileSummaryResponse result = service.getProfileSummary();

            assertThat(result.role()).isEqualTo("STUDENT");
            assertThat(result.profileId()).isNull();
            assertThat(result.profileExists()).isFalse();
        }

        @Test
        @DisplayName("ментор с профилем — возвращает MENTOR, profileExists=true")
        void mentorWithProfile_shouldReturnMentorSummary() {
            User user = userWithRole(RoleCode.MENTOR);
            MentorProfile profile = MentorProfile.builder().user(user).build();
            ReflectionTestUtils.setField(profile, "id", 20L);

            when(userService.getCurrentUserEntity()).thenReturn(user);
            when(mentorProfileRepository.findByUserId(any())).thenReturn(Optional.of(profile));

            ProfileSummaryResponse result = service.getProfileSummary();

            assertThat(result.role()).isEqualTo("MENTOR");
            assertThat(result.profileId()).isEqualTo(20L);
            assertThat(result.profileExists()).isTrue();
            verify(studentProfileRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("ментор без профиля — возвращает MENTOR, profileExists=false")
        void mentorWithoutProfile_shouldReturnNoProfile() {
            User user = userWithRole(RoleCode.MENTOR);

            when(userService.getCurrentUserEntity()).thenReturn(user);
            when(mentorProfileRepository.findByUserId(any())).thenReturn(Optional.empty());

            ProfileSummaryResponse result = service.getProfileSummary();

            assertThat(result.role()).isEqualTo("MENTOR");
            assertThat(result.profileId()).isNull();
            assertThat(result.profileExists()).isFalse();
        }

        @Test
        @DisplayName("админ — возвращает ADMIN, profileExists=false")
        void admin_shouldReturnAdminWithNoProfile() {
            User user = userWithRole(RoleCode.ADMIN);

            when(userService.getCurrentUserEntity()).thenReturn(user);

            ProfileSummaryResponse result = service.getProfileSummary();

            assertThat(result.role()).isEqualTo("ADMIN");
            assertThat(result.profileId()).isNull();
            assertThat(result.profileExists()).isFalse();
        }

        @Test
        @DisplayName("пользователь без ролей — возвращает STUDENT (дефолт)")
        void noRoles_shouldDefaultToStudent() {
            User user = User.builder().email(TEST_EMAIL).build();

            when(userService.getCurrentUserEntity()).thenReturn(user);
            when(studentProfileRepository.findByUserId(any())).thenReturn(Optional.empty());

            ProfileSummaryResponse result = service.getProfileSummary();

            assertThat(result.role()).isEqualTo("STUDENT");
        }
    }

    private User userWithRole(RoleCode code) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "code", code);
        ReflectionTestUtils.setField(role, "name", code.name());
        return User.builder().email(TEST_EMAIL).roles(Set.of(role)).build();
    }
}
