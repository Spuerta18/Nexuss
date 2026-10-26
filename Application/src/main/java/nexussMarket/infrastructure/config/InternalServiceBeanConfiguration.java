package nexussMarket.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nexussMarket.domain.ports.out.AuditLogRepositoryPort;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.NotificationPort;
import nexussMarket.domain.ports.out.OperationRepositoryPort;
import nexussMarket.domain.services.NotifyUserService;
import nexussMarket.domain.services.RegisterAuditLogService;
import nexussMarket.domain.services.RegisterOperationAndAuditService;
import nexussMarket.domain.services.RegisterOperationService;
import nexussMarket.domain.services.ReserveInventoryService;

/**
 * Internal domain services: no role input port exposes them, and they are injected into
 * other services ({@link ReserveInventoryService}, {@link NotifyUserService}, and
 * {@link RegisterOperationAndAuditService}, which every state-changing service
 * calls to record its operation and audit log). See
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
    public RegisterOperationService registerOperationService(OperationRepositoryPort operationRepositoryPort) {
        return new RegisterOperationService(operationRepositoryPort);
    }

    @Bean
    public RegisterAuditLogService registerAuditLogService(AuditLogRepositoryPort auditLogRepositoryPort) {
        return new RegisterAuditLogService(auditLogRepositoryPort);
    }

    @Bean
    public RegisterOperationAndAuditService registerOperationAndAuditService(
            RegisterOperationService registerOperationService, RegisterAuditLogService registerAuditLogService) {
        return new RegisterOperationAndAuditService(registerOperationService, registerAuditLogService);
    }

    @Bean
    public NotifyUserService notifyUserService(NotificationPort notificationPort) {
        return new NotifyUserService(notificationPort);
    }
}
