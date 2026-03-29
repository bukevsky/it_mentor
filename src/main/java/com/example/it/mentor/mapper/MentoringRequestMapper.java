package com.example.it.mentor.mapper;

import com.example.it.mentor.dto.mentoring.MentorProfileShortResponse;
import com.example.it.mentor.dto.mentoring.MentoringRequestResponse;
import com.example.it.mentor.dto.mentoring.StudentProfileShortResponse;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.StudentProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EnumMapper.class})
public interface MentoringRequestMapper {

    @Mapping(target = "studentProfileId", source = "studentProfile.id")
    @Mapping(target = "mentorProfileId",  source = "mentorProfile.id")
    MentoringRequestResponse toResponse(MentoringRequest request);

    StudentProfileShortResponse toStudentShort(StudentProfile profile);

    MentorProfileShortResponse toMentorShort(MentorProfile profile);
}
