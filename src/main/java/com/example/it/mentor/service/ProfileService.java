package com.example.it.mentor.service;

import com.example.it.mentor.dto.ProfileSummaryResponse;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserService userService;
    private final StudentProfileRepository studentProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;

    @Transactional(readOnly = true)
    public ProfileSummaryResponse getProfileSummary() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(email);

        RoleCode role = resolveRole(user);
        String roleName = role.name();

        if (role == RoleCode.STUDENT) {
            return studentProfileRepository.findByUserId(user.getId())
                    .map(p -> new ProfileSummaryResponse(roleName, p.getId(), true))
                    .orElse(new ProfileSummaryResponse(roleName, null, false));
        }

        if (role == RoleCode.MENTOR) {
            return mentorProfileRepository.findByUserId(user.getId())
                    .map(p -> new ProfileSummaryResponse(roleName, p.getId(), true))
                    .orElse(new ProfileSummaryResponse(roleName, null, false));
        }

        return new ProfileSummaryResponse(roleName, null, false);
    }

    private RoleCode resolveRole(User user) {
        boolean isMentor = user.getRoles().stream()
                .map(Role::getCode)
                .anyMatch(code -> code == RoleCode.MENTOR);
        if (isMentor) {
            return RoleCode.MENTOR;
        }

        boolean isAdmin = user.getRoles().stream()
                .map(Role::getCode)
                .anyMatch(code -> code == RoleCode.ADMIN);
        if (isAdmin) {
            return RoleCode.ADMIN;
        }

        return RoleCode.STUDENT;
    }
}
