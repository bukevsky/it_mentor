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

/**
 * Репозиторий заявок на менторство.
 */
@Repository
public interface MentoringRequestRepository extends JpaRepository<MentoringRequest, Long> {

    /**
     * Проверяет наличие активной заявки между конкретным студентом и ментором.
     *
     * @param studentId идентификатор профиля студента
     * @param mentorId идентификатор профиля ментора
     * @param activeStatuses набор статусов, считающихся активными
     * @return {@code true}, если активная заявка уже существует
     */
    boolean existsByStudentProfileIdAndMentorProfileIdAndStatusIn(
            Long studentId, Long mentorId, List<MentoringRequestStatus> activeStatuses);

    /**
     * Возвращает страницу заявок конкретного ментора с предзагрузкой профилей.
     *
     * @param id идентификатор профиля ментора
     * @param p параметры пагинации
     * @return страница заявок
     */
    @EntityGraph(value = "MentoringRequest.withProfiles")
    Page<MentoringRequest> findByMentorProfileId(Long id, Pageable p);

    /**
     * Возвращает страницу заявок ментора, отфильтрованных по статусу.
     *
     * @param id идентификатор профиля ментора
     * @param s статус заявки
     * @param p параметры пагинации
     * @return страница заявок
     */
    @EntityGraph(value = "MentoringRequest.withProfiles")
    Page<MentoringRequest> findByMentorProfileIdAndStatus(Long id, MentoringRequestStatus s, Pageable p);

    /**
     * Возвращает страницу заявок конкретного студента с предзагрузкой профилей.
     *
     * @param id идентификатор профиля студента
     * @param p параметры пагинации
     * @return страница заявок
     */
    @EntityGraph(value = "MentoringRequest.withProfiles")
    Page<MentoringRequest> findByStudentProfileId(Long id, Pageable p);

    /**
     * Возвращает страницу заявок студента, отфильтрованных по статусу.
     *
     * @param id идентификатор профиля студента
     * @param s статус заявки
     * @param p параметры пагинации
     * @return страница заявок
     */
    @EntityGraph(value = "MentoringRequest.withProfiles")
    Page<MentoringRequest> findByStudentProfileIdAndStatus(Long id, MentoringRequestStatus s, Pageable p);

    /**
     * Считает количество заявок ментора в конкретном статусе.
     *
     * @param mentorId идентификатор профиля ментора
     * @param status статус заявки
     * @return количество заявок
     */
    long countByMentorProfileIdAndStatus(Long mentorId, MentoringRequestStatus status);

    /**
     * Загружает заявку вместе с профилями и пользователями обеих сторон.
     *
     * @param id идентификатор заявки
     * @return найденная заявка
     */
    @EntityGraph(value = "MentoringRequest.withProfilesAndUsers")
    Optional<MentoringRequest> findWithProfilesById(Long id);

    long countByStudentProfileIdAndStatus(Long studentId, MentoringRequestStatus status);

    long countByMentorProfileIdAndStatusIn(Long mentorId, List<MentoringRequestStatus> statuses);

    long countByStudentProfileIdAndStatusIn(Long studentId, List<MentoringRequestStatus> statuses);

    Page<MentoringRequest> findTop20ByStudentProfileIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);

    Page<MentoringRequest> findTop20ByMentorProfileIdOrderByCreatedAtDesc(Long mentorId, Pageable pageable);
}
