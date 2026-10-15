package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.valueobjects.InventoryMovementType;

@Service
public class ReleaseInventoryReservationService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    public ReleaseInventoryReservationService(InventoryRepositoryPort inventoryRepositoryPort,
                                              InventoryMovementRepositoryPort inventoryMovementRepositoryPort) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.inventoryMovementRepositoryPort = inventoryMovementRepositoryPort;
    }

    public InventoryItem execute(Product product, Warehouse warehouse, int quantity) {
        String productId = product.getIdentifier();
        String warehouseId = warehouse.getIdentifier();
        InventoryItem item = inventoryRepositoryPort
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No inventory item found for product " + productId + " in warehouse " + warehouseId));
        item.releaseReservation(quantity);
        InventoryItem saved = inventoryRepositoryPort.save(item);
        // A released reservation is recorded as a negative RESERVATION movement.
        inventoryMovementRepositoryPort.save(
                InventoryMovement.of(item, InventoryMovementType.RESERVATION, -quantity));
        return saved;
    }
}
