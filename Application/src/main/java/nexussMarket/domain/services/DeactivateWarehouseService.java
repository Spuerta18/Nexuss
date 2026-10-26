package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeWarehouseOperationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;

@Service
public class DeactivateWarehouseService {

    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final AuthorizeWarehouseOperationService authorizeWarehouseOperationService;

    public DeactivateWarehouseService(WarehouseRepositoryPort warehouseRepositoryPort,
                                      AuthorizeWarehouseOperationService authorizeWarehouseOperationService,
                                      RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.warehouseRepositoryPort = warehouseRepositoryPort;
        this.authorizeWarehouseOperationService = authorizeWarehouseOperationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Warehouse execute(User actor, Warehouse warehouseReference) {
        String warehouseId = warehouseReference.getIdentifier();
        Warehouse warehouse = warehouseRepositoryPort.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("No warehouse found with id " + warehouseId));
        authorizeWarehouseOperationService.execute(actor, warehouse);
        boolean previousActive = warehouse.isActive();
        warehouse.deactivate();
        Warehouse saved = warehouseRepositoryPort.save(warehouse);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.WAREHOUSE_DEACTIVATION, actor, AffectedEntityType.WAREHOUSE, warehouseId),
                AuditDetails.of("previousActive", previousActive, "newActive", warehouse.isActive()));
        return saved;
    }
}
