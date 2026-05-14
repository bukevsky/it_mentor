package com.example.it.mentor.controller;

import com.example.it.mentor.dto.dashboard.MentorStatsResponse;
import com.example.it.mentor.service.MentorStatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mentor-stats")
@RequiredArgsConstructor
@Tag(name = "Mentor Stats", description = "Статистика ментора")
public class MentorStatsController {

    private final MentorStatsService mentorStatsService;

    @GetMapping("/me")
    public MentorStatsResponse getMyStats() {
        return mentorStatsService.getMyStats();
    }
}
