package nexussMarket.domain.ports.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.valueobjects.PageQuery;

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

    /** A page of every inventory record, ordered by identifier ({@code productId:warehouseId}). */
    ResultPage<InventoryItem> findPage(PageQuery page);

    /** A page of the inventory records of the given products, ordered by identifier. */
    ResultPage<InventoryItem> findPageByProductIds(Collection<String> productIds, PageQuery page);
}
