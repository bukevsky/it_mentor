package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.complaint.ComplaintResponse;
import com.example.it.mentor.dto.complaint.CreateComplaintRequest;
import com.example.it.mentor.dto.complaint.ResolveComplaintRequest;
import com.example.it.mentor.entity.Complaint;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.ComplaintStatus;
import com.example.it.mentor.entity.enums.ComplaintTargetType;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.ComplaintMapper;
import com.example.it.mentor.repository.ComplaintRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComplaintService")
class ComplaintServiceTest {

    @InjectMocks private ComplaintService service;

    @Mock private ComplaintRepository complaintRepository;
    @Mock private UserService userService;
    @Mock private ComplaintMapper mapper;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private User reporter;
    private User admin;
    private Complaint openComplaint;
    private ComplaintResponse complaintResponse;

    @BeforeEach
    void setUp() {
        reporter = User.builder().email("student@test.com").build();
        ReflectionTestUtils.setField(reporter, "id", 1L);

        admin = User.builder().email("admin@test.com").build();
        ReflectionTestUtils.setField(admin, "id", 99L);

        openComplaint = Complaint.builder()
                .targetType(ComplaintTargetType.REVIEW)
                .targetId(10L)
                .reporterUserId(1L)
                .reason("Спам")
                .build();
        ReflectionTestUtils.setField(openComplaint, "id", 50L);

        complaintResponse = new ComplaintResponse(50L, ComplaintTargetType.REVIEW, 10L, 1L,
                "Спам", ComplaintStatus.OPEN, null, null, null, null);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("happyPath — пользователь создаёт жалобу на отзыв")
        void create_happyPath_shouldReturnResponse() {
            when(userService.getCurrentUserEntity()).thenReturn(reporter);
            when(complaintRepository.save(any())).thenReturn(openComplaint);
            when(mapper.toResponse(openComplaint)).thenReturn(complaintResponse);

            CreateComplaintRequest dto = new CreateComplaintRequest(ComplaintTargetType.REVIEW, 10L, "Спам");
            ComplaintResponse result = service.create(dto);

            assertThat(result.targetType()).isEqualTo(ComplaintTargetType.REVIEW);
            assertThat(result.targetId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("happyPath — сохраняет правильные поля и reporterUserId")
        void create_happyPath_shouldSaveCorrectEntity() {
            when(userService.getCurrentUserEntity()).thenReturn(reporter);
            when(complaintRepository.save(any())).thenReturn(openComplaint);
            when(mapper.toResponse(any())).thenReturn(complaintResponse);

            CreateComplaintRequest dto = new CreateComplaintRequest(ComplaintTargetType.USER, 5L, "Нарушение правил");
            service.create(dto);

            ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
            verify(complaintRepository).save(captor.capture());
            Complaint saved = captor.getValue();
            assertThat(saved.getTargetType()).isEqualTo(ComplaintTargetType.USER);
            assertThat(saved.getTargetId()).isEqualTo(5L);
            assertThat(saved.getReporterUserId()).isEqualTo(1L);
            assertThat(saved.getReason()).isEqualTo("Нарушение правил");
            assertThat(saved.getStatus()).isEqualTo(ComplaintStatus.OPEN);
        }
    }

    // ── getList ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getList")
    class GetList {

        @Test
        @DisplayName("filtersByStatus — возвращает страницу по фильтру статуса")
        void getList_filtersByStatus_shouldReturnPage() {
            Page<Complaint> page = new PageImpl<>(List.of(openComplaint));
            when(complaintRepository.findFiltered(eq(ComplaintStatus.OPEN), eq(null), any()))
                    .thenReturn(page);
            when(mapper.toResponse(openComplaint)).thenReturn(complaintResponse);

            PagedResponse<ComplaintResponse> result = service.getList(ComplaintStatus.OPEN, null, PageRequest.of(0, 20));

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("filtersByTargetType — возвращает страницу по фильтру targetType")
        void getList_filtersByTargetType_shouldReturnPage() {
            Page<Complaint> page = new PageImpl<>(List.of(openComplaint));
            when(complaintRepository.findFiltered(eq(null), eq(ComplaintTargetType.REVIEW), any()))
                    .thenReturn(page);
            when(mapper.toResponse(openComplaint)).thenReturn(complaintResponse);

            PagedResponse<ComplaintResponse> result = service.getList(null, ComplaintTargetType.REVIEW, PageRequest.of(0, 20));

            assertThat(result.content()).hasSize(1);
        }
    }

    // ── resolve ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("happyPath — admin закрывает жалобу как RESOLVED")
        void resolve_admin_setsStatusAndResolution() {
            when(complaintRepository.findById(50L)).thenReturn(Optional.of(openComplaint));
            when(userService.getCurrentUserEntity()).thenReturn(admin);
            when(complaintRepository.save(any())).thenReturn(openComplaint);
            when(mapper.toResponse(openComplaint)).thenReturn(complaintResponse);

            ResolveComplaintRequest dto = new ResolveComplaintRequest(ComplaintStatus.RESOLVED, "Удалён отзыв");
            service.resolve(50L, dto);

            ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
            verify(complaintRepository).save(captor.capture());
            Complaint saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ComplaintStatus.RESOLVED);
            assertThat(saved.getResolution()).isEqualTo("Удалён отзыв");
            assertThat(saved.getResolvedBy()).isEqualTo(99L);
            assertThat(saved.getResolvedAt()).isNotNull();
        }

        @Test
        @DisplayName("alreadyClosed — жалоба уже закрыта → ConflictException")
        void resolve_alreadyClosed_shouldThrowConflict() {
            openComplaint.setStatus(ComplaintStatus.RESOLVED);
            when(complaintRepository.findById(50L)).thenReturn(Optional.of(openComplaint));

            ResolveComplaintRequest dto = new ResolveComplaintRequest(ComplaintStatus.RESOLVED, null);

            assertThatThrownBy(() -> service.resolve(50L, dto))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("invalidStatus — статус OPEN → BusinessRuleViolationException")
        void resolve_invalidStatus_shouldThrowBusinessRule() {
            ResolveComplaintRequest dto = new ResolveComplaintRequest(ComplaintStatus.OPEN, null);

            assertThatThrownBy(() -> service.resolve(50L, dto))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verifyNoInteractions(complaintRepository);
        }

        @Test
        @DisplayName("notFound — жалоба не найдена → NotFoundException")
        void resolve_notFound_shouldThrowNotFound() {
            when(complaintRepository.findById(999L)).thenReturn(Optional.empty());

            ResolveComplaintRequest dto = new ResolveComplaintRequest(ComplaintStatus.REJECTED, null);

            assertThatThrownBy(() -> service.resolve(999L, dto))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("rejected — admin отклоняет жалобу как REJECTED")
        void resolve_rejected_setsRejectedStatus() {
            when(complaintRepository.findById(50L)).thenReturn(Optional.of(openComplaint));
            when(userService.getCurrentUserEntity()).thenReturn(admin);
            when(complaintRepository.save(any())).thenReturn(openComplaint);
            when(mapper.toResponse(any())).thenReturn(complaintResponse);

            ResolveComplaintRequest dto = new ResolveComplaintRequest(ComplaintStatus.REJECTED, "Жалоба необоснована");
            service.resolve(50L, dto);

            ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
            verify(complaintRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ComplaintStatus.REJECTED);
        }
    }
}
