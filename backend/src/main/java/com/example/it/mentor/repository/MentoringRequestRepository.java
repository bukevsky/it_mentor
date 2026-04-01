package com.example.it.mentor.repository;

import com.example.it.mentor.entity.MentoringRequest;
import com.example.it.mentor.entity.enums.MentoringRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentoringRequestRepository extends JpaRepository<MentoringRequest, Long> {

    boolean existsByStudentProfileIdAndMentorProfileIdAndStatusIn(
            Long studentId, Long mentorId, List<MentoringRequestStatus> activeStatuses);

    @EntityGraph(value = "MentoringRequest.withProfiles")
    Page<MentoringRequest> findByMentorProfileId(Long id, Pageable p);

    @EntityGraph(value = "MentoringRequest.withProfiles")
    Page<MentoringRequest> findByMentorProfileIdAndStatus(Long id, MentoringRequestStatus s, Pageable p);

    @EntityGraph(value = "MentoringRequest.withProfiles")
    Page<MentoringRequest> findByStudentProfileId(Long id, Pageable p);

    @EntityGraph(value = "MentoringRequest.withProfiles")
    Page<MentoringRequest> findByStudentProfileIdAndStatus(Long id, MentoringRequestStatus s, Pageable p);

    long countByMentorProfileIdAndStatus(Long mentorId, MentoringRequestStatus status);

    @EntityGraph(value = "MentoringRequest.withProfiles")
    Optional<MentoringRequest> findWithProfilesById(Long id);
}
