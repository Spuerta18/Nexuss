package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InsufficientStockException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.ports.in.AdjustInventoryUseCase;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;

public class AdjustInventoryService implements AdjustInventoryUseCase {

    private final InventoryRepositoryPort inventoryRepositoryPort;

    public AdjustInventoryService(InventoryRepositoryPort inventoryRepositoryPort) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
    }

    @Override
    public InventoryItem execute(Command command) {
        InventoryItem item = inventoryRepositoryPort
                .findByProductIdAndWarehouseId(command.productId(), command.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No inventory item found for product " + command.productId() + " in warehouse " + command.warehouseId()));

        int adjustedQuantity = item.getAvailableQuantity() + command.quantityDelta();
        if (adjustedQuantity < 0) {
            throw new InsufficientStockException(
                    "Adjustment of " + command.quantityDelta() + " would leave negative stock for product "
                            + command.productId() + " in warehouse " + command.warehouseId());
        }
        item.setAvailableQuantity(adjustedQuantity);
        return inventoryRepositoryPort.save(item);
    }
}
