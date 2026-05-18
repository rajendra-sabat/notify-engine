package com.notifyengine.repository;

import com.notifyengine.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByStatus(String status);

    List<Notification> findByCreatedAtAfter(OffsetDateTime date);
}
