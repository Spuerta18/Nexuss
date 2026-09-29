package nexussMarket.adapters.out.persistence.mongodb.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stock of one product in one warehouse. The {@code id} is derived from both
 * references (see {@link #idOf(String, String)}), so there is exactly one
 * document per product and warehouse.
 */
@Document(collection = "inventory_items")
@CompoundIndex(name = "product_warehouse", def = "{'productId': 1, 'warehouseId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
public class InventoryItemDocument {

    @Id
    private String id;

    @Indexed
    private String productId;

    private String warehouseId;
    private int availableQuantity;
    private int reservedQuantity;
    private int damagedQuantity;

    /** Identifier of the inventory document for {@code productId} held in {@code warehouseId}. */
    public static String idOf(String productId, String warehouseId) {
        return productId + ":" + warehouseId;
    }
}
