package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.mappers.WarehouseMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.WarehouseMongoRepository;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.valueobjects.PageQuery;

/** Implements {@link WarehouseRepositoryPort} on the {@code warehouses} collection. */
@Repository
@Profile("!memory")
public class MongoWarehouseRepositoryAdapter implements WarehouseRepositoryPort {

    private final WarehouseMongoRepository warehouseRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoWarehouseRepositoryAdapter(WarehouseMongoRepository warehouseRepository,
                                           MongoReferenceResolver referenceResolver) {
        this.warehouseRepository = warehouseRepository;
        this.referenceResolver = referenceResolver;
    }

    /** A real {@code insert}: an existing document with the same key is never replaced. */
    @Override
    public Warehouse insert(Warehouse warehouse) {
        MongoInsertion.insert(() -> warehouseRepository.insert(WarehouseMapper.toDocument(warehouse)),
                "A warehouse with id " + warehouse.getIdentifier() + " already exists");
        return warehouse;
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

    @Override
    public ResultPage<Warehouse> findPage(PageQuery page) {
        return MongoPages.toResultPage(warehouseRepository.findAll(MongoPages.pageable(page, Sort.by("_id"))), page,
                referenceResolver::toWarehouses);
    }

    @Override
    public ResultPage<Warehouse> findPageByOwnerId(String ownerId, PageQuery page) {
        return MongoPages.toResultPage(warehouseRepository.findByOwnerId(ownerId,
                MongoPages.pageable(page, Sort.by("_id"))), page, referenceResolver::toWarehouses);
    }
}
