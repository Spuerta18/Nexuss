package nexussMarket.adapters.out.persistence.mongodb.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A {@code Warehouse}. The owning seller is referenced by ID and is absent
 * for Marketplace-owned warehouses.
 */
@Document(collection = "warehouses")
@Getter
@Setter
@NoArgsConstructor
public class WarehouseDocument {

    @Id
    private String id;

    private String name;
    private String address;
    private String ownerType;
    private String ownerId;
    private boolean active;
}
