package nexussMarket.adapters.out.persistence.mongodb.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
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

    /**
     * Optimistic-locking version ({@code @Version}): a save matches only the version it read and increments it.
     * Documents stored before versioning get {@code 0} from {@code VersionFieldMigration} at startup.
     */
    @Version
    private Long version;

    /** Identifier of the inventory document for {@code productId} held in {@code warehouseId}. */
    public static String idOf(String productId, String warehouseId) {
        return productId + ":" + warehouseId;
    }
}
