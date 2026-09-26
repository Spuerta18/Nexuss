package nexussMarket.domain.services;

import java.util.List;

import nexussMarket.domain.exceptions.InsufficientStockException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;

/**
 * Internal service, not exposed as a use case. Injected as a constructor
 * dependency by services that need to reserve stock (e.g. ConfirmOrderService).
 */
public class ReserveInventoryService {

    private final InventoryRepositoryPort inventoryRepositoryPort;

    public ReserveInventoryService(InventoryRepositoryPort inventoryRepositoryPort) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
    }

    /** Reserves {@code quantity} units of {@code productId} from the first warehouse with enough stock. */
    public InventoryItem reserve(String productId, int quantity) {
        List<InventoryItem> items = inventoryRepositoryPort.findAllByProductId(productId);
        for (InventoryItem item : items) {
            if (item.getAvailableQuantity() >= quantity) {
                item.reserve(quantity);
                return inventoryRepositoryPort.save(item);
            }
        }
        throw new InsufficientStockException(
                "No warehouse has sufficient stock for product " + productId + " to reserve " + quantity + " units");
    }
}
