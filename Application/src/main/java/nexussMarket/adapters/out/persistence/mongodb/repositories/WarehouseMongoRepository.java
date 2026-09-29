package nexussMarket.adapters.out.persistence.mongodb.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.WarehouseDocument;

/** Spring Data repository for the {@code warehouses} collection. */
public interface WarehouseMongoRepository extends MongoRepository<WarehouseDocument, String> {
}
