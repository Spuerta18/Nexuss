package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.ReturnRequestDocument;

/** Spring Data repository for the {@code return_requests} collection. */
public interface ReturnRequestMongoRepository extends MongoRepository<ReturnRequestDocument, String> {

    Optional<ReturnRequestDocument> findByOrderId(String orderId);
}
