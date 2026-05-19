package com.example.it.mentor.service;

import com.example.it.mentor.dto.dict.CityResponse;
import com.example.it.mentor.dto.dict.InteractionTypeResponse;
import com.example.it.mentor.dto.dict.LanguageResponse;
import com.example.it.mentor.dto.dict.SkillResponse;
import com.example.it.mentor.entity.dict.DictCity;
import com.example.it.mentor.entity.dict.DictInteractionType;
import com.example.it.mentor.entity.dict.DictLanguage;
import com.example.it.mentor.entity.dict.DictSkill;
import com.example.it.mentor.mapper.DictionaryMapper;
import com.example.it.mentor.repository.DictCityRepository;
import com.example.it.mentor.repository.DictInteractionTypeRepository;
import com.example.it.mentor.repository.DictLanguageRepository;
import com.example.it.mentor.repository.DictSkillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DictionaryService")
class DictionaryServiceTest {

    @InjectMocks
    private DictionaryService dictionaryService;

    @Mock private DictCityRepository cityRepository;
    @Mock private DictSkillRepository skillRepository;
    @Mock private DictLanguageRepository languageRepository;
    @Mock private DictInteractionTypeRepository interactionTypeRepository;
    @Mock private DictionaryMapper dictionaryMapper;

    // ── getCities ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCities")
    class GetCities {

        @Test
        @DisplayName("список городов → возвращает отмапленный список, сортировку делегирует репозиторию")
        void happyPath_shouldReturnMappedList() {
            var city = DictCity.builder().name("Москва").country("Россия").build();
            var response = new CityResponse(1L, "Москва", null, "Россия", true);

            when(cityRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(city));
            when(dictionaryMapper.toCityResponses(List.of(city))).thenReturn(List.of(response));

            var result = dictionaryService.getCities();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Москва");
            assertThat(result.get(0).country()).isEqualTo("Россия");
            verify(cityRepository).findByActiveTrueOrderByNameAsc();
            verify(dictionaryMapper).toCityResponses(List.of(city));
        }

        @Test
        @DisplayName("несколько городов → все возвращаются в порядке из репозитория")
        void multipleCities_shouldReturnAllInRepositoryOrder() {
            var cities = List.of(
                    DictCity.builder().name("Казань").build(),
                    DictCity.builder().name("Москва").build(),
                    DictCity.builder().name("Санкт-Петербург").build()
            );
            var responses = List.of(
                    new CityResponse(1L, "Казань", null, null, true),
                    new CityResponse(2L, "Москва", null, null, true),
                    new CityResponse(3L, "Санкт-Петербург", null, null, true)
            );

            when(cityRepository.findByActiveTrueOrderByNameAsc()).thenReturn(cities);
            when(dictionaryMapper.toCityResponses(cities)).thenReturn(responses);

            var result = dictionaryService.getCities();

            assertThat(result)
                    .hasSize(3)
                    .extracting(CityResponse::name)
                    .containsExactly("Казань", "Москва", "Санкт-Петербург");
        }

        @Test
        @DisplayName("пустой репозиторий → пустой список")
        void emptyRepository_shouldReturnEmptyList() {
            when(cityRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());
            when(dictionaryMapper.toCityResponses(List.of())).thenReturn(List.of());

            var result = dictionaryService.getCities();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("вызывает только активные города (findByActiveTrue)")
        void shouldCallActiveCitiesRepository() {
            when(cityRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());
            when(dictionaryMapper.toCityResponses(any())).thenReturn(List.of());

            dictionaryService.getCities();

            verify(cityRepository).findByActiveTrueOrderByNameAsc();
            verifyNoMoreInteractions(cityRepository);
        }
    }

    // ── getSkills ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSkills")
    class GetSkills {

        @Test
        @DisplayName("список навыков → возвращает отмапленный список с именем и категорией")
        void happyPath_shouldReturnMappedList() {
            var skill = DictSkill.builder().name("Java").category("Backend").build();
            var response = new SkillResponse(1L, "Java", "Backend", true);

            when(skillRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(skill));
            when(dictionaryMapper.toSkillResponses(List.of(skill))).thenReturn(List.of(response));

            var result = dictionaryService.getSkills();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Java");
            assertThat(result.get(0).category()).isEqualTo("Backend");
            verify(skillRepository).findByActiveTrueOrderByNameAsc();
        }

        @Test
        @DisplayName("несколько навыков из разных категорий → все возвращаются")
        void multipleSkills_shouldReturnAll() {
            var skills = List.of(
                    DictSkill.builder().name("Java").category("Backend").build(),
                    DictSkill.builder().name("React").category("Frontend").build(),
                    DictSkill.builder().name("Kotlin").category("Backend").build()
            );
            var responses = List.of(
                    new SkillResponse(1L, "Java", "Backend", true),
                    new SkillResponse(2L, "React", "Frontend", true),
                    new SkillResponse(3L, "Kotlin", "Backend", true)
            );

            when(skillRepository.findByActiveTrueOrderByNameAsc()).thenReturn(skills);
            when(dictionaryMapper.toSkillResponses(skills)).thenReturn(responses);

            var result = dictionaryService.getSkills();

            assertThat(result)
                    .hasSize(3)
                    .extracting(SkillResponse::name)
                    .containsExactly("Java", "React", "Kotlin");
        }

