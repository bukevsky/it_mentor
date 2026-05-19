package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.mentoring.MentoringRequestClarifyRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestCreateRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestRejectRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestResponse;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringRequestDirection;
import com.example.it.mentor.entity.enums.MentoringRequestStatus;
import com.example.it.mentor.entity.enums.MentoringType;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.MentoringRequestMapper;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.MentoringRequestRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.example.it.mentor.entity.enums.MentoringRequestStatus.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentoringRequestService")
class MentoringRequestServiceTest {

    @InjectMocks private MentoringRequestService service;

    @Mock private MentoringRequestRepository requestRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private MentorProfileRepository mentorProfileRepository;
    @Mock private UserService userService;
    @Mock private MentoringRequestMapper mapper;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private User studentUser;
    private User mentorUser;
    private StudentProfile studentProfile;
    private MentorProfile mentorProfile;

    @BeforeEach
    void setUp() {
        Role studentRole = new Role();
        ReflectionTestUtils.setField(studentRole, "code", RoleCode.STUDENT);

        Role mentorRole = new Role();
        ReflectionTestUtils.setField(mentorRole, "code", RoleCode.MENTOR);

        studentUser = User.builder().email("student@test.com").roles(Set.of(studentRole)).build();
        ReflectionTestUtils.setField(studentUser, "id", 1L);

        mentorUser = User.builder().email("mentor@test.com").roles(Set.of(mentorRole)).build();
        ReflectionTestUtils.setField(mentorUser, "id", 2L);

        studentProfile = StudentProfile.builder().user(studentUser).build();
        ReflectionTestUtils.setField(studentProfile, "id", 10L);

        mentorProfile = MentorProfile.builder()
                .user(mentorUser)
                .recruitmentStatus(RecruitmentStatus.OPEN)
                .build();
        ReflectionTestUtils.setField(mentorProfile, "id", 20L);
    }

    // ── createRequest ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createRequest")
    class CreateRequest {

        private final MentoringRequestCreateRequest dto =
                new MentoringRequestCreateRequest(20L, MentoringType.PRACTICE, "Хочу учиться");

