package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Shipment;

public interface ShipmentRepositoryPort {

    /**
     * Stores a new shipment. Fails with {@link DuplicateResourceException} if a shipment with the same
     * identifier, or one for the same order, already exists; the stored one is never replaced.
     */
    Shipment insert(Shipment shipment);

    /** Stores the changes of an existing shipment. */
    Shipment save(Shipment shipment);

    Optional<Shipment> findById(String identifier);

    Optional<Shipment> findByOrderId(String orderId);
}
