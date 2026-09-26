package nexussMarket.domain.services;

import nexussMarket.domain.enums.NotificationChannel;
import nexussMarket.domain.ports.out.NotificationPort;

/**
 * Internal service, not exposed as a use case. Injected as a constructor
 * dependency by services that need to notify a user of an outcome.
 */
public class NotifyUserService {

    private final NotificationPort notificationPort;

    public NotifyUserService(NotificationPort notificationPort) {
        this.notificationPort = notificationPort;
    }

    public void notify(String userId, NotificationChannel channel, String message) {
        notificationPort.send(userId, channel, message);
    }
}
