package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.ShipmentDocument;

/** Spring Data repository for the {@code shipments} collection. */
public interface ShipmentMongoRepository extends MongoRepository<ShipmentDocument, String> {

    Optional<ShipmentDocument> findByOrderId(String orderId);
}
