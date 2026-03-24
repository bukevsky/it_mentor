package com.example.it.mentor.service;

import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentor.MentorSkillRequest;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.entity.enums.SkillLevel;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.MentorProfileMapper;
import com.example.it.mentor.repository.DictCityRepository;
import com.example.it.mentor.repository.DictSkillRepository;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.MentorSkillRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentorProfileService")
class MentorProfileServiceTest {

    @InjectMocks
    private MentorProfileService service;

    @Mock private MentorProfileRepository profileRepository;
    @Mock private MentorSkillRepository skillRepository;
    @Mock private UserService userService;
    @Mock private DictCityRepository cityRepository;
    @Mock private DictSkillRepository skillRefRepository;
    @Mock private MentorProfileMapper mapper;

    private static final String TEST_EMAIL = "mentor@example.com";
    private User testUser;

    @BeforeEach
    void setUpSecurityContextAndUser() {
        var auth = new UsernamePasswordAuthenticationToken(TEST_EMAIL, null, List.of());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
        testUser = User.builder().email(TEST_EMAIL).build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── upsertProfile ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("upsertProfile")
    class UpsertProfile {

        @Test
        @DisplayName("новый профиль — создаёт запись и возвращает response")
        void newProfile_shouldCreate() {
            MentorProfileRequest request = minimalRequest("Алексей", "Смирнов");
            MentorProfile newProfile = MentorProfile.builder().user(testUser).firstName("Алексей").lastName("Смирнов").build();
            MentorProfileResponse expectedResponse = mockResponse(1L, "Алексей", "Смирнов");

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
            when(profileRepository.save(any())).thenReturn(newProfile);
            when(profileRepository.findWithDetailsByUserId(any())).thenReturn(Optional.of(newProfile));
            when(mapper.toResponse(newProfile)).thenReturn(expectedResponse);

            MentorProfileResponse result = service.upsertProfile(request);

            assertThat(result).isNotNull();
            assertThat(result.firstName()).isEqualTo("Алексей");
            assertThat(result.lastName()).isEqualTo("Смирнов");
            verify(profileRepository, times(2)).save(any(MentorProfile.class));
        }

        @Test
        @DisplayName("существующий профиль — обновляет поля")
        void existingProfile_shouldUpdate() {
            MentorProfile existing = MentorProfile.builder().user(testUser).firstName("Старое").lastName("Имя").build();
            ReflectionTestUtils.setField(existing, "id", 1L);
            MentorProfileRequest request = minimalRequest("Новое", "Имя");
            MentorProfileResponse updatedResponse = mockResponse(1L, "Новое", "Имя");

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.of(existing));
            when(profileRepository.save(any())).thenReturn(existing);
            when(profileRepository.findWithDetailsByUserId(any())).thenReturn(Optional.of(existing));
            when(mapper.toResponse(existing)).thenReturn(updatedResponse);

            MentorProfileResponse result = service.upsertProfile(request);

            assertThat(result.firstName()).as("firstName должен обновиться").isEqualTo("Новое");
            verify(profileRepository).save(existing);
        }

        @Test
        @DisplayName("несуществующий cityId — бросает NotFoundException")
        void cityNotFound_shouldThrowNotFoundException() {
            MentorProfileRequest request = requestWithCity("Иван", "Иванов", 999L);

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
            when(cityRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.upsertProfile(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Город не найден");
        }

        @Test
        @DisplayName("несуществующий skillId — бросает NotFoundException")
        void skillNotFound_shouldThrowNotFoundException() {
            MentorProfileRequest request = requestWithSkill("Иван", "Иванов", 99L);
            MentorProfile profile = MentorProfile.builder().user(testUser).build();

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
            when(profileRepository.save(any())).thenReturn(profile);
            when(skillRefRepository.findAllById(List.of(99L))).thenReturn(List.of());

            assertThatThrownBy(() -> service.upsertProfile(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Навык не найден");
        }

        @Test
        @DisplayName("skills null — пропускает заполнение коллекции, deleteAll всё равно вызывается")
        void nullSkills_shouldSkipCollectionUpdate() {
            MentorProfileRequest request = minimalRequest("Иван", "Иванов");
            MentorProfile profile = MentorProfile.builder().user(testUser).build();
            MentorProfileResponse response = mockResponse(1L, "Иван", "Иванов");

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
            when(profileRepository.save(any())).thenReturn(profile);
            when(profileRepository.findWithDetailsByUserId(any())).thenReturn(Optional.of(profile));
            when(mapper.toResponse(profile)).thenReturn(response);

            assertThatNoException().isThrownBy(() -> service.upsertProfile(request));
            verify(skillRepository).deleteAllByMentorProfile(profile);
        }

        @Test
        @DisplayName("recruitmentStatus передан — устанавливается в профиле")
        void withRecruitmentStatus_shouldSetStatus() {
            MentorProfileRequest request = requestWithStatus("Иван", "Иванов", RecruitmentStatus.CLOSED);
            MentorProfile profile = MentorProfile.builder().user(testUser).build();
            MentorProfileResponse response = mockResponse(1L, "Иван", "Иванов");

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenReturn(profile);
            when(profileRepository.findWithDetailsByUserId(any())).thenReturn(Optional.of(profile));
            when(mapper.toResponse(profile)).thenReturn(response);

            service.upsertProfile(request);

            assertThat(profile.getRecruitmentStatus()).isEqualTo(RecruitmentStatus.CLOSED);
        }

        @Test
        @DisplayName("recruitmentStatus null — сбрасывается к дефолту OPEN")
        void upsertProfile_recruitmentStatusNull_shouldDefaultToOpen() {
            MentorProfileRequest request = minimalRequest("Иван", "Иванов");
            MentorProfile profile = MentorProfile.builder().user(testUser)
                    .recruitmentStatus(RecruitmentStatus.PAUSED).build();
            MentorProfileResponse response = mockResponse(1L, "Иван", "Иванов");

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenReturn(profile);
            when(profileRepository.findWithDetailsByUserId(any())).thenReturn(Optional.of(profile));
            when(mapper.toResponse(profile)).thenReturn(response);

            service.upsertProfile(request);

            assertThat(profile.getRecruitmentStatus())
                    .as("Статус должен быть сброшен к дефолту OPEN при null")
                    .isEqualTo(RecruitmentStatus.OPEN);
        }
    }

    // ── getMyProfile ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyProfile")
    class GetMyProfile {

        @Test
        @DisplayName("профиль найден — возвращает response")
        void profileExists_shouldReturn() {
            MentorProfile profile = MentorProfile.builder().user(testUser).firstName("Дмитрий").build();
            MentorProfileResponse response = mockResponse(1L, "Дмитрий", "Козлов");

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findWithDetailsByUserId(any())).thenReturn(Optional.of(profile));
            when(mapper.toResponse(profile)).thenReturn(response);

            MentorProfileResponse result = service.getMyProfile();

            assertThat(result).isNotNull();
            assertThat(result.firstName()).isEqualTo("Дмитрий");
            verify(profileRepository).findWithDetailsByUserId(any());
        }

        @Test
        @DisplayName("профиль не найден — бросает NotFoundException")
        void profileNotFound_shouldThrowNotFoundException() {
            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findWithDetailsByUserId(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMyProfile())
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Профиль ментора не найден");
        }
    }

    // ── getProfileById ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProfileById")
    class GetProfileById {

        @Test
        @DisplayName("профиль найден по id — возвращает response")
        void exists_shouldReturn() {
            MentorProfile profile = MentorProfile.builder().user(testUser).firstName("Ольга").build();
            MentorProfileResponse response = mockResponse(7L, "Ольга", "Новикова");

            when(profileRepository.findWithDetailsById(7L)).thenReturn(Optional.of(profile));
            when(mapper.toResponse(profile)).thenReturn(response);

            MentorProfileResponse result = service.getProfileById(7L);

            assertThat(result.id()).isEqualTo(7L);
            assertThat(result.firstName()).isEqualTo("Ольга");
        }

        @ParameterizedTest(name = "id={0}")
        @ValueSource(longs = {1L, 999L, Long.MAX_VALUE})
        @DisplayName("несуществующий id — бросает NotFoundException")
        void notFound_shouldThrowNotFoundException(long id) {
            when(profileRepository.findWithDetailsById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProfileById(id))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Профиль ментора не найден");
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private MentorProfileRequest minimalRequest(String firstName, String lastName) {
        return new MentorProfileRequest(firstName, lastName, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private MentorProfileRequest requestWithCity(String firstName, String lastName, Long cityId) {
        return new MentorProfileRequest(firstName, lastName, null, null, null,
                cityId, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private MentorProfileRequest requestWithSkill(String firstName, String lastName, Long skillId) {
        return new MentorProfileRequest(firstName, lastName, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                List.of(new MentorSkillRequest(skillId, SkillLevel.INTERMEDIATE)));
    }

    private MentorProfileRequest requestWithStatus(String firstName, String lastName, RecruitmentStatus status) {
        return new MentorProfileRequest(firstName, lastName, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, status, null);
    }

    private MentorProfileResponse mockResponse(Long id, String firstName, String lastName) {
        return new MentorProfileResponse(id, null, firstName, lastName, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, List.of());
    }
}
