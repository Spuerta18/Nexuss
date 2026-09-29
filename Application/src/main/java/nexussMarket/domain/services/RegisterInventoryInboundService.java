package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.BusinessRuleViolationException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.RegisterInventoryInboundUseCase;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeWarehouseOperationService;
import nexussMarket.domain.valueobjects.InventoryMovementType;
import nexussMarket.domain.valueobjects.ProductType;

/**
 * Registers inbound stock. Creates the {@link InventoryItem} the first time a
 * product is stocked in a warehouse; only {@code PHYSICAL} products can be
 * stocked.
 */
public class RegisterInventoryInboundService implements RegisterInventoryInboundUseCase {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final AuthorizeWarehouseOperationService authorizeWarehouseOperationService;

    public RegisterInventoryInboundService(InventoryRepositoryPort inventoryRepositoryPort,
                                           InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
                                           ProductRepositoryPort productRepositoryPort,
                                           WarehouseRepositoryPort warehouseRepositoryPort,
                                           AuthorizeWarehouseOperationService authorizeWarehouseOperationService) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.inventoryMovementRepositoryPort = inventoryMovementRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
        this.warehouseRepositoryPort = warehouseRepositoryPort;
        this.authorizeWarehouseOperationService = authorizeWarehouseOperationService;
    }

    @Override
    public InventoryItem execute(User actor, Command command) {
        InventoryItem item = inventoryRepositoryPort
                .findByProductIdAndWarehouseId(command.productId(), command.warehouseId())
                .orElseGet(() -> newInventoryItem(command));
        authorizeWarehouseOperationService.execute(actor, item.getWarehouse());
        if (!item.getWarehouse().isActive()) {
            throw new BusinessRuleViolationException(
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
        // Checked only when the item is created: an existing item already passed this check.
        if (product.getProductType() != ProductType.PHYSICAL) {
            throw new BusinessRuleViolationException("DIGITAL products neither require nor allow inventory tracking.");
        }
        Warehouse warehouse = warehouseRepositoryPort.findById(command.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("No warehouse found with id " + command.warehouseId()));
        return new InventoryItem(product, warehouse, 0, 0, 0);
    }
}
