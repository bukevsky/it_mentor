package com.example.it.mentor.repository;

import com.example.it.mentor.entity.StudentEducation;
import com.example.it.mentor.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий элементов образования в профиле студента.
 */
@Repository
public interface StudentEducationRepository extends JpaRepository<StudentEducation, Long> {

    /**
     * Удаляет все записи об образовании, принадлежащие профилю студента.
     *
     * @param profile профиль студента
     */
    @Modifying
    @Query("delete from StudentEducation e where e.studentProfile = :profile")
    void deleteAllByStudentProfile(@Param("profile") StudentProfile profile);
}
