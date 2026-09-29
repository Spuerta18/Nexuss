package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.ports.in.AdjustInventoryUseCase;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.valueobjects.InventoryMovementType;

public class AdjustInventoryService implements AdjustInventoryUseCase {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    public AdjustInventoryService(InventoryRepositoryPort inventoryRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.inventoryMovementRepositoryPort = inventoryMovementRepositoryPort;
    }

    @Override
    public InventoryItem execute(Command command) {
        InventoryItem item = inventoryRepositoryPort
                .findByProductIdAndWarehouseId(command.productId(), command.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No inventory item found for product " + command.productId() + " in warehouse " + command.warehouseId()));
        item.adjust(command.quantityDelta());
        InventoryItem saved = inventoryRepositoryPort.save(item);
        inventoryMovementRepositoryPort.save(
                InventoryMovement.of(item, InventoryMovementType.ADJUSTMENT, command.quantityDelta()));
        return saved;
    }
}
