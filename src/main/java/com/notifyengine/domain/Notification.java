package com.notifyengine.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Schema(description = "Persisted notification record")
@Entity
@Table(name = "notifications")
@Getter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @Schema(description = "Unique notification ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Column(name = "type", nullable = false, length = 10)
    @Schema(description = "Notification channel type", example = "EMAIL")
    private String type;

    @Column(name = "recipient_email", length = 255)
    @Schema(description = "Recipient email address", example = "user@example.com")
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 50)
    @Schema(description = "Recipient phone number", example = "+32499123456")
    private String recipientPhone;

    @Column(name = "recipient_name", length = 255)
    @Schema(description = "Display name of the recipient", example = "Jane Doe")
    private String recipientName;

    @Type(JsonType.class)
    @Column(name = "template_variables", columnDefinition = "jsonb")
    @Schema(description = "Template variable substitutions", example = "{\"otp\": \"123456\"}")
    private Map<String, String> templateVariables;

    @Column(name = "status", nullable = false, length = 20)
    @Schema(description = "Processing status", example = "PENDING", allowableValues = {"PENDING", "SENT", "FAILED"}, accessMode = Schema.AccessMode.READ_ONLY)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private OffsetDateTime updatedAt;

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Notification() {}

    public Notification(UUID id, String type, String recipientEmail, String recipientPhone,
                        String recipientName, Map<String, String> templateVariables,
                        String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.type = type;
        this.recipientEmail = recipientEmail;
        this.recipientPhone = recipientPhone;
        this.recipientName = recipientName;
        this.templateVariables = templateVariables;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
