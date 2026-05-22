package com.notifyengine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Payload for creating a new notification")
public record NotificationRequest(
        @NotBlank
        @Schema(description = "Notification channel type", example = "EMAIL", allowableValues = {"EMAIL", "SMS", "PUSH"})
        String type,

        @Email
        @Schema(description = "Recipient email address (required when type is EMAIL)", example = "user@example.com")
        String recipientEmail,


        @Schema(description = "Recipient phone number in E.164 format (required when type is SMS)", example = "+32499123456")
        String recipientPhone,

        @NotNull
        @Schema(description = "Display name of the recipient", example = "Jane Doe")
        String recipientName,

        @Schema(description = "Key-value pairs injected into the notification template", example = "{\"otp\": \"123456\"}")
        Map<String, String> templateVariables
) {
}
