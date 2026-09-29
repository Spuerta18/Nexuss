package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.RefundDocument;

/** Spring Data repository for the {@code refunds} collection. */
public interface RefundMongoRepository extends MongoRepository<RefundDocument, String> {

    Optional<RefundDocument> findByReturnRequestId(String returnRequestId);
}
