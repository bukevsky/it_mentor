package com.example.it.mentor.repository;

import com.example.it.mentor.entity.NotificationOutbox;
import com.example.it.mentor.entity.enums.NotificationOutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    List<NotificationOutbox> findTop50ByStatusAndNextAttemptAtBefore(
            NotificationOutboxStatus status, OffsetDateTime now);

    Page<NotificationOutbox> findByStatus(NotificationOutboxStatus status, Pageable pageable);
}
