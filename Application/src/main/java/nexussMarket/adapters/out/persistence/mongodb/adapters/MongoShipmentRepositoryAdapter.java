package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.ShipmentDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.ShipmentMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ShipmentMongoRepository;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;

/** Implements {@link ShipmentRepositoryPort} on the {@code shipments} collection. */
@Repository
public class MongoShipmentRepositoryAdapter implements ShipmentRepositoryPort {

    private final ShipmentMongoRepository shipmentRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoShipmentRepositoryAdapter(ShipmentMongoRepository shipmentRepository,
                                          MongoReferenceResolver referenceResolver) {
        this.shipmentRepository = shipmentRepository;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Shipment save(Shipment shipment) {
        shipmentRepository.save(ShipmentMapper.toDocument(shipment));
        return shipment;
    }

    @Override
    public Optional<Shipment> findById(String identifier) {
        return shipmentRepository.findById(identifier).map(this::toDomain);
    }

    @Override
    public Optional<Shipment> findByOrderId(String orderId) {
        return shipmentRepository.findByOrderId(orderId).map(this::toDomain);
    }

    private Shipment toDomain(ShipmentDocument document) {
        return ShipmentMapper.toDomain(document, referenceResolver.order(document.getOrderId()));
    }
}
