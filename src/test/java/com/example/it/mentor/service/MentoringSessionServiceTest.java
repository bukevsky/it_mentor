package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.session.CancelSessionRequest;
import com.example.it.mentor.dto.session.CreateSessionRequest;
import com.example.it.mentor.dto.session.NextSessionSummary;
import com.example.it.mentor.dto.session.RescheduleSessionRequest;
import com.example.it.mentor.dto.session.SessionResponse;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.MentoringSession;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringRequestStatus;
import com.example.it.mentor.entity.enums.MentoringSessionStatus;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.MentoringSessionMapper;
import com.example.it.mentor.repository.MentoringRequestRepository;
import com.example.it.mentor.repository.MentoringSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentoringSessionService")
class MentoringSessionServiceTest {

    @InjectMocks private MentoringSessionService service;

    @Mock private MentoringSessionRepository sessionRepository;
    @Mock private MentoringRequestRepository requestRepository;
    @Mock private UserService userService;
    @Mock private MentoringSessionMapper mapper;

    private User studentUser;
    private User mentorUser;
    private MentoringRequest acceptedRequest;
    private MentoringSession scheduledSession;
    private SessionResponse sessionResponse;

    @BeforeEach
    void setUp() {
        studentUser = User.builder().email("student@test.com").build();
        ReflectionTestUtils.setField(studentUser, "id", 1L);

        mentorUser = User.builder().email("mentor@test.com").build();
        ReflectionTestUtils.setField(mentorUser, "id", 2L);

        StudentProfile studentProfile = StudentProfile.builder().user(studentUser).firstName("Иван").lastName("Иванов").build();
        ReflectionTestUtils.setField(studentProfile, "id", 10L);

        MentorProfile mentorProfile = MentorProfile.builder().user(mentorUser).firstName("Пётр").lastName("Петров").build();
        ReflectionTestUtils.setField(mentorProfile, "id", 20L);

        acceptedRequest = MentoringRequest.builder()
                .studentProfile(studentProfile)
                .mentorProfile(mentorProfile)
                .build();
        ReflectionTestUtils.setField(acceptedRequest, "id", 100L);
        acceptedRequest.setStatus(MentoringRequestStatus.ACCEPTED);

        scheduledSession = MentoringSession.builder()
                .mentoringRequest(acceptedRequest)
                .studentUser(studentUser)
                .mentorUser(mentorUser)
                .scheduledAt(OffsetDateTime.now().plusDays(1))
                .durationMinutes(60)
                .status(MentoringSessionStatus.SCHEDULED)
                .build();
        ReflectionTestUtils.setField(scheduledSession, "id", 500L);

        sessionResponse = new SessionResponse(
                500L, 100L, 1L, 2L, "Иван Иванов", "Пётр Петров",
                scheduledSession.getScheduledAt(), 60, MentoringSessionStatus.SCHEDULED,
                null, null, OffsetDateTime.now(), OffsetDateTime.now());
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("acceptedRequest — успешно создаёт сессию")
        void create_acceptedRequest_shouldReturnResponse() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(acceptedRequest));
            when(sessionRepository.findByMentorUserIdAndStatusInAndScheduledAtBetween(any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(sessionRepository.findByStudentUserIdAndStatusInAndScheduledAtBetween(any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(sessionRepository.save(any())).thenReturn(scheduledSession);
            when(mapper.toResponse(scheduledSession)).thenReturn(sessionResponse);

            CreateSessionRequest dto = new CreateSessionRequest(100L, scheduledSession.getScheduledAt(), 60);
            SessionResponse result = service.create(dto);

            assertThat(result).isEqualTo(sessionResponse);

            ArgumentCaptor<MentoringSession> captor = ArgumentCaptor.forClass(MentoringSession.class);
            verify(sessionRepository).save(captor.capture());
            MentoringSession saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(MentoringSessionStatus.SCHEDULED);
            assertThat(saved.getStudentUser()).isSameAs(studentUser);
            assertThat(saved.getMentorUser()).isSameAs(mentorUser);
        }

        @Test
        @DisplayName("requestNotAccepted — заявка не принята → BusinessRule")
        void create_requestNotAccepted_shouldThrowBusinessRule() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            acceptedRequest.setStatus(MentoringRequestStatus.SENT);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(acceptedRequest));

            CreateSessionRequest dto = new CreateSessionRequest(100L, OffsetDateTime.now().plusDays(1), 60);

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(BusinessRuleViolationException.class);
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("nonParticipant — посторонний → Forbidden")
        void create_nonParticipant_shouldThrowForbidden() {
            User outsider = User.builder().email("o@test.com").build();
            ReflectionTestUtils.setField(outsider, "id", 99L);
            when(userService.getCurrentUserEntity()).thenReturn(outsider);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(acceptedRequest));

            CreateSessionRequest dto = new CreateSessionRequest(100L, OffsetDateTime.now().plusDays(1), 60);

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("conflictOnMentorSide — у ментора есть пересекающаяся сессия → Conflict")
        void create_conflictOnMentorSide_shouldThrowConflict() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(acceptedRequest));
            OffsetDateTime newStart = OffsetDateTime.now().plusDays(1);
            MentoringSession conflicting = MentoringSession.builder()
                    .scheduledAt(newStart.minusMinutes(10))
                    .durationMinutes(60)
                    .status(MentoringSessionStatus.SCHEDULED)
                    .build();
            ReflectionTestUtils.setField(conflicting, "id", 999L);
            when(sessionRepository.findByMentorUserIdAndStatusInAndScheduledAtBetween(any(), any(), any(), any()))
                    .thenReturn(List.of(conflicting));

            CreateSessionRequest dto = new CreateSessionRequest(100L, newStart, 60);

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(ConflictException.class);
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("conflictOnStudentSide — у студента есть пересекающаяся сессия → Conflict")
        void create_conflictOnStudentSide_shouldThrowConflict() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(acceptedRequest));
            when(sessionRepository.findByMentorUserIdAndStatusInAndScheduledAtBetween(any(), any(), any(), any()))
                    .thenReturn(List.of());
            OffsetDateTime newStart = OffsetDateTime.now().plusDays(1);
            MentoringSession conflicting = MentoringSession.builder()
                    .scheduledAt(newStart.plusMinutes(15))
                    .durationMinutes(60)
                    .status(MentoringSessionStatus.SCHEDULED)
                    .build();
            ReflectionTestUtils.setField(conflicting, "id", 998L);
            when(sessionRepository.findByStudentUserIdAndStatusInAndScheduledAtBetween(any(), any(), any(), any()))
                    .thenReturn(List.of(conflicting));

