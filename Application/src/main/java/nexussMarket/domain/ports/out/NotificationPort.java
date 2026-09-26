package nexussMarket.domain.ports.out;

import nexussMarket.domain.enums.NotificationChannel;

public interface NotificationPort {

    void send(String userId, NotificationChannel channel, String message);
}
