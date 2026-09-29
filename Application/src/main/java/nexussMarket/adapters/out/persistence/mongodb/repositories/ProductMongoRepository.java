package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.ProductDocument;

/** Spring Data repository for the {@code products} collection. */
public interface ProductMongoRepository extends MongoRepository<ProductDocument, String> {

    List<ProductDocument> findBySellerId(String sellerId);

    List<ProductDocument> findByStatus(String status);
}
