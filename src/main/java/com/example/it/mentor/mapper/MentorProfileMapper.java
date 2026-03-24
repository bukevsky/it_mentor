package com.example.it.mentor.mapper;

import com.example.it.mentor.dto.mentor.MentorCardResponse;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentor.MentorSkillResponse;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentorSkill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {DictionaryMapper.class, EnumMapper.class})
public interface MentorProfileMapper {

    @Mapping(target = "userId", source = "user.id")
    MentorProfileResponse toResponse(MentorProfile profile);

    MentorSkillResponse toSkillResponse(MentorSkill skill);

    List<MentorSkillResponse> toSkillResponses(Set<MentorSkill> skills);

    @Mapping(target = "userId", source = "user.id")
    MentorCardResponse toCardResponse(MentorProfile profile);
}
