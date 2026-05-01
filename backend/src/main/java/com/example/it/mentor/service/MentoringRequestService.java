package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.mentoring.MentoringRequestClarifyRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestCreateRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestRejectRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestResponse;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringRequestDirection;
import com.example.it.mentor.entity.enums.MentoringRequestStatus;
import com.example.it.mentor.entity.enums.RecruitmentStatus;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.mapper.MentoringRequestMapper;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.MentoringRequestRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static com.example.it.mentor.entity.enums.MentoringRequestStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentoringRequestService {

    private static final List<MentoringRequestStatus> ACTIVE_STATUSES =
            List.of(SENT, REVIEWING, NEEDS_CLARIFICATION, ACCEPTED);

    private static final List<MentoringRequestStatus> PRE_ACCEPT_STATUSES =
            List.of(SENT, REVIEWING, NEEDS_CLARIFICATION);

    private final MentoringRequestRepository requestRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserService userService;
    private final MentoringRequestMapper mapper;
    private final ChatService chatService;

    @Transactional
    public MentoringRequestResponse createRequest(MentoringRequestCreateRequest dto) {
        User currentUser = userService.getCurrentUserEntity();

        boolean isStudent = hasRole(currentUser, RoleCode.STUDENT);
        boolean isMentor  = hasRole(currentUser, RoleCode.MENTOR);

        if (!isStudent && !isMentor) {
            throw new ForbiddenException("Только студент или ментор может создавать заявку");
        }

        StudentProfile studentProfile;
        MentorProfile mentorProfile;
        MentoringRequestDirection direction;

        if (isMentor) {
            direction = MentoringRequestDirection.MENTOR_TO_STUDENT;
            mentorProfile = mentorProfileRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Профиль ментора не найден"));
            studentProfile = studentProfileRepository.findById(dto.targetProfileId())
                    .orElseThrow(() -> new NotFoundException("Профиль студента не найден: " + dto.targetProfileId()));
        } else {
            direction = MentoringRequestDirection.STUDENT_TO_MENTOR;
            studentProfile = studentProfileRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Профиль студента не найден"));
            mentorProfile = mentorProfileRepository.findById(dto.targetProfileId())
                    .orElseThrow(() -> new NotFoundException("Профиль ментора не найден: " + dto.targetProfileId()));
        }

        if (Objects.equals(studentProfile.getUser().getId(), mentorProfile.getUser().getId())) {
            throw new BusinessRuleViolationException("Нельзя создать заявку самому себе");
        }

        if (isStudent && mentorProfile.getRecruitmentStatus() != RecruitmentStatus.OPEN) {
            throw new BusinessRuleViolationException("Ментор не принимает новых студентов");
        }

        if (requestRepository.existsByStudentProfileIdAndMentorProfileIdAndStatusIn(
                studentProfile.getId(), mentorProfile.getId(), ACTIVE_STATUSES)) {
            throw new ConflictException("Активная заявка между этим студентом и ментором уже существует");
        }

        MentoringRequest request = MentoringRequest.builder()
                .studentProfile(studentProfile)
                .mentorProfile(mentorProfile)
                .direction(direction)
                .goalType(dto.goalType())
                .message(dto.message())
                .build();

        requestRepository.save(request);
        log.info("Заявка создана: requestId={}, direction={}, studentProfileId={}, mentorProfileId={}", request.getId(), direction, studentProfile.getId(), mentorProfile.getId());

        return mapper.toResponse(request);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MentoringRequestResponse> getRequests(MentoringRequestStatus status, Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();

        Page<MentoringRequest> page;

        if (hasRole(currentUser, RoleCode.MENTOR)) {
            MentorProfile mentorProfile = mentorProfileRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Профиль ментора не найден"));
            page = status != null
                    ? requestRepository.findByMentorProfileIdAndStatus(mentorProfile.getId(), status, pageable)
                    : requestRepository.findByMentorProfileId(mentorProfile.getId(), pageable);
        } else if (hasRole(currentUser, RoleCode.STUDENT)) {
            StudentProfile studentProfile = studentProfileRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Профиль студента не найден"));
            page = status != null
                    ? requestRepository.findByStudentProfileIdAndStatus(studentProfile.getId(), status, pageable)
                    : requestRepository.findByStudentProfileId(studentProfile.getId(), pageable);
        } else {
            throw new ForbiddenException("Только студент или ментор может просматривать заявки");
        }

        return PagedResponse.from(page.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public MentoringRequestResponse getById(Long requestId) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringRequest request = loadWithProfiles(requestId);
        checkParticipant(request, currentUser);
        return mapper.toResponse(request);
    }

    @Transactional
    public MentoringRequestResponse markAsReviewing(Long requestId) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringRequest request = loadWithProfiles(requestId);
        checkRecipient(request, currentUser);

        if (request.getStatus() == SENT) {
            request.setStatus(REVIEWING);
            requestRepository.save(request);
            log.info("Заявка взята в работу: requestId={}, userId={}", requestId, currentUser.getId());
        }
        return mapper.toResponse(request);
    }

    @Transactional
    public MentoringRequestResponse requestClarification(Long requestId, MentoringRequestClarifyRequest dto) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringRequest request = loadWithProfiles(requestId);
        checkRecipient(request, currentUser);
        requirePreAcceptStatus(request);

        request.setStatus(NEEDS_CLARIFICATION);
        request.setClarificationNote(dto.clarificationNote());
        request.setRespondedAt(OffsetDateTime.now());
        requestRepository.save(request);
        log.info("Запрошено уточнение: requestId={}, userId={}", requestId, currentUser.getId());
        return mapper.toResponse(request);
    }

    @Transactional
    public MentoringRequestResponse acceptRequest(Long requestId) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringRequest request = loadWithProfiles(requestId);
        checkRecipient(request, currentUser);
        requirePreAcceptStatus(request);

        if (request.getDirection() == MentoringRequestDirection.STUDENT_TO_MENTOR) {
            MentorProfile mentorProfile = request.getMentorProfile();
            if (mentorProfile.getMenteeLimit() != null) {
                long accepted = requestRepository.countByMentorProfileIdAndStatus(mentorProfile.getId(), ACCEPTED);
                if (accepted >= mentorProfile.getMenteeLimit()) {
                    throw new BusinessRuleViolationException("Ментор достиг лимита студентов");
                }
            }
        }

        request.setStatus(ACCEPTED);
        request.setRespondedAt(OffsetDateTime.now());
        requestRepository.save(request);
        log.info("Заявка принята: requestId={}, userId={}", requestId, currentUser.getId());
        chatService.createForRequest(request);
        return mapper.toResponse(request);
    }

    @Transactional
    public MentoringRequestResponse rejectRequest(Long requestId, MentoringRequestRejectRequest dto) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringRequest request = loadWithProfiles(requestId);
        checkRecipient(request, currentUser);
        requirePreAcceptStatus(request);

        request.setStatus(REJECTED);
        request.setReason(dto.reason());
        request.setRespondedAt(OffsetDateTime.now());
        requestRepository.save(request);
        log.info("Заявка отклонена: requestId={}, userId={}", requestId, currentUser.getId());
        return mapper.toResponse(request);
    }

    @Transactional
    public MentoringRequestResponse cancelRequest(Long requestId) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringRequest request = loadWithProfiles(requestId);
        checkInitiator(request, currentUser);

        if (!PRE_ACCEPT_STATUSES.contains(request.getStatus())) {
            throw new BusinessRuleViolationException("Нельзя отменить уже обработанную заявку");
        }

        request.setStatus(CANCELLED);
        requestRepository.save(request);
        log.info("Заявка отменена: requestId={}, userId={}", requestId, currentUser.getId());
        return mapper.toResponse(request);
    }

    @Transactional
    public MentoringRequestResponse completeRequest(Long requestId) {
        User currentUser = userService.getCurrentUserEntity();
        MentoringRequest request = loadWithProfiles(requestId);
        checkParticipant(request, currentUser);

        if (request.getStatus() != ACCEPTED) {
            throw new BusinessRuleViolationException("Завершить можно только принятую заявку");
        }

        request.setStatus(COMPLETED);
        request.setCompletedAt(OffsetDateTime.now());
        requestRepository.save(request);
        log.info("Менторинг завершён: requestId={}, userId={}", requestId, currentUser.getId());
        return mapper.toResponse(request);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MentoringRequest loadWithProfiles(Long id) {
        return requestRepository.findWithProfilesById(id)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена: " + id));
    }

    private boolean hasRole(User user, RoleCode code) {
        return user.getRoles().stream().anyMatch(r -> r.getCode() == code);
    }

    private Long initiatorUserId(MentoringRequest request) {
        return request.getDirection() == MentoringRequestDirection.STUDENT_TO_MENTOR
                ? request.getStudentProfile().getUser().getId()
                : request.getMentorProfile().getUser().getId();
    }

    private Long recipientUserId(MentoringRequest request) {
        return request.getDirection() == MentoringRequestDirection.STUDENT_TO_MENTOR
                ? request.getMentorProfile().getUser().getId()
                : request.getStudentProfile().getUser().getId();
    }

    private void checkRecipient(MentoringRequest request, User currentUser) {
        if (!Objects.equals(recipientUserId(request), currentUser.getId())) {
            throw new ForbiddenException("Операция доступна только адресату заявки");
        }
    }

    private void checkInitiator(MentoringRequest request, User currentUser) {
        if (!Objects.equals(initiatorUserId(request), currentUser.getId())) {
            throw new ForbiddenException("Операция доступна только инициатору заявки");
        }
    }

    private void checkParticipant(MentoringRequest request, User currentUser) {
        Long userId = currentUser.getId();
        if (!Objects.equals(initiatorUserId(request), userId)
                && !Objects.equals(recipientUserId(request), userId)) {
            throw new ForbiddenException("Доступ к заявке запрещён");
        }
    }

    private void requirePreAcceptStatus(MentoringRequest request) {
        if (!PRE_ACCEPT_STATUSES.contains(request.getStatus())) {
            throw new BusinessRuleViolationException("Заявка уже обработана");
        }
    }
}
