package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryItemDocument;

/** Spring Data repository for the {@code inventory_items} collection. */
public interface InventoryItemMongoRepository extends MongoRepository<InventoryItemDocument, String> {

    List<InventoryItemDocument> findAllByProductId(String productId);
}
