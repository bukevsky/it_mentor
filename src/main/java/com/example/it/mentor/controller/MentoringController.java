package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.mentoring.MentoringRequestClarifyRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestCreateRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestRejectRequest;
import com.example.it.mentor.dto.mentoring.MentoringRequestResponse;
import com.example.it.mentor.entity.enums.MentoringRequestStatus;
import com.example.it.mentor.service.MentoringRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/mentoring/requests")
@RequiredArgsConstructor
@Tag(name = "Mentoring Requests", description = "Система заявок на менторство")
public class MentoringController {

    private final MentoringRequestService mentoringRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MentoringRequestResponse createRequest(@Valid @RequestBody MentoringRequestCreateRequest request) {
        return mentoringRequestService.createRequest(request);
    }

    @GetMapping
    public PagedResponse<MentoringRequestResponse> getRequests(
            @RequestParam(required = false) MentoringRequestStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mentoringRequestService.getRequests(status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{id}")
    public MentoringRequestResponse getById(@PathVariable Long id) {
        return mentoringRequestService.getById(id);
    }

    @PutMapping("/{id}/view")
    public MentoringRequestResponse markAsReviewing(@PathVariable Long id) {
        return mentoringRequestService.markAsReviewing(id);
    }

    @PutMapping("/{id}/needs-clarification")
    public MentoringRequestResponse requestClarification(
            @PathVariable Long id,
            @Valid @RequestBody MentoringRequestClarifyRequest request) {
        return mentoringRequestService.requestClarification(id, request);
    }

    @PutMapping("/{id}/accept")
    public MentoringRequestResponse acceptRequest(@PathVariable Long id) {
        return mentoringRequestService.acceptRequest(id);
    }

    @PutMapping("/{id}/reject")
    public MentoringRequestResponse rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody MentoringRequestRejectRequest request) {
        return mentoringRequestService.rejectRequest(id, request);
    }

    @PutMapping("/{id}/cancel")
    public MentoringRequestResponse cancelRequest(@PathVariable Long id) {
        return mentoringRequestService.cancelRequest(id);
    }

    @PutMapping("/{id}/complete")
    public MentoringRequestResponse completeRequest(@PathVariable Long id) {
        return mentoringRequestService.completeRequest(id);
    }
}
