package com.example.it.mentor.service;

import com.example.it.mentor.dto.dict.*;
import com.example.it.mentor.entity.dict.*;
import com.example.it.mentor.entity.enums.DictionaryOperation;
import com.example.it.mentor.entity.enums.DictionaryType;
import com.example.it.mentor.event.audit.DictionaryChangedAuditEvent;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.DictionaryMapper;
import com.example.it.mentor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDictionaryService {

    private final DictCityRepository cityRepository;
    private final DictSkillRepository skillRepository;
    private final DictLanguageRepository languageRepository;
    private final DictInteractionTypeRepository interactionTypeRepository;
    private final DictionaryMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;

    // === Cities ===

    @Transactional(readOnly = true)
    public List<CityResponse> listAllCities() {
        return cityRepository.findAllByOrderByNameAsc().stream().map(mapper::toCityResponse).toList();
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'cities'")
    public CityResponse createCity(CreateCityRequest dto) {
        log.info("Создание города: name={}, step={}", dto.name(), "dictionary_city_create_started");
        if (cityRepository.existsByNameIgnoreCaseAndActiveTrue(dto.name())) {
            throw new ConflictException("Город с именем '" + dto.name() + "' уже существует");
        }
        DictCity entity = mapper.toEntity(dto);
        entity = cityRepository.save(entity);
        log.info("Город создан: id={}, name={}, step={}", entity.getId(), entity.getName(), "dictionary_city_created");
        publishDictEvent(DictionaryType.CITY, entity.getId(), DictionaryOperation.CREATE, entity.getName());
        return mapper.toCityResponse(entity);
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'cities'")
    public CityResponse updateCity(Long id, UpdateCityRequest dto) {
        DictCity entity = cityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Город не найден: " + id));
        if (!entity.getName().equalsIgnoreCase(dto.name()) &&
                cityRepository.existsByNameIgnoreCaseAndActiveTrue(dto.name())) {
            throw new ConflictException("Город с именем '" + dto.name() + "' уже существует");
        }
        mapper.updateFromDto(dto, entity);
        cityRepository.save(entity);
        log.info("Город обновлён: id={}, name={}, step={}", entity.getId(), entity.getName(), "dictionary_city_updated");
        publishDictEvent(DictionaryType.CITY, entity.getId(), DictionaryOperation.UPDATE, entity.getName());
        return mapper.toCityResponse(entity);
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'cities'")
    public void deactivateCity(Long id) {
        DictCity entity = cityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Город не найден: " + id));
        if (!entity.isActive()) {
            throw new ConflictException("Город уже деактивирован: " + id);
        }
        entity.setActive(false);
        cityRepository.save(entity);
        log.info("Город деактивирован: id={}, name={}, step={}", entity.getId(), entity.getName(), "dictionary_city_deactivated");
        publishDictEvent(DictionaryType.CITY, entity.getId(), DictionaryOperation.DELETE, entity.getName());
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'cities'")
    public CityResponse restoreCity(Long id) {
        DictCity entity = cityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Город не найден: " + id));
        if (entity.isActive()) {
            throw new ConflictException("Город уже активен: " + id);
        }
        if (cityRepository.existsByNameIgnoreCaseAndActiveTrue(entity.getName())) {
            throw new ConflictException("Активный город с именем '" + entity.getName() + "' уже существует");
        }
        entity.setActive(true);
        cityRepository.save(entity);
        log.info("Город восстановлен: id={}, name={}, step={}", entity.getId(), entity.getName(), "dictionary_city_restored");
        publishDictEvent(DictionaryType.CITY, entity.getId(), DictionaryOperation.RESTORE, entity.getName());
        return mapper.toCityResponse(entity);
    }

    // === Skills ===

    @Transactional(readOnly = true)
    public List<SkillResponse> listAllSkills() {
        return skillRepository.findAllByOrderByNameAsc().stream().map(mapper::toSkillResponse).toList();
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'skills'")
    public SkillResponse createSkill(CreateSkillRequest dto) {
        log.info("Создание навыка: name={}, step={}", dto.name(), "dictionary_skill_create_started");
        if (skillRepository.existsByNameIgnoreCaseAndActiveTrue(dto.name())) {
            throw new ConflictException("Навык с именем '" + dto.name() + "' уже существует");
        }
        DictSkill entity = mapper.toEntity(dto);
        entity = skillRepository.save(entity);
        log.info("Навык создан: id={}, name={}, step={}", entity.getId(), entity.getName(), "dictionary_skill_created");
        publishDictEvent(DictionaryType.SKILL, entity.getId(), DictionaryOperation.CREATE, entity.getName());
        return mapper.toSkillResponse(entity);
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'skills'")
    public SkillResponse updateSkill(Long id, UpdateSkillRequest dto) {
        DictSkill entity = skillRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Навык не найден: " + id));
        if (!entity.getName().equalsIgnoreCase(dto.name()) &&
                skillRepository.existsByNameIgnoreCaseAndActiveTrue(dto.name())) {
            throw new ConflictException("Навык с именем '" + dto.name() + "' уже существует");
        }
        mapper.updateFromDto(dto, entity);
        skillRepository.save(entity);
        log.info("Навык обновлён: id={}, name={}, step={}", entity.getId(), entity.getName(), "dictionary_skill_updated");
        publishDictEvent(DictionaryType.SKILL, entity.getId(), DictionaryOperation.UPDATE, entity.getName());
        return mapper.toSkillResponse(entity);
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'skills'")
    public void deactivateSkill(Long id) {
        DictSkill entity = skillRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Навык не найден: " + id));
        if (!entity.isActive()) {
            throw new ConflictException("Навык уже деактивирован: " + id);
        }
        entity.setActive(false);
        skillRepository.save(entity);
        log.info("Навык деактивирован: id={}, name={}, step={}", entity.getId(), entity.getName(), "dictionary_skill_deactivated");
        publishDictEvent(DictionaryType.SKILL, entity.getId(), DictionaryOperation.DELETE, entity.getName());
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'skills'")
    public SkillResponse restoreSkill(Long id) {
        DictSkill entity = skillRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Навык не найден: " + id));
        if (entity.isActive()) {
            throw new ConflictException("Навык уже активен: " + id);
        }
        if (skillRepository.existsByNameIgnoreCaseAndActiveTrue(entity.getName())) {
            throw new ConflictException("Активный навык с именем '" + entity.getName() + "' уже существует");
        }
        entity.setActive(true);
        skillRepository.save(entity);
        log.info("Навык восстановлен: id={}, name={}, step={}", entity.getId(), entity.getName(), "dictionary_skill_restored");
        publishDictEvent(DictionaryType.SKILL, entity.getId(), DictionaryOperation.RESTORE, entity.getName());
        return mapper.toSkillResponse(entity);
    }

    // === Languages ===

    @Transactional(readOnly = true)
    public List<LanguageResponse> listAllLanguages() {
        return languageRepository.findAllByOrderByNameAsc().stream().map(mapper::toLanguageResponse).toList();
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'languages'")
    public LanguageResponse createLanguage(CreateLanguageRequest dto) {
        log.info("Создание языка: name={}, code={}, step={}", dto.name(), dto.code(), "dictionary_language_create_started");
        if (languageRepository.existsByNameIgnoreCaseAndActiveTrue(dto.name())) {
            throw new ConflictException("Язык с именем '" + dto.name() + "' уже существует");
        }
        if (languageRepository.existsByCodeAndActiveTrue(dto.code())) {
            throw new ConflictException("Язык с кодом '" + dto.code() + "' уже существует");
        }
        DictLanguage entity = mapper.toEntity(dto);
        entity = languageRepository.save(entity);
        log.info("Язык создан: id={}, name={}, code={}, step={}", entity.getId(), entity.getName(), entity.getCode(),
                "dictionary_language_created");
        publishDictEvent(DictionaryType.LANGUAGE, entity.getId(), DictionaryOperation.CREATE, entity.getName());
        return mapper.toLanguageResponse(entity);
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'languages'")
    public LanguageResponse updateLanguage(Long id, UpdateLanguageRequest dto) {
        DictLanguage entity = languageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Язык не найден: " + id));
        if (!entity.getName().equalsIgnoreCase(dto.name()) &&
                languageRepository.existsByNameIgnoreCaseAndActiveTrue(dto.name())) {
            throw new ConflictException("Язык с именем '" + dto.name() + "' уже существует");
        }
        if (!entity.getCode().equalsIgnoreCase(dto.code()) &&
                languageRepository.existsByCodeAndActiveTrue(dto.code())) {
            throw new ConflictException("Язык с кодом '" + dto.code() + "' уже существует");
        }
        mapper.updateFromDto(dto, entity);
        languageRepository.save(entity);
        log.info("Язык обновлён: id={}, name={}, code={}, step={}", entity.getId(), entity.getName(), entity.getCode(),
                "dictionary_language_updated");
        publishDictEvent(DictionaryType.LANGUAGE, entity.getId(), DictionaryOperation.UPDATE, entity.getName());
        return mapper.toLanguageResponse(entity);
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'languages'")
    public void deactivateLanguage(Long id) {
        DictLanguage entity = languageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Язык не найден: " + id));
        if (!entity.isActive()) {
            throw new ConflictException("Язык уже деактивирован: " + id);
        }
        entity.setActive(false);
        languageRepository.save(entity);
        log.info("Язык деактивирован: id={}, name={}, code={}, step={}", entity.getId(), entity.getName(), entity.getCode(),
                "dictionary_language_deactivated");
        publishDictEvent(DictionaryType.LANGUAGE, entity.getId(), DictionaryOperation.DELETE, entity.getName());
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'languages'")
    public LanguageResponse restoreLanguage(Long id) {
        DictLanguage entity = languageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Язык не найден: " + id));
        if (entity.isActive()) {
            throw new ConflictException("Язык уже активен: " + id);
        }
        if (languageRepository.existsByNameIgnoreCaseAndActiveTrue(entity.getName())) {
            throw new ConflictException("Активный язык с именем '" + entity.getName() + "' уже существует");
        }
        entity.setActive(true);
        languageRepository.save(entity);
        log.info("Язык восстановлен: id={}, name={}, code={}, step={}", entity.getId(), entity.getName(), entity.getCode(),
                "dictionary_language_restored");
        publishDictEvent(DictionaryType.LANGUAGE, entity.getId(), DictionaryOperation.RESTORE, entity.getName());
        return mapper.toLanguageResponse(entity);
    }

    // === Interaction Types ===

    @Transactional(readOnly = true)
    public List<InteractionTypeResponse> listAllInteractionTypes() {
        return interactionTypeRepository.findAllByOrderByNameAsc().stream()
                .map(mapper::toInteractionTypeResponse).toList();
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'interactionTypes'")
    public InteractionTypeResponse createInteractionType(CreateInteractionTypeRequest dto) {
        log.info("Создание типа взаимодействия: name={}, step={}", dto.name(),
                "dictionary_interaction_type_create_started");
        if (interactionTypeRepository.existsByNameIgnoreCaseAndActiveTrue(dto.name())) {
            throw new ConflictException("Тип взаимодействия с именем '" + dto.name() + "' уже существует");
        }
        DictInteractionType entity = mapper.toEntity(dto);
        entity = interactionTypeRepository.save(entity);
        log.info("Тип взаимодействия создан: id={}, name={}, step={}", entity.getId(), entity.getName(),
                "dictionary_interaction_type_created");
        publishDictEvent(DictionaryType.INTERACTION_TYPE, entity.getId(), DictionaryOperation.CREATE, entity.getName());
        return mapper.toInteractionTypeResponse(entity);
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'interactionTypes'")
    public InteractionTypeResponse updateInteractionType(Long id, UpdateInteractionTypeRequest dto) {
        DictInteractionType entity = interactionTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Тип взаимодействия не найден: " + id));
        if (!entity.getName().equalsIgnoreCase(dto.name()) &&
                interactionTypeRepository.existsByNameIgnoreCaseAndActiveTrue(dto.name())) {
            throw new ConflictException("Тип взаимодействия с именем '" + dto.name() + "' уже существует");
        }
        mapper.updateFromDto(dto, entity);
        interactionTypeRepository.save(entity);
        log.info("Тип взаимодействия обновлён: id={}, name={}, step={}", entity.getId(), entity.getName(),
                "dictionary_interaction_type_updated");
        publishDictEvent(DictionaryType.INTERACTION_TYPE, entity.getId(), DictionaryOperation.UPDATE, entity.getName());
        return mapper.toInteractionTypeResponse(entity);
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'interactionTypes'")
    public void deactivateInteractionType(Long id) {
        DictInteractionType entity = interactionTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Тип взаимодействия не найден: " + id));
        if (!entity.isActive()) {
            throw new ConflictException("Тип взаимодействия уже деактивирован: " + id);
        }
        entity.setActive(false);
        interactionTypeRepository.save(entity);
        log.info("Тип взаимодействия деактивирован: id={}, name={}, step={}", entity.getId(), entity.getName(),
                "dictionary_interaction_type_deactivated");
        publishDictEvent(DictionaryType.INTERACTION_TYPE, entity.getId(), DictionaryOperation.DELETE, entity.getName());
    }

    @Transactional
    @CacheEvict(value = "dictionaries", key = "'interactionTypes'")
    public InteractionTypeResponse restoreInteractionType(Long id) {
        DictInteractionType entity = interactionTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Тип взаимодействия не найден: " + id));
        if (entity.isActive()) {
            throw new ConflictException("Тип взаимодействия уже активен: " + id);
        }
        if (interactionTypeRepository.existsByNameIgnoreCaseAndActiveTrue(entity.getName())) {
            throw new ConflictException("Активный тип взаимодействия с именем '" + entity.getName() + "' уже существует");
        }
        entity.setActive(true);
        interactionTypeRepository.save(entity);
        log.info("Тип взаимодействия восстановлен: id={}, name={}, step={}", entity.getId(), entity.getName(),
                "dictionary_interaction_type_restored");
        publishDictEvent(DictionaryType.INTERACTION_TYPE, entity.getId(), DictionaryOperation.RESTORE, entity.getName());
        return mapper.toInteractionTypeResponse(entity);
    }

    private void publishDictEvent(DictionaryType type, Long entryId, DictionaryOperation op, String name) {
        Long adminId = userService.getCurrentUserEntity().getId();
        log.debug("Audit-событие справочника опубликовано: adminId={}, dictionaryType={}, entryId={}, operation={}, step={}",
                adminId, type, entryId, op, "dictionary_audit_event_published");
        eventPublisher.publishEvent(new DictionaryChangedAuditEvent(adminId, type, entryId, op, name));
    }
}
