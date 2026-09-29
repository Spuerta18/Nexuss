package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.models.Shipment;

public interface ShipmentRepositoryPort {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(String identifier);

    Optional<Shipment> findByOrderId(String orderId);
}
