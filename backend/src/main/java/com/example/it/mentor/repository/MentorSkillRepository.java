package com.example.it.mentor.repository;

import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentorSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий навыков в профиле ментора.
 */
@Repository
public interface MentorSkillRepository extends JpaRepository<MentorSkill, Long> {

    /**
     * Удаляет все навыки, принадлежащие профилю ментора.
     *
     * @param profile профиль ментора
     */
    @Modifying
    @Query("delete from MentorSkill sk where sk.mentorProfile = :profile")
    void deleteAllByMentorProfile(@Param("profile") MentorProfile profile);
}
