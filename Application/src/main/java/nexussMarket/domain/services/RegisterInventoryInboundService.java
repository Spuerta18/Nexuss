package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.RegisterInventoryInboundUseCase;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.valueobjects.InventoryMovementType;

/**
 * Registers inbound stock. Creates the {@link InventoryItem} the first time a
 * product is stocked in a warehouse.
 */
public class RegisterInventoryInboundService implements RegisterInventoryInboundUseCase {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;

    public RegisterInventoryInboundService(InventoryRepositoryPort inventoryRepositoryPort,
                                           InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
                                           ProductRepositoryPort productRepositoryPort,
                                           WarehouseRepositoryPort warehouseRepositoryPort) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.inventoryMovementRepositoryPort = inventoryMovementRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
        this.warehouseRepositoryPort = warehouseRepositoryPort;
    }

    @Override
    public InventoryItem execute(Command command) {
        InventoryItem item = inventoryRepositoryPort
                .findByProductIdAndWarehouseId(command.productId(), command.warehouseId())
                .orElseGet(() -> newInventoryItem(command));
        if (!item.getWarehouse().isActive()) {
            throw new OperationNotAllowedException(
                    "Warehouse " + command.warehouseId() + " is inactive and cannot receive stock");
        }
        item.receive(command.quantity());
        InventoryItem saved = inventoryRepositoryPort.save(item);
        inventoryMovementRepositoryPort.save(
                InventoryMovement.of(item, InventoryMovementType.INBOUND, command.quantity()));
        return saved;
    }

    private InventoryItem newInventoryItem(Command command) {
        Product product = productRepositoryPort.findById(command.productId())
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + command.productId()));
        Warehouse warehouse = warehouseRepositoryPort.findById(command.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("No warehouse found with id " + command.warehouseId()));
        return new InventoryItem(product, warehouse, 0, 0, 0);
    }
}
