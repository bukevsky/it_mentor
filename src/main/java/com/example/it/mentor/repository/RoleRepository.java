package com.example.it.mentor.repository;

import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий справочника ролей пользователей.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Ищет роль по её коду.
     *
     * @param code код роли
     * @return найденная роль
     */
    Optional<Role> findByCode(RoleCode code);
}
