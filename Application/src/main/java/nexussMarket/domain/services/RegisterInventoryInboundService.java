package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.ports.in.RegisterInventoryInboundUseCase;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;

public class RegisterInventoryInboundService implements RegisterInventoryInboundUseCase {

    private final InventoryRepositoryPort inventoryRepositoryPort;

    public RegisterInventoryInboundService(InventoryRepositoryPort inventoryRepositoryPort) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
    }

    @Override
    public InventoryItem execute(Command command) {
        InventoryItem item = inventoryRepositoryPort
                .findByProductIdAndWarehouseId(command.productId(), command.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No inventory item found for product " + command.productId() + " in warehouse " + command.warehouseId()));
        item.setAvailableQuantity(item.getAvailableQuantity() + command.quantity());
        return inventoryRepositoryPort.save(item);
    }
}
