package com.example.it.mentor.entity;

import com.example.it.mentor.entity.enums.AuditAction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
class AuditActionCheckIT {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Transactional
    @ParameterizedTest(name = "AuditAction.{0} принимается CHECK-constraint БД")
    @EnumSource(AuditAction.class)
    void eachAuditAction_satisfiesCheckConstraint(AuditAction action) {
        assertThatCode(() ->
            jdbcTemplate.update(
                "INSERT INTO admin_audit_log (admin_user_id, action, target_type, created_at, updated_at) " +
                "VALUES (1, ?, 'TEST', now(), now())",
                action.name()
            )
        ).doesNotThrowAnyException();
    }
}
