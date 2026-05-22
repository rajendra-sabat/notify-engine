package com.notifyengine.notification.channel;

import com.notifyengine.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SnsSmsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SnsSmsChannel.class);

    @Override
    public void send(Notification notification) {
        String to = notification.getRecipientPhone();
        log.info("Sending via SNS");
    }

    @Override
    public boolean supports(ChannelType channelType) {
        return channelType == ChannelType.SMS;
    }
}
