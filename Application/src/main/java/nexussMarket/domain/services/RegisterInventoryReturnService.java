package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.ports.in.RegisterInventoryReturnUseCase;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;

public class RegisterInventoryReturnService implements RegisterInventoryReturnUseCase {

    private final InventoryRepositoryPort inventoryRepositoryPort;

    public RegisterInventoryReturnService(InventoryRepositoryPort inventoryRepositoryPort) {
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
