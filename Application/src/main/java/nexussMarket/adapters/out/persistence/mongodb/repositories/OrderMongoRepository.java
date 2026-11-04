package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import nexussMarket.adapters.out.persistence.mongodb.documents.OrderDocument;

/** Spring Data repository for the {@code orders} collection. */
public interface OrderMongoRepository extends MongoRepository<OrderDocument, String> {

    List<OrderDocument> findAllByBuyerIdOrderByCreatedAtDesc(String buyerId);

    /** Created from {@code from} (included) to {@code to} (excluded): one condition on {@code createdAt}. */
    @Query("{ 'createdAt': { '$gte': ?0, '$lt': ?1 } }")
    List<OrderDocument> findCreatedBetween(LocalDateTime from, LocalDateTime to);

    boolean existsByLinesProductIdAndStatusIn(String productId, Collection<String> statuses);

    Page<OrderDocument> findByLinesProductIdIn(Collection<String> productIds, Pageable pageable);
}
