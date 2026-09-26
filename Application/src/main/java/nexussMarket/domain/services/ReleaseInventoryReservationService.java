package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InsufficientStockException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.ports.in.ReleaseInventoryReservationUseCase;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;

public class ReleaseInventoryReservationService implements ReleaseInventoryReservationUseCase {

    private final InventoryRepositoryPort inventoryRepositoryPort;

    public ReleaseInventoryReservationService(InventoryRepositoryPort inventoryRepositoryPort) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
    }

    @Override
    public InventoryItem execute(Command command) {
        InventoryItem item = findItem(command.productId(), command.warehouseId());
        if (item.getReservedQuantity() < command.quantity()) {
            throw new InsufficientStockException(
                    "Cannot release " + command.quantity() + " units for product " + command.productId()
                            + ": only " + item.getReservedQuantity() + " reserved");
        }
        item.setReservedQuantity(item.getReservedQuantity() - command.quantity());
        item.setAvailableQuantity(item.getAvailableQuantity() + command.quantity());
        return inventoryRepositoryPort.save(item);
    }

    private InventoryItem findItem(String productId, String warehouseId) {
        return inventoryRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No inventory item found for product " + productId + " in warehouse " + warehouseId));
    }
}
