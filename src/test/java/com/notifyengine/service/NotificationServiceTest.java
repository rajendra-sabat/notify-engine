package com.notifyengine.service;

import com.notifyengine.domain.ChannelType;
import com.notifyengine.domain.Notification;
import com.notifyengine.domain.NotificationStatus;
import com.notifyengine.dto.NotificationRequest;
import com.notifyengine.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationChannel emailChannel;

    private NotificationService service;

    private Notification savedNotification;

    @BeforeEach
    void setUp() {
        savedNotification = new Notification(
                UUID.randomUUID(), ChannelType.EMAIL,
                "jane@example.com", null, "Jane Doe",
                "Your verification code", "Your one-time password is 42",
                null, NotificationStatus.PENDING,
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(emailChannel.supports(ChannelType.EMAIL)).thenReturn(true);
        when(emailChannel.supports(ChannelType.SMS)).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        service = new NotificationService(notificationRepository, List.of(emailChannel));
    }

    @Test
    void emailNotification_channelSucceeds_returnsSent() {
        Notification result = service.createAndSendNotification(emailRequest());

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void emailNotification_channelSucceeds_savesExactlyTwice() {
        service.createAndSendNotification(emailRequest());

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }


    @Test
    void emailNotification_channelThrows_returnsFailed() {
        doThrow(new RuntimeException("SES unavailable")).when(emailChannel).send(any());

        Notification result = service.createAndSendNotification(emailRequest());

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void emailNotification_channelThrows_stillSavesTwice() {
        doThrow(new RuntimeException("SES unavailable")).when(emailChannel).send(any());

        service.createAndSendNotification(emailRequest());

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void notification_noChannelForType_returnsFailed() {
        service = new NotificationService(notificationRepository, List.of());

        Notification result = service.createAndSendNotification(emailRequest());

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void invalidChannelType_throwsBadRequest() {
        NotificationRequest request = new NotificationRequest(
                "WEBHOOK", "jane@example.com", null, "Jane Doe",
                "Subject", "Body", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createAndSendNotification(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidChannelType_neverCallsRepository() {
        NotificationRequest request = new NotificationRequest(
                "WEBHOOK", "jane@example.com", null, "Jane Doe",
                "Subject", "Body", null);

        assertThrows(ResponseStatusException.class,
                () -> service.createAndSendNotification(request));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void emailRequest_fieldsMapToNotification() {
        NotificationRequest request = new NotificationRequest(
                "EMAIL", "jane@example.com", null, "Jane Doe",
                "Your OTP", "Code is 42", null);

        service.createAndSendNotification(request);

        // first save() receives the freshly built notification — verify its fields
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        Notification saved = captor.getAllValues().get(0);

        assertThat(saved.getType()).isEqualTo(ChannelType.EMAIL);
        assertThat(saved.getRecipientEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getRecipientName()).isEqualTo("Jane Doe");
        assertThat(saved.getSubject()).isEqualTo("Your OTP");
        assertThat(saved.getBody()).isEqualTo("Code is 42");
    }

    private NotificationRequest emailRequest() {
        return new NotificationRequest(
                "EMAIL", "jane@example.com", null, "Jane Doe",
                "Your verification code", "Your one-time password is 42", null);
    }
}
