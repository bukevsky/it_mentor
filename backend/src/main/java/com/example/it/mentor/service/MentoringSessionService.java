package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.session.CancelSessionRequest;
import com.example.it.mentor.dto.session.CreateSessionRequest;
import com.example.it.mentor.dto.session.NextSessionSummary;
import com.example.it.mentor.dto.session.RescheduleSessionRequest;
import com.example.it.mentor.dto.session.SessionResponse;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.MentoringSession;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringRequestStatus;
import com.example.it.mentor.entity.enums.MentoringSessionStatus;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.event.session.*;
import com.example.it.mentor.mapper.MentoringSessionMapper;
import com.example.it.mentor.repository.MentoringRequestRepository;
import com.example.it.mentor.repository.MentoringSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Сервис управления календарными сессиями менторинга.
 *
 * <p>Создание, перенос, отмена, завершение и просмотр сессий, привязанных к
 * принятым заявкам на менторство.</p>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MentoringSessionService {

    private static final Set<MentoringSessionStatus> ACTIVE_STATUSES =
            Set.of(MentoringSessionStatus.SCHEDULED, MentoringSessionStatus.RESCHEDULED);

    private static final int CONFLICT_BUFFER_MINUTES = 5;

    private static final int MAX_DURATION_MINUTES = 480;

    private final MentoringSessionRepository sessionRepository;
    private final MentoringRequestRepository requestRepository;
    private final UserService userService;
    private final MentoringSessionMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Создаёт новую сессию менторинга для принятой заявки.
     *
     * @param dto параметры создания сессии
     * @return созданная сессия
     */
    @Transactional
    public SessionResponse create(CreateSessionRequest dto) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringRequest request = requestRepository.findWithProfilesById(dto.mentoringRequestId())
                .orElseThrow(() -> new NotFoundException("Заявка не найдена: " + dto.mentoringRequestId()));

        Long studentUserId = request.getStudentProfile().getUser().getId();
        Long mentorUserId = request.getMentorProfile().getUser().getId();
        ensureParticipant(currentUser.getId(), studentUserId, mentorUserId);

        if (request.getStatus() != MentoringRequestStatus.ACCEPTED) {
            throw new BusinessRuleViolationException("Сессию можно создать только для принятой заявки");
        }

        ensureNoConflict(null, studentUserId, mentorUserId, dto.scheduledAt(), dto.durationMinutes());

        MentoringSession session = MentoringSession.builder()
                .mentoringRequest(request)
                .studentUser(request.getStudentProfile().getUser())
                .mentorUser(request.getMentorProfile().getUser())
                .scheduledAt(dto.scheduledAt())
                .durationMinutes(dto.durationMinutes())
                .status(MentoringSessionStatus.SCHEDULED)
                .build();

        session = sessionRepository.save(session);
        log.info("Сессия создана: sessionId={}, requestId={}, userId={}, studentUserId={}, mentorUserId={}, " +
                        "status={}, scheduledAt={}, durationMinutes={}, step={}",
                session.getId(), request.getId(), currentUser.getId(), studentUserId, mentorUserId,
                session.getStatus(), session.getScheduledAt(), session.getDurationMinutes(), "mentoring_session_created");
        eventPublisher.publishEvent(new MentoringSessionCreatedEvent(session.getId(), studentUserId));
        eventPublisher.publishEvent(new MentoringSessionCreatedEvent(session.getId(), mentorUserId));
        return mapper.toResponse(session);
    }

    /**
     * Переносит сессию на новое время.
     *
     * @param sessionId идентификатор сессии
     * @param dto параметры переноса
     * @return обновлённая сессия
     */
    @Transactional
    public SessionResponse reschedule(Long sessionId, RescheduleSessionRequest dto) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringSession session = loadWithParticipants(sessionId);
        ensureParticipant(currentUser.getId(), session.getStudentUser().getId(), session.getMentorUser().getId());
        ensureActive(session, "перенести");

        ensureNoConflict(session.getId(), session.getStudentUser().getId(), session.getMentorUser().getId(),
                dto.newScheduledAt(), dto.durationMinutes());

        MentoringSessionStatus oldStatus = session.getStatus();
        OffsetDateTime oldScheduledAt = session.getScheduledAt();
        session.setScheduledAt(dto.newScheduledAt());
        session.setDurationMinutes(dto.durationMinutes());
        session.setRescheduleReason(dto.reason());
        session.setStatus(MentoringSessionStatus.RESCHEDULED);
        sessionRepository.save(session);
        log.info("Сессия перенесена: sessionId={}, userId={}, oldStatus={}, newStatus={}, oldScheduledAt={}, " +
                        "scheduledAt={}, durationMinutes={}, step={}",
                session.getId(), currentUser.getId(), oldStatus, session.getStatus(), oldScheduledAt,
                session.getScheduledAt(), session.getDurationMinutes(), "mentoring_session_rescheduled");
        Long otherUserId = currentUser.getId().equals(session.getStudentUser().getId())
                ? session.getMentorUser().getId()
                : session.getStudentUser().getId();
        eventPublisher.publishEvent(new MentoringSessionRescheduledEvent(session.getId(), otherUserId));
        return mapper.toResponse(session);
    }

    /**
     * Отменяет запланированную сессию.
     *
     * @param sessionId идентификатор сессии
     * @param dto параметры отмены
     * @return обновлённая сессия
     */
    @Transactional
    public SessionResponse cancel(Long sessionId, CancelSessionRequest dto) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringSession session = loadWithParticipants(sessionId);
        ensureParticipant(currentUser.getId(), session.getStudentUser().getId(), session.getMentorUser().getId());
        ensureActive(session, "отменить");

        MentoringSessionStatus oldStatus = session.getStatus();
        session.setStatus(MentoringSessionStatus.CANCELLED);
        session.setCancelReason(dto != null ? dto.reason() : null);
        sessionRepository.save(session);
        log.info("Сессия отменена: sessionId={}, userId={}, oldStatus={}, newStatus={}, step={}",
                session.getId(), currentUser.getId(), oldStatus, session.getStatus(), "mentoring_session_cancelled");
        Long otherUserId = currentUser.getId().equals(session.getStudentUser().getId())
                ? session.getMentorUser().getId()
                : session.getStudentUser().getId();
        eventPublisher.publishEvent(new MentoringSessionCancelledEvent(session.getId(), otherUserId));
        return mapper.toResponse(session);
    }

    /**
     * Помечает сессию как завершённую после её планового времени.
     *
     * @param sessionId идентификатор сессии
     * @return обновлённая сессия
     */
    @Transactional
    public SessionResponse complete(Long sessionId) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringSession session = loadWithParticipants(sessionId);
        ensureParticipant(currentUser.getId(), session.getStudentUser().getId(), session.getMentorUser().getId());
        ensureActive(session, "завершить");

        if (session.getScheduledAt().isAfter(OffsetDateTime.now())) {
            throw new BusinessRuleViolationException("Сессию нельзя завершить до её планового времени");
        }

        MentoringSessionStatus oldStatus = session.getStatus();
        session.setStatus(MentoringSessionStatus.COMPLETED);
        sessionRepository.save(session);
        log.info("Сессия завершена: sessionId={}, userId={}, oldStatus={}, newStatus={}, step={}",
                session.getId(), currentUser.getId(), oldStatus, session.getStatus(), "mentoring_session_completed");
        return mapper.toResponse(session);
    }

    /**
     * Возвращает сессию по идентификатору, если пользователь участвует.
     *
     * @param sessionId идентификатор сессии
     * @return найденная сессия
     */
    public SessionResponse getById(Long sessionId) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringSession session = loadWithParticipants(sessionId);
        ensureParticipant(currentUser.getId(), session.getStudentUser().getId(), session.getMentorUser().getId());
        log.debug("Сессия загружена: sessionId={}, userId={}, status={}, step={}",
                sessionId, currentUser.getId(), session.getStatus(), "mentoring_session_loaded");
        return mapper.toResponse(session);
    }

    /**
     * Возвращает страницу сессий, в которых участвует текущий пользователь.
     *
     * @param pageable параметры пагинации
     * @return страница сессий
     */
    public PagedResponse<SessionResponse> list(Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();
        Page<MentoringSession> page = sessionRepository.findByStudentUserIdOrMentorUserId(
                currentUser.getId(), currentUser.getId(), pageable);
        log.debug("Список сессий загружен: userId={}, page={}, size={}, resultCount={}, total={}, step={}",
                currentUser.getId(), pageable.getPageNumber(), pageable.getPageSize(), page.getNumberOfElements(),
                page.getTotalElements(), "mentoring_sessions_loaded");
        return PagedResponse.from(page.map(mapper::toResponse));
    }

    /**
     * Возвращает ближайшую SCHEDULED/RESCHEDULED сессию для текущего пользователя.
     *
     * @return ближайшая сессия для дашборда
     */
    public Optional<NextSessionSummary> findNextForCurrentUser() {
        User currentUser = userService.getCurrentUserEntity();
        List<MentoringSession> next = sessionRepository.findNextForUser(
                currentUser.getId(), OffsetDateTime.now(), PageRequest.of(0, 1));
        if (next.isEmpty()) {
            log.debug("Ближайшая сессия не найдена: userId={}, step={}",
                    currentUser.getId(), "next_mentoring_session_not_found");
            return Optional.empty();
        }
        NextSessionSummary summary = mapper.toSummary(next.get(0), currentUser.getId());
        log.debug("Ближайшая сессия найдена: userId={}, sessionId={}, step={}",
                currentUser.getId(), summary.id(), "next_mentoring_session_found");
        return Optional.of(summary);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private MentoringSession loadWithParticipants(Long id) {
        return sessionRepository.findWithParticipantsById(id)
                .orElseThrow(() -> new NotFoundException("Сессия не найдена: " + id));
    }

    private void ensureParticipant(Long userId, Long studentUserId, Long mentorUserId) {
        if (!Objects.equals(userId, studentUserId) && !Objects.equals(userId, mentorUserId)) {
            throw new ForbiddenException("Доступ к сессии запрещён");
        }
    }

    private void ensureActive(MentoringSession session, String actionVerb) {
        if (!ACTIVE_STATUSES.contains(session.getStatus())) {
            throw new BusinessRuleViolationException("Нельзя " + actionVerb + " сессию в статусе " + session.getStatus());
        }
    }

    private void ensureNoConflict(Long excludeSessionId, Long studentUserId, Long mentorUserId,
                                  OffsetDateTime scheduledAt, Integer durationMinutes) {
        OffsetDateTime newStart = scheduledAt;
        OffsetDateTime newEnd = scheduledAt.plusMinutes(durationMinutes);
        // Wide DB lookup window: any existing session whose start lies here could possibly overlap;
        // precise overlap is then checked in Java because the existing session's duration matters.
        OffsetDateTime lookupFrom = scheduledAt.minusMinutes(MAX_DURATION_MINUTES + CONFLICT_BUFFER_MINUTES);
        OffsetDateTime lookupTo = scheduledAt.plusMinutes(durationMinutes + CONFLICT_BUFFER_MINUTES);

        boolean mentorConflict = sessionRepository
                .findByMentorUserIdAndStatusInAndScheduledAtBetween(mentorUserId, ACTIVE_STATUSES, lookupFrom, lookupTo)
                .stream()
                .filter(existing -> !Objects.equals(existing.getId(), excludeSessionId))
                .anyMatch(existing -> overlaps(existing, newStart, newEnd));
        if (mentorConflict) {
            throw new ConflictException("У ментора уже есть сессия в это время");
        }

        boolean studentConflict = sessionRepository
                .findByStudentUserIdAndStatusInAndScheduledAtBetween(studentUserId, ACTIVE_STATUSES, lookupFrom, lookupTo)
                .stream()
                .filter(existing -> !Objects.equals(existing.getId(), excludeSessionId))
                .anyMatch(existing -> overlaps(existing, newStart, newEnd));
        if (studentConflict) {
            throw new ConflictException("У студента уже есть сессия в это время");
        }
    }

    private static boolean overlaps(MentoringSession existing, OffsetDateTime newStart, OffsetDateTime newEnd) {
        OffsetDateTime existingStart = existing.getScheduledAt().minusMinutes(CONFLICT_BUFFER_MINUTES);
        OffsetDateTime existingEnd = existing.getScheduledAt()
                .plusMinutes(existing.getDurationMinutes() + CONFLICT_BUFFER_MINUTES);
        return existingStart.isBefore(newEnd) && existingEnd.isAfter(newStart);
    }
}
