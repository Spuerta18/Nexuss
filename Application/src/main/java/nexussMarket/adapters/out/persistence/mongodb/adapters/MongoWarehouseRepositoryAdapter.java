package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.mappers.WarehouseMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.WarehouseMongoRepository;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;

/** Implements {@link WarehouseRepositoryPort} on the {@code warehouses} collection. */
@Repository
public class MongoWarehouseRepositoryAdapter implements WarehouseRepositoryPort {

    private final WarehouseMongoRepository warehouseRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoWarehouseRepositoryAdapter(WarehouseMongoRepository warehouseRepository,
                                           MongoReferenceResolver referenceResolver) {
        this.warehouseRepository = warehouseRepository;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        warehouseRepository.save(WarehouseMapper.toDocument(warehouse));
        return warehouse;
    }

    @Override
    public Optional<Warehouse> findById(String identifier) {
        return warehouseRepository.findById(identifier).map(referenceResolver::toWarehouse);
    }
}
