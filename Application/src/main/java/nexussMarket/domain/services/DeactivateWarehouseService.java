package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.DeactivateWarehouseUseCase;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;

public class DeactivateWarehouseService implements DeactivateWarehouseUseCase {

    private final WarehouseRepositoryPort warehouseRepositoryPort;

    public DeactivateWarehouseService(WarehouseRepositoryPort warehouseRepositoryPort) {
        this.warehouseRepositoryPort = warehouseRepositoryPort;
    }

    @Override
    public Warehouse execute(Command command) {
        Warehouse warehouse = warehouseRepositoryPort.findById(command.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("No warehouse found with id " + command.warehouseId()));
        warehouse.setActive(false);
        return warehouseRepositoryPort.save(warehouse);
    }
}
