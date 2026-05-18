package com.example.it.mentor.controller;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.complaint.ComplaintResponse;
import com.example.it.mentor.dto.complaint.ResolveComplaintRequest;
import com.example.it.mentor.entity.enums.ComplaintStatus;
import com.example.it.mentor.entity.enums.ComplaintTargetType;
import com.example.it.mentor.service.ComplaintService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Validated
@RestController
@RequestMapping("/admin/complaints")
@RequiredArgsConstructor
@Tag(name = "Admin: Complaints", description = "Администрирование жалоб")
public class AdminComplaintController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "status", "resolvedAt");

    private final ComplaintService complaintService;

    @GetMapping
    public PagedResponse<ComplaintResponse> getComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintTargetType targetType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        String[] parts = sort.split(",");
        String field = ALLOWED_SORT_FIELDS.contains(parts[0]) ? parts[0] : "createdAt";
        Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return complaintService.getList(status, targetType, PageRequest.of(page, size, Sort.by(dir, field)));
    }

    @PutMapping("/{id}/resolve")
    public ComplaintResponse resolve(@PathVariable Long id,
                                     @Valid @RequestBody ResolveComplaintRequest request) {
        return complaintService.resolve(id, request);
    }
}
