package nexussMarket.adapters.out.persistence.mongodb.mappers;

import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryItemDocument;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Warehouse;

/**
 * Converts {@link InventoryItem} to and from {@link InventoryItemDocument}. The
 * product and warehouse are stored as IDs and must be resolved by the caller.
 */
public final class InventoryItemMapper {

    private InventoryItemMapper() {
    }

    public static InventoryItemDocument toDocument(InventoryItem item) {
        String productId = item.getProduct().getIdentifier();
        String warehouseId = item.getWarehouse().getIdentifier();
        InventoryItemDocument document = new InventoryItemDocument();
        document.setId(InventoryItemDocument.idOf(productId, warehouseId));
        document.setProductId(productId);
        document.setWarehouseId(warehouseId);
        document.setAvailableQuantity(item.getAvailableQuantity());
        document.setReservedQuantity(item.getReservedQuantity());
        document.setDamagedQuantity(item.getDamagedQuantity());
        return document;
    }

    /** {@code product} and {@code warehouse} are the entities referenced by the document. */
    public static InventoryItem toDomain(InventoryItemDocument document, Product product, Warehouse warehouse) {
        return new InventoryItem(product, warehouse, document.getAvailableQuantity(), document.getReservedQuantity(),
                document.getDamagedQuantity());
    }
}
