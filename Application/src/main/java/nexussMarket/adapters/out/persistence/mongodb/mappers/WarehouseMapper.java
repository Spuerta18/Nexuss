package nexussMarket.adapters.out.persistence.mongodb.mappers;

import nexussMarket.adapters.out.persistence.mongodb.documents.WarehouseDocument;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * Converts {@link Warehouse} to and from {@link WarehouseDocument}. The owner
 * is stored as {@code ownerId} and must be resolved by the caller.
 */
public final class WarehouseMapper {

    private WarehouseMapper() {
    }

    public static WarehouseDocument toDocument(Warehouse warehouse) {
        WarehouseDocument document = new WarehouseDocument();
        document.setId(warehouse.getIdentifier());
        document.setName(warehouse.getName());
        document.setAddress(warehouse.getAddress());
        document.setOwnerType(warehouse.getOwnerType().getCode());
        document.setOwnerId(warehouse.getOwner() != null ? warehouse.getOwner().getIdentifier() : null);
        document.setActive(warehouse.isActive());
        return document;
    }

    /** {@code owner} is the seller referenced by {@code ownerId}, or {@code null} for Marketplace warehouses. */
    public static Warehouse toDomain(WarehouseDocument document, Seller owner) {
        Warehouse warehouse = new Warehouse(document.getId(), document.getName(), document.getAddress(),
                CatalogCodes.resolve(WarehouseOwnerType::fromCode, document.getOwnerType(), "WarehouseOwnerType"));
        warehouse.setOwner(owner);
        warehouse.setActive(document.isActive());
        return warehouse;
    }
}
