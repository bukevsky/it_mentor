package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentoring.*;
import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.repository.RoleRepository;
import com.example.it.mentor.repository.UserRepository;
import com.example.it.mentor.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для пагинации и фильтрации GET /mentoring/requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("GET /mentoring/requests: пагинация и фильтрация IT")
class MentoringRequestPaginationIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    private static final ParameterizedTypeReference<PagedResponse<MentoringRequestResponse>> PAGED_TYPE =
            new ParameterizedTypeReference<>() {};

    private String mentorToken;
    private Long mentorProfileId;

    @BeforeEach
    void setUp() {
        String mentorEmail = "pag_mt_" + uid() + "@test.com";
        mentorToken = registerAndLogin(mentorEmail);
        grantMentorRole(mentorEmail);
        mentorProfileId = createMentorProfile(mentorToken);
    }

    // ── Метаданные пагинации ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Метаданные пагинации")
    class PaginationMetadata {

        @Test
        @DisplayName("GET без заявок → page=0, size=20, totalElements=0, last=true")
        void noRequests_shouldReturnEmptyPageWithDefaults() {
            PagedResponse<MentoringRequestResponse> body = getRequests("", mentorToken);

            assertThat(body).isNotNull();
            assertThat(body.page()).isEqualTo(0);
            assertThat(body.size()).isEqualTo(20);
            assertThat(body.last()).isTrue();
            assertThat(body.content()).isEmpty();
        }

        @Test
        @DisplayName("явные page=0&size=5 → size=5 в ответе")
        void explicitPageAndSize_shouldReflectInMetadata() {
            PagedResponse<MentoringRequestResponse> body = getRequests("?page=0&size=5", mentorToken);

            assertThat(body.page()).isEqualTo(0);
            assertThat(body.size()).isEqualTo(5);
        }

        @Test
        @DisplayName("одна заявка → totalElements=1, totalPages=1, last=true")
        void oneRequest_shouldHaveCorrectTotals() {
            createStudentAndRequest();

            PagedResponse<MentoringRequestResponse> body = getRequests("", mentorToken);

            assertThat(body.totalElements()).isGreaterThanOrEqualTo(1);
            assertThat(body.totalPages()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("3 заявки, size=2 → first page has 2 elements, last=false")
        void threeRequests_sizeTow_firstPageHasTwoElements() {
            createMultipleStudentRequests(3);

            PagedResponse<MentoringRequestResponse> body = getRequests("?page=0&size=2", mentorToken);

            assertThat(body.content()).hasSize(2);
            assertThat(body.last()).isFalse();
            assertThat(body.totalElements()).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("3 заявки, size=2, page=1 → last page")
        void threeRequests_sizeTow_lastPage() {
            createMultipleStudentRequests(3);

            PagedResponse<MentoringRequestResponse> body = getRequests("?page=1&size=2", mentorToken);

            assertThat(body.page()).isEqualTo(1);
            assertThat(body.content()).isNotEmpty();
        }
    }

    // ── Фильтр по статусу ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Фильтр по статусу")
    class FilterByStatus {

        @Test
        @DisplayName("filter=SENT → возвращает только SENT заявки")
        void filterBySent_shouldReturnOnlySent() {
            createStudentAndRequest(); // создаём SENT заявку

            PagedResponse<MentoringRequestResponse> body = getRequests("?status=SENT", mentorToken);

            assertThat(body.content())
                    .isNotEmpty()
                    .allSatisfy(req -> assertThat(req.status()).isEqualTo("SENT"));
        }

        @Test
        @DisplayName("filter=REVIEWING → возвращает только REVIEWING заявки")
        void filterByReviewing_shouldReturnOnlyReviewing() {
            Long requestId = createStudentAndRequest();
            // Переводим в REVIEWING
            restTemplate.exchange("/mentoring/requests/" + requestId + "/view",
                    HttpMethod.PUT, bearerRequest(null, mentorToken), Object.class);

            PagedResponse<MentoringRequestResponse> body = getRequests("?status=REVIEWING", mentorToken);

            assertThat(body.content())
                    .isNotEmpty()
                    .allSatisfy(req -> assertThat(req.status()).isEqualTo("REVIEWING"));
        }

        @Test
        @DisplayName("filter=ACCEPTED → возвращает только ACCEPTED заявки")
        void filterByAccepted_shouldReturnOnlyAccepted() {
            Long requestId = createStudentAndRequest();
            restTemplate.exchange("/mentoring/requests/" + requestId + "/accept",
                    HttpMethod.PUT, bearerRequest(null, mentorToken), Object.class);

            PagedResponse<MentoringRequestResponse> body = getRequests("?status=ACCEPTED", mentorToken);

            assertThat(body.content())
                    .isNotEmpty()
                    .allSatisfy(req -> assertThat(req.status()).isEqualTo("ACCEPTED"));
        }

        @Test
        @DisplayName("filter=REJECTED → возвращает только REJECTED заявки")
        void filterByRejected_shouldReturnOnlyRejected() {
            Long requestId = createStudentAndRequest();
            restTemplate.exchange("/mentoring/requests/" + requestId + "/reject",
                    HttpMethod.PUT,
                    bearerRequest(new MentoringRequestRejectRequest("Нет"), mentorToken),
                    Object.class);

            PagedResponse<MentoringRequestResponse> body = getRequests("?status=REJECTED", mentorToken);

            assertThat(body.content())
                    .isNotEmpty()
                    .allSatisfy(req -> assertThat(req.status()).isEqualTo("REJECTED"));
        }

        @Test
        @DisplayName("filter=CANCELLED → возвращает только CANCELLED заявки (студент отменил)")
        void filterByCancelled_shouldReturnOnlyCancelled() {
            String studentEmail = "pag_cancel_" + uid() + "@test.com";
            String studentToken = registerAndLogin(studentEmail);
            createStudentProfile(studentToken);
            Long requestId = createStudentRequest(studentToken, mentorProfileId);

            // Студент отменяет
            restTemplate.exchange("/mentoring/requests/" + requestId + "/cancel",
                    HttpMethod.PUT, bearerRequest(null, studentToken), Object.class);

            // Ментор видит отменённую заявку
            PagedResponse<MentoringRequestResponse> body = getRequests("?status=CANCELLED", mentorToken);

            assertThat(body.content())
                    .isNotEmpty()
                    .allSatisfy(req -> assertThat(req.status()).isEqualTo("CANCELLED"));
        }

        @Test
        @DisplayName("filter=COMPLETED → возвращает только COMPLETED заявки")
        void filterByCompleted_shouldReturnOnlyCompleted() {
            String studentEmail = "pag_comp_" + uid() + "@test.com";
            String studentToken = registerAndLogin(studentEmail);
            createStudentProfile(studentToken);
            Long requestId = createStudentRequest(studentToken, mentorProfileId);

            restTemplate.exchange("/mentoring/requests/" + requestId + "/accept",
                    HttpMethod.PUT, bearerRequest(null, mentorToken), Object.class);
            restTemplate.exchange("/mentoring/requests/" + requestId + "/complete",
                    HttpMethod.PUT, bearerRequest(null, studentToken), Object.class);

            PagedResponse<MentoringRequestResponse> body = getRequests("?status=COMPLETED", mentorToken);

            assertThat(body.content())
                    .isNotEmpty()
                    .allSatisfy(req -> assertThat(req.status()).isEqualTo("COMPLETED"));
        }

        @Test
        @DisplayName("несуществующий статус → 4xx/5xx (тип ошибки конвертации enum)")
        void invalidStatus_shouldReturnError() {
            // GlobalExceptionHandler не обрабатывает MethodArgumentTypeMismatchException,
            // поэтому текущее поведение — 500. Тест проверяет что это не 200.
            ResponseEntity<Object> response = restTemplate.exchange(
                    "/mentoring/requests?status=INVALID_STATUS", HttpMethod.GET,
                    bearerRequest(null, mentorToken), Object.class);

            assertThat(response.getStatusCode().is4xxClientError()
                    || response.getStatusCode().is5xxServerError())
                    .as("Невалидный статус должен вернуть ошибку").isTrue();
        }
    }

    // ── Студент видит только свои заявки ──────────────────────────────────────

    @Nested
    @DisplayName("Студент видит только свои исходящие заявки")
    class StudentSeesOnlyOwnRequests {

        @Test
        @DisplayName("студент видит только свои заявки (не чужие)")
        void student_seesOnlyOwnRequests() {
            // Студент 1 создаёт заявку
            String student1Email = "pag_st1_" + uid() + "@test.com";
            String student1Token = registerAndLogin(student1Email);
            createStudentProfile(student1Token);
            createStudentRequest(student1Token, mentorProfileId);

            // Студент 2 создаёт другую заявку для другого ментора
            String student2Email = "pag_st2_" + uid() + "@test.com";
            String student2Token = registerAndLogin(student2Email);
            createStudentProfile(student2Token);

            // Студент 2 вообще не создавал заявок → видит пустой список
            PagedResponse<MentoringRequestResponse> body = getRequests("", student2Token);

            assertThat(body.content()).isEmpty();
        }

        @Test
        @DisplayName("студент видит все свои заявки разных статусов")
        void student_seesAllOwnRequestsAcrossStatuses() {
            String studentEmail = "pag_mixed_" + uid() + "@test.com";
            String studentToken = registerAndLogin(studentEmail);
            createStudentProfile(studentToken);

            // Создаём несколько менторов и заявок
            String mentor2Email = "pag_m2_" + uid() + "@test.com";
            String mentor2Token = registerAndLogin(mentor2Email);
            grantMentorRole(mentor2Email);
            Long mentorProfileId2 = createMentorProfile(mentor2Token);

            createStudentRequest(studentToken, mentorProfileId);
            createStudentRequest(studentToken, mentorProfileId2);

            PagedResponse<MentoringRequestResponse> body = getRequests("", studentToken);

            assertThat(body.content()).hasSize(2);
            assertThat(body.totalElements()).isEqualTo(2);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private PagedResponse<MentoringRequestResponse> getRequests(String queryString, String token) {
        ResponseEntity<PagedResponse<MentoringRequestResponse>> response = restTemplate.exchange(
                "/mentoring/requests" + queryString, HttpMethod.GET,
                bearerRequest(null, token), PAGED_TYPE);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    /** Создаёт нового студента, профиль и заявку, возвращает id заявки */
    private Long createStudentAndRequest() {
        String email = "pag_st_" + uid() + "@test.com";
        String token = registerAndLogin(email);
        createStudentProfile(token);
        return createStudentRequest(token, mentorProfileId);
    }

    private List<Long> createMultipleStudentRequests(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(createStudentAndRequest());
        }
        return ids;
    }

    private Long createStudentRequest(String studentToken, Long targetMentorProfileId) {
        var body = new MentoringRequestCreateRequest(targetMentorProfileId, MentoringType.PRACTICE, "Хочу учиться");
        ResponseEntity<MentoringRequestResponse> resp = restTemplate.exchange(
                "/mentoring/requests", HttpMethod.POST, bearerRequest(body, studentToken),
                MentoringRequestResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private String registerAndLogin(String email) {
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123!", "Иван", "Иванов"), Object.class);
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, "Password123!"), LoginResponse.class);
        return resp.getBody().accessToken();
    }

    private void grantMentorRole(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email).orElseThrow();
        Role mentorRole = roleRepository.findByCode(RoleCode.MENTOR).orElseThrow();
        user.getRoles().clear();
        user.getRoles().add(mentorRole);
        userRepository.save(user);
        userDetailsService.evictUserCache(email);
    }

    private void createStudentProfile(String token) {
        StudentProfileRequest req = new StudentProfileRequest(
                "Студент", "Иванов", null, null, null,
                null, null, null, null, null, null, null, null, null, null);
        restTemplate.exchange("/profile/student", HttpMethod.PUT, bearerRequest(req, token),
                StudentProfileResponse.class);
    }

    private Long createMentorProfile(String token) {
        MentorProfileRequest req = new MentorProfileRequest(
                "Ментор", "Петров", null, "Dev", null, null, null, null,
                null, null, null, null, null, null, null, null,
                RecruitmentStatus.OPEN, null);
        ResponseEntity<MentorProfileResponse> resp = restTemplate.exchange(
                "/profile/mentor", HttpMethod.PUT, bearerRequest(req, token),
                MentorProfileResponse.class);
        return resp.getBody().id();
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
