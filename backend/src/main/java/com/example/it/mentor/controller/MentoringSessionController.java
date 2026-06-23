package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.session.CancelSessionRequest;
import com.example.it.mentor.dto.session.CreateSessionRequest;
import com.example.it.mentor.dto.session.RescheduleSessionRequest;
import com.example.it.mentor.dto.session.SessionResponse;
import com.example.it.mentor.service.MentoringSessionService;
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

import java.util.Set;

/**
 * REST-контроллер для управления календарными сессиями менторинга.
 */
@Validated
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions", description = "Календарные сессии менторинга")
public class MentoringSessionController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("scheduledAt", "createdAt", "status");

    private final MentoringSessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
        return sessionService.create(request);
    }

    @GetMapping
    public PagedResponse<SessionResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "scheduledAt,asc") String sort) {
        String[] parts = sort.split(",");
        String rawField = parts[0];
        String field = ALLOWED_SORT_FIELDS.contains(rawField) ? rawField : "scheduledAt";
        Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return sessionService.list(PageRequest.of(page, size, Sort.by(dir, field)));
    }

    @GetMapping("/{id}")
    public SessionResponse getById(@PathVariable Long id) {
        return sessionService.getById(id);
    }

    @PutMapping("/{id}/reschedule")
    public SessionResponse reschedule(@PathVariable Long id,
                                      @Valid @RequestBody RescheduleSessionRequest request) {
        return sessionService.reschedule(id, request);
    }

    @PutMapping("/{id}/cancel")
    public SessionResponse cancel(@PathVariable Long id,
                                  @Valid @RequestBody(required = false) CancelSessionRequest request) {
        return sessionService.cancel(id, request);
    }

    @PutMapping("/{id}/complete")
    public SessionResponse complete(@PathVariable Long id) {
        return sessionService.complete(id);
    }
}
