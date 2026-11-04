package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import nexussMarket.adapters.out.persistence.mongodb.documents.ShipmentDocument;

/** Spring Data repository for the {@code shipments} collection. */
public interface ShipmentMongoRepository extends MongoRepository<ShipmentDocument, String> {

    Optional<ShipmentDocument> findByOrderId(String orderId);

    /** Created from {@code from} (included) to {@code to} (excluded): one condition on {@code createdAt}. */
    @Query("{ 'createdAt': { '$gte': ?0, '$lt': ?1 } }")
    List<ShipmentDocument> findCreatedBetween(LocalDateTime from, LocalDateTime to);
}
