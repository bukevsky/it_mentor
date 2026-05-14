package com.example.it.mentor.controller;

import com.example.it.mentor.dto.dashboard.ActivityItemResponse;
import com.example.it.mentor.dto.dashboard.DashboardSummaryResponse;
import com.example.it.mentor.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Сводка и лента активности")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/activity")
    public List<ActivityItemResponse> getActivity(
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return dashboardService.getActivity(limit);
    }
}