        @Test
        @DisplayName("студент → ментор: создаёт заявку и возвращает response")
        void studentToMentor_happyPath_shouldCreate() {
            MentoringRequestResponse expected = mockResponse();

            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(studentProfile));
            when(mentorProfileRepository.findById(20L)).thenReturn(Optional.of(mentorProfile));
            when(requestRepository.existsByStudentProfileIdAndMentorProfileIdAndStatusIn(
                    eq(10L), eq(20L), any())).thenReturn(false);
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any(MentoringRequest.class))).thenReturn(expected);

            MentoringRequestResponse result = service.createRequest(dto);

            assertThat(result).isEqualTo(expected);
            verify(requestRepository).save(any(MentoringRequest.class));
        }

        @Test
        @DisplayName("ментор → студент: создаёт приглашение и возвращает response")
        void mentorToStudent_happyPath_shouldCreate() {
            MentoringRequestCreateRequest inviteDto =
                    new MentoringRequestCreateRequest(10L, MentoringType.PRACTICE, "Приглашаю");
            MentoringRequestResponse expected = mockResponse();

            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(mentorProfileRepository.findByUserId(2L)).thenReturn(Optional.of(mentorProfile));
            when(studentProfileRepository.findById(10L)).thenReturn(Optional.of(studentProfile));
            when(requestRepository.existsByStudentProfileIdAndMentorProfileIdAndStatusIn(
                    eq(10L), eq(20L), any())).thenReturn(false);
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any(MentoringRequest.class))).thenReturn(expected);

            MentoringRequestResponse result = service.createRequest(inviteDto);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("пользователь без роли студент/ментор → ForbiddenException")
        void noStudentOrMentorRole_shouldThrowForbidden() {
            User adminUser = User.builder().email("admin@test.com").roles(Set.of()).build();
            when(userService.getCurrentUserEntity()).thenReturn(adminUser);

            assertThatThrownBy(() -> service.createRequest(dto))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("ментор закрыт (CLOSED) → BusinessRuleViolationException")
        void mentorClosed_shouldThrowBusinessRuleViolation() {
            MentorProfile closedMentor = MentorProfile.builder()
                    .user(mentorUser)
                    .recruitmentStatus(RecruitmentStatus.CLOSED)
                    .build();
            ReflectionTestUtils.setField(closedMentor, "id", 20L);

            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(studentProfile));
            when(mentorProfileRepository.findById(20L)).thenReturn(Optional.of(closedMentor));

            assertThatThrownBy(() -> service.createRequest(dto))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("не принимает");
        }

        @Test
        @DisplayName("дубликат активной заявки → ConflictException")
        void duplicateActiveRequest_shouldThrowConflict() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(studentProfile));
            when(mentorProfileRepository.findById(20L)).thenReturn(Optional.of(mentorProfile));
            when(requestRepository.existsByStudentProfileIdAndMentorProfileIdAndStatusIn(
                    eq(10L), eq(20L), any())).thenReturn(true);

            assertThatThrownBy(() -> service.createRequest(dto))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("самозаявка (студент и ментор — один user) → BusinessRuleViolationException")
        void selfRequest_shouldThrowBusinessRuleViolation() {
            MentorProfile selfMentor = MentorProfile.builder()
                    .user(studentUser)
                    .recruitmentStatus(RecruitmentStatus.OPEN)
                    .build();
            ReflectionTestUtils.setField(selfMentor, "id", 20L);

            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(studentProfile));
            when(mentorProfileRepository.findById(20L)).thenReturn(Optional.of(selfMentor));

            assertThatThrownBy(() -> service.createRequest(dto))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("самому себе");
        }
    }

    // ── getRequests ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRequests")
    class GetRequests {

        private final PageRequest pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("ментор — возвращает входящие заявки")
        void asMentor_shouldQueryByMentorProfileId() {
            Page<MentoringRequest> emptyPage = new PageImpl<>(List.of());
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(mentorProfileRepository.findByUserId(2L)).thenReturn(Optional.of(mentorProfile));
            when(requestRepository.findByMentorProfileId(20L, pageable)).thenReturn(emptyPage);

            PagedResponse<MentoringRequestResponse> result = service.getRequests(null, pageable);

            assertThat(result).isNotNull();
            verify(requestRepository).findByMentorProfileId(20L, pageable);
        }

        @Test
        @DisplayName("студент — возвращает исходящие заявки")
        void asStudent_shouldQueryByStudentProfileId() {
            Page<MentoringRequest> emptyPage = new PageImpl<>(List.of());
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(studentProfile));
            when(requestRepository.findByStudentProfileId(10L, pageable)).thenReturn(emptyPage);

            service.getRequests(null, pageable);

            verify(requestRepository).findByStudentProfileId(10L, pageable);
        }

        @Test
        @DisplayName("с фильтром по статусу — использует метод с фильтром")
        void withStatusFilter_shouldQueryByStatus() {
            Page<MentoringRequest> emptyPage = new PageImpl<>(List.of());
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(mentorProfileRepository.findByUserId(2L)).thenReturn(Optional.of(mentorProfile));
            when(requestRepository.findByMentorProfileIdAndStatus(20L, SENT, pageable)).thenReturn(emptyPage);

            service.getRequests(SENT, pageable);

            verify(requestRepository).findByMentorProfileIdAndStatus(20L, SENT, pageable);
        }
    }

    // ── markAsReviewing ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAsReviewing")
    class MarkAsReviewing {

        @Test
        @DisplayName("статус SENT → переходит в REVIEWING")
        void sentStatus_shouldSetReviewing() {
            MentoringRequest request = buildRequest(SENT);
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));
            when(requestRepository.save(request)).thenReturn(request);
            when(mapper.toResponse(request)).thenReturn(mockResponse());

            service.markAsReviewing(100L);

            assertThat(request.getStatus()).isEqualTo(REVIEWING);
            verify(requestRepository).save(request);
        }

        @Test
        @DisplayName("уже REVIEWING — идемпотентно, save не вызывается")
        void alreadyReviewing_shouldBeIdempotent() {
            MentoringRequest request = buildRequest(REVIEWING);
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));
            when(mapper.toResponse(request)).thenReturn(mockResponse());

            service.markAsReviewing(100L);

            assertThat(request.getStatus()).isEqualTo(REVIEWING);
            verify(requestRepository, never()).save(any());
        }
    }

    // ── requestClarification ──────────────────────────────────────────────────

    @Nested
    @DisplayName("requestClarification")
    class RequestClarification {

        private final MentoringRequestClarifyRequest dto =
                new MentoringRequestClarifyRequest("Уточните, пожалуйста");

        @Test
        @DisplayName("статус SENT → переходит в NEEDS_CLARIFICATION")
        void happyPath_shouldSetNeedsClarification() {
            MentoringRequest request = buildRequest(SENT);
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));
            when(requestRepository.save(request)).thenReturn(request);
            when(mapper.toResponse(request)).thenReturn(mockResponse());

            service.requestClarification(100L, dto);

            assertThat(request.getStatus()).isEqualTo(NEEDS_CLARIFICATION);
            assertThat(request.getClarificationNote()).isEqualTo("Уточните, пожалуйста");
        }

        @Test
        @DisplayName("статус ACCEPTED → BusinessRuleViolationException")
        void alreadyAccepted_shouldThrowBusinessRuleViolation() {
            MentoringRequest request = buildRequest(ACCEPTED);
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.requestClarification(100L, dto))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("не адресат → ForbiddenException")
        void notRecipient_shouldThrowForbidden() {
            MentoringRequest request = buildRequest(SENT);
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.requestClarification(100L, dto))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ── acceptRequest ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("acceptRequest")
    class AcceptRequest {

        @Test
        @DisplayName("статус SENT → переходит в ACCEPTED")
        void happyPath_shouldAccept() {
            MentoringRequest request = buildRequest(SENT);
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));
            when(requestRepository.save(request)).thenReturn(request);
            when(mapper.toResponse(request)).thenReturn(mockResponse());

            service.acceptRequest(100L);

            assertThat(request.getStatus()).isEqualTo(ACCEPTED);
            assertThat(request.getRespondedAt()).isNotNull();
        }

        @Test
        @DisplayName("лимит студентов достигнут → BusinessRuleViolationException")
        void menteeLimitExceeded_shouldThrowBusinessRuleViolation() {
            MentorProfile limitedMentor = MentorProfile.builder()
                    .user(mentorUser)
                    .menteeLimit(1)
                    .recruitmentStatus(RecruitmentStatus.OPEN)
                    .build();
            ReflectionTestUtils.setField(limitedMentor, "id", 20L);

            MentoringRequest request = MentoringRequest.builder()
                    .studentProfile(studentProfile)
                    .mentorProfile(limitedMentor)
                    .direction(MentoringRequestDirection.STUDENT_TO_MENTOR)
                    .status(SENT)
                    .goalType(MentoringType.PRACTICE)
                    .message("msg")
                    .build();
            ReflectionTestUtils.setField(request, "id", 100L);

            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));
            when(requestRepository.countByMentorProfileIdAndStatus(20L, ACCEPTED)).thenReturn(1L);

            assertThatThrownBy(() -> service.acceptRequest(100L))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("лимит");
        }

        @Test
        @DisplayName("уже обработана (ACCEPTED) → BusinessRuleViolationException")
        void alreadyAccepted_shouldThrowBusinessRuleViolation() {
            MentoringRequest request = buildRequest(ACCEPTED);
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.acceptRequest(100L))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("не адресат → ForbiddenException")
        void notRecipient_shouldThrowForbidden() {
            MentoringRequest request = buildRequest(SENT);
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.acceptRequest(100L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("заявка не найдена → NotFoundException")
        void notFound_shouldThrowNotFoundException() {
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(requestRepository.findWithProfilesById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.acceptRequest(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── rejectRequest ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectRequest")
    class RejectRequest {

        private final MentoringRequestRejectRequest dto = new MentoringRequestRejectRequest("Не подходит");

        @Test
        @DisplayName("статус SENT → переходит в REJECTED")
        void happyPath_shouldReject() {
            MentoringRequest request = buildRequest(SENT);
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));
            when(requestRepository.save(request)).thenReturn(request);
            when(mapper.toResponse(request)).thenReturn(mockResponse());

            service.rejectRequest(100L, dto);

            assertThat(request.getStatus()).isEqualTo(REJECTED);
            assertThat(request.getReason()).isEqualTo("Не подходит");
        }

        @Test
        @DisplayName("уже обработана → BusinessRuleViolationException")
        void alreadyProcessed_shouldThrowBusinessRuleViolation() {
            MentoringRequest request = buildRequest(REJECTED);
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.rejectRequest(100L, dto))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    // ── cancelRequest ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelRequest")
    class CancelRequest {

        @Test
        @DisplayName("статус SENT → переходит в CANCELLED")
        void happyPath_shouldCancel() {
            MentoringRequest request = buildRequest(SENT);
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));
            when(requestRepository.save(request)).thenReturn(request);
            when(mapper.toResponse(request)).thenReturn(mockResponse());

            service.cancelRequest(100L);

            assertThat(request.getStatus()).isEqualTo(CANCELLED);
        }

        @Test
        @DisplayName("статус ACCEPTED → BusinessRuleViolationException")
        void alreadyAccepted_shouldThrowBusinessRuleViolation() {
            MentoringRequest request = buildRequest(ACCEPTED);
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.cancelRequest(100L))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("обработанную");
        }

        @Test
        @DisplayName("не инициатор → ForbiddenException")
        void notInitiator_shouldThrowForbidden() {
            MentoringRequest request = buildRequest(SENT);
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.cancelRequest(100L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ── completeRequest ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("completeRequest")
    class CompleteRequest {

        @Test
        @DisplayName("статус ACCEPTED → переходит в COMPLETED")
        void happyPath_shouldComplete() {
            MentoringRequest request = buildRequest(ACCEPTED);
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));
            when(requestRepository.save(request)).thenReturn(request);
            when(mapper.toResponse(request)).thenReturn(mockResponse());

            service.completeRequest(100L);

            assertThat(request.getStatus()).isEqualTo(COMPLETED);
            assertThat(request.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("статус не ACCEPTED → BusinessRuleViolationException")
        void notAccepted_shouldThrowBusinessRuleViolation() {
            MentoringRequest request = buildRequest(SENT);
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.completeRequest(100L))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MentoringRequest buildRequest(MentoringRequestStatus status) {
        MentoringRequest request = MentoringRequest.builder()
                .studentProfile(studentProfile)
                .mentorProfile(mentorProfile)
                .direction(MentoringRequestDirection.STUDENT_TO_MENTOR)
                .status(status)
                .goalType(MentoringType.PRACTICE)
                .message("Test message")
                .build();
        ReflectionTestUtils.setField(request, "id", 100L);
        return request;
    }

    private MentoringRequestResponse mockResponse() {
        return new MentoringRequestResponse(
                100L, 10L, 20L, null, null,
                "STUDENT_TO_MENTOR", "SENT", "PRACTICE",
                "Test message", null, null, null, null, null);
    }
}
