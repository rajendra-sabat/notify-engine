package com.notifyengine.notification.channel;

import com.notifyengine.domain.Notification;

public interface NotificationChannel {

    void send(Notification notification);

    // Used by the dispatcher to pick the right channel at runtime.
    boolean supports(ChannelType channelType);
}
