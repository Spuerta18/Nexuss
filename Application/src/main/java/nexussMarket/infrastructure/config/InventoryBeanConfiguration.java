package nexussMarket.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nexussMarket.domain.ports.in.AdjustInventoryUseCase;
import nexussMarket.domain.ports.in.ConfirmInventoryOutboundUseCase;
import nexussMarket.domain.ports.in.ConsultInventoryUseCase;
import nexussMarket.domain.ports.in.DeactivateWarehouseUseCase;
import nexussMarket.domain.ports.in.RegisterInventoryInboundUseCase;
import nexussMarket.domain.ports.in.RegisterInventoryReturnUseCase;
import nexussMarket.domain.ports.in.RegisterWarehouseUseCase;
import nexussMarket.domain.ports.in.ReleaseInventoryReservationUseCase;
import nexussMarket.domain.ports.in.ReportDamagedInventoryUseCase;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.services.AdjustInventoryService;
import nexussMarket.domain.services.ConfirmInventoryOutboundService;
import nexussMarket.domain.services.ConsultInventoryService;
import nexussMarket.domain.services.DeactivateWarehouseService;
import nexussMarket.domain.services.RegisterInventoryInboundService;
import nexussMarket.domain.services.RegisterInventoryReturnService;
import nexussMarket.domain.services.RegisterWarehouseService;
import nexussMarket.domain.services.ReleaseInventoryReservationService;
import nexussMarket.domain.services.ReportDamagedInventoryService;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.services.authorization.AuthorizeWarehouseOperationService;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;

/**
 * Use cases of the Warehouses and Inventory areas: warehouse registration and
 * deactivation, every stock movement, and inventory queries. See
 * {@link AuthorizationBeanConfiguration} for how the domain wiring is split.
 */
@Configuration
public class InventoryBeanConfiguration {

    @Bean
    public RegisterWarehouseUseCase registerWarehouseUseCase(WarehouseRepositoryPort warehouseRepositoryPort,
                                                             UserRepositoryPort userRepositoryPort) {
        return new RegisterWarehouseService(warehouseRepositoryPort, userRepositoryPort);
    }

    @Bean
    public DeactivateWarehouseUseCase deactivateWarehouseUseCase(
            WarehouseRepositoryPort warehouseRepositoryPort,
            AuthorizeWarehouseOperationService authorizeWarehouseOperationService) {
        return new DeactivateWarehouseService(warehouseRepositoryPort, authorizeWarehouseOperationService);
    }

    @Bean
    public RegisterInventoryInboundUseCase registerInventoryInboundUseCase(
            InventoryRepositoryPort inventoryRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            ProductRepositoryPort productRepositoryPort,
            WarehouseRepositoryPort warehouseRepositoryPort,
            AuthorizeWarehouseOperationService authorizeWarehouseOperationService) {
        return new RegisterInventoryInboundService(inventoryRepositoryPort, inventoryMovementRepositoryPort,
                productRepositoryPort, warehouseRepositoryPort, authorizeWarehouseOperationService);
    }

    @Bean
    public RegisterInventoryReturnUseCase registerInventoryReturnUseCase(
            InventoryRepositoryPort inventoryRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            AuthorizeWarehouseOperationService authorizeWarehouseOperationService) {
        return new RegisterInventoryReturnService(inventoryRepositoryPort, inventoryMovementRepositoryPort,
                authorizeWarehouseOperationService);
    }

    @Bean
    public AdjustInventoryUseCase adjustInventoryUseCase(
            InventoryRepositoryPort inventoryRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            AuthorizeWarehouseOperationService authorizeWarehouseOperationService) {
        return new AdjustInventoryService(inventoryRepositoryPort, inventoryMovementRepositoryPort,
                authorizeWarehouseOperationService);
    }

    @Bean
    public ReportDamagedInventoryUseCase reportDamagedInventoryUseCase(
            InventoryRepositoryPort inventoryRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            AuthorizeWarehouseOperationService authorizeWarehouseOperationService) {
        return new ReportDamagedInventoryService(inventoryRepositoryPort, inventoryMovementRepositoryPort,
                authorizeWarehouseOperationService);
    }

    @Bean
    public ReleaseInventoryReservationUseCase releaseInventoryReservationUseCase(
            InventoryRepositoryPort inventoryRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort) {
        return new ReleaseInventoryReservationService(inventoryRepositoryPort, inventoryMovementRepositoryPort);
    }

    @Bean
    public ConfirmInventoryOutboundUseCase confirmInventoryOutboundUseCase(
            InventoryRepositoryPort inventoryRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort) {
        return new ConfirmInventoryOutboundService(inventoryRepositoryPort, inventoryMovementRepositoryPort);
    }

    @Bean
    public ConsultInventoryUseCase consultInventoryUseCase(
            ProductRepositoryPort productRepositoryPort,
            InventoryRepositoryPort inventoryRepositoryPort,
            ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService,
            AuthorizeProductOwnershipService authorizeProductOwnershipService) {
        return new ConsultInventoryService(productRepositoryPort, inventoryRepositoryPort,
                validateUserAuthorizationStatusService, authorizeProductOwnershipService);
    }
}
