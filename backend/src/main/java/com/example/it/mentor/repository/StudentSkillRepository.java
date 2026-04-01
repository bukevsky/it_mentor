package com.example.it.mentor.repository;

import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.StudentSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentSkillRepository extends JpaRepository<StudentSkill, Long> {

    @Modifying
    @Query("delete from StudentSkill sk where sk.studentProfile = :profile")
    void deleteAllByStudentProfile(@Param("profile") StudentProfile profile);
}
