package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.PageQuery;

public interface WarehouseRepositoryPort {

    /**
     * Stores a new warehouse. Fails with {@link DuplicateResourceException} if a warehouse with the same
     * identifier already exists; the stored one is never replaced.
     */
    Warehouse insert(Warehouse warehouse);

    /** Stores the changes of an existing warehouse. */
    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findById(String identifier);

    /** A page of every warehouse, ordered by identifier. */
    ResultPage<Warehouse> findPage(PageQuery page);

    /** A page of the warehouses owned by seller {@code ownerId}, ordered by identifier. */
    ResultPage<Warehouse> findPageByOwnerId(String ownerId, PageQuery page);
}
