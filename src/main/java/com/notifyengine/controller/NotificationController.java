package com.notifyengine.controller;

import com.notifyengine.domain.Notification;
import com.notifyengine.dto.NotificationRequest;
import com.notifyengine.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<Notification> create(@Valid @RequestBody NotificationRequest request) {
        Notification created = notificationService.createNotification(request);
        return ResponseEntity
                .created(URI.create("/api/v1/notifications/" + created.getId()))
                .body(created);
    }
}
