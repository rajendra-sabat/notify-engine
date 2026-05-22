package com.notifyengine.service;

import com.notifyengine.domain.ChannelType;
import com.notifyengine.domain.Notification;
import org.springframework.stereotype.Service;

@Service
public class SnsSmsChannel implements NotificationChannel {

    @Override
    public void send(Notification notification) {
        throw new UnsupportedOperationException("SNS channel not yet implemented");
    }

    @Override
    public boolean supports(ChannelType channelType) {
        return channelType == ChannelType.SMS;
    }
}
