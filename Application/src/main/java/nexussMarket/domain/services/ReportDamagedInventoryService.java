package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.ports.in.ReportDamagedInventoryUseCase;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.valueobjects.InventoryMovementType;

public class ReportDamagedInventoryService implements ReportDamagedInventoryUseCase {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    public ReportDamagedInventoryService(InventoryRepositoryPort inventoryRepositoryPort,
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
        item.markDamaged(command.quantity());
        InventoryItem saved = inventoryRepositoryPort.save(item);
        // Damaged units leave the sellable stock, so they are recorded as a negative ADJUSTMENT.
        inventoryMovementRepositoryPort.save(
                InventoryMovement.of(item, InventoryMovementType.ADJUSTMENT, -command.quantity()));
        return saved;
    }
}
