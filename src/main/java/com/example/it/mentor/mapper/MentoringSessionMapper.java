package com.example.it.mentor.mapper;

import com.example.it.mentor.dto.session.NextSessionSummary;
import com.example.it.mentor.dto.session.SessionResponse;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.MentoringSession;
import com.example.it.mentor.entity.StudentProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MentoringSessionMapper {

    @Mapping(target = "mentoringRequestId", source = "mentoringRequest.id")
    @Mapping(target = "studentUserId", source = "studentUser.id")
    @Mapping(target = "mentorUserId", source = "mentorUser.id")
    @Mapping(target = "studentName", source = "mentoringRequest.studentProfile", qualifiedByName = "studentFullName")
    @Mapping(target = "mentorName", source = "mentoringRequest.mentorProfile", qualifiedByName = "mentorFullName")
    SessionResponse toResponse(MentoringSession session);

    default NextSessionSummary toSummary(MentoringSession session, Long currentUserId) {
        if (session == null) {
            return null;
        }
        MentoringRequest request = session.getMentoringRequest();
        boolean currentIsStudent = session.getStudentUser() != null
                && currentUserId != null
                && currentUserId.equals(session.getStudentUser().getId());
        String counterpartyName = currentIsStudent
                ? mentorFullName(request != null ? request.getMentorProfile() : null)
                : studentFullName(request != null ? request.getStudentProfile() : null);
        return new NextSessionSummary(
                session.getId(),
                request != null ? request.getId() : null,
                session.getScheduledAt(),
                session.getDurationMinutes(),
                counterpartyName
        );
    }

    @Named("studentFullName")
    default String studentFullName(StudentProfile profile) {
        if (profile == null) {
            return null;
        }
        return joinName(profile.getFirstName(), profile.getLastName());
    }

    @Named("mentorFullName")
    default String mentorFullName(MentorProfile profile) {
        if (profile == null) {
            return null;
        }
        return joinName(profile.getFirstName(), profile.getLastName());
    }

    private static String joinName(String first, String last) {
        String f = first == null ? "" : first.trim();
        String l = last == null ? "" : last.trim();
        String full = (f + " " + l).trim();
        return full.isEmpty() ? null : full;
    }
}
