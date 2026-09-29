package nexussMarket.adapters.out.persistence.mongodb.adapters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import nexussMarket.domain.enums.NotificationChannel;
import nexussMarket.domain.ports.out.NotificationPort;

/**
 * Placeholder {@link NotificationPort}: there is no real notification channel
 * yet, so notifications are only written to the application log. To be
 * replaced by a real adapter (in its own {@code adapters/out/notification}
 * package) once email, SMS or push delivery is implemented.
 */
@Component
public class LoggingNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationAdapter.class);

    @Override
    public void send(String userId, NotificationChannel channel, String message) {
        log.info("Notification to user {} via {}: {}", userId, channel, message);
    }
}
