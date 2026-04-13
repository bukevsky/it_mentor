package com.example.it.mentor.repository;

import com.example.it.mentor.entity.StudentLanguage;
import com.example.it.mentor.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий языков в профиле студента.
 */
@Repository
public interface StudentLanguageRepository extends JpaRepository<StudentLanguage, Long> {

    /**
     * Удаляет все языки, принадлежащие профилю студента.
     *
     * @param profile профиль студента
     */
    @Modifying
    @Query("delete from StudentLanguage sl where sl.studentProfile = :profile")
    void deleteAllByStudentProfile(@Param("profile") StudentProfile profile);
}
