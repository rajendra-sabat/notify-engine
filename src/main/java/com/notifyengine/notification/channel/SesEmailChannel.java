package com.notifyengine.notification.channel;

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
        String to = notification.getRecipientEmail();

        sesClient.sendEmail(SendEmailRequest.builder()
                .source(fromEmail)
                .destination(Destination.builder()
                        .toAddresses(to)
                        .build())
                .message(Message.builder()
                        .subject(Content.builder().data("Test notification").build())
                        .body(Body.builder()
                                .text(Content.builder().data("This is a test.").build())
                                .build())
                        .build())
                .build());

        log.info("Sent via SES, notificationId={}", notification.getId());
    }

    @Override
    public boolean supports(ChannelType channelType) {
        return channelType == ChannelType.EMAIL;
    }
}
