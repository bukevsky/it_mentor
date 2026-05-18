package com.example.it.mentor.service;

import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.complaint.ComplaintResponse;
import com.example.it.mentor.dto.complaint.CreateComplaintRequest;
import com.example.it.mentor.dto.complaint.ResolveComplaintRequest;
import com.example.it.mentor.entity.Complaint;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.ComplaintStatus;
import com.example.it.mentor.entity.enums.ComplaintTargetType;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ConflictException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.event.audit.ComplaintResolvedAuditEvent;
import com.example.it.mentor.mapper.ComplaintMapper;
import com.example.it.mentor.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserService userService;
    private final ComplaintMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ComplaintResponse create(CreateComplaintRequest dto) {
        User currentUser = userService.getCurrentUserEntity();

        Complaint complaint = Complaint.builder()
                .targetType(dto.targetType())
                .targetId(dto.targetId())
                .reporterUserId(currentUser.getId())
                .reason(dto.reason())
                .build();

        complaint = complaintRepository.save(complaint);
        log.info("Жалоба создана: complaintId={}, targetType={}, targetId={}, reporterUserId={}",
                complaint.getId(), dto.targetType(), dto.targetId(), currentUser.getId());
        return mapper.toResponse(complaint);
    }

    public PagedResponse<ComplaintResponse> getList(ComplaintStatus status, ComplaintTargetType targetType, Pageable pageable) {
        return PagedResponse.from(complaintRepository.findFiltered(status, targetType, pageable)
                .map(mapper::toResponse));
    }

    @Transactional
    public ComplaintResponse resolve(Long complaintId, ResolveComplaintRequest dto) {
        if (dto.status() == ComplaintStatus.OPEN) {
            throw new BusinessRuleViolationException("Статус жалобы должен быть RESOLVED или REJECTED");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("Жалоба не найдена: " + complaintId));

        if (complaint.getStatus() != ComplaintStatus.OPEN) {
            throw new ConflictException("Жалоба уже закрыта: " + complaintId);
        }

        User admin = userService.getCurrentUserEntity();
        complaint.setStatus(dto.status());
        complaint.setResolution(dto.resolution());
        complaint.setResolvedBy(admin.getId());
        complaint.setResolvedAt(OffsetDateTime.now());

        complaint = complaintRepository.save(complaint);
        log.info("Жалоба рассмотрена: complaintId={}, status={}, adminId={}",
                complaint.getId(), dto.status(), admin.getId());
        eventPublisher.publishEvent(new ComplaintResolvedAuditEvent(
                admin.getId(), complaint.getId(), dto.status(), dto.resolution()));
        return mapper.toResponse(complaint);
    }
}
