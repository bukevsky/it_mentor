package com.example.it.mentor.service;

import com.example.it.mentor.dto.student.StudentEducationRequest;
import com.example.it.mentor.dto.student.StudentLanguageRequest;
import com.example.it.mentor.dto.student.StudentProfileRequest;
import com.example.it.mentor.dto.student.StudentProfileResponse;
import com.example.it.mentor.dto.student.StudentSkillRequest;
import com.example.it.mentor.entity.BaseEntity;
import com.example.it.mentor.entity.StudentEducation;
import com.example.it.mentor.entity.StudentLanguage;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.StudentSkill;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.dict.DictCity;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис управления профилями студентов.
 *
 * <p>Поддерживает upsert-поведение для профиля и полную замену дочерних коллекций
 * образования, языков и навыков.</p>
 */
@Slf4j
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

    /**
     * Возвращает профиль текущего аутентифицированного студента вместе со связанными данными.
     *
     * @return профиль студента
     */
    @Transactional(readOnly = true)
    public StudentProfileResponse getMyProfile() {
        User user = userService.getCurrentUserEntity();
        StudentProfile profile = profileRepository.findWithDetailsByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Профиль студента не найден"));
        return mapper.toResponse(profile);
    }

    /**
     * Создаёт или обновляет профиль текущего студента.
     *
     * @param request данные профиля
     * @return актуальное состояние профиля после сохранения
     */
    @Transactional
    public StudentProfileResponse upsertProfile(StudentProfileRequest request) {
        User user = userService.getCurrentUserEntity();
        StudentProfile profile = loadOrCreateProfile(user);
        boolean isNew = profile.getId() == null;

        applyProfileFields(profile, request);
        profile = persistIfNew(profile);
        replaceEducations(profile, request.educations());
        replaceLanguages(profile, request.languages());
        replaceSkills(profile, request.skills());

        StudentProfile saved = profileRepository.save(profile);
        log.info("{} профиль студента: userId={}, profileId={}", isNew ? "Создан" : "Обновлён", user.getId(), saved.getId());

        return loadResponse(saved.getId());
    }

    /**
     * Проверяет, что у пользователя существует профиль студента.
     *
     * @param userId идентификатор пользователя
     */
    @Transactional(readOnly = true)
    public void requireStudentProfile(Long userId) {
        if (!profileRepository.existsByUserId(userId)) {
            throw new NotFoundException("Профиль студента не найден для пользователя: " + userId);
        }
    }

    /**
     * Привязывает ранее загруженное резюме к профилю студента.
     *
     * @param userId идентификатор пользователя
     * @param fileId идентификатор файла резюме
     */
    @Transactional
    public void linkResume(Long userId, Long fileId) {
        fileStorage.requireOwned(fileId, userId);
        StudentProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Профиль студента не найден для пользователя: " + userId));
        profile.setResumeFileId(fileId);
        profileRepository.save(profile);
        log.info("Резюме привязано к профилю студента: userId={}, fileId={}", userId, fileId);
    }

    /**
     * Возвращает публичный профиль студента по идентификатору.
     *
     * @param id идентификатор профиля
     * @return профиль студента
     */
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfileById(Long id) {
        StudentProfile profile = profileRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Профиль студента не найден"));
        return mapper.toResponse(profile);
    }

    /**
     * Загружает существующий профиль пользователя или создаёт новый черновик сущности.
     *
     * @param user текущий пользователь
     * @return существующий или новый профиль
     */
    private StudentProfile loadOrCreateProfile(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> StudentProfile.builder().user(user).build());
    }

    /**
     * Копирует скалярные поля запроса в профиль студента.
     *
     * @param profile профиль для обновления
     * @param request входные данные
     */
    private void applyProfileFields(StudentProfile profile, StudentProfileRequest request) {
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setMiddleName(request.middleName());
        profile.setPhone(request.phone());
        profile.setDesiredPosition(request.desiredPosition());
        profile.setHoursPerWeek(request.hoursPerWeek());
        profile.setAvailableFrom(request.availableFrom());
        profile.setAbout(request.about());
        profile.setMaxContact(request.maxContact());
        profile.setCity(resolveCity(request.cityId()));
        replaceValues(profile.getEmploymentTypes(), request.employmentTypes());
        replaceValues(profile.getWorkFormats(), request.workFormats());
    }

    /**
     * Разрешает ссылку на город из справочника.
     *
     * @param cityId идентификатор города
     * @return найденный город или {@code null}, если значение не задано
     */
    private DictCity resolveCity(Long cityId) {
        if (cityId == null) {
            return null;
        }
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new NotFoundException("Город не найден: " + cityId));
    }

    /**
     * Полностью заменяет содержимое целевого множества.
     *
     * @param target изменяемое множество
     * @param values новые значения
     * @param <T> тип элементов множества
     */
    private <T> void replaceValues(Set<T> target, Set<T> values) {
        target.clear();
        if (values != null) {
            target.addAll(values);
        }
    }

    /**
     * Сохраняет профиль только в момент его первого создания.
     *
     * @param profile профиль студента
     * @return сохранённый или исходный профиль
     */
    private StudentProfile persistIfNew(StudentProfile profile) {
        if (profile.getId() == null) {
            return profileRepository.save(profile);
        }
        return profile;
    }

    /**
     * Повторно загружает профиль с полным набором связанных сущностей и маппит его в DTO.
     *
     * @param profileId идентификатор профиля
     * @return DTO профиля
     */
    private StudentProfileResponse loadResponse(Long profileId) {
        return mapper.toResponse(profileRepository.findWithDetailsById(profileId)
                .orElseThrow(() -> new NotFoundException("Профиль студента не найден: id=" + profileId)));
    }

    /**
     * Полностью заменяет список образований студента.
     *
     * @param profile профиль студента
     * @param educations новые элементы образования
     */
    private void replaceEducations(StudentProfile profile, List<StudentEducationRequest> educations) {
        educationRepository.deleteAllByStudentProfile(profile);
        profile.getEducations().clear();
        if (educations == null) return;
        Set<StudentEducation> newEducations = educations.stream()
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

    /**
     * Полностью заменяет список языков студента с валидацией справочника.
     *
     * @param profile профиль студента
     * @param languages новые языки
     */
    private void replaceLanguages(StudentProfile profile, List<StudentLanguageRequest> languages) {
        languageRepository.deleteAllByStudentProfile(profile);
        profile.getLanguages().clear();
        if (languages == null) return;

        List<Long> langIds = languages.stream()
                .map(StudentLanguageRequest::languageId)
                .toList();
        Map<Long, DictLanguage> langMap = languageRefRepository.findAllById(langIds).stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

        Set<StudentLanguage> newLanguages = languages.stream()
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

    /**
     * Полностью заменяет список навыков студента с валидацией справочника.
     *
     * @param profile профиль студента
     * @param skills новые навыки
     */
    private void replaceSkills(StudentProfile profile, List<StudentSkillRequest> skills) {
        skillRepository.deleteAllByStudentProfile(profile);
        profile.getSkills().clear();
        if (skills == null) return;

        List<Long> skillIds = skills.stream()
                .map(StudentSkillRequest::skillId)
                .toList();
        Map<Long, DictSkill> skillMap = skillRefRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

        Set<StudentSkill> newSkills = skills.stream()
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
