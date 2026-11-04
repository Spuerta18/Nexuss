package nexussMarket.domain.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InactiveWarehouseException;
import nexussMarket.domain.exceptions.InventoryNotTrackedException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeWarehouseOperationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.InventoryMovementType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.ProductType;

/**
 * Registers inbound stock. Creates the {@link InventoryItem} the first time a
 * product is stocked in a warehouse; only {@code PHYSICAL} products can be
 * stocked.
 */
@Service
public class RegisterInventoryInboundService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final AuthorizeWarehouseOperationService authorizeWarehouseOperationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public RegisterInventoryInboundService(InventoryRepositoryPort inventoryRepositoryPort,
                                           InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
                                           ProductRepositoryPort productRepositoryPort,
                                           WarehouseRepositoryPort warehouseRepositoryPort,
                                           AuthorizeWarehouseOperationService authorizeWarehouseOperationService,
                                           RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.inventoryMovementRepositoryPort = inventoryMovementRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
        this.warehouseRepositoryPort = warehouseRepositoryPort;
        this.authorizeWarehouseOperationService = authorizeWarehouseOperationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public InventoryItem execute(User actor, Product product, Warehouse warehouse, int quantity) {
        String productId = product.getIdentifier();
        String warehouseId = warehouse.getIdentifier();
        int[] previousAvailable = new int[1];
        InventoryItem item = OptimisticRetry.run(() -> receiveOnce(actor, productId, warehouseId, quantity,
                previousAvailable));
        InventoryItem saved = item;
        inventoryMovementRepositoryPort.save(
                InventoryMovement.of(item, InventoryMovementType.INBOUND, quantity));
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.INVENTORY_INBOUND, actor, AffectedEntityType.INVENTORY_ITEM, item.getIdentifier()),
                AuditDetails.of("productId", productId, "warehouseId", warehouseId,
                        "quantity", quantity, "previousAvailableQuantity", previousAvailable[0],
                        "newAvailableQuantity", item.getAvailableQuantity()));
        return saved;
    }

    /**
     * One read-change-save attempt. The first inbound of a product into a
     * warehouse inserts its item; if a concurrent inbound inserted it first,
     * the attempt is retried and updates the stored one instead.
     */
    private InventoryItem receiveOnce(User actor, String productId, String warehouseId, int quantity,
                                      int[] previousAvailable) {
        Optional<InventoryItem> stored = inventoryRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId);
        InventoryItem item = stored.orElseGet(() -> newInventoryItem(productId, warehouseId));
        authorizeWarehouseOperationService.execute(actor, item.getWarehouse());
        if (!item.getWarehouse().isActive()) {
            throw new InactiveWarehouseException(
                    "Warehouse " + warehouseId + " is inactive and cannot receive stock");
        }
        previousAvailable[0] = item.getAvailableQuantity();
        item.receive(quantity);
        if (stored.isPresent()) {
            return inventoryRepositoryPort.save(item);
        }
        try {
            return inventoryRepositoryPort.insert(item);
        } catch (DuplicateResourceException e) {
            throw new ConcurrentUpdateException("Inventory item " + item.getIdentifier() + " was created concurrently");
        }
    }

    private InventoryItem newInventoryItem(String productId, String warehouseId) {
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + productId));
        // Checked only when the item is created: an existing item already passed this check.
        if (product.getProductType() != ProductType.PHYSICAL) {
            throw new InventoryNotTrackedException("DIGITAL products neither require nor allow inventory tracking.");
        }
        Warehouse warehouse = warehouseRepositoryPort.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("No warehouse found with id " + warehouseId));
        return new InventoryItem(product, warehouse, 0, 0, 0);
    }
}