            CreateSessionRequest dto = new CreateSessionRequest(100L, newStart, 60);

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("nonOverlappingExistingSession — соседняя сессия далеко → OK")
        void create_nonOverlappingExistingSession_shouldPass() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(requestRepository.findWithProfilesById(100L)).thenReturn(Optional.of(acceptedRequest));
            OffsetDateTime newStart = OffsetDateTime.now().plusDays(1);
            MentoringSession farAway = MentoringSession.builder()
                    .scheduledAt(newStart.plusHours(5))
                    .durationMinutes(60)
                    .status(MentoringSessionStatus.SCHEDULED)
                    .build();
            ReflectionTestUtils.setField(farAway, "id", 997L);
            when(sessionRepository.findByMentorUserIdAndStatusInAndScheduledAtBetween(any(), any(), any(), any()))
                    .thenReturn(List.of(farAway));
            when(sessionRepository.findByStudentUserIdAndStatusInAndScheduledAtBetween(any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(sessionRepository.save(any())).thenReturn(scheduledSession);
            when(mapper.toResponse(scheduledSession)).thenReturn(sessionResponse);

            SessionResponse result = service.create(new CreateSessionRequest(100L, newStart, 60));

            assertThat(result).isEqualTo(sessionResponse);
        }

        @Test
        @DisplayName("requestNotFound — заявка не найдена → NotFound")
        void create_requestNotFound_shouldThrowNotFound() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(requestRepository.findWithProfilesById(999L)).thenReturn(Optional.empty());

            CreateSessionRequest dto = new CreateSessionRequest(999L, OffsetDateTime.now().plusDays(1), 60);

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── reschedule ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reschedule")
    class Reschedule {

        @Test
        @DisplayName("happyPath — переводит в RESCHEDULED")
        void reschedule_happyPath_shouldSetRescheduledStatus() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(sessionRepository.findWithParticipantsById(500L)).thenReturn(Optional.of(scheduledSession));
            when(sessionRepository.findByMentorUserIdAndStatusInAndScheduledAtBetween(any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(sessionRepository.findByStudentUserIdAndStatusInAndScheduledAtBetween(any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(sessionRepository.save(any())).thenReturn(scheduledSession);
            when(mapper.toResponse(scheduledSession)).thenReturn(sessionResponse);

            OffsetDateTime newTime = OffsetDateTime.now().plusDays(2);
            RescheduleSessionRequest dto = new RescheduleSessionRequest(newTime, 90, "Конфликт расписания");
            service.reschedule(500L, dto);

            assertThat(scheduledSession.getStatus()).isEqualTo(MentoringSessionStatus.RESCHEDULED);
            assertThat(scheduledSession.getScheduledAt()).isEqualTo(newTime);
            assertThat(scheduledSession.getDurationMinutes()).isEqualTo(90);
            assertThat(scheduledSession.getRescheduleReason()).isEqualTo("Конфликт расписания");
        }

        @Test
        @DisplayName("completedSession — нельзя перенести → BusinessRule")
        void reschedule_completedSession_shouldThrowBusinessRule() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            scheduledSession.setStatus(MentoringSessionStatus.COMPLETED);
            when(sessionRepository.findWithParticipantsById(500L)).thenReturn(Optional.of(scheduledSession));

            RescheduleSessionRequest dto = new RescheduleSessionRequest(OffsetDateTime.now().plusDays(2), 60, null);

            assertThatThrownBy(() -> service.reschedule(500L, dto))
                    .isInstanceOf(BusinessRuleViolationException.class);
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("rescheduleConflictExcludesSelf — собственная сессия не блокирует перенос")
        void reschedule_existingSessionItselfMatches_shouldNotBeConflict() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(sessionRepository.findWithParticipantsById(500L)).thenReturn(Optional.of(scheduledSession));
            when(sessionRepository.findByMentorUserIdAndStatusInAndScheduledAtBetween(any(), any(), any(), any()))
                    .thenReturn(List.of(scheduledSession));
            when(sessionRepository.findByStudentUserIdAndStatusInAndScheduledAtBetween(any(), any(), any(), any()))
                    .thenReturn(List.of(scheduledSession));
            when(sessionRepository.save(any())).thenReturn(scheduledSession);
            when(mapper.toResponse(scheduledSession)).thenReturn(sessionResponse);

            RescheduleSessionRequest dto = new RescheduleSessionRequest(OffsetDateTime.now().plusDays(3), 60, null);
            SessionResponse result = service.reschedule(500L, dto);

            assertThat(result).isEqualTo(sessionResponse);
        }
    }

    // ── cancel ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("happyPath — сохраняет CANCELLED и причину")
        void cancel_happyPath_shouldSetCancelled() {
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            when(sessionRepository.findWithParticipantsById(500L)).thenReturn(Optional.of(scheduledSession));
            when(sessionRepository.save(any())).thenReturn(scheduledSession);
            when(mapper.toResponse(scheduledSession)).thenReturn(sessionResponse);

            service.cancel(500L, new CancelSessionRequest("Заболел"));

            assertThat(scheduledSession.getStatus()).isEqualTo(MentoringSessionStatus.CANCELLED);
            assertThat(scheduledSession.getCancelReason()).isEqualTo("Заболел");
        }

        @Test
        @DisplayName("completedSession — нельзя отменить завершённую → BusinessRule")
        void cancel_completedSession_shouldThrowBusinessRule() {
            when(userService.getCurrentUserEntity()).thenReturn(mentorUser);
            scheduledSession.setStatus(MentoringSessionStatus.COMPLETED);
            when(sessionRepository.findWithParticipantsById(500L)).thenReturn(Optional.of(scheduledSession));

            assertThatThrownBy(() -> service.cancel(500L, new CancelSessionRequest(null)))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    // ── complete ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("beforeScheduledAt — рано завершать → BusinessRule")
        void complete_beforeScheduledAt_shouldThrowBusinessRule() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(sessionRepository.findWithParticipantsById(500L)).thenReturn(Optional.of(scheduledSession));

            assertThatThrownBy(() -> service.complete(500L))
                    .isInstanceOf(BusinessRuleViolationException.class);
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("afterScheduledAt — переводит в COMPLETED")
        void complete_afterScheduledAt_shouldSetCompleted() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            scheduledSession.setScheduledAt(OffsetDateTime.now().minusHours(2));
            when(sessionRepository.findWithParticipantsById(500L)).thenReturn(Optional.of(scheduledSession));
            when(sessionRepository.save(any())).thenReturn(scheduledSession);
            when(mapper.toResponse(scheduledSession)).thenReturn(sessionResponse);

            service.complete(500L);

            assertThat(scheduledSession.getStatus()).isEqualTo(MentoringSessionStatus.COMPLETED);
        }

        @Test
        @DisplayName("alreadyCompleted — повторный complete → BusinessRule")
        void complete_alreadyCompleted_shouldThrowBusinessRule() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            scheduledSession.setStatus(MentoringSessionStatus.COMPLETED);
            when(sessionRepository.findWithParticipantsById(500L)).thenReturn(Optional.of(scheduledSession));

            assertThatThrownBy(() -> service.complete(500L))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    // ── getById / list ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById and list")
    class Reads {

        @Test
        @DisplayName("getById — посторонний → Forbidden")
        void getById_nonParticipant_shouldThrowForbidden() {
            User outsider = User.builder().build();
            ReflectionTestUtils.setField(outsider, "id", 99L);
            when(userService.getCurrentUserEntity()).thenReturn(outsider);
            when(sessionRepository.findWithParticipantsById(500L)).thenReturn(Optional.of(scheduledSession));

            assertThatThrownBy(() -> service.getById(500L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("list — возвращает пагинированный ответ для текущего пользователя")
        void list_currentUser_shouldReturnPage() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            Page<MentoringSession> page = new PageImpl<>(List.of(scheduledSession));
            when(sessionRepository.findByStudentUserIdOrMentorUserId(anyLong(), anyLong(), any())).thenReturn(page);
            when(mapper.toResponse(scheduledSession)).thenReturn(sessionResponse);

            PagedResponse<SessionResponse> result = service.list(PageRequest.of(0, 20));

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0)).isEqualTo(sessionResponse);
        }
    }

    // ── findNextForCurrentUser ───────────────────────────────────────────────

    @Nested
    @DisplayName("findNextForCurrentUser")
    class FindNext {

        @Test
        @DisplayName("returnsNearestScheduled — есть ближайшая сессия")
        void findNextForCurrentUser_returnsNearestScheduled() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(sessionRepository.findNextForUser(any(), any(), any()))
                    .thenReturn(List.of(scheduledSession));
            NextSessionSummary summary = new NextSessionSummary(500L, 100L,
                    scheduledSession.getScheduledAt(), 60, "Пётр Петров");
            lenient().when(mapper.toSummary(scheduledSession, 1L)).thenReturn(summary);

            Optional<NextSessionSummary> result = service.findNextForCurrentUser();

            assertThat(result).isPresent();
            assertThat(result.get().counterpartyName()).isEqualTo("Пётр Петров");
        }

        @Test
        @DisplayName("noUpcoming — нет сессий → Optional.empty")
        void findNextForCurrentUser_noUpcoming_returnsEmpty() {
            when(userService.getCurrentUserEntity()).thenReturn(studentUser);
            when(sessionRepository.findNextForUser(any(), any(), any())).thenReturn(List.of());

            Optional<NextSessionSummary> result = service.findNextForCurrentUser();

            assertThat(result).isEmpty();
        }
    }
}
