package com.notifyengine.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.notifyengine.domain.ChannelType;
import com.notifyengine.domain.Notification;
import com.notifyengine.domain.NotificationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SesEmailChannelTest {

    @Mock
    private SesClient sesClient;

    private SesEmailChannel channel;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        channel = new SesEmailChannel(sesClient, "noreply@example.com");

        Logger logger = (Logger) LoggerFactory.getLogger(SesEmailChannel.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(SesEmailChannel.class)).detachAppender(logAppender);
    }

    @Test
    void send_success_callsSesClient() {
        channel.send(notification("Subject", "Body"));

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void send_success_logsInfo() {
        channel.send(notification("Subject", "Body"));

        assertThat(logAppender.list)
                .anyMatch(e -> e.getLevel() == Level.INFO && e.getFormattedMessage().contains("Sent via SES"));
    }

    @Test
    void send_sesThrows_rethrowsSameException() {
        SesException exception = sesException("MessageRejected", "req-abc-123");
        doThrow(exception).when(sesClient).sendEmail(any(SendEmailRequest.class));

        SesException thrown = assertThrows(SesException.class, () -> channel.send(notification("Subject", "Body")));

        assertThat(thrown).isSameAs(exception);
    }

    @Test
    void send_sesThrows_logsErrorCodeAndRequestId() {
        doThrow(sesException("MessageRejected", "req-abc-123"))
                .when(sesClient).sendEmail(any(SendEmailRequest.class));

        assertThrows(SesException.class, () -> channel.send(notification("Subject", "Body")));

        ILoggingEvent errorLog = logAppender.list.stream()
                .filter(e -> e.getLevel() == Level.ERROR)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected ERROR log not emitted"));

        assertThat(errorLog.getFormattedMessage())
                .contains("MessageRejected")
                .contains("req-abc-123");
    }

    @Test
    void send_nullSubject_substitutesEmptyString() {
        channel.send(notification(null, "Body"));

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void supports_emailType_returnsTrue() {
        assertThat(channel.supports(ChannelType.EMAIL)).isTrue();
    }

    @Test
    void supports_smsType_returnsFalse() {
        assertThat(channel.supports(ChannelType.SMS)).isFalse();
    }

    private Notification notification(String subject, String body) {
        return new Notification(
                UUID.randomUUID(), ChannelType.EMAIL,
                "jane@example.com", null, "Jane Doe",
                subject, body,
                null, NotificationStatus.PENDING,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    private SesException sesException(String errorCode, String requestId) {
        return (SesException) SesException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode(errorCode)
                        .build())
                .requestId(requestId)
                .build();
    }
}
