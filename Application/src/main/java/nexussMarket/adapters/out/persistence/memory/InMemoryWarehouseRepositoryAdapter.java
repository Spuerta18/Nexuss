package nexussMarket.adapters.out.persistence.memory;

import java.util.Comparator;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.WarehouseDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.WarehouseMapper;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.valueobjects.PageQuery;

/** {@link WarehouseRepositoryPort} on the in-memory {@code warehouses} collection. */
@Repository
@Profile("memory")
public class InMemoryWarehouseRepositoryAdapter implements WarehouseRepositoryPort {

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryWarehouseRepositoryAdapter(InMemoryDatabase database, InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Warehouse insert(Warehouse warehouse) {
        database.warehouses.insert(WarehouseMapper.toDocument(warehouse),
                "A warehouse with id " + warehouse.getIdentifier() + " already exists");
        return warehouse;
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        database.warehouses.save(WarehouseMapper.toDocument(warehouse), "Warehouse " + warehouse.getIdentifier());
        return warehouse;
    }

    @Override
    public Optional<Warehouse> findById(String identifier) {
        return database.warehouses.findById(identifier).map(referenceResolver::toWarehouse);
    }

    @Override
    public ResultPage<Warehouse> findPage(PageQuery page) {
        return InMemoryPages.page(database.warehouses.findAll(), Comparator.comparing(WarehouseDocument::getId), page,
                referenceResolver::toWarehouses);
    }

    @Override
    public ResultPage<Warehouse> findPageByOwnerId(String ownerId, PageQuery page) {
        return InMemoryPages.page(database.warehouses.find(document -> ownerId.equals(document.getOwnerId())),
                Comparator.comparing(WarehouseDocument::getId), page, referenceResolver::toWarehouses);
    }
}
