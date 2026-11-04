package nexussMarket.adapters.out.persistence.memory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.ShipmentDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.ShipmentMapper;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;

/** {@link ShipmentRepositoryPort} on the in-memory {@code shipments} collection (one per order, versioned). */
@Repository
@Profile("memory")
public class InMemoryShipmentRepositoryAdapter implements ShipmentRepositoryPort {

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryShipmentRepositoryAdapter(InMemoryDatabase database, InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Shipment insert(Shipment shipment) {
        database.shipments.insert(ShipmentMapper.toDocument(shipment), "A shipment with id "
                + shipment.getIdentifier() + ", or one for order " + shipment.getOrder().getIdentifier()
                + ", already exists");
        return shipment;
    }

    @Override
    public Shipment save(Shipment shipment) {
        ShipmentDocument saved = database.shipments.save(ShipmentMapper.toDocument(shipment),
                "Shipment " + shipment.getIdentifier());
        shipment.restoreVersion(saved.getVersion());
        return shipment;
    }

    @Override
    public Optional<Shipment> findById(String identifier) {
        return database.shipments.findById(identifier).map(this::toDomain);
    }

    @Override
    public Optional<Shipment> findByOrderId(String orderId) {
        return database.shipments.find(document -> Objects.equals(orderId, document.getOrderId())).stream()
                .findFirst().map(this::toDomain);
    }

    private Shipment toDomain(ShipmentDocument document) {
        return ShipmentMapper.toDomain(document, referenceResolver.order(document.getOrderId()));
    }

    @Override
    public List<Shipment> findAllCreatedBetween(LocalDateTime from, LocalDateTime to) {
        return database.shipments.find(document -> document.getCreatedAt() != null
                        && !document.getCreatedAt().isBefore(from) && document.getCreatedAt().isBefore(to))
                .stream().map(this::toDomain).toList();
    }
}
