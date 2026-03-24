package com.example.it.mentor.repository;

import com.example.it.mentor.entity.MentorProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long>, JpaSpecificationExecutor<MentorProfile> {

    Optional<MentorProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"city", "skills", "skills.skill"})
    Optional<MentorProfile> findWithDetailsByUserId(Long userId);

    @EntityGraph(attributePaths = {"city", "skills", "skills.skill"})
    Optional<MentorProfile> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"city", "skills", "skills.skill"})
    List<MentorProfile> findAllWithDetailsByIdIn(List<Long> ids);
}
