package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryItemDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.InventoryItemMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.InventoryItemMongoRepository;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.valueobjects.PageQuery;

/** Implements {@link InventoryRepositoryPort} on the {@code inventory_items} collection. */
@Repository
@Profile("!memory")
public class MongoInventoryRepositoryAdapter implements InventoryRepositoryPort {

    private final InventoryItemMongoRepository inventoryItemRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoInventoryRepositoryAdapter(InventoryItemMongoRepository inventoryItemRepository,
                                           MongoReferenceResolver referenceResolver) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.referenceResolver = referenceResolver;
    }

    /** A real {@code insert}: the record of a product in a warehouse is created once and never replaced. */
    @Override
    public InventoryItem insert(InventoryItem item) {
        MongoInsertion.insert(() -> inventoryItemRepository.insert(InventoryItemMapper.toDocument(item)),
                "An inventory item for product " + item.getProduct().getIdentifier() + " in warehouse "
                        + item.getWarehouse().getIdentifier() + " already exists");
        return item;
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        item.restoreVersion(MongoVersioning.save(() -> inventoryItemRepository.save(InventoryItemMapper.toDocument(item)),
                "Inventory item " + item.getIdentifier()).getVersion());
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

    @Override
    public ResultPage<InventoryItem> findPage(PageQuery page) {
        return MongoPages.toResultPage(inventoryItemRepository.findAll(MongoPages.pageable(page, Sort.by("_id"))),
                page, this::toDomain);
    }

    @Override
    public ResultPage<InventoryItem> findPageByProductIds(Collection<String> productIds, PageQuery page) {
        if (productIds.isEmpty()) {
            return new ResultPage<>(List.of(), page.page(), page.size(), 0);
        }
        return MongoPages.toResultPage(inventoryItemRepository.findByProductIdIn(productIds,
                MongoPages.pageable(page, Sort.by("_id"))), page, this::toDomain);
    }

    /** Builds {@code documents} into items, resolving their products and warehouses with one query each. */
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
        return toDomain(inventoryItemRepository.findAll(Sort.by("_id")));
    }
}
