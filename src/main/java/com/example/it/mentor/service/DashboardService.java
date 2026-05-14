package com.example.it.mentor.service;

import com.example.it.mentor.dto.dashboard.ActivityItemResponse;
import com.example.it.mentor.dto.dashboard.DashboardSummaryResponse;
import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringRequestStatus;
import com.example.it.mentor.repository.ChatReadStateRepository;
import com.example.it.mentor.repository.ChatRepository;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.MentoringRequestRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private final MentoringRequestRepository requestRepository;
    private final ChatRepository chatRepository;
    private final ChatReadStateRepository readStateRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserService userService;
    private final StudentProfileService studentProfileService;
    private final MentoringSessionService mentoringSessionService;

    public DashboardSummaryResponse getSummary() {
        User user = userService.getCurrentUserEntity();
        String role = user.primaryRole().name();

        int sentRequests = 0;
        int pendingRequests = 0;
        int acceptedRequests = 0;
        int profileCompletion = 0;

        if ("STUDENT".equals(role)) {
            var profileOpt = studentProfileRepository.findByUserId(user.getId());
            if (profileOpt.isPresent()) {
                Long pid = profileOpt.get().getId();
                sentRequests = (int) requestRepository.countByStudentProfileIdAndStatus(
                        pid, MentoringRequestStatus.SENT);
                pendingRequests = (int) requestRepository.countByStudentProfileIdAndStatusIn(
                        pid, List.of(MentoringRequestStatus.REVIEWING, MentoringRequestStatus.NEEDS_CLARIFICATION));
                acceptedRequests = (int) requestRepository.countByStudentProfileIdAndStatus(
                        pid, MentoringRequestStatus.ACCEPTED);
                profileCompletion = studentProfileService.getCompletion().percent();
            }
        } else if ("MENTOR".equals(role)) {
            var profileOpt = mentorProfileRepository.findByUserId(user.getId());
            if (profileOpt.isPresent()) {
                Long pid = profileOpt.get().getId();
                sentRequests = (int) requestRepository.countByMentorProfileIdAndStatus(
                        pid, MentoringRequestStatus.SENT);
                pendingRequests = (int) requestRepository.countByMentorProfileIdAndStatusIn(
                        pid, List.of(MentoringRequestStatus.REVIEWING, MentoringRequestStatus.NEEDS_CLARIFICATION));
                acceptedRequests = (int) requestRepository.countByMentorProfileIdAndStatus(
                        pid, MentoringRequestStatus.ACCEPTED);
            }
        }

        int totalChats = (int) chatRepository.countByUserId(user.getId());
        Map<Long, Long> unreadMap = readStateRepository.countUnreadPerChat(user.getId());
        int unreadChats = (int) unreadMap.values().stream().filter(v -> v > 0).count();

        return new DashboardSummaryResponse(
                role, sentRequests, pendingRequests, acceptedRequests,
                totalChats, unreadChats, profileCompletion,
                mentoringSessionService.findNextForCurrentUser().orElse(null));
    }

    public List<ActivityItemResponse> getActivity(int limit) {
        User user = userService.getCurrentUserEntity();
        String role = user.primaryRole().name();
        List<ActivityItemResponse> items = new ArrayList<>();

        if ("STUDENT".equals(role)) {
            studentProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
                requestRepository.findByStudentProfileId(profile.getId(),
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .forEach(req -> items.add(toRequestActivity(req)));
            });
        } else if ("MENTOR".equals(role)) {
            mentorProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
                requestRepository.findByMentorProfileId(profile.getId(),
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .forEach(req -> items.add(toRequestActivity(req)));
            });
        }

        items.sort(Comparator.comparing(ActivityItemResponse::occurredAt).reversed());
        return items.stream().limit(limit).toList();
    }

    private ActivityItemResponse toRequestActivity(MentoringRequest req) {
        return new ActivityItemResponse(
                "REQUEST_" + req.getStatus().name(),
                req.getId(),
                "Заявка #" + req.getId() + " — " + req.getStatus().name(),
                req.getUpdatedAt() != null ? req.getUpdatedAt() : req.getCreatedAt()
        );
    }
}
