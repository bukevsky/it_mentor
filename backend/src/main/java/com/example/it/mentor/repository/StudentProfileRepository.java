package com.example.it.mentor.repository;

import com.example.it.mentor.entity.StudentProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @EntityGraph("StudentProfile.withDetails")
    Optional<StudentProfile> findWithDetailsByUserId(Long userId);

    @EntityGraph("StudentProfile.withDetails")
    Optional<StudentProfile> findWithDetailsById(Long id);
}
