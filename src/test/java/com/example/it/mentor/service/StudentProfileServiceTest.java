package com.example.it.mentor.service;

import com.example.it.mentor.dto.student.StudentLanguageRequest;
import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.EmploymentType;
import com.example.it.mentor.entity.enums.LanguageLevel;
import com.example.it.mentor.entity.enums.WorkFormat;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.StudentProfileMapper;
import com.example.it.mentor.repository.*;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentProfileService")
class StudentProfileServiceTest {

    @InjectMocks
    private StudentProfileService service;

    @Mock private StudentProfileRepository profileRepository;
    @Mock private StudentEducationRepository educationRepository;
    @Mock private StudentLanguageRepository languageRepository;
    @Mock private StudentSkillRepository skillRepository;
    @Mock private UserService userService;
    @Mock private DictCityRepository cityRepository;
    @Mock private DictLanguageRepository languageRefRepository;
    @Mock private DictSkillRepository skillRefRepository;
    @Mock private StudentProfileMapper mapper;
    @Mock private FileStorage fileStorage;

    private static final String TEST_EMAIL = "student@example.com";
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
        void newProfile_shouldCreateAndReturn() {
            // Arrange
            StudentProfileRequest request = minimalRequest("Иван", "Иванов");
            StudentProfile newProfile = StudentProfile.builder().user(testUser).firstName("Иван").lastName("Иванов").build();
            StudentProfileResponse expectedResponse = mockResponse(1L, "Иван", "Иванов");

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
            when(profileRepository.save(any())).thenReturn(newProfile);
            when(profileRepository.findWithDetailsById(any())).thenReturn(Optional.of(newProfile));
            when(mapper.toResponse(newProfile)).thenReturn(expectedResponse);

            // Act
            StudentProfileResponse result = service.upsertProfile(request);

            // Assert
            assertThat(result)
                    .as("Ответ не должен быть null")
                    .isNotNull();
            assertThat(result.firstName()).isEqualTo("Иван");
            assertThat(result.lastName()).isEqualTo("Иванов");
            // save вызывается дважды: для получения ID (новый профиль) и после замены коллекций
            verify(profileRepository, times(2)).save(any(StudentProfile.class));
        }

        @Test
        @DisplayName("существующий профиль — обновляет поля")
        void existingProfile_shouldUpdateFields() {
            // Arrange
            StudentProfile existing = StudentProfile.builder().user(testUser).firstName("Старое").lastName("Имя").build();
            ReflectionTestUtils.setField(existing, "id", 1L);
            StudentProfileRequest request = minimalRequest("Новое", "Имя");
            StudentProfileResponse updatedResponse = mockResponse(1L, "Новое", "Имя");

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.of(existing));
            when(profileRepository.save(any())).thenReturn(existing);
            when(profileRepository.findWithDetailsById(any())).thenReturn(Optional.of(existing));
            when(mapper.toResponse(existing)).thenReturn(updatedResponse);

            // Act
            StudentProfileResponse result = service.upsertProfile(request);

