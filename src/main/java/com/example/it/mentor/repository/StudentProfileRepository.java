package com.example.it.mentor.repository;

import com.example.it.mentor.entity.StudentProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий профилей студентов.
 */
@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    /**
     * Ищет профиль студента по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя
     * @return найденный профиль
     */
    Optional<StudentProfile> findByUserId(Long userId);

    /**
     * Проверяет наличие профиля студента у пользователя.
     *
     * @param userId идентификатор пользователя
     * @return {@code true}, если профиль существует
     */
    boolean existsByUserId(Long userId);

    /**
     * Загружает профиль студента с полным набором связанных сущностей.
     *
     * @param userId идентификатор пользователя
     * @return профиль с деталями
     */
    @EntityGraph("StudentProfile.withDetails")
    Optional<StudentProfile> findWithDetailsByUserId(Long userId);

    /**
     * Загружает профиль студента по идентификатору с полным набором связанных сущностей.
     *
     * @param id идентификатор профиля
     * @return профиль с деталями
     */
    @EntityGraph("StudentProfile.withDetails")
    Optional<StudentProfile> findWithDetailsById(Long id);
}
