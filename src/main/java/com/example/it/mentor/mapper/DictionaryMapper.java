package com.example.it.mentor.mapper;

import com.example.it.mentor.dto.dict.CityResponse;
import com.example.it.mentor.dto.dict.CreateCityRequest;
import com.example.it.mentor.dto.dict.CreateInteractionTypeRequest;
import com.example.it.mentor.dto.dict.CreateLanguageRequest;
import com.example.it.mentor.dto.dict.CreateSkillRequest;
import com.example.it.mentor.dto.dict.InteractionTypeResponse;
import com.example.it.mentor.dto.dict.LanguageResponse;
import com.example.it.mentor.dto.dict.SkillResponse;
import com.example.it.mentor.dto.dict.UpdateCityRequest;
import com.example.it.mentor.dto.dict.UpdateInteractionTypeRequest;
import com.example.it.mentor.dto.dict.UpdateLanguageRequest;
import com.example.it.mentor.dto.dict.UpdateSkillRequest;
import com.example.it.mentor.entity.dict.DictCity;
import com.example.it.mentor.entity.dict.DictInteractionType;
import com.example.it.mentor.entity.dict.DictLanguage;
import com.example.it.mentor.entity.dict.DictSkill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

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

    @Mapping(target = "active", constant = "true")
    DictCity toEntity(CreateCityRequest request);

    void updateFromDto(UpdateCityRequest dto, @MappingTarget DictCity entity);

    @Mapping(target = "active", constant = "true")
    DictSkill toEntity(CreateSkillRequest request);

    void updateFromDto(UpdateSkillRequest dto, @MappingTarget DictSkill entity);

    @Mapping(target = "active", constant = "true")
    DictLanguage toEntity(CreateLanguageRequest request);

    void updateFromDto(UpdateLanguageRequest dto, @MappingTarget DictLanguage entity);

    @Mapping(target = "active", constant = "true")
    DictInteractionType toEntity(CreateInteractionTypeRequest request);

    void updateFromDto(UpdateInteractionTypeRequest dto, @MappingTarget DictInteractionType entity);
}
