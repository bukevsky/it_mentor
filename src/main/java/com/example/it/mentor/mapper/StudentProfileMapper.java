package com.example.it.mentor.mapper;

import com.example.it.mentor.dto.student.StudentEducationResponse;
import com.example.it.mentor.dto.student.StudentLanguageResponse;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.dto.student.StudentSkillResponse;
import com.example.it.mentor.entity.StudentEducation;
import com.example.it.mentor.entity.StudentLanguage;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.StudentSkill;
import com.example.it.mentor.entity.enums.EmploymentType;
import com.example.it.mentor.entity.enums.WorkFormat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {DictionaryMapper.class, EnumMapper.class})
public interface StudentProfileMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "employmentTypes", source = "employmentTypes")
    @Mapping(target = "workFormats", source = "workFormats")
    StudentProfileResponse toResponse(StudentProfile profile);

    @Mapping(target = "degree", source = "degree")
    @Mapping(target = "educationForm", source = "educationForm")
    StudentEducationResponse toEducationResponse(StudentEducation edu);

    @Mapping(target = "level", source = "level")
    StudentLanguageResponse toLanguageResponse(StudentLanguage lang);

    @Mapping(target = "level", source = "level")
    StudentSkillResponse toSkillResponse(StudentSkill skill);

    List<StudentEducationResponse> toEducationResponses(Set<StudentEducation> educations);

    List<StudentLanguageResponse> toLanguageResponses(Set<StudentLanguage> languages);

    List<StudentSkillResponse> toSkillResponses(Set<StudentSkill> skills);

    default Set<String> employmentTypesToStrings(Set<EmploymentType> types) {
        if (types == null) return null;
        return types.stream().map(Enum::name).collect(Collectors.toSet());
    }

    default Set<String> workFormatsToStrings(Set<WorkFormat> formats) {
        if (formats == null) return null;
        return formats.stream().map(Enum::name).collect(Collectors.toSet());
    }
}
