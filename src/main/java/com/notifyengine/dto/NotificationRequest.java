package com.notifyengine.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record NotificationRequest(
        @NotBlank String type,
        @Email String recipientEmail,
        String recipientPhone,
        String recipientName,
        Map<String, String> templateVariables
) {}
