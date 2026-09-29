package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryMovementDocument;

/** Spring Data repository for the append-only {@code inventory_movements} collection. */
public interface InventoryMovementMongoRepository extends MongoRepository<InventoryMovementDocument, String> {

    List<InventoryMovementDocument> findAllByProductIdAndWarehouseIdOrderByOccurredAtAsc(String productId,
                                                                                          String warehouseId);
}
