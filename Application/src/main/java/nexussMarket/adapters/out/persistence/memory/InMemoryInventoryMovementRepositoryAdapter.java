package nexussMarket.adapters.out.persistence.memory;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryMovementDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.InventoryMovementMapper;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;

/** {@link InventoryMovementRepositoryPort} on the in-memory {@code inventory_movements} collection (append-only). */
@Repository
@Profile("memory")
public class InMemoryInventoryMovementRepositoryAdapter implements InventoryMovementRepositoryPort {

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryInventoryMovementRepositoryAdapter(InMemoryDatabase database,
                                                      InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public InventoryMovement save(InventoryMovement movement) {
        InventoryMovementDocument document = InventoryMovementMapper.toDocument(movement);
        // MongoDB gives a movement an ObjectId when it is inserted; the in-memory store does the same with a UUID.
        if (document.getId() == null) {
            document.setId(UUID.randomUUID().toString());
        }
        database.inventoryMovements.insert(document, "This inventory movement is already recorded");
        return movement;
    }

    /** Movements of the given product in the given warehouse, oldest first. */
    @Override
    public List<InventoryMovement> findAllByProductIdAndWarehouseId(String productId, String warehouseId) {
        List<InventoryMovementDocument> documents = database.inventoryMovements.find(
                document -> productId.equals(document.getProductId()) && warehouseId.equals(document.getWarehouseId()));
        if (documents.isEmpty()) {
            return List.of();
        }
        Product product = referenceResolver.product(productId);
        Warehouse warehouse = referenceResolver.warehouse(warehouseId);
        return documents.stream()
                .sorted(Comparator.comparing(InventoryMovementDocument::getOccurredAt))
                .map(document -> InventoryMovementMapper.toDomain(document, product, warehouse))
                .toList();
    }
}
