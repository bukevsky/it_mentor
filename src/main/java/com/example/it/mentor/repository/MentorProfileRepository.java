package com.example.it.mentor.repository;

import com.example.it.mentor.entity.MentorProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long> {

    Optional<MentorProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"city", "skills", "skills.skill"})
    Optional<MentorProfile> findWithDetailsByUserId(Long userId);

    @EntityGraph(attributePaths = {"city", "skills", "skills.skill"})
    Optional<MentorProfile> findWithDetailsById(Long id);
}
