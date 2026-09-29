package nexussMarket.domain.ports.out;

import java.time.LocalDateTime;

public interface AuditLogPort {

    void record(String userId, String operation, LocalDateTime occurredAt);
}
