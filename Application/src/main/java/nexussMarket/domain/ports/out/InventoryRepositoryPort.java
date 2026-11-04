package nexussMarket.domain.ports.out;

import java.util.List;
import java.util.Optional;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.InventoryItem;

public interface InventoryRepositoryPort {

    /**
     * Stores the first inventory record of a product in a warehouse. Fails with {@link DuplicateResourceException}
     * if that record already exists; the stored one is never replaced.
     */
    InventoryItem insert(InventoryItem item);

    /**
     * Stores the changes of an existing inventory record. Fails with {@link ConcurrentUpdateException} if it was saved by
     * someone else since it was read (its version changed); nothing is stored then. The new version is set on the
     * argument.
     */
    InventoryItem save(InventoryItem item);

    Optional<InventoryItem> findByProductIdAndWarehouseId(String productId, String warehouseId);

    List<InventoryItem> findAllByProductId(String productId);
}
