package com.example.it.mentor.controller;

import com.example.it.mentor.dto.dict.CityResponse;
import com.example.it.mentor.dto.dict.InteractionTypeResponse;
import com.example.it.mentor.dto.dict.LanguageResponse;
import com.example.it.mentor.dto.dict.SkillResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
class DictionaryControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    // ── GET /dictionaries/cities ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /dictionaries/cities: без токена → 200 и список городов")
    void getCities_withoutToken_shouldReturn200() {
        var response = restTemplate.getForEntity("/dictionaries/cities", CityResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("GET /dictionaries/cities: ответ содержит Москву из сида")
    void getCities_shouldContainSeededMoscow() {
        var response = restTemplate.getForEntity("/dictionaries/cities", CityResponse[].class);

        assertThat(response.getBody())
                .extracting(CityResponse::name)
                .contains("Москва");
    }

    @Test
    @DisplayName("GET /dictionaries/cities: 'Удалённо' не входит в список (формат работы — WorkFormat, не город)")
    void getCities_shouldNotContainUdalenno() {
        var response = restTemplate.getForEntity("/dictionaries/cities", CityResponse[].class);

        assertThat(response.getBody())
                .extracting(CityResponse::name)
                .doesNotContain("Удалённо");
    }

    @Test
    @DisplayName("GET /dictionaries/cities: все города имеют id и name")
    void getCities_allItemsHaveRequiredFields() {
        var response = restTemplate.getForEntity("/dictionaries/cities", CityResponse[].class);

        assertThat(response.getBody())
                .allMatch(city -> city.id() != null && city.name() != null && !city.name().isBlank());
    }

    // ── GET /dictionaries/skills ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /dictionaries/skills: без токена → 200 и список навыков")
    void getSkills_withoutToken_shouldReturn200() {
        var response = restTemplate.getForEntity("/dictionaries/skills", SkillResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("GET /dictionaries/skills: ответ содержит Java из сида")
    void getSkills_shouldContainSeededJava() {
        var response = restTemplate.getForEntity("/dictionaries/skills", SkillResponse[].class);

        assertThat(response.getBody())
                .extracting(SkillResponse::name)
                .contains("Java");
    }

    @Test
    @DisplayName("GET /dictionaries/skills: все навыки имеют id и name")
    void getSkills_allItemsHaveRequiredFields() {
        var response = restTemplate.getForEntity("/dictionaries/skills", SkillResponse[].class);

        assertThat(response.getBody())
                .allMatch(skill -> skill.id() != null && skill.name() != null && !skill.name().isBlank());
    }

    // ── GET /dictionaries/languages ───────────────────────────────────────────

    @Test
    @DisplayName("GET /dictionaries/languages: без токена → 200 и список языков")
    void getLanguages_withoutToken_shouldReturn200() {
        var response = restTemplate.getForEntity("/dictionaries/languages", LanguageResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("GET /dictionaries/languages: ответ содержит английский язык (en)")
    void getLanguages_shouldContainEnglish() {
        var response = restTemplate.getForEntity("/dictionaries/languages", LanguageResponse[].class);

        assertThat(response.getBody())
                .extracting(LanguageResponse::code)
                .contains("en");
    }

    @Test
    @DisplayName("GET /dictionaries/languages: все языки имеют id, name и code")
    void getLanguages_allItemsHaveRequiredFields() {
        var response = restTemplate.getForEntity("/dictionaries/languages", LanguageResponse[].class);

        assertThat(response.getBody())
                .allMatch(lang -> lang.id() != null
                        && lang.name() != null && !lang.name().isBlank()
                        && lang.code() != null && !lang.code().isBlank());
    }

    // ── GET /dictionaries/interaction-types ───────────────────────────────────

    @Test
    @DisplayName("GET /dictionaries/interaction-types: без токена → 200 и список типов")
    void getInteractionTypes_withoutToken_shouldReturn200() {
        var response = restTemplate.getForEntity("/dictionaries/interaction-types", InteractionTypeResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("GET /dictionaries/interaction-types: все типы имеют id и name")
    void getInteractionTypes_allItemsHaveRequiredFields() {
        var response = restTemplate.getForEntity("/dictionaries/interaction-types", InteractionTypeResponse[].class);

        assertThat(response.getBody())
                .allMatch(type -> type.id() != null && type.name() != null && !type.name().isBlank());
    }
}
