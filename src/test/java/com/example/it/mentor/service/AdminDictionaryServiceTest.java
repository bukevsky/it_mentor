package com.example.it.mentor.service;

import com.example.it.mentor.dto.dict.*;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.dict.*;
import com.example.it.mentor.entity.enums.DictionaryOperation;
import com.example.it.mentor.entity.enums.DictionaryType;
import com.example.it.mentor.event.audit.DictionaryChangedAuditEvent;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.DictionaryMapper;
import com.example.it.mentor.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminDictionaryService")
class AdminDictionaryServiceTest {

    @InjectMocks private AdminDictionaryService service;

    @Mock private DictCityRepository cityRepository;
    @Mock private DictSkillRepository skillRepository;
    @Mock private DictLanguageRepository languageRepository;
    @Mock private DictInteractionTypeRepository interactionTypeRepository;
    @Mock private DictionaryMapper mapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserService userService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder().email("admin@test.com").build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        when(userService.getCurrentUserEntity()).thenReturn(admin);
    }

    // ── Cities ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("City")
    class CityTests {

        @Test
        @DisplayName("create_happyPath_savesAndPublishesEvent")
        void create_happyPath_savesAndPublishesEvent() {
            CreateCityRequest dto = new CreateCityRequest("Калининград", "Калинингр. обл", "Россия");
            DictCity entity = DictCity.builder().name("Калининград").country("Россия").active(true).build();
            ReflectionTestUtils.setField(entity, "id", 7L);
            CityResponse response = new CityResponse(7L, "Калининград", "Калинингр. обл", "Россия", true);

            when(cityRepository.existsByNameIgnoreCaseAndActiveTrue("Калининград")).thenReturn(false);
            when(mapper.toEntity(dto)).thenReturn(entity);
            when(cityRepository.save(entity)).thenReturn(entity);
            when(mapper.toCityResponse(entity)).thenReturn(response);

            CityResponse result = service.createCity(dto);

            assertThat(result.name()).isEqualTo("Калининград");
            ArgumentCaptor<DictionaryChangedAuditEvent> captor =
                    ArgumentCaptor.forClass(DictionaryChangedAuditEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().operation()).isEqualTo(DictionaryOperation.CREATE);
            assertThat(captor.getValue().dictionaryType()).isEqualTo(DictionaryType.CITY);
        }

        @Test
        @DisplayName("create_duplicateActiveName_throwsConflict")
        void create_duplicateActiveName_throwsConflict() {
            when(cityRepository.existsByNameIgnoreCaseAndActiveTrue("Москва")).thenReturn(true);

            assertThatThrownBy(() -> service.createCity(new CreateCityRequest("Москва", null, "Россия")))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("update_happyPath_persistsChanges_andPublishesEvent")
        void update_happyPath_persistsChanges_andPublishesEvent() {
            DictCity entity = DictCity.builder().name("Казань").country("Россия").active(true).build();
            ReflectionTestUtils.setField(entity, "id", 3L);
            UpdateCityRequest dto = new UpdateCityRequest("Казань обновлена", null, "Россия", true);
            CityResponse response = new CityResponse(3L, "Казань обновлена", null, "Россия", true);

            when(cityRepository.findById(3L)).thenReturn(Optional.of(entity));
            when(cityRepository.existsByNameIgnoreCaseAndActiveTrue("Казань обновлена")).thenReturn(false);
            when(mapper.toCityResponse(entity)).thenReturn(response);

            CityResponse result = service.updateCity(3L, dto);

            assertThat(result.name()).isEqualTo("Казань обновлена");
            ArgumentCaptor<Object> updateCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(updateCaptor.capture());
            assertThat(((DictionaryChangedAuditEvent) updateCaptor.getValue()).operation())
                    .isEqualTo(DictionaryOperation.UPDATE);
        }

        @Test
        @DisplayName("update_nonExisting_throwsNotFound")
        void update_nonExisting_throwsNotFound() {
            when(cityRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCity(999L,
                    new UpdateCityRequest("X", null, "Y", true)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("deactivate_active_setsActiveFalse_andPublishesEvent")
        void deactivate_active_setsActiveFalse_andPublishesEvent() {
            DictCity entity = DictCity.builder().name("Тест").country("Россия").active(true).build();
            ReflectionTestUtils.setField(entity, "id", 1L);
            when(cityRepository.findById(1L)).thenReturn(Optional.of(entity));

            service.deactivateCity(1L);

            assertThat(entity.isActive()).isFalse();
            ArgumentCaptor<Object> deleteCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(deleteCaptor.capture());
            assertThat(((DictionaryChangedAuditEvent) deleteCaptor.getValue()).operation())
                    .isEqualTo(DictionaryOperation.DELETE);
        }

        @Test
        @DisplayName("deactivate_alreadyInactive_throwsConflict")
        void deactivate_alreadyInactive_throwsConflict() {
            DictCity entity = DictCity.builder().name("Тест").country("Россия").active(false).build();
            when(cityRepository.findById(1L)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.deactivateCity(1L))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("deactivate_nonExisting_throwsNotFound")
        void deactivate_nonExisting_throwsNotFound() {
            when(cityRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivateCity(999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("restore_inactive_setsActiveTrue_andPublishesEvent")
        void restore_inactive_setsActiveTrue_andPublishesEvent() {
            DictCity entity = DictCity.builder().name("Тест").country("Россия").active(false).build();
            ReflectionTestUtils.setField(entity, "id", 1L);
            CityResponse response = new CityResponse(1L, "Тест", null, "Россия", true);

            when(cityRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(cityRepository.existsByNameIgnoreCaseAndActiveTrue("Тест")).thenReturn(false);
            when(mapper.toCityResponse(entity)).thenReturn(response);

            service.restoreCity(1L);

            assertThat(entity.isActive()).isTrue();
            ArgumentCaptor<Object> restoreCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(restoreCaptor.capture());
            assertThat(((DictionaryChangedAuditEvent) restoreCaptor.getValue()).operation())
                    .isEqualTo(DictionaryOperation.RESTORE);
        }

        @Test
        @DisplayName("restore_alreadyActive_throwsConflict")
        void restore_alreadyActive_throwsConflict() {
            DictCity entity = DictCity.builder().name("Тест").country("Россия").active(true).build();
            when(cityRepository.findById(1L)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.restoreCity(1L))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("restore_nameCollision_throwsConflict")
        void restore_nameCollision_throwsConflict() {
            DictCity entity = DictCity.builder().name("Дубль").country("Россия").active(false).build();
            when(cityRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(cityRepository.existsByNameIgnoreCaseAndActiveTrue("Дубль")).thenReturn(true);

            assertThatThrownBy(() -> service.restoreCity(1L))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("listAll_includesInactive")
        void listAll_includesInactive() {
            DictCity active = DictCity.builder().name("А").country("Россия").active(true).build();
            DictCity inactive = DictCity.builder().name("Б").country("Россия").active(false).build();

            when(cityRepository.findAllByOrderByNameAsc()).thenReturn(List.of(active, inactive));
            when(mapper.toCityResponse(active)).thenReturn(new CityResponse(1L, "А", null, "Россия", true));
            when(mapper.toCityResponse(inactive)).thenReturn(new CityResponse(2L, "Б", null, "Россия", false));

            List<CityResponse> result = service.listAllCities();
            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(r -> !r.active());
        }
    }

    // ── Skills ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Skill")
    class SkillTests {

        @Test
        @DisplayName("create_happyPath_savesAndPublishesEvent")
        void create_happyPath_savesAndPublishesEvent() {
            CreateSkillRequest dto = new CreateSkillRequest("Java", "Backend");
            DictSkill entity = DictSkill.builder().name("Java").category("Backend").active(true).build();
            ReflectionTestUtils.setField(entity, "id", 1L);
            SkillResponse response = new SkillResponse(1L, "Java", "Backend", true);

            when(skillRepository.existsByNameIgnoreCaseAndActiveTrue("Java")).thenReturn(false);
            when(mapper.toEntity(dto)).thenReturn(entity);
            when(skillRepository.save(entity)).thenReturn(entity);
            when(mapper.toSkillResponse(entity)).thenReturn(response);

            service.createSkill(dto);

            ArgumentCaptor<Object> skillCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(skillCaptor.capture());
            assertThat(skillCaptor.getValue()).isInstanceOf(DictionaryChangedAuditEvent.class);
            DictionaryChangedAuditEvent skillEvent = (DictionaryChangedAuditEvent) skillCaptor.getValue();
            assertThat(skillEvent.dictionaryType()).isEqualTo(DictionaryType.SKILL);
            assertThat(skillEvent.operation()).isEqualTo(DictionaryOperation.CREATE);
        }

        @Test
        @DisplayName("create_duplicateActiveName_throwsConflict")
        void create_duplicateActiveName_throwsConflict() {
            when(skillRepository.existsByNameIgnoreCaseAndActiveTrue("Java")).thenReturn(true);

            assertThatThrownBy(() -> service.createSkill(new CreateSkillRequest("Java", null)))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("deactivate_active_setsActiveFalse_andPublishesEvent")
        void deactivate_active_setsActiveFalse_andPublishesEvent() {
            DictSkill entity = DictSkill.builder().name("Java").active(true).build();
            ReflectionTestUtils.setField(entity, "id", 1L);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(entity));

            service.deactivateSkill(1L);

            assertThat(entity.isActive()).isFalse();
        }

        @Test
        @DisplayName("deactivate_nonExisting_throwsNotFound")
        void deactivate_nonExisting_throwsNotFound() {
            when(skillRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivateSkill(999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("listAll_includesInactive")
        void listAll_includesInactive() {
            DictSkill active = DictSkill.builder().name("Java").active(true).build();
            DictSkill inactive = DictSkill.builder().name("Cobol").active(false).build();

            when(skillRepository.findAllByOrderByNameAsc()).thenReturn(List.of(active, inactive));
            when(mapper.toSkillResponse(any())).thenReturn(new SkillResponse(1L, "X", null, true));

            assertThat(service.listAllSkills()).hasSize(2);
        }
    }

    // ── Languages ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Language")
    class LanguageTests {

        @Test
        @DisplayName("create_happyPath_savesAndPublishesEvent")
        void create_happyPath_savesAndPublishesEvent() {
            CreateLanguageRequest dto = new CreateLanguageRequest("Русский", "ru");
            DictLanguage entity = DictLanguage.builder().name("Русский").code("ru").active(true).build();
            ReflectionTestUtils.setField(entity, "id", 1L);

            when(languageRepository.existsByNameIgnoreCaseAndActiveTrue("Русский")).thenReturn(false);
            when(languageRepository.existsByCodeAndActiveTrue("ru")).thenReturn(false);
            when(mapper.toEntity(dto)).thenReturn(entity);
            when(languageRepository.save(entity)).thenReturn(entity);
            when(mapper.toLanguageResponse(entity)).thenReturn(new LanguageResponse(1L, "Русский", "ru", true));

            service.createLanguage(dto);

            ArgumentCaptor<Object> langCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(langCaptor.capture());
            assertThat(((DictionaryChangedAuditEvent) langCaptor.getValue()).dictionaryType())
                    .isEqualTo(DictionaryType.LANGUAGE);
        }

        @Test
        @DisplayName("create_duplicateActiveName_throwsConflict")
        void create_duplicateActiveName_throwsConflict() {
            when(languageRepository.existsByNameIgnoreCaseAndActiveTrue("Русский")).thenReturn(true);

            assertThatThrownBy(() -> service.createLanguage(new CreateLanguageRequest("Русский", "ru")))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("deactivate_nonExisting_throwsNotFound")
        void deactivate_nonExisting_throwsNotFound() {
            when(languageRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivateLanguage(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── InteractionTypes ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("InteractionType")
    class InteractionTypeTests {

        @Test
        @DisplayName("create_happyPath_savesAndPublishesEvent")
        void create_happyPath_savesAndPublishesEvent() {
            CreateInteractionTypeRequest dto = new CreateInteractionTypeRequest("Онлайн", "Удалённо");
            DictInteractionType entity = DictInteractionType.builder().name("Онлайн").active(true).build();
            ReflectionTestUtils.setField(entity, "id", 1L);

            when(interactionTypeRepository.existsByNameIgnoreCaseAndActiveTrue("Онлайн")).thenReturn(false);
            when(mapper.toEntity(dto)).thenReturn(entity);
            when(interactionTypeRepository.save(entity)).thenReturn(entity);
            when(mapper.toInteractionTypeResponse(entity)).thenReturn(
                    new InteractionTypeResponse(1L, "Онлайн", "Удалённо", true));

            service.createInteractionType(dto);

            ArgumentCaptor<Object> itCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(itCaptor.capture());
            assertThat(((DictionaryChangedAuditEvent) itCaptor.getValue()).dictionaryType())
                    .isEqualTo(DictionaryType.INTERACTION_TYPE);
        }

        @Test
        @DisplayName("create_duplicateActiveName_throwsConflict")
        void create_duplicateActiveName_throwsConflict() {
            when(interactionTypeRepository.existsByNameIgnoreCaseAndActiveTrue("Онлайн")).thenReturn(true);

            assertThatThrownBy(() -> service.createInteractionType(
                    new CreateInteractionTypeRequest("Онлайн", null)))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("deactivate_active_setsActiveFalse")
        void deactivate_active_setsActiveFalse() {
            DictInteractionType entity = DictInteractionType.builder().name("Оффлайн").active(true).build();
            ReflectionTestUtils.setField(entity, "id", 1L);
            when(interactionTypeRepository.findById(1L)).thenReturn(Optional.of(entity));

            service.deactivateInteractionType(1L);

            assertThat(entity.isActive()).isFalse();
        }

        @Test
        @DisplayName("deactivate_alreadyInactive_throwsConflict")
        void deactivate_alreadyInactive_throwsConflict() {
            DictInteractionType entity = DictInteractionType.builder().name("Оффлайн").active(false).build();
            when(interactionTypeRepository.findById(1L)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.deactivateInteractionType(1L))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("listAll_includesInactive")
        void listAll_includesInactive() {
            DictInteractionType t1 = DictInteractionType.builder().name("A").active(true).build();
            DictInteractionType t2 = DictInteractionType.builder().name("B").active(false).build();

            when(interactionTypeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(t1, t2));
            when(mapper.toInteractionTypeResponse(any())).thenReturn(
                    new InteractionTypeResponse(1L, "X", null, true));

            assertThat(service.listAllInteractionTypes()).hasSize(2);
        }
    }
}
