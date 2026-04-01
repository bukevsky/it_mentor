package com.example.it.mentor.mapper;

import com.example.it.mentor.dto.dict.CityResponse;
import com.example.it.mentor.dto.dict.InteractionTypeResponse;
import com.example.it.mentor.dto.dict.LanguageResponse;
import com.example.it.mentor.dto.dict.SkillResponse;
import com.example.it.mentor.entity.dict.DictCity;
import com.example.it.mentor.entity.dict.DictInteractionType;
import com.example.it.mentor.entity.dict.DictLanguage;
import com.example.it.mentor.entity.dict.DictSkill;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DictionaryMapper {

    CityResponse toCityResponse(DictCity city);

    SkillResponse toSkillResponse(DictSkill skill);

    LanguageResponse toLanguageResponse(DictLanguage language);

    InteractionTypeResponse toInteractionTypeResponse(DictInteractionType type);

    List<CityResponse> toCityResponses(List<DictCity> cities);

    List<SkillResponse> toSkillResponses(List<DictSkill> skills);

    List<LanguageResponse> toLanguageResponses(List<DictLanguage> languages);

    List<InteractionTypeResponse> toInteractionTypeResponses(List<DictInteractionType> types);
}
