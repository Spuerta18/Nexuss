package nexussMarket.adapters.out.persistence.mongodb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.WarehouseDocument;

/** Spring Data repository for the {@code warehouses} collection. */
public interface WarehouseMongoRepository extends MongoRepository<WarehouseDocument, String> {

    Page<WarehouseDocument> findByOwnerId(String ownerId, Pageable pageable);
}
