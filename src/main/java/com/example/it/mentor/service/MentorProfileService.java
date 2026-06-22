package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.mentor.MentorCardResponse;
import com.example.it.mentor.dto.mentor.MentorProfileRequest;
import com.example.it.mentor.dto.mentor.MentorProfileResponse;
import com.example.it.mentor.dto.mentor.MentorSkillRequest;
import com.example.it.mentor.dto.mentor.MentorSearchFilter;
import com.example.it.mentor.entity.BaseEntity;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentorSkill;
import com.example.it.mentor.entity.dict.DictSkill;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.dict.DictCity;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.MentorProfileMapper;
import com.example.it.mentor.repository.DictCityRepository;
import com.example.it.mentor.repository.DictSkillRepository;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.MentorProfileSpecification;
import com.example.it.mentor.repository.MentorSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Сервис управления профилями менторов и поиском по ним.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MentorProfileService {

    private final MentorProfileRepository profileRepository;
    private final MentorSkillRepository skillRepository;
    private final UserService userService;
    private final DictCityRepository cityRepository;
    private final DictSkillRepository skillRefRepository;
    private final MentorProfileMapper mapper;

    /**
     * Создаёт или обновляет профиль текущего ментора.
     *
     * @param request данные профиля
     * @return актуальное состояние профиля
     */
    @Transactional
    public MentorProfileResponse upsertProfile(MentorProfileRequest request) {
        User user = userService.getCurrentUserEntity();
        MentorProfile profile = loadOrCreateProfile(user);
        boolean isNew = profile.getId() == null;

        applyProfileFields(profile, request);
        profile = persistIfNew(profile);
        replaceSkills(profile, request.skills());
        profileRepository.save(profile);
        log.info("{} профиль ментора: userId={}, profileId={}, recruitmentStatus={}, skillCount={}, step={}",
                isNew ? "Создан" : "Обновлён", user.getId(), profile.getId(), profile.getRecruitmentStatus(),
                request.skills() == null ? 0 : request.skills().size(),
                isNew ? "mentor_profile_created" : "mentor_profile_updated");

        return loadResponse(user.getId());
    }

    /**
     * Возвращает профиль текущего аутентифицированного ментора.
     *
     * @return профиль ментора
     */
    @Transactional(readOnly = true)
    public MentorProfileResponse getMyProfile() {
        User user = userService.getCurrentUserEntity();
        MentorProfile profile = profileRepository.findWithDetailsByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Профиль ментора не найден"));
        return mapper.toResponse(profile);
    }

    /**
     * Выполняет поиск менторов по фильтру и пагинации.
     *
     * <p>Сначала получает страницу идентификаторов, затем догружает полные данные
     * профилей для корректного формирования карточек без потери порядка.</p>
     *
     * @param filter фильтр поиска
     * @param pageable параметры пагинации и сортировки
     * @return страница карточек менторов
     */
    @Transactional(readOnly = true)
    public PagedResponse<MentorCardResponse> searchMentors(MentorSearchFilter filter, Pageable pageable) {
        Specification<MentorProfile> spec = MentorProfileSpecification.build(filter);
        Page<MentorProfile> page = profileRepository.findAll(spec, pageable);
        log.debug("Поиск менторов выполнен: total={}, page={}, size={}, resultCount={}, step={}",
                page.getTotalElements(), pageable.getPageNumber(), pageable.getPageSize(),
                page.getNumberOfElements(), "mentor_search_completed");
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

    /**
     * Возвращает публичный профиль ментора по идентификатору.
     *
     * @param id идентификатор профиля
     * @return профиль ментора
     */
    @Transactional(readOnly = true)
    public MentorProfileResponse getProfileById(Long id) {
        MentorProfile profile = profileRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Профиль ментора не найден"));
        return mapper.toResponse(profile);
    }

    /**
     * Загружает существующий профиль пользователя или создаёт новый черновик сущности.
     *
     * @param user текущий пользователь
     * @return существующий или новый профиль
     */
    private MentorProfile loadOrCreateProfile(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> MentorProfile.builder().user(user).build());
    }

    /**
     * Копирует скалярные поля запроса в профиль ментора.
     *
     * @param profile профиль для обновления
     * @param request входные данные
     */
    private void applyProfileFields(MentorProfile profile, MentorProfileRequest request) {
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setMiddleName(request.middleName());
        profile.setPosition(request.position());
        profile.setDepartment(request.department());
        profile.setPhone(request.phone());
        profile.setMaxContact(request.maxContact());
        profile.setDescription(request.description());
        profile.setExpectations(request.expectations());
        profile.setCanHelpWith(request.canHelpWith());
        profile.setMentoringType(request.mentoringType());
        profile.setMentoringChannel(request.mentoringChannel());
        profile.setMentoringFrequency(request.mentoringFrequency());
        profile.setMentoringDuration(request.mentoringDuration());
        profile.setMenteeLimit(request.menteeLimit());
        profile.setCity(resolveCity(request.cityId()));
        profile.setRecruitmentStatus(request.recruitmentStatus() != null
                ? request.recruitmentStatus()
                : RecruitmentStatus.OPEN);
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
     * Сохраняет профиль только в момент его первого создания.
     *
     * @param profile профиль ментора
     * @return сохранённый или исходный профиль
     */
    private MentorProfile persistIfNew(MentorProfile profile) {
        if (profile.getId() == null) {
            return profileRepository.save(profile);
        }
        return profile;
    }

    /**
     * Загружает профиль текущего пользователя со всеми связанными данными и маппит в DTO.
     *
     * @param userId идентификатор пользователя
     * @return DTO профиля
     */
    private MentorProfileResponse loadResponse(Long userId) {
        return mapper.toResponse(
                profileRepository.findWithDetailsByUserId(userId)
                        .orElseThrow(() -> new NotFoundException("Профиль ментора не найден: userId=" + userId))
        );
    }

    /**
     * Полностью заменяет список навыков ментора с валидацией справочника.
     *
     * @param profile профиль ментора
     * @param skills новые навыки
     */
    private void replaceSkills(MentorProfile profile, List<MentorSkillRequest> skills) {
        skillRepository.deleteAllByMentorProfile(profile);
        profile.getSkills().clear();
        if (skills == null) return;

        List<Long> skillIds = skills.stream()
                .map(MentorSkillRequest::skillId)
                .toList();
        Map<Long, DictSkill> skillMap = skillRefRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

        Set<MentorSkill> newSkills = skills.stream()
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
