package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.List;

import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryMovementDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.InventoryMovementMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.InventoryMovementMongoRepository;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;

/**
 * Implements {@link InventoryMovementRepositoryPort} on the append-only
 * {@code inventory_movements} collection: movements are only ever inserted.
 */
@Repository
public class MongoInventoryMovementRepositoryAdapter implements InventoryMovementRepositoryPort {

    private final InventoryMovementMongoRepository inventoryMovementRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoInventoryMovementRepositoryAdapter(InventoryMovementMongoRepository inventoryMovementRepository,
                                                   MongoReferenceResolver referenceResolver) {
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public InventoryMovement save(InventoryMovement movement) {
        inventoryMovementRepository.insert(InventoryMovementMapper.toDocument(movement));
        return movement;
    }

    /** Movements of the given product in the given warehouse, oldest first. */
    @Override
    public List<InventoryMovement> findAllByProductIdAndWarehouseId(String productId, String warehouseId) {
        List<InventoryMovementDocument> documents = inventoryMovementRepository
                .findAllByProductIdAndWarehouseIdOrderByOccurredAtAsc(productId, warehouseId);
        if (documents.isEmpty()) {
            return List.of();
        }
        Product product = referenceResolver.product(productId);
        Warehouse warehouse = referenceResolver.warehouse(warehouseId);
        return documents.stream()
                .map(document -> InventoryMovementMapper.toDomain(document, product, warehouse))
                .toList();
    }
}
