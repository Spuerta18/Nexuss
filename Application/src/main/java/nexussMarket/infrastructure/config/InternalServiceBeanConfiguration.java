package nexussMarket.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nexussMarket.domain.ports.out.AuditLogPort;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.NotificationPort;
import nexussMarket.domain.services.AuditOperationService;
import nexussMarket.domain.services.NotifyUserService;
import nexussMarket.domain.services.ReserveInventoryService;

/**
 * Internal domain services: they have no input port and are injected into
 * other services ({@link ReserveInventoryService}, {@link NotifyUserService})
 * or into the security layer ({@link AuditOperationService}). See
 * {@link AuthorizationBeanConfiguration} for how the domain wiring is split.
 */
@Configuration
public class InternalServiceBeanConfiguration {

    @Bean
    public ReserveInventoryService reserveInventoryService(InventoryRepositoryPort inventoryRepositoryPort,
                                                           InventoryMovementRepositoryPort inventoryMovementRepositoryPort) {
        return new ReserveInventoryService(inventoryRepositoryPort, inventoryMovementRepositoryPort);
    }

    @Bean
    public AuditOperationService auditOperationService(AuditLogPort auditLogPort) {
        return new AuditOperationService(auditLogPort);
    }

    @Bean
    public NotifyUserService notifyUserService(NotificationPort notificationPort) {
        return new NotifyUserService(notificationPort);
    }
}
