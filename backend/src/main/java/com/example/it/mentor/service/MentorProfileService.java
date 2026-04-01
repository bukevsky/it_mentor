package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.mentor.MentorCardResponse;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentor.MentorSearchFilter;
import com.example.it.mentor.entity.BaseEntity;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentorSkill;
import com.example.it.mentor.entity.dict.DictSkill;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.MentorProfileMapper;
import com.example.it.mentor.repository.DictCityRepository;
import com.example.it.mentor.repository.DictSkillRepository;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.MentorProfileSpecification;
import com.example.it.mentor.repository.MentorSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
        User user = userService.getCurrentUserEntity();

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

        // Для новых профилей нужен первый save для получения ID
        if (profile.getId() == null) {
            profile = profileRepository.save(profile);
        }

        replaceSkills(profile, request);
        profileRepository.save(profile);

        return mapper.toResponse(
                profileRepository.findWithDetailsByUserId(user.getId())
                        .orElseThrow(() -> new NotFoundException("Профиль ментора не найден: userId=" + user.getId()))
        );
    }

    @Transactional(readOnly = true)
    public MentorProfileResponse getMyProfile() {
        User user = userService.getCurrentUserEntity();
        MentorProfile profile = profileRepository.findWithDetailsByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Профиль ментора не найден"));
        return mapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MentorCardResponse> searchMentors(MentorSearchFilter filter, Pageable pageable) {
        Specification<MentorProfile> spec = MentorProfileSpecification.build(filter);
        Page<MentorProfile> page = profileRepository.findAll(spec, pageable);
        if (page.isEmpty()) {
            return PagedResponse.from(page.map(mapper::toCardResponse));
        }
        List<Long> ids = page.getContent().stream().map(MentorProfile::getId).toList();
        List<MentorProfile> withDetails = profileRepository.findAllWithDetailsByIdIn(ids);
        Map<Long, MentorProfile> detailsMap = withDetails.stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));
        List<MentorCardResponse> cards = ids.stream()
                .map(detailsMap::get)
                .filter(Objects::nonNull)
                .map(mapper::toCardResponse)
                .toList();
        return PagedResponse.from(new PageImpl<>(cards, pageable, page.getTotalElements()));
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

        List<Long> skillIds = request.skills().stream()
                .map(req -> req.skillId())
                .toList();
        Map<Long, DictSkill> skillMap = skillRefRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

        Set<MentorSkill> newSkills = request.skills().stream()
                .map(req -> {
                    DictSkill dictSkill = skillMap.get(req.skillId());
                    if (dictSkill == null) {
                        throw new NotFoundException("Навык не найден: " + req.skillId());
                    }
                    return MentorSkill.builder()
                            .mentorProfile(profile)
                            .skill(dictSkill)
                            .level(req.level())
                            .build();
                })
                .collect(Collectors.toSet());
        profile.getSkills().addAll(newSkills);
    }
}
