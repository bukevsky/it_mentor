package com.example.it.mentor.service;

import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.entity.BaseEntity;
import com.example.it.mentor.entity.StudentEducation;
import com.example.it.mentor.entity.StudentLanguage;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.StudentSkill;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.dict.DictLanguage;
import com.example.it.mentor.entity.dict.DictSkill;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.StudentProfileMapper;
import com.example.it.mentor.repository.DictCityRepository;
import com.example.it.mentor.repository.DictLanguageRepository;
import com.example.it.mentor.repository.DictSkillRepository;
import com.example.it.mentor.repository.StudentEducationRepository;
import com.example.it.mentor.repository.StudentLanguageRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import com.example.it.mentor.repository.StudentSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final StudentProfileRepository profileRepository;
    private final StudentEducationRepository educationRepository;
    private final StudentLanguageRepository languageRepository;
    private final StudentSkillRepository skillRepository;
    private final UserService userService;
    private final DictCityRepository cityRepository;
    private final DictLanguageRepository languageRefRepository;
    private final DictSkillRepository skillRefRepository;
    private final StudentProfileMapper mapper;
    private final FileStorage fileStorage;

    @Transactional(readOnly = true)
    public StudentProfileResponse getMyProfile() {
        User user = userService.getCurrentUserEntity();
        StudentProfile profile = profileRepository.findWithDetailsByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Профиль студента не найден"));
        return mapper.toResponse(profile);
    }

    @Transactional
    public StudentProfileResponse upsertProfile(StudentProfileRequest request) {
        User user = userService.getCurrentUserEntity();

        StudentProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> StudentProfile.builder().user(user).build());

        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setMiddleName(request.middleName());
        profile.setPhone(request.phone());
        profile.setDesiredPosition(request.desiredPosition());
        profile.setHoursPerWeek(request.hoursPerWeek());
        profile.setAvailableFrom(request.availableFrom());
        profile.setAbout(request.about());
        profile.setMax(request.max());

        if (request.cityId() != null) {
            profile.setCity(cityRepository.findById(request.cityId())
                    .orElseThrow(() -> new NotFoundException("Город не найден: " + request.cityId())));
        } else {
            profile.setCity(null);
        }

        profile.getEmploymentTypes().clear();
        if (request.employmentTypes() != null) {
            profile.getEmploymentTypes().addAll(request.employmentTypes());
        }
        profile.getWorkFormats().clear();
        if (request.workFormats() != null) {
            profile.getWorkFormats().addAll(request.workFormats());
        }

        // Для новых профилей нужен первый save для получения ID
        if (profile.getId() == null) {
            profile = profileRepository.save(profile);
        }

        replaceEducations(profile, request);
        replaceLanguages(profile, request);
        replaceSkills(profile, request);

        StudentProfile saved = profileRepository.save(profile);

        return mapper.toResponse(profileRepository.findWithDetailsById(saved.getId())
                .orElseThrow(() -> new NotFoundException("Профиль студента не найден: id=" + saved.getId())));
    }

    @Transactional(readOnly = true)
    public void requireStudentProfile(Long userId) {
        if (!profileRepository.existsByUserId(userId)) {
            throw new NotFoundException("Профиль студента не найден для пользователя: " + userId);
        }
    }

    @Transactional
    public void linkResume(Long userId, Long fileId) {
        fileStorage.requireOwned(fileId, userId);
        StudentProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Профиль студента не найден для пользователя: " + userId));
        profile.setResumeFileId(fileId);
        profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfileById(Long id) {
        StudentProfile profile = profileRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Профиль студента не найден"));
        return mapper.toResponse(profile);
    }

    private void replaceEducations(StudentProfile profile, StudentProfileRequest request) {
        educationRepository.deleteAllByStudentProfile(profile);
        profile.getEducations().clear();
        if (request.educations() == null) return;
        Set<StudentEducation> newEducations = request.educations().stream()
                .map(req -> StudentEducation.builder()
                        .studentProfile(profile)
                        .institution(req.institution())
                        .specialty(req.specialty())
                        .degree(req.degree())
                        .educationForm(req.educationForm())
                        .startYear(req.startYear())
                        .graduationYear(req.graduationYear())
                        .build())
                .collect(Collectors.toSet());
        profile.getEducations().addAll(newEducations);
    }

    private void replaceLanguages(StudentProfile profile, StudentProfileRequest request) {
        languageRepository.deleteAllByStudentProfile(profile);
        profile.getLanguages().clear();
        if (request.languages() == null) return;

        List<Long> langIds = request.languages().stream()
                .map(req -> req.languageId())
                .toList();
        Map<Long, DictLanguage> langMap = languageRefRepository.findAllById(langIds).stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

        Set<StudentLanguage> newLanguages = request.languages().stream()
                .map(req -> {
                    DictLanguage lang = langMap.get(req.languageId());
                    if (lang == null) {
                        throw new NotFoundException("Язык не найден: " + req.languageId());
                    }
                    return StudentLanguage.builder()
                            .studentProfile(profile)
                            .language(lang)
                            .level(req.level())
                            .build();
                })
                .collect(Collectors.toSet());
        profile.getLanguages().addAll(newLanguages);
    }

    private void replaceSkills(StudentProfile profile, StudentProfileRequest request) {
        skillRepository.deleteAllByStudentProfile(profile);
        profile.getSkills().clear();
        if (request.skills() == null) return;

        List<Long> skillIds = request.skills().stream()
                .map(req -> req.skillId())
                .toList();
        Map<Long, DictSkill> skillMap = skillRefRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

        Set<StudentSkill> newSkills = request.skills().stream()
                .map(req -> {
                    DictSkill skill = skillMap.get(req.skillId());
                    if (skill == null) {
                        throw new NotFoundException("Навык не найден: " + req.skillId());
                    }
                    return StudentSkill.builder()
                            .studentProfile(profile)
                            .skill(skill)
                            .level(req.level())
                            .build();
                })
                .collect(Collectors.toSet());
        profile.getSkills().addAll(newSkills);
    }
}
