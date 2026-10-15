package nexussMarket.adapters.out.persistence.mongodb.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryItemDocument;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

class InventoryItemMapperTest {

    private final Product product = new Product("P-1", "Shirt", ProductType.PHYSICAL, ProductStatus.PUBLISHED);
    private final Warehouse warehouse = new Warehouse("WH-1", "North Warehouse", "1 Main Street",
            WarehouseOwnerType.MARKETPLACE);

    @Test
    void quantitiesSurviveRoundTrip() {
        InventoryItem item = new InventoryItem(product, warehouse, 7, 3, 2);

        InventoryItemDocument document = InventoryItemMapper.toDocument(item);
        InventoryItem rebuilt = InventoryItemMapper.toDomain(document, product, warehouse);

        assertEquals(InventoryItemDocument.idOf("P-1", "WH-1"), document.getId());
        assertEquals(7, rebuilt.getAvailableQuantity());
        assertEquals(3, rebuilt.getReservedQuantity());
        assertEquals(2, rebuilt.getDamagedQuantity());
        assertSame(product, rebuilt.getProduct());
        assertSame(warehouse, rebuilt.getWarehouse());
    }

    @Test
    void negativeStoredQuantityFails() {
        InventoryItemDocument document = new InventoryItemDocument();
        document.setAvailableQuantity(-1);

        assertThrows(IllegalArgumentException.class,
                () -> InventoryItemMapper.toDomain(document, product, warehouse));
    }
}
