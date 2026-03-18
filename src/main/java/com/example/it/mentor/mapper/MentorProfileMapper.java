package com.example.it.mentor.mapper;

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
    @Mapping(target = "mentoringType", source = "mentoringType")
    @Mapping(target = "mentoringChannel", source = "mentoringChannel")
    @Mapping(target = "mentoringDuration", source = "mentoringDuration")
    @Mapping(target = "recruitmentStatus", source = "recruitmentStatus")
    MentorProfileResponse toResponse(MentorProfile profile);

    @Mapping(target = "level", source = "level")
    MentorSkillResponse toSkillResponse(MentorSkill skill);

    List<MentorSkillResponse> toSkillResponses(Set<MentorSkill> skills);
}
