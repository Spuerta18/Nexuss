package nexussMarket.domain.ports.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Shipment;

public interface ShipmentRepositoryPort {

    /**
     * Stores a new shipment. Fails with {@link DuplicateResourceException} if a shipment with the same
     * identifier, or one for the same order, already exists; the stored one is never replaced.
     */
    Shipment insert(Shipment shipment);

    /**
     * Stores the changes of an existing shipment. Fails with {@link ConcurrentUpdateException} if it was saved by
     * someone else since it was read (its version changed); nothing is stored then. The new version is set on the
     * argument.
     */
    Shipment save(Shipment shipment);

    Optional<Shipment> findById(String identifier);

    Optional<Shipment> findByOrderId(String orderId);

    /** Every shipment created from {@code from} (included) to {@code to} (excluded). */
    List<Shipment> findAllCreatedBetween(LocalDateTime from, LocalDateTime to);
}
