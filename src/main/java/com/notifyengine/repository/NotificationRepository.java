package com.notifyengine.repository;

import com.notifyengine.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByStatus(String status);

    List<Notification> findByCreatedAtAfter(OffsetDateTime date);
}
