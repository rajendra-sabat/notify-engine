package com.notifyengine.controller;

import com.notifyengine.domain.Notification;
import com.notifyengine.dto.NotificationRequest;
import com.notifyengine.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Create and manage notifications per tenant")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
        summary = "Create a notification",
        description = "Creates a new notification in the caller's tenant schema. The tenant is resolved from the `X-API-Key` header.",
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Notification created",
                headers = @Header(name = "Location", description = "URL of the created notification", schema = @Schema(type = "string")),
                content = @Content(schema = @Schema(implementation = Notification.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key", content = @Content)
        }
    )
    @PostMapping
    public ResponseEntity<Notification> create(@Valid @RequestBody NotificationRequest request) {
        Notification created = notificationService.createNotification(request);
        return ResponseEntity
                .created(URI.create("/api/v1/notifications/" + created.getId()))
                .body(created);
    }
}
