package com.example.it.mentor.controller;

import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.dto.mentor.MentorCardResponse;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorSkillRequest;
import com.example.it.mentor.entity.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("GET /profiles/mentors IT")
class MentorSearchControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final ParameterizedTypeReference<PagedResponse<MentorCardResponse>> PAGED_MENTOR_TYPE =
            new ParameterizedTypeReference<>() {};

    // ── базовые случаи ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Базовые случаи")
    class BasicCases {

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            ResponseEntity<Object> response = restTemplate.getForEntity("/profiles/mentors", Object.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("поиск без фильтров → 200, корректные метаданные пагинации")
        void searchWithoutFilters_shouldReturn200WithPaginationMetadata() {
            String token = registerAndLogin();

            ResponseEntity<PagedResponse<MentorCardResponse>> response = search("", token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            // Тесты разделяют один контейнер БД, поэтому контент может быть непустым.
            // Проверяем только корректность структуры пагинации.
            assertThat(response.getBody().page()).isEqualTo(0);
            assertThat(response.getBody().size()).isEqualTo(20);
            assertThat(response.getBody().totalElements()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("созданный ментор отображается в поиске")
        void createdMentor_shouldAppearInSearch() {
            String token = registerAndLogin();
            createMentorProfile("Поиск", "Первый", token);

            ResponseEntity<PagedResponse<MentorCardResponse>> response = search("", token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .extracting(MentorCardResponse::firstName)
                    .contains("Поиск");
        }
    }

    // ── пагинация ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Пагинация")
    class Pagination {

        @Test
        @DisplayName("size=2, page=0 — возвращает 2 элемента")
        void sizeTwo_shouldReturnTwoElements() {
            String token = registerAndLogin();
            createMentorProfile("Ментор", "Первый", token);
            String token2 = registerAndLogin();
            createMentorProfile("Ментор", "Второй", token2);
            String token3 = registerAndLogin();
            createMentorProfile("Ментор", "Третий", token3);

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?page=0&size=2", token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content()).hasSize(2);
            assertThat(response.getBody().size()).isEqualTo(2);
            assertThat(response.getBody().last()).isFalse();
        }

        @Test
        @DisplayName("size=51 — возвращает 400")
        void invalidSize_51_shouldReturn400() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profiles/mentors?size=51", HttpMethod.GET,
                    bearerRequest(null, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("page=-1 — возвращает 400")
        void invalidPage_negative_shouldReturn400() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profiles/mentors?page=-1", HttpMethod.GET,
                    bearerRequest(null, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("size=1 — валидное значение, возвращает 200")
        void minSize_shouldReturn200() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profiles/mentors?size=1", HttpMethod.GET,
                    bearerRequest(null, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("size=50 — валидное значение, возвращает 200")
        void maxSize_shouldReturn200() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profiles/mentors?size=50", HttpMethod.GET,
                    bearerRequest(null, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ── фильтрация ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Фильтрация по q (имя/фамилия)")
    class FilterByQ {

        @Test
        @DisplayName("q=уникальное имя — находит только совпадающего ментора")
        void q_matchesFirstName_shouldReturnMatchingMentor() {
            String token = registerAndLogin();
            String token2 = registerAndLogin();
            String uniqueName = "Зиновий" + UUID.randomUUID().toString().substring(0, 4);
            createMentorProfile(uniqueName, "Тестов", token);
            createMentorProfile("Другой", "Ментор", token2);

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?q=" + uniqueName, token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .extracting(MentorCardResponse::firstName)
                    .containsOnly(uniqueName);
        }

        @Test
        @DisplayName("q без совпадений — пустой список")
        void q_noMatch_shouldReturnEmpty() {
            String token = registerAndLogin();
            createMentorProfile("Алексей", "Иванов", token);

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?q=НесуществующийЗапрос99999", token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Фильтрация по recruitmentStatus")
    class FilterByRecruitmentStatus {

        @Test
        @DisplayName("recruitmentStatus=CLOSED — возвращает только закрытых менторов")
        void filterByClosedStatus_shouldReturnOnlyClosed() {
            String token = registerAndLogin();
            createMentorProfileWithStatus("Открытый", "Ментор", RecruitmentStatus.OPEN, token);
            String token2 = registerAndLogin();
            createMentorProfileWithStatus("Закрытый", "Ментор", RecruitmentStatus.CLOSED, token2);

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?recruitmentStatus=CLOSED", token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .allSatisfy(card ->
                            assertThat(card.recruitmentStatus()).isEqualTo(RecruitmentStatus.CLOSED.name()));
        }
    }

    @Nested
    @DisplayName("Фильтрация по mentoringType")
    class FilterByMentoringType {

        @Test
        @DisplayName("mentoringType=PRACTICE — возвращает только менторов с типом PRACTICE")
        void filterByMentoringType_shouldReturnMatchingMentors() {
            String token = registerAndLogin();
            createMentorProfileWithType("Практика", "Ментор", MentoringType.PRACTICE, token);
            String token2 = registerAndLogin();
            createMentorProfileWithType("Стажировка", "Ментор", MentoringType.INTERNSHIP, token2);

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?mentoringType=PRACTICE", token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .allSatisfy(card ->
                            assertThat(card.mentoringType()).isEqualTo(MentoringType.PRACTICE.name()));
        }
    }

    @Nested
    @DisplayName("Фильтрация по mentoringChannel")
    class FilterByMentoringChannel {

        @Test
        @DisplayName("mentoringChannel=CALLS — возвращает только менторов с каналом CALLS")
        void filterByMentoringChannel_shouldReturnMatchingMentors() {
            String token = registerAndLogin();
            createMentorProfileWithChannel("Звонки", "Ментор", MentoringChannel.CALLS, token);
            String token2 = registerAndLogin();
            createMentorProfileWithChannel("Чат", "Ментор", MentoringChannel.CHAT, token2);

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?mentoringChannel=CALLS", token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .isNotEmpty()
                    .allSatisfy(card ->
                            assertThat(card.mentoringChannel()).isEqualTo(MentoringChannel.CALLS.name()));
        }
    }

    @Nested
    @DisplayName("Фильтрация по skillIds")
    class FilterBySkillIds {

        @Test
        @DisplayName("skillIds=[1] — возвращает только менторов с этим навыком")
        void filterBySkillId_shouldReturnOnlyMentorsWithThatSkill() {
            String tokenWithSkill = registerAndLogin();
            createMentorProfileWithSkill("СоСкиллом", "Ментор", 1L, tokenWithSkill);
            String tokenWithoutSkill = registerAndLogin();
            createMentorProfile("БезСкилла", "Ментор", tokenWithoutSkill);

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?skillIds=1", tokenWithSkill);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .isNotEmpty()
                    .allSatisfy(card ->
                            assertThat(card.skills())
                                    .extracting(s -> s.skill().id())
                                    .contains(1L));
        }

        @Test
        @DisplayName("skillIds=[999999] (несуществующий навык) — пустой список")
        void filterByNonExistentSkillId_shouldReturnEmpty() {
            String token = registerAndLogin();
            createMentorProfile("Алексей", "Иванов", token);

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?skillIds=999999", token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Фильтрация по q — регистронезависимость")
    class FilterByQCaseInsensitive {

        @Test
        @DisplayName("q в нижнем регистре — находит ментора с именем в верхнем регистре")
        void q_lowercase_shouldMatchUppercaseName() {
            String token = registerAndLogin();
            String uniqueName = "Феликс" + UUID.randomUUID().toString().substring(0, 4);
            createMentorProfile(uniqueName, "Тестов", token);

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?q=" + uniqueName.toLowerCase(), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .extracting(MentorCardResponse::firstName)
                    .contains(uniqueName);
        }

        @Test
        @DisplayName("q по фамилии — находит ментора")
        void q_matchesLastName_shouldReturnMentor() {
            String token = registerAndLogin();
            String uniqueLastName = "Редкофамильный" + UUID.randomUUID().toString().substring(0, 4);
            createMentorProfile("Алексей", uniqueLastName, token);

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?q=" + uniqueLastName, token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .extracting(MentorCardResponse::lastName)
                    .contains(uniqueLastName);
        }
    }

    @Nested
    @DisplayName("Сортировка")
    class Sorting {

        @Test
        @DisplayName("sort=firstName,asc — возвращает 200")
        void sortByFirstNameAsc_shouldReturn200() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profiles/mentors?sort=firstName,asc", HttpMethod.GET,
                    bearerRequest(null, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("sort=неизвестноеПоле — fallback на createdAt, возвращает 200 (не 400)")
        void sortByUnknownField_shouldFallbackAndReturn200() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/profiles/mentors?sort=password,asc", HttpMethod.GET,
                    bearerRequest(null, token), Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("Комбинированные фильтры")
    class CombinedFilters {

        @Test
        @DisplayName("q + mentoringType — находит только совпадающего по обоим критериям")
        void qAndMentoringType_shouldReturnOnlyMatchingBothCriteria() {
            String token = registerAndLogin();
            String uniqueName = "Комбо" + UUID.randomUUID().toString().substring(0, 4);
            createMentorProfileWithType(uniqueName, "Иванов", MentoringType.PRACTICE, token);
            String token2 = registerAndLogin();
            createMentorProfileWithType(uniqueName, "Иванов", MentoringType.INTERNSHIP, token2);

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?q=" + uniqueName + "&mentoringType=PRACTICE", token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content())
                    .allSatisfy(card -> {
                        assertThat(card.firstName()).isEqualTo(uniqueName);
                        assertThat(card.mentoringType()).isEqualTo(MentoringType.PRACTICE.name());
                    });
        }
    }

    // ── структура ответа ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Структура ответа")
    class ResponseStructure {

        @Test
        @DisplayName("ответ содержит все метаданные пагинации")
        void response_shouldContainPaginationMetadata() {
            String token = registerAndLogin();

            ResponseEntity<PagedResponse<MentorCardResponse>> response =
                    search("?page=0&size=10", token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            PagedResponse<MentorCardResponse> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.page()).isEqualTo(0);
            assertThat(body.size()).isEqualTo(10);
            assertThat(body.totalElements()).isGreaterThanOrEqualTo(0);
            assertThat(body.totalPages()).isGreaterThanOrEqualTo(0);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin() {
        String email = "search_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, "Password123"), LoginResponse.class);
        return loginResponse.getBody().accessToken();
    }

    private void createMentorProfile(String firstName, String lastName, String token) {
        MentorProfileRequest request = new MentorProfileRequest(
                firstName, lastName, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        restTemplate.exchange("/profile/mentor", HttpMethod.PUT, bearerRequest(request, token), Object.class);
    }

    private void createMentorProfileWithStatus(String firstName, String lastName,
                                               RecruitmentStatus status, String token) {
        MentorProfileRequest request = new MentorProfileRequest(
                firstName, lastName, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, status, null);
        restTemplate.exchange("/profile/mentor", HttpMethod.PUT, bearerRequest(request, token), Object.class);
    }

    private void createMentorProfileWithType(String firstName, String lastName,
                                             MentoringType type, String token) {
        MentorProfileRequest request = new MentorProfileRequest(
                firstName, lastName, null, null, null,
                null, null, null, null, null, null,
                type, null, null, null, null, null, null);
        restTemplate.exchange("/profile/mentor", HttpMethod.PUT, bearerRequest(request, token), Object.class);
    }

    private void createMentorProfileWithChannel(String firstName, String lastName,
                                                MentoringChannel channel, String token) {
        MentorProfileRequest request = new MentorProfileRequest(
                firstName, lastName, null, null, null,
                null, null, null, null, null, null,
                null, channel, null, null, null, null, null);
        restTemplate.exchange("/profile/mentor", HttpMethod.PUT, bearerRequest(request, token), Object.class);
    }

    private void createMentorProfileWithSkill(String firstName, String lastName,
                                              Long skillId, String token) {
        MentorProfileRequest request = new MentorProfileRequest(
                firstName, lastName, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                List.of(new MentorSkillRequest(skillId, SkillLevel.INTERMEDIATE)));
        restTemplate.exchange("/profile/mentor", HttpMethod.PUT, bearerRequest(request, token), Object.class);
    }

    private ResponseEntity<PagedResponse<MentorCardResponse>> search(String queryString, String token) {
        return restTemplate.exchange(
                "/profiles/mentors" + queryString, HttpMethod.GET,
                bearerRequest(null, token), PAGED_MENTOR_TYPE);
    }

    private <T> HttpEntity<T> bearerRequest(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
    }
}
