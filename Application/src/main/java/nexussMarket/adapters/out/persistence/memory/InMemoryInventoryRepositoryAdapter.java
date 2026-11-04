package nexussMarket.adapters.out.persistence.memory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryItemDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.InventoryItemMapper;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.valueobjects.PageQuery;

/** {@link InventoryRepositoryPort} on the in-memory {@code inventory_items} collection (versioned). */
@Repository
@Profile("memory")
public class InMemoryInventoryRepositoryAdapter implements InventoryRepositoryPort {

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryInventoryRepositoryAdapter(InMemoryDatabase database, InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public InventoryItem insert(InventoryItem item) {
        database.inventoryItems.insert(InventoryItemMapper.toDocument(item),
                "An inventory item for product " + item.getProduct().getIdentifier() + " in warehouse "
                        + item.getWarehouse().getIdentifier() + " already exists");
        return item;
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        InventoryItemDocument saved = database.inventoryItems.save(InventoryItemMapper.toDocument(item),
                "Inventory item " + item.getIdentifier());
        item.restoreVersion(saved.getVersion());
        return item;
    }

    @Override
    public Optional<InventoryItem> findByProductIdAndWarehouseId(String productId, String warehouseId) {
        return database.inventoryItems.findById(InventoryItemDocument.idOf(productId, warehouseId))
                .map(document -> InventoryItemMapper.toDomain(document, referenceResolver.product(productId),
                        referenceResolver.warehouse(warehouseId)));
    }

    @Override
    public List<InventoryItem> findAllByProductId(String productId) {
        List<InventoryItemDocument> documents =
                database.inventoryItems.find(document -> productId.equals(document.getProductId()));
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

    @Override
    public ResultPage<InventoryItem> findPage(PageQuery page) {
        return InMemoryPages.page(database.inventoryItems.findAll(), Comparator.comparing(InventoryItemDocument::getId),
                page, this::toDomain);
    }

    @Override
    public ResultPage<InventoryItem> findPageByProductIds(Collection<String> productIds, PageQuery page) {
        return InMemoryPages.page(database.inventoryItems.find(document -> productIds.contains(document.getProductId())),
                Comparator.comparing(InventoryItemDocument::getId), page, this::toDomain);
    }

    private List<InventoryItem> toDomain(List<InventoryItemDocument> documents) {
        Map<String, Product> products = referenceResolver.products(
                documents.stream().map(InventoryItemDocument::getProductId).toList());
        Map<String, Warehouse> warehouses = referenceResolver.warehouses(
                documents.stream().map(InventoryItemDocument::getWarehouseId).toList());
        return documents.stream()
                .map(document -> InventoryItemMapper.toDomain(document, products.get(document.getProductId()),
                        warehouses.get(document.getWarehouseId())))
                .toList();
    }

    @Override
    public List<InventoryItem> findAll() {
        return toDomain(database.inventoryItems.findAll().stream()
                .sorted(Comparator.comparing(InventoryItemDocument::getId)).toList());
    }
}
