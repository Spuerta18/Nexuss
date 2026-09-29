package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryItemDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.InventoryItemMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.InventoryItemMongoRepository;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;

/** Implements {@link InventoryRepositoryPort} on the {@code inventory_items} collection. */
@Repository
public class MongoInventoryRepositoryAdapter implements InventoryRepositoryPort {

    private final InventoryItemMongoRepository inventoryItemRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoInventoryRepositoryAdapter(InventoryItemMongoRepository inventoryItemRepository,
                                           MongoReferenceResolver referenceResolver) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        inventoryItemRepository.save(InventoryItemMapper.toDocument(item));
        return item;
    }

    @Override
    public Optional<InventoryItem> findByProductIdAndWarehouseId(String productId, String warehouseId) {
        return inventoryItemRepository.findById(InventoryItemDocument.idOf(productId, warehouseId))
                .map(document -> InventoryItemMapper.toDomain(document, referenceResolver.product(productId),
                        referenceResolver.warehouse(warehouseId)));
    }

    @Override
    public List<InventoryItem> findAllByProductId(String productId) {
        List<InventoryItemDocument> documents = inventoryItemRepository.findAllByProductId(productId);
        if (documents.isEmpty()) {
            return List.of();
        }
        Product product = referenceResolver.product(productId);
        Map<String, Warehouse> warehouses = referenceResolver.warehouses(
                documents.stream().map(InventoryItemDocument::getWarehouseId).toList());
        return documents.stream()
                .map(document -> InventoryItemMapper.toDomain(document, product,
                        warehouses.get(document.getWarehouseId())))
                .toList();
    }
}
