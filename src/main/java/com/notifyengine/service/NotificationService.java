package com.notifyengine.service;

import com.notifyengine.domain.Notification;
import com.notifyengine.domain.NotificationStatus;
import com.notifyengine.dto.NotificationRequest;
import com.notifyengine.notification.channel.ChannelType;
import com.notifyengine.notification.channel.NotificationChannel;
import com.notifyengine.repository.NotificationRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
                ChannelType.valueOf(request.type()),
                request.recipientEmail(),
                request.recipientPhone(),
                request.recipientName(),
                request.templateVariables(),
                NotificationStatus.PENDING,
                now,
                now
        );
        return sendNotification(notification);
    }

    private @NonNull Notification sendNotification(Notification notification) {

        NotificationChannel channel = getNotificationChannel(notification);
        try {
            channel.send(notification);
            notification.updateStatus(NotificationStatus.SENT);
        } catch (Exception e) {
            notification.updateStatus(NotificationStatus.FAILED);
            log.error("error caught {}", notification.getId(), e);
        }
        return notificationRepository.save(notification);
    }

    private @NonNull NotificationChannel getNotificationChannel(Notification notification) {
        return channels.stream()
                .filter(c -> c.supports(notification.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No channel found for type: " + notification.getType()));
    }
}
