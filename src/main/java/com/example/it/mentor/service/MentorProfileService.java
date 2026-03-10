package com.example.it.mentor.service;

import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentorSkill;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.MentorProfileMapper;
import com.example.it.mentor.repository.DictCityRepository;
import com.example.it.mentor.repository.DictSkillRepository;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.MentorSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MentorProfileService {

    private final MentorProfileRepository profileRepository;
    private final MentorSkillRepository skillRepository;
    private final UserService userService;
    private final DictCityRepository cityRepository;
    private final DictSkillRepository skillRefRepository;
    private final MentorProfileMapper mapper;

    @Transactional
    public MentorProfileResponse upsertProfile(MentorProfileRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(email);

        MentorProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> MentorProfile.builder().user(user).build());

        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setMiddleName(request.middleName());
        profile.setPosition(request.position());
        profile.setDepartment(request.department());
        profile.setPhone(request.phone());
        profile.setMax(request.max());
        profile.setDescription(request.description());
        profile.setExpectations(request.expectations());
        profile.setCanHelpWith(request.canHelpWith());
        profile.setMentoringType(request.mentoringType());
        profile.setMentoringChannel(request.mentoringChannel());
        profile.setMentoringFrequency(request.mentoringFrequency());
        profile.setMentoringDuration(request.mentoringDuration());
        profile.setMenteeLimit(request.menteeLimit());

        if (request.cityId() != null) {
            profile.setCity(cityRepository.findById(request.cityId())
                    .orElseThrow(() -> new NotFoundException("Город не найден: " + request.cityId())));
        } else {
            profile.setCity(null);
        }

        profile.setRecruitmentStatus(
                request.recruitmentStatus() != null ? request.recruitmentStatus() : RecruitmentStatus.OPEN);

        profile = profileRepository.save(profile);
        replaceSkills(profile, request);
        profileRepository.save(profile);

        return mapper.toResponse(
                profileRepository.findWithDetailsByUserId(user.getId())
                        .orElseThrow(() -> new NotFoundException("Профиль ментора не найден: userId=" + user.getId()))
        );
    }

    @Transactional(readOnly = true)
    public MentorProfileResponse getMyProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(email);
        MentorProfile profile = profileRepository.findWithDetailsByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Профиль ментора не найден"));
        return mapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public MentorProfileResponse getProfileById(Long id) {
        MentorProfile profile = profileRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Профиль ментора не найден"));
        return mapper.toResponse(profile);
    }

    private void replaceSkills(MentorProfile profile, MentorProfileRequest request) {
        skillRepository.deleteAllByMentorProfile(profile);
        profile.getSkills().clear();
        if (request.skills() == null) return;
        Set<MentorSkill> newSkills = request.skills().stream()
                .map(req -> MentorSkill.builder()
                        .mentorProfile(profile)
                        .skill(skillRefRepository.findById(req.skillId())
                                .orElseThrow(() -> new NotFoundException("Навык не найден: " + req.skillId())))
                        .level(req.level())
                        .build())
                .collect(Collectors.toSet());
        profile.getSkills().addAll(newSkills);
    }
}
