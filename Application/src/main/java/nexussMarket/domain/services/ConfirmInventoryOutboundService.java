package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InsufficientStockException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.ports.in.ConfirmInventoryOutboundUseCase;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;

public class ConfirmInventoryOutboundService implements ConfirmInventoryOutboundUseCase {

    private final InventoryRepositoryPort inventoryRepositoryPort;

    public ConfirmInventoryOutboundService(InventoryRepositoryPort inventoryRepositoryPort) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
    }

    @Override
    public InventoryItem execute(Command command) {
        InventoryItem item = inventoryRepositoryPort
                .findByProductIdAndWarehouseId(command.productId(), command.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No inventory item found for product " + command.productId() + " in warehouse " + command.warehouseId()));
        if (item.getReservedQuantity() < command.quantity()) {
            throw new InsufficientStockException(
                    "Cannot confirm outbound of " + command.quantity() + " units for product " + command.productId()
                            + ": only " + item.getReservedQuantity() + " reserved");
        }
        item.setReservedQuantity(item.getReservedQuantity() - command.quantity());
        return inventoryRepositoryPort.save(item);
    }
}
