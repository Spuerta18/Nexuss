package nexussMarket.domain.ports.out;

import java.util.List;

import nexussMarket.domain.models.InventoryMovement;

public interface InventoryMovementRepositoryPort {

    InventoryMovement save(InventoryMovement movement);

    List<InventoryMovement> findAllByProductIdAndWarehouseId(String productId, String warehouseId);
}
