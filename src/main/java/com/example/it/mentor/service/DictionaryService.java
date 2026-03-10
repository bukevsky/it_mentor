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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DictionaryService {

    private final DictCityRepository cityRepository;
    private final DictSkillRepository skillRepository;
    private final DictLanguageRepository languageRepository;
    private final DictInteractionTypeRepository interactionTypeRepository;
    private final DictionaryMapper dictionaryMapper;

    public List<CityResponse> getCities() {
        return dictionaryMapper.toCityResponses(cityRepository.findByActiveTrueOrderByNameAsc());
    }

    public List<SkillResponse> getSkills() {
        return dictionaryMapper.toSkillResponses(skillRepository.findByActiveTrueOrderByNameAsc());
    }

    public List<LanguageResponse> getLanguages() {
        return dictionaryMapper.toLanguageResponses(languageRepository.findByActiveTrueOrderByNameAsc());
    }

    public List<InteractionTypeResponse> getInteractionTypes() {
        return dictionaryMapper.toInteractionTypeResponses(interactionTypeRepository.findByActiveTrueOrderByNameAsc());
    }
}
