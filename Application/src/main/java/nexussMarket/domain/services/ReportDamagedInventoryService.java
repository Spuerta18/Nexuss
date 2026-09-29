package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ReportDamagedInventoryUseCase;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeWarehouseOperationService;
import nexussMarket.domain.valueobjects.InventoryMovementType;

public class ReportDamagedInventoryService implements ReportDamagedInventoryUseCase {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final AuthorizeWarehouseOperationService authorizeWarehouseOperationService;

    public ReportDamagedInventoryService(InventoryRepositoryPort inventoryRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            AuthorizeWarehouseOperationService authorizeWarehouseOperationService) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.inventoryMovementRepositoryPort = inventoryMovementRepositoryPort;
        this.authorizeWarehouseOperationService = authorizeWarehouseOperationService;
    }

    @Override
    public InventoryItem execute(User actor, Command command) {
        InventoryItem item = inventoryRepositoryPort
                .findByProductIdAndWarehouseId(command.productId(), command.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No inventory item found for product " + command.productId() + " in warehouse " + command.warehouseId()));
        authorizeWarehouseOperationService.execute(actor, item.getWarehouse());
        item.markDamaged(command.quantity());
        InventoryItem saved = inventoryRepositoryPort.save(item);
        // Damaged units leave the sellable stock, so they are recorded as a negative ADJUSTMENT.
        inventoryMovementRepositoryPort.save(
                InventoryMovement.of(item, InventoryMovementType.ADJUSTMENT, -command.quantity()));
        return saved;
    }
}
