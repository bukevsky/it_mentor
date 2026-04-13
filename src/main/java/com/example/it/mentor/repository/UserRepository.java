package com.example.it.mentor.repository;

import com.example.it.mentor.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий доступа к пользователям приложения.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Ищет пользователя по email без дополнительных ограничений.
     *
     * @param email email пользователя
     * @return найденный пользователь
     */
    Optional<User> findByEmail(String email);

    /**
     * Ищет не удалённого пользователя по email.
     *
     * @param email email пользователя
     * @return найденный пользователь
     */
    Optional<User> findByEmailAndDeletedFalse(String email);

    /**
     * Ищет не удалённого пользователя по email с предзагрузкой ролей.
     *
     * @param email email пользователя
     * @return найденный пользователь вместе с ролями
     */
    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByEmailAndDeletedFalse(String email);

    /**
     * Ищет пользователя по идентификатору с предзагрузкой ролей.
     *
     * @param id идентификатор пользователя
     * @return найденный пользователь вместе с ролями
     */
    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(Long id);

    /**
     * Проверяет наличие активного пользователя с указанным email.
     *
     * @param email email пользователя
     * @return {@code true}, если пользователь существует и не удалён
     */
    boolean existsByEmailAndDeletedFalse(String email);
}
