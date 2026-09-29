package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.DeactivateWarehouseUseCase;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeWarehouseOperationService;

public class DeactivateWarehouseService implements DeactivateWarehouseUseCase {

    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final AuthorizeWarehouseOperationService authorizeWarehouseOperationService;

    public DeactivateWarehouseService(WarehouseRepositoryPort warehouseRepositoryPort,
                                      AuthorizeWarehouseOperationService authorizeWarehouseOperationService) {
        this.warehouseRepositoryPort = warehouseRepositoryPort;
        this.authorizeWarehouseOperationService = authorizeWarehouseOperationService;
    }

    @Override
    public Warehouse execute(User actor, Command command) {
        Warehouse warehouse = warehouseRepositoryPort.findById(command.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("No warehouse found with id " + command.warehouseId()));
        authorizeWarehouseOperationService.execute(actor, warehouse);
        warehouse.setActive(false);
        return warehouseRepositoryPort.save(warehouse);
    }
}
