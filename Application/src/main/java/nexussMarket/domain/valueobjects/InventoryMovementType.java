package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Type of stock movement applied to an
 * {@link nexussMarket.domain.models.InventoryItem}.
 */
public final class InventoryMovementType extends DomainCatalog {

    public static final InventoryMovementType INBOUND =
            new InventoryMovementType("INBOUND", "Inbound", "Stock added to the warehouse.");
    public static final InventoryMovementType RESERVATION =
            new InventoryMovementType("RESERVATION", "Reservation", "Stock reserved for a pending order.");
    public static final InventoryMovementType SALE_OUTBOUND =
            new InventoryMovementType("SALE_OUTBOUND", "Sale Outbound", "Stock removed due to a completed sale.");
    public static final InventoryMovementType ADJUSTMENT =
            new InventoryMovementType("ADJUSTMENT", "Adjustment", "Manual correction of recorded stock.");
    public static final InventoryMovementType RETURN =
            new InventoryMovementType("RETURN", "Return", "Stock added back due to a returned product.");

    private InventoryMovementType(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code InventoryMovementType} value list. */
    public static List<InventoryMovementType> values() {
        return List.of(INBOUND, RESERVATION, SALE_OUTBOUND, ADJUSTMENT, RETURN);
    }
}