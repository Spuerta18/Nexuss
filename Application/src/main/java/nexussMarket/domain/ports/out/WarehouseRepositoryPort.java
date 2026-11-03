package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Warehouse;

public interface WarehouseRepositoryPort {

    /**
     * Stores a new warehouse. Fails with {@link DuplicateResourceException} if a warehouse with the same
     * identifier already exists; the stored one is never replaced.
     */
    Warehouse insert(Warehouse warehouse);

    /** Stores the changes of an existing warehouse. */
    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findById(String identifier);
}
