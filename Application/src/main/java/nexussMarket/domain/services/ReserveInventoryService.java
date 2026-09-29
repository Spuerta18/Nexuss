package nexussMarket.domain.services;

import java.util.List;

import nexussMarket.domain.exceptions.InsufficientStockException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.valueobjects.InventoryMovementType;

/**
 * Internal service, not exposed as a use case. Injected as a constructor
 * dependency by services that need to reserve stock (e.g. ConfirmOrderService).
 */
public class ReserveInventoryService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    public ReserveInventoryService(InventoryRepositoryPort inventoryRepositoryPort,
                                   InventoryMovementRepositoryPort inventoryMovementRepositoryPort) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.inventoryMovementRepositoryPort = inventoryMovementRepositoryPort;
    }

    /**
     * Reserves {@code quantity} units of {@code productId} from the first active
     * warehouse with enough available (non-damaged) stock.
     */
    public InventoryItem reserve(String productId, int quantity) {
        List<InventoryItem> items = inventoryRepositoryPort.findAllByProductId(productId);
        for (InventoryItem item : items) {
            if (item.getWarehouse().isActive() && item.getAvailableQuantity() >= quantity) {
                item.reserve(quantity);
                InventoryItem saved = inventoryRepositoryPort.save(item);
                inventoryMovementRepositoryPort.save(
                        InventoryMovement.of(item, InventoryMovementType.RESERVATION, quantity));
                return saved;
            }
        }
        throw new InsufficientStockException(
                "No active warehouse has sufficient stock for product " + productId + " to reserve " + quantity + " units");
    }
}
