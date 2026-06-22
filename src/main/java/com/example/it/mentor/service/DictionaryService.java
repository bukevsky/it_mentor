package com.example.it.mentor.service;

import com.example.it.mentor.dto.dict.CityResponse;
import com.example.it.mentor.dto.dict.InteractionTypeResponse;
import com.example.it.mentor.dto.dict.LanguageResponse;
import com.example.it.mentor.dto.dict.SkillResponse;
import com.example.it.mentor.mapper.DictionaryMapper;
import com.example.it.mentor.repository.DictCityRepository;
import com.example.it.mentor.repository.DictInteractionTypeRepository;
import com.example.it.mentor.repository.DictLanguageRepository;
import com.example.it.mentor.repository.DictSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис чтения справочных данных.
 *
 * <p>Возвращает только активные записи и кэширует результат для повторных запросов.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DictionaryService {

    private final DictCityRepository cityRepository;
    private final DictSkillRepository skillRepository;
    private final DictLanguageRepository languageRepository;
    private final DictInteractionTypeRepository interactionTypeRepository;
    private final DictionaryMapper dictionaryMapper;

    /**
     * Возвращает список активных городов.
     *
     * @return города, отсортированные по имени
     */
    @Cacheable(value = "dictionaries", key = "'cities'")
    public List<CityResponse> getCities() {
        List<CityResponse> cities = dictionaryMapper.toCityResponses(cityRepository.findByActiveTrueOrderByNameAsc());
        logDictionaryRead("cities", cities.size());
        return cities;
    }

    /**
     * Возвращает список активных навыков.
     *
     * @return навыки, отсортированные по имени
     */
    @Cacheable(value = "dictionaries", key = "'skills'")
    public List<SkillResponse> getSkills() {
        List<SkillResponse> skills = dictionaryMapper.toSkillResponses(skillRepository.findByActiveTrueOrderByNameAsc());
        logDictionaryRead("skills", skills.size());
        return skills;
    }

    /**
     * Возвращает список активных языков.
     *
     * @return языки, отсортированные по имени
     */
    @Cacheable(value = "dictionaries", key = "'languages'")
    public List<LanguageResponse> getLanguages() {
        List<LanguageResponse> languages = dictionaryMapper.toLanguageResponses(languageRepository.findByActiveTrueOrderByNameAsc());
        logDictionaryRead("languages", languages.size());
        return languages;
    }

    /**
     * Возвращает список активных форматов взаимодействия.
     *
     * @return типы взаимодействия, отсортированные по имени
     */
    @Cacheable(value = "dictionaries", key = "'interactionTypes'")
    public List<InteractionTypeResponse> getInteractionTypes() {
        List<InteractionTypeResponse> interactionTypes = dictionaryMapper.toInteractionTypeResponses(
                interactionTypeRepository.findByActiveTrueOrderByNameAsc());
        logDictionaryRead("interactionTypes", interactionTypes.size());
        return interactionTypes;
    }

    private void logDictionaryRead(String dictionary, int resultCount) {
        log.debug("Справочник загружен: dictionary={}, resultCount={}, step={}",
                dictionary, resultCount, "dictionary_loaded");
    }
}