        @Test
        @DisplayName("пустой репозиторий → пустой список")
        void emptyRepository_shouldReturnEmptyList() {
            when(skillRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());
            when(dictionaryMapper.toSkillResponses(List.of())).thenReturn(List.of());

            var result = dictionaryService.getSkills();

            assertThat(result).isEmpty();
        }
    }

    // ── getLanguages ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLanguages")
    class GetLanguages {

        @Test
        @DisplayName("список языков → возвращает отмапленный список с кодом языка")
        void happyPath_shouldReturnMappedListWithCode() {
            var language = DictLanguage.builder().name("Английский").code("en").build();
            var response = new LanguageResponse(1L, "Английский", "en", true);

            when(languageRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(language));
            when(dictionaryMapper.toLanguageResponses(List.of(language))).thenReturn(List.of(response));

            var result = dictionaryService.getLanguages();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Английский");
            assertThat(result.get(0).code()).isEqualTo("en");
            verify(languageRepository).findByActiveTrueOrderByNameAsc();
        }

        @Test
        @DisplayName("несколько языков → все возвращаются с уникальными кодами")
        void multipleLanguages_shouldReturnAllWithUniqueCodes() {
            var languages = List.of(
                    DictLanguage.builder().name("Английский").code("en").build(),
                    DictLanguage.builder().name("Немецкий").code("de").build(),
                    DictLanguage.builder().name("Русский").code("ru").build()
            );
            var responses = List.of(
                    new LanguageResponse(1L, "Английский", "en", true),
                    new LanguageResponse(2L, "Немецкий", "de", true),
                    new LanguageResponse(3L, "Русский", "ru", true)
            );

            when(languageRepository.findByActiveTrueOrderByNameAsc()).thenReturn(languages);
            when(dictionaryMapper.toLanguageResponses(languages)).thenReturn(responses);

            var result = dictionaryService.getLanguages();

            assertThat(result)
                    .hasSize(3)
                    .extracting(LanguageResponse::code)
                    .containsExactlyInAnyOrder("en", "de", "ru");
        }

        @Test
        @DisplayName("пустой репозиторий → пустой список")
        void emptyRepository_shouldReturnEmptyList() {
            when(languageRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());
            when(dictionaryMapper.toLanguageResponses(List.of())).thenReturn(List.of());

            var result = dictionaryService.getLanguages();

            assertThat(result).isEmpty();
        }
    }

    // ── getInteractionTypes ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getInteractionTypes")
    class GetInteractionTypes {

        @Test
        @DisplayName("список типов → возвращает отмапленный список с name и description")
        void happyPath_shouldReturnMappedListWithDescription() {
            var type = DictInteractionType.builder().name("Онлайн").description("Удалённый формат").build();
            var response = new InteractionTypeResponse(1L, "Онлайн", "Удалённый формат", true);

            when(interactionTypeRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(type));
            when(dictionaryMapper.toInteractionTypeResponses(List.of(type))).thenReturn(List.of(response));

            var result = dictionaryService.getInteractionTypes();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Онлайн");
            assertThat(result.get(0).description()).isEqualTo("Удалённый формат");
            verify(interactionTypeRepository).findByActiveTrueOrderByNameAsc();
        }

        @Test
        @DisplayName("несколько типов → возвращает все в порядке из репозитория")
        void multipleItems_shouldReturnAllInOrder() {
            var types = List.of(
                    DictInteractionType.builder().name("Онлайн").build(),
                    DictInteractionType.builder().name("Оффлайн").build()
            );
            var responses = List.of(
                    new InteractionTypeResponse(1L, "Онлайн", null, true),
                    new InteractionTypeResponse(2L, "Оффлайн", null, true)
            );

            when(interactionTypeRepository.findByActiveTrueOrderByNameAsc()).thenReturn(types);
            when(dictionaryMapper.toInteractionTypeResponses(types)).thenReturn(responses);

            var result = dictionaryService.getInteractionTypes();

            assertThat(result)
                    .hasSize(2)
                    .extracting(InteractionTypeResponse::name)
                    .containsExactly("Онлайн", "Оффлайн");
        }

        @Test
        @DisplayName("пустой репозиторий → пустой список")
        void emptyRepository_shouldReturnEmptyList() {
            when(interactionTypeRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());
            when(dictionaryMapper.toInteractionTypeResponses(List.of())).thenReturn(List.of());

            var result = dictionaryService.getInteractionTypes();

            assertThat(result).isEmpty();
        }
    }
}
