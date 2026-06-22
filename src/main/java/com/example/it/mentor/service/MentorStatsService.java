package com.example.it.mentor.service;

import com.example.it.mentor.dto.dashboard.MentorStatsResponse;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.MentoringRequestStatus;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.repository.MentorProfileRepository;
import com.example.it.mentor.repository.MentoringRequestRepository;
import com.example.it.mentor.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MentorStatsService {

    private final ReviewRepository reviewRepository;
    private final MentoringRequestRepository requestRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserService userService;

    public MentorStatsResponse getMyStats() {
        User user = userService.getCurrentUserEntity();
        Long userId = user.getId();

        Long mentorProfileId = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Профиль ментора не найден"))
                .getId();

        double averageRating = reviewRepository.averageRatingByMentorUserId(userId)
                .orElse(0.0);
        int reviewCount = (int) reviewRepository.countByMentorUserId(userId);
        int completedRequests = (int) requestRepository.countByMentorProfileIdAndStatus(
                mentorProfileId, MentoringRequestStatus.COMPLETED);

        long accepted = requestRepository.countByMentorProfileIdAndStatus(
                mentorProfileId, MentoringRequestStatus.ACCEPTED)
                + completedRequests;
        long rejected = requestRepository.countByMentorProfileIdAndStatus(
                mentorProfileId, MentoringRequestStatus.REJECTED);
        double responseRate = (accepted + rejected) > 0
                ? (double) accepted / (accepted + rejected) * 100.0
                : 0.0;

        String level;
        int progress;
        if (completedRequests >= 20) {
            level = "EXPERT";
            progress = 100;
        } else if (completedRequests >= 5) {
            level = "INTERMEDIATE";
            progress = (completedRequests - 5) * 100 / 15;
        } else {
            level = "BEGINNER";
            progress = completedRequests * 100 / 5;
        }

        MentorStatsResponse response = new MentorStatsResponse(
                Math.round(averageRating * 10.0) / 10.0,
                reviewCount,
                completedRequests,
                Math.round(responseRate * 10.0) / 10.0,
                level,
                progress
        );
        log.debug("Статистика ментора загружена: userId={}, mentorProfileId={}, reviewCount={}, " +
                        "completedRequests={}, responseRate={}, level={}, step={}",
                userId, mentorProfileId, reviewCount, completedRequests, response.responseRate(),
                level, "mentor_stats_loaded");
        return response;
    }
}
