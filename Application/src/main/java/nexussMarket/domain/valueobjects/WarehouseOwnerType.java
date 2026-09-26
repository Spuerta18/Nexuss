package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Ownership classification of a {@link nexussMarket.domain.models.Warehouse}.
 */
public final class WarehouseOwnerType extends DomainCatalog {

    public static final WarehouseOwnerType MARKETPLACE =
            new WarehouseOwnerType("MARKETPLACE", "Marketplace", "The warehouse is owned and operated by the platform.");
    public static final WarehouseOwnerType SELLER =
            new WarehouseOwnerType("SELLER", "Seller", "The warehouse is owned and operated by a seller.");

    private WarehouseOwnerType(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code WarehouseOwnerType} value list. */
    public static List<WarehouseOwnerType> values() {
        return List.of(MARKETPLACE, SELLER);
    }
}