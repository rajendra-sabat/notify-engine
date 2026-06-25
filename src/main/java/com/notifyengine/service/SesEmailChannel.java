package com.notifyengine.service;

import com.notifyengine.domain.ChannelType;
import com.notifyengine.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

@Service
public class SesEmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SesEmailChannel.class);

    private final SesClient sesClient;
    private final String fromEmail;

    public SesEmailChannel(SesClient sesClient, @Value("${aws.ses.from-email}") String fromEmail) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(Notification notification) {
        String subject = notification.getSubject() != null ? notification.getSubject() : "";
        try {
            sesClient.sendEmail(SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(notification.getRecipientEmail())
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder()
                                    .text(Content.builder().data(notification.getBody()).build())
                                    .build())
                            .build())
                    .build());
            log.info("Sent via SES, notificationId={}", notification.getId());
        } catch (SesException e) {
            log.error("SES rejected email notificationId={} errorCode={} requestId={}",
                    notification.getId(), e.awsErrorDetails().errorCode(), e.requestId());
            throw e;
        }
    }

    @Override
    public boolean supports(ChannelType channelType) {
        return channelType == ChannelType.EMAIL;
    }
}
