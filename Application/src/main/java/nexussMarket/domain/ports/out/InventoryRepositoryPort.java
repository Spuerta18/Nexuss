package nexussMarket.domain.ports.out;

import java.util.List;
import java.util.Optional;

import nexussMarket.domain.models.InventoryItem;

public interface InventoryRepositoryPort {

    InventoryItem save(InventoryItem item);

    Optional<InventoryItem> findByProductIdAndWarehouseId(String productId, String warehouseId);

    List<InventoryItem> findAllByProductId(String productId);
}
