package com.notifyengine.service;

import com.notifyengine.domain.Notification;
import com.notifyengine.domain.NotificationStatus;
import com.notifyengine.dto.NotificationRequest;
import com.notifyengine.domain.ChannelType;
import com.notifyengine.service.NotificationChannel;
import com.notifyengine.repository.NotificationRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;
    private final List<NotificationChannel> channels;

    public NotificationService(NotificationRepository notificationRepository,
                               List<NotificationChannel> channels) {
        this.notificationRepository = notificationRepository;
        this.channels = channels;
    }

    public Notification createAndSendNotification(NotificationRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        Notification notification = new Notification(
                null,
                parseChannelType(request.type()),
                request.recipientEmail(),
                request.recipientPhone(),
                request.recipientName(),
                request.subject(),
                request.body(),
                request.templateVariables(),
                NotificationStatus.PENDING,
                now,
                now
        );
        return sendNotification(notification);
    }

    private @NonNull Notification sendNotification(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        try {
            getNotificationChannel(saved).send(saved);
            saved.updateStatus(NotificationStatus.SENT);
        } catch (Exception e) {
            saved.updateStatus(NotificationStatus.FAILED);
            log.error("Failed to dispatch notification id={} type={}", saved.getId(), saved.getType(), e);
            notificationRepository.save(saved);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Notification dispatch failed");
        }
        return notificationRepository.save(saved);
    }

    private @NonNull NotificationChannel getNotificationChannel(Notification notification) {
        return channels.stream()
                .filter(c -> c.supports(notification.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No channel found for type: " + notification.getType()));
    }

    private ChannelType parseChannelType(String type) {
        try {
            return ChannelType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid channel type: " + type);
        }
    }
}
