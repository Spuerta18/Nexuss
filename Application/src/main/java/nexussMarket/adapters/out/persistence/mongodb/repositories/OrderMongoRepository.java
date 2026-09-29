package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.OrderDocument;

/** Spring Data repository for the {@code orders} collection. */
public interface OrderMongoRepository extends MongoRepository<OrderDocument, String> {

    List<OrderDocument> findAllByBuyerIdOrderByCreatedAtDesc(String buyerId);
}
