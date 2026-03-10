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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    @InjectMocks
    private DictionaryService dictionaryService;

    @Mock private DictCityRepository cityRepository;
    @Mock private DictSkillRepository skillRepository;
    @Mock private DictLanguageRepository languageRepository;
    @Mock private DictInteractionTypeRepository interactionTypeRepository;
    @Mock private DictionaryMapper dictionaryMapper;

    // ── getCities ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCities: возвращает отмапленный список активных городов")
    void getCities_happyPath_shouldReturnMappedList() {
        var city = DictCity.builder().name("Москва").country("Россия").build();
        var response = new CityResponse(1L, "Москва", null, "Россия");

        when(cityRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(city));
        when(dictionaryMapper.toCityResponses(List.of(city))).thenReturn(List.of(response));

        var result = dictionaryService.getCities();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Москва");
        verify(cityRepository).findByActiveTrueOrderByNameAsc();
        verify(dictionaryMapper).toCityResponses(List.of(city));
    }

    @Test
    @DisplayName("getCities: пустой репозиторий → пустой список")
    void getCities_emptyRepository_shouldReturnEmptyList() {
        when(cityRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(dictionaryMapper.toCityResponses(List.of())).thenReturn(List.of());

        var result = dictionaryService.getCities();

        assertThat(result).isEmpty();
    }

    // ── getSkills ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSkills: возвращает отмапленный список активных навыков")
    void getSkills_happyPath_shouldReturnMappedList() {
        var skill = DictSkill.builder().name("Java").category("Backend").build();
        var response = new SkillResponse(1L, "Java", "Backend");

        when(skillRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(skill));
        when(dictionaryMapper.toSkillResponses(List.of(skill))).thenReturn(List.of(response));

        var result = dictionaryService.getSkills();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Java");
        assertThat(result.get(0).category()).isEqualTo("Backend");
        verify(skillRepository).findByActiveTrueOrderByNameAsc();
    }

    // ── getLanguages ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLanguages: возвращает отмапленный список активных языков")
    void getLanguages_happyPath_shouldReturnMappedList() {
        var language = DictLanguage.builder().name("Английский").code("en").build();
        var response = new LanguageResponse(1L, "Английский", "en");

        when(languageRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(language));
        when(dictionaryMapper.toLanguageResponses(List.of(language))).thenReturn(List.of(response));

        var result = dictionaryService.getLanguages();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("en");
        verify(languageRepository).findByActiveTrueOrderByNameAsc();
    }

    // ── getInteractionTypes ───────────────────────────────────────────────────

    @Test
    @DisplayName("getInteractionTypes: возвращает отмапленный список типов взаимодействия")
    void getInteractionTypes_happyPath_shouldReturnMappedList() {
        var type = DictInteractionType.builder().name("Онлайн").description("Удалённый формат").build();
        var response = new InteractionTypeResponse(1L, "Онлайн", "Удалённый формат");

        when(interactionTypeRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(type));
        when(dictionaryMapper.toInteractionTypeResponses(List.of(type))).thenReturn(List.of(response));

        var result = dictionaryService.getInteractionTypes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Онлайн");
        assertThat(result.get(0).description()).isEqualTo("Удалённый формат");
        verify(interactionTypeRepository).findByActiveTrueOrderByNameAsc();
    }

    @Test
    @DisplayName("getInteractionTypes: несколько типов → возвращает все")
    void getInteractionTypes_multipleItems_shouldReturnAll() {
        var types = List.of(
                DictInteractionType.builder().name("Онлайн").build(),
                DictInteractionType.builder().name("Оффлайн").build()
        );
        var responses = List.of(
                new InteractionTypeResponse(1L, "Онлайн", null),
                new InteractionTypeResponse(2L, "Оффлайн", null)
        );

        when(interactionTypeRepository.findByActiveTrueOrderByNameAsc()).thenReturn(types);
        when(dictionaryMapper.toInteractionTypeResponses(types)).thenReturn(responses);

        var result = dictionaryService.getInteractionTypes();

        assertThat(result)
                .hasSize(2)
                .extracting(InteractionTypeResponse::name)
                .containsExactly("Онлайн", "Оффлайн");
    }
}
