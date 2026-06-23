package com.example.it.mentor.repository;

import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndDeletedFalse(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByEmailAndDeletedFalse(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(Long id);

    boolean existsByEmailAndDeletedFalse(String email);

    @EntityGraph(attributePaths = "roles")
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r " +
           "WHERE u.deleted = false " +
           "AND (:q IS NULL OR LOWER(u.email) LIKE %:q%) " +
           "AND (:status IS NULL OR u.status = :status) " +
           "AND (:roleCode IS NULL OR r.code = :roleCode)")
    Page<User> searchUsers(
            @Param("q") String q,
            @Param("status") UserStatus status,
            @Param("roleCode") RoleCode roleCode,
            Pageable pageable);

    @Query("SELECT r.code, COUNT(u) FROM User u JOIN u.roles r WHERE u.deleted = false GROUP BY r.code")
    List<Object[]> countByRoleRaw();

    @Query("SELECT u.status, COUNT(u) FROM User u WHERE u.deleted = false GROUP BY u.status")
    List<Object[]> countByStatusRaw();

    long countByDeletedFalse();
}
