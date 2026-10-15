package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeWarehouseOperationService;

@Service
public class DeactivateWarehouseService {

    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final AuthorizeWarehouseOperationService authorizeWarehouseOperationService;

    public DeactivateWarehouseService(WarehouseRepositoryPort warehouseRepositoryPort,
                                      AuthorizeWarehouseOperationService authorizeWarehouseOperationService) {
        this.warehouseRepositoryPort = warehouseRepositoryPort;
        this.authorizeWarehouseOperationService = authorizeWarehouseOperationService;
    }

    public Warehouse execute(User actor, Warehouse warehouseReference) {
        String warehouseId = warehouseReference.getIdentifier();
        Warehouse warehouse = warehouseRepositoryPort.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("No warehouse found with id " + warehouseId));
        authorizeWarehouseOperationService.execute(actor, warehouse);
        warehouse.setActive(false);
        return warehouseRepositoryPort.save(warehouse);
    }
}
