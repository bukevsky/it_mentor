package com.example.it.mentor.repository;

import com.example.it.mentor.entity.MentorProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий профилей менторов с поддержкой {@link JpaSpecificationExecutor}.
 */
@Repository
public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long>, JpaSpecificationExecutor<MentorProfile> {

    /**
     * Ищет профиль ментора по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя
     * @return найденный профиль
     */
    Optional<MentorProfile> findByUserId(Long userId);

    /**
     * Загружает профиль ментора по пользователю с предзагрузкой города и навыков.
     *
     * @param userId идентификатор пользователя
     * @return профиль с деталями
     */
    @EntityGraph(attributePaths = {"city", "skills", "skills.skill"})
    Optional<MentorProfile> findWithDetailsByUserId(Long userId);

    /**
     * Загружает профиль ментора по идентификатору с предзагрузкой города и навыков.
     *
     * @param id идентификатор профиля
     * @return профиль с деталями
     */
    @EntityGraph(attributePaths = {"city", "skills", "skills.skill"})
    Optional<MentorProfile> findWithDetailsById(Long id);

    /**
     * Загружает набор профилей менторов по списку идентификаторов с полными деталями.
     *
     * @param ids идентификаторы профилей
     * @return список профилей с деталями
     */
    @EntityGraph(attributePaths = {"city", "skills", "skills.skill"})
    List<MentorProfile> findAllWithDetailsByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"user"})
    List<MentorProfile> findAllByUserIdIn(java.util.Collection<Long> userIds);
}