            // Assert
            assertThat(result.firstName()).as("firstName должен обновиться").isEqualTo("Новое");
            verify(profileRepository).save(existing);
        }

        @Test
        @DisplayName("запрос с city — загружает город из репозитория")
        void withCityId_shouldLoadCityFromRepo() {
            // Arrange
            StudentProfileRequest request = requestWithCity("Алина", "Иванова", 1L);
            StudentProfile profile = StudentProfile.builder().user(testUser).build();

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
            when(cityRepository.findById(1L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> service.upsertProfile(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Город не найден");
        }

        @Test
        @DisplayName("запрос с языком, которого нет — бросает NotFoundException")
        void withMissingLanguage_shouldThrowNotFoundException() {
            // Arrange
            StudentProfileRequest request = requestWithLanguage("Иван", "Иванов", 99L);
            StudentProfile profile = StudentProfile.builder().user(testUser).build();

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
            when(profileRepository.save(any())).thenReturn(profile);
            when(languageRefRepository.findAllById(List.of(99L))).thenReturn(List.of());

            // Act + Assert
            assertThatThrownBy(() -> service.upsertProfile(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Язык не найден");
        }

        @Test
        @DisplayName("запрос с employmentTypes и workFormats — обновляет коллекции")
        void withEnumCollections_shouldUpdateCollections() {
            // Arrange
            StudentProfileRequest request = new StudentProfileRequest(
                    "Иван", "Иванов", null, null, null, null, null, null, null, null,
                    Set.of(EmploymentType.FULL_TIME, EmploymentType.PART_TIME),
                    Set.of(WorkFormat.REMOTE),
                    null, null, null);
            StudentProfile profile = StudentProfile.builder().user(testUser).build();
            StudentProfileResponse response = mockResponse(1L, "Иван", "Иванов");

            // Возвращаем существующий profile из репозитория, чтобы сервис модифицировал именно его
            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenReturn(profile);
            when(profileRepository.findWithDetailsById(any())).thenReturn(Optional.of(profile));
            when(mapper.toResponse(profile)).thenReturn(response);

            // Act
            service.upsertProfile(request);

            // Assert - profile должен содержать переданные типы занятости
            assertThat(profile.getEmploymentTypes())
                    .containsExactlyInAnyOrder(EmploymentType.FULL_TIME, EmploymentType.PART_TIME);
            assertThat(profile.getWorkFormats())
                    .containsExactly(WorkFormat.REMOTE);
        }

        @Test
        @DisplayName("все коллекции null — вызывает delete-репозитории, но пропускает addAll")
        void withNullCollections_shouldCallDeleteButSkipAddAll() {
            // Arrange
            StudentProfileRequest request = minimalRequest("Иван", "Иванов");
            StudentProfile profile = StudentProfile.builder().user(testUser).build();
            StudentProfileResponse response = mockResponse(1L, "Иван", "Иванов");

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
            when(profileRepository.save(any())).thenReturn(profile);
            when(profileRepository.findWithDetailsById(any())).thenReturn(Optional.of(profile));
            when(mapper.toResponse(profile)).thenReturn(response);

            // Act + Assert: не должно упасть, коллекции delete вызываются, но addAll пропускается
            assertThatNoException().isThrownBy(() -> service.upsertProfile(request));
            verify(educationRepository).deleteAllByStudentProfile(profile);
            verify(languageRepository).deleteAllByStudentProfile(profile);
            verify(skillRepository).deleteAllByStudentProfile(profile);
        }
    }

    // ── getMyProfile ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyProfile")
    class GetMyProfile {

        @Test
        @DisplayName("профиль найден — возвращает response")
        void profileExists_shouldReturnResponse() {
            // Arrange
            StudentProfile profile = StudentProfile.builder().user(testUser).firstName("Анна").build();
            StudentProfileResponse response = mockResponse(1L, "Анна", "Иванова");

            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findWithDetailsByUserId(any())).thenReturn(Optional.of(profile));
            when(mapper.toResponse(profile)).thenReturn(response);

            // Act
            StudentProfileResponse result = service.getMyProfile();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.firstName()).isEqualTo("Анна");
            verify(profileRepository).findWithDetailsByUserId(any());
        }

        @Test
        @DisplayName("профиль не найден — бросает NotFoundException с нужным сообщением")
        void profileNotFound_shouldThrowNotFoundException() {
            // Arrange
            when(userService.getCurrentUserEntity()).thenReturn(testUser);
            when(profileRepository.findWithDetailsByUserId(any())).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> service.getMyProfile())
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Профиль студента не найден");
        }

        @Test
        @DisplayName("пользователь не найден — пробрасывает NotFoundException от UserService")
        void userNotFound_shouldPropagateNotFoundException() {
            // Arrange
            when(userService.getCurrentUserEntity())
                    .thenThrow(new NotFoundException("Пользователь не найден"));

            // Act + Assert
            assertThatThrownBy(() -> service.getMyProfile())
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Пользователь не найден");
        }
    }

    // ── getProfileById ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProfileById")
    class GetProfileById {

        @Test
        @DisplayName("профиль найден по id — возвращает response")
        void profileExists_shouldReturnResponse() {
            // Arrange
            StudentProfile profile = StudentProfile.builder().user(testUser).firstName("Пётр").build();
            StudentProfileResponse response = mockResponse(42L, "Пётр", "Сидоров");

            when(profileRepository.findWithDetailsById(42L)).thenReturn(Optional.of(profile));
            when(mapper.toResponse(profile)).thenReturn(response);

            // Act
            StudentProfileResponse result = service.getProfileById(42L);

            // Assert
            assertThat(result.id()).isEqualTo(42L);
            assertThat(result.firstName()).isEqualTo("Пётр");
        }

        @ParameterizedTest(name = "id={0}")
        @ValueSource(longs = {1L, 999L, Long.MAX_VALUE})
        @DisplayName("несуществующий id — бросает NotFoundException")
        void nonExistentId_shouldThrowNotFoundException(long id) {
            // Arrange
            when(profileRepository.findWithDetailsById(id)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> service.getProfileById(id))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Профиль студента не найден");
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private StudentProfileRequest minimalRequest(String firstName, String lastName) {
        return new StudentProfileRequest(firstName, lastName, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null);
    }

    private StudentProfileRequest requestWithCity(String firstName, String lastName, Long cityId) {
        return new StudentProfileRequest(firstName, lastName, null, null, cityId,
                null, null, null, null, null,
                null, null, null, null, null);
    }

    private StudentProfileRequest requestWithLanguage(String firstName, String lastName, Long languageId) {
        return new StudentProfileRequest(firstName, lastName, null, null, null,
                null, null, null, null, null,
                null, null, null,
                List.of(new StudentLanguageRequest(languageId, LanguageLevel.B2)),
                null);
    }

    private StudentProfileResponse mockResponse(Long id, String firstName, String lastName) {
        return new StudentProfileResponse(id, null, firstName, lastName, null, null, null,
                null, null, null, null, null,
                Set.of(), Set.of(), List.of(), List.of(), List.of(), null);
    }
}
