package com.notifyengine.service;

import com.notifyengine.domain.Notification;
import com.notifyengine.dto.NotificationRequest;
import com.notifyengine.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification createNotification(NotificationRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        Notification notification = new Notification(
                null,
                request.type(),
                request.recipientEmail(),
                request.recipientPhone(),
                request.recipientName(),
                request.templateVariables(),
                "PENDING",
                now,
                now
        );
        return notificationRepository.save(notification);
    }
}
